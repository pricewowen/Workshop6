// Contributor(s): Robbie
// Main: Robbie - STOMP WebSocket client for live chat typing and messages.

package com.example.workshop6.data.ws;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Minimal STOMP over OkHttp WebSocket for chat message and typing frames with reconnect backoff.
 */
public class StompClient {

    public interface FrameListener {
        void onMessage(String body);
    }

    public interface ConnectionListener {
        void onConnected();
        void onDisconnected(Throwable cause);
    }

    private static final String NULL_TERM = "\u0000";
    private static final long HEARTBEAT_INTERVAL_MS = 10_000L;
    private static final long INBOUND_TIMEOUT_MS = 25_000L;
    private static final long[] BACKOFF_MS = { 1_000L, 2_000L, 4_000L, 8_000L, 15_000L };
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    private final String wsUrl;
    private final String host;
    private final String bearerToken;
    private final OkHttpClient httpClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger subCounter = new AtomicInteger(0);

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private WebSocket webSocket;
    private ConnectionListener connectionListener;
    private boolean connected = false;
    private boolean userDisconnected = false;
    private int reconnectAttempts = 0;
    private long lastInboundMs = 0L;
    private long lastOutboundMs = 0L;

    private Runnable heartbeatTask;
    private Runnable inboundWatchdogTask;

    private static final class Subscription {
        final String id;
        final String destination;
        final FrameListener listener;
        Subscription(String id, String destination, FrameListener listener) {
            this.id = id;
            this.destination = destination;
            this.listener = listener;
        }
    }

    public StompClient(String httpBaseUrl, String bearerToken, OkHttpClient httpClient) {
        this.bearerToken = bearerToken;
        this.httpClient = httpClient;
        String base = httpBaseUrl == null ? "" : httpBaseUrl;
        String ws;
        if (base.startsWith("https://")) {
            ws = "wss://" + base.substring("https://".length());
        } else if (base.startsWith("http://")) {
            ws = "ws://" + base.substring("http://".length());
        } else {
            ws = base;
        }
        if (!ws.endsWith("/")) ws = ws + "/";
        this.wsUrl = ws + "ws";
        String h;
        try {
            URI u = URI.create(this.wsUrl);
            h = u.getHost();
            if (h == null) h = "localhost";
        } catch (Exception e) {
            h = "localhost";
        }
        this.host = h;
    }

    public synchronized void connect(ConnectionListener listener) {
        this.connectionListener = listener;
        this.userDisconnected = false;
        openSocket();
    }

    private void openSocket() {
        Request req = new Request.Builder().url(wsUrl).build();
        webSocket = httpClient.newWebSocket(req, new InternalListener());
    }

    public synchronized String subscribe(String destination, FrameListener listener) {
        String id = "sub-" + subCounter.incrementAndGet();
        Subscription sub = new Subscription(id, destination, listener);
        subscriptions.put(id, sub);
        if (connected) {
            sendSubscribeFrame(sub);
        }
        return id;
    }

    public synchronized void unsubscribe(String subscriptionId) {
        Subscription sub = subscriptions.remove(subscriptionId);
        if (sub != null && connected) {
            Map<String, String> h = new LinkedHashMap<>();
            h.put("id", sub.id);
            sendFrame("UNSUBSCRIBE", h, null);
        }
    }

    public synchronized void send(String destination, String jsonBody) {
        if (!connected) return;
        Map<String, String> h = new LinkedHashMap<>();
        h.put("destination", destination);
        h.put("content-type", "application/json");
        sendFrame("SEND", h, jsonBody);
    }

    public synchronized void disconnect() {
        userDisconnected = true;
        cancelTimers();
        if (connected && webSocket != null) {
            Map<String, String> h = new LinkedHashMap<>();
            h.put("receipt", "disconnect-0");
            sendFrame("DISCONNECT", h, null);
        }
        if (webSocket != null) {
            try { webSocket.close(1000, "client disconnect"); } catch (Exception ignored) {}
        }
        connected = false;
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    private void sendConnectFrame() {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("accept-version", "1.2");
        h.put("host", host);
        h.put("heart-beat", "10000,10000");
        if (bearerToken != null && !bearerToken.isEmpty()) {
            h.put("Authorization", "Bearer " + bearerToken);
        }
        sendFrame("CONNECT", h, null);
    }

    private void sendSubscribeFrame(Subscription sub) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("id", sub.id);
        h.put("destination", sub.destination);
        sendFrame("SUBSCRIBE", h, null);
    }

    private void sendFrame(String command, Map<String, String> headers, @Nullable String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(command).append('\n');
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                sb.append(e.getKey()).append(':').append(e.getValue()).append('\n');
            }
        }
        sb.append('\n');
        if (body != null) sb.append(body);
        sb.append(NULL_TERM);
        if (webSocket != null) {
            webSocket.send(sb.toString());
            lastOutboundMs = System.currentTimeMillis();
        }
    }

    private void handleFrame(String raw) {
        lastInboundMs = System.currentTimeMillis();
        // Heartbeat frames are a bare newline. Ignore payload on those frames.
        if (raw == null || raw.isEmpty() || "\n".equals(raw)) return;

        String trimmed = raw;
        if (trimmed.endsWith(NULL_TERM)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int sep = trimmed.indexOf("\n\n");
        if (sep < 0) return;
        String head = trimmed.substring(0, sep);
        String body = trimmed.substring(sep + 2);

        String[] headLines = head.split("\n");
        if (headLines.length == 0) return;
        String command = headLines[0].trim();
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < headLines.length; i++) {
            String line = headLines[i];
            int c = line.indexOf(':');
            if (c > 0) {
                String k = line.substring(0, c);
                String v = line.substring(c + 1);
                if (!headers.containsKey(k)) headers.put(k, v);
            }
        }

        switch (command) {
            case "CONNECTED":
                onStompConnected();
                break;
            case "MESSAGE": {
                String dest = headers.get("destination");
                if (dest == null) break;
                for (Subscription sub : subscriptions.values()) {
                    if (dest.equals(sub.destination)) {
                        final String b = body;
                        final FrameListener l = sub.listener;
                        mainHandler.post(() -> l.onMessage(b));
                    }
                }
                break;
            }
            case "ERROR":
                forceCloseAndReconnect(new RuntimeException("STOMP ERROR: " + headers.get("message")));
                break;
            default:
                break;
        }
    }

    private synchronized void onStompConnected() {
        connected = true;
        reconnectAttempts = 0;
        for (Subscription s : subscriptions.values()) sendSubscribeFrame(s);
        scheduleHeartbeat();
        scheduleInboundWatchdog();
        if (connectionListener != null) {
            final ConnectionListener l = connectionListener;
            mainHandler.post(l::onConnected);
        }
    }

    private void scheduleHeartbeat() {
        cancelHeartbeat();
        heartbeatTask = new Runnable() {
            @Override public void run() {
                synchronized (StompClient.this) {
                    if (!connected || webSocket == null) return;
                    long now = System.currentTimeMillis();
                    if (now - lastOutboundMs >= HEARTBEAT_INTERVAL_MS) {
                        webSocket.send("\n");
                        lastOutboundMs = now;
                    }
                    mainHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
                }
            }
        };
        mainHandler.postDelayed(heartbeatTask, HEARTBEAT_INTERVAL_MS);
    }

    private void scheduleInboundWatchdog() {
        cancelInboundWatchdog();
        inboundWatchdogTask = new Runnable() {
            @Override public void run() {
                synchronized (StompClient.this) {
                    if (!connected) return;
                    long now = System.currentTimeMillis();
                    if (now - lastInboundMs > INBOUND_TIMEOUT_MS) {
                        forceCloseAndReconnect(new RuntimeException("inbound heartbeat timeout"));
                        return;
                    }
                    mainHandler.postDelayed(this, 5_000L);
                }
            }
        };
        mainHandler.postDelayed(inboundWatchdogTask, 5_000L);
    }

    private void cancelHeartbeat() {
        if (heartbeatTask != null) {
            mainHandler.removeCallbacks(heartbeatTask);
            heartbeatTask = null;
        }
    }

    private void cancelInboundWatchdog() {
        if (inboundWatchdogTask != null) {
            mainHandler.removeCallbacks(inboundWatchdogTask);
            inboundWatchdogTask = null;
        }
    }

    private void cancelTimers() {
        cancelHeartbeat();
        cancelInboundWatchdog();
    }

    private synchronized void forceCloseAndReconnect(Throwable cause) {
        connected = false;
        cancelTimers();
        if (webSocket != null) {
            try { webSocket.cancel(); } catch (Exception ignored) {}
            webSocket = null;
        }
        if (connectionListener != null) {
            final ConnectionListener l = connectionListener;
            final Throwable t = cause;
            mainHandler.post(() -> l.onDisconnected(t));
        }
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (userDisconnected) return;
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return;
        long delay = BACKOFF_MS[Math.min(reconnectAttempts, BACKOFF_MS.length - 1)];
        reconnectAttempts++;
        mainHandler.postDelayed(() -> {
            synchronized (StompClient.this) {
                if (userDisconnected || connected) return;
                openSocket();
            }
        }, delay);
    }

    private final class InternalListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
            lastInboundMs = System.currentTimeMillis();
            sendConnectFrame();
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            handleFrame(text);
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull ByteString bytes) {
            handleFrame(bytes.utf8());
        }

        @Override
        public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
            try { ws.close(1000, null); } catch (Exception ignored) {}
        }

        @Override
        public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
            forceCloseAndReconnect(new RuntimeException("socket closed: " + code + " " + reason));
        }

        @Override
        public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, @Nullable Response response) {
            forceCloseAndReconnect(t);
        }
    }
}
