package com.example.workshop6.ui.cart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ConfirmStripePaymentRequest;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.api.dto.OrderItemDto;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.payments.PendingStripeConfirm;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.ui.orders.OrderHistoryAdapter;
import com.example.workshop6.ui.orders.OrderHistoryActivity;
import com.example.workshop6.util.MoneyFormat;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutReturnActivity extends AppCompatActivity {

    private static final String PENDING_PREFS = "pending_stripe_confirm";
    private static final String K_ORDER = "order_id";
    private static final String K_PI = "payment_intent_id";

    private View loadingContainer;
    private CardView summaryCard;
    private TextView tvHeroTitle;
    private TextView tvHeroSubtitle;
    private TextView tvOrderNumber;
    private TextView tvMethod;
    private TextView tvBakery;
    private TextView tvScheduled;
    private TextView tvSubtotal;
    private TextView tvTax;
    private TextView tvTotal;
    private TextView tvLoadFailed;
    private LinearLayout itemsContainer;
    private LinearLayout bakeryRow;
    private LinearLayout scheduledRow;
    private MaterialButton btnPrimary;
    private MaterialButton btnSecondary;
    private View heroIconContainer;
    private android.widget.ImageView ivHeroIcon;

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private final SimpleDateFormat scheduleFormat =
            new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.CANADA);

    private SessionManager sessionManager;
    private boolean isCancelState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_return);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadingContainer = findViewById(R.id.loadingContainer);
        summaryCard = findViewById(R.id.summaryCard);
        tvHeroTitle = findViewById(R.id.tvHeroTitle);
        tvHeroSubtitle = findViewById(R.id.tvHeroSubtitle);
        tvOrderNumber = findViewById(R.id.tvOrderNumber);
        tvMethod = findViewById(R.id.tvMethod);
        tvBakery = findViewById(R.id.tvBakery);
        tvScheduled = findViewById(R.id.tvScheduled);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTax = findViewById(R.id.tvTax);
        tvTotal = findViewById(R.id.tvTotal);
        tvLoadFailed = findViewById(R.id.tvLoadFailed);
        itemsContainer = findViewById(R.id.itemsContainer);
        bakeryRow = findViewById(R.id.bakeryRow);
        scheduledRow = findViewById(R.id.scheduledRow);
        btnPrimary = findViewById(R.id.btnPrimary);
        btnSecondary = findViewById(R.id.btnSecondary);
        heroIconContainer = findViewById(R.id.heroIconContainer);
        ivHeroIcon = findViewById(R.id.ivHeroIcon);

        sessionManager = new SessionManager(this);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        boolean success = data != null && "success".equals(data.getLastPathSegment());
        if (success) {
            renderSuccessChrome();
            confirmAndLoadOrder();
        } else {
            renderCancelState();
        }
    }

    private void renderSuccessChrome() {
        isCancelState = false;
        heroIconContainer.setBackgroundResource(R.drawable.bg_confirmation_hero_circle);
        ivHeroIcon.setImageResource(R.drawable.ic_check_circle);
        tvHeroTitle.setText(R.string.checkout_return_success_title);
        tvHeroSubtitle.setText(R.string.checkout_return_success_subtitle);
        loadingContainer.setVisibility(View.VISIBLE);
        summaryCard.setVisibility(View.GONE);
        tvLoadFailed.setVisibility(View.GONE);
        tvOrderNumber.setVisibility(View.GONE);

        btnSecondary.setText(R.string.checkout_return_view_order);
        btnSecondary.setOnClickListener(v -> openOrderHistory());

        btnPrimary.setText(R.string.checkout_return_continue_shopping);
        btnPrimary.setOnClickListener(v -> openMain());
    }

    private void renderCancelState() {
        isCancelState = true;
        heroIconContainer.setBackgroundResource(R.drawable.bg_confirmation_hero_circle_error);
        ivHeroIcon.setImageResource(R.drawable.ic_error_circle);
        tvHeroTitle.setText(R.string.checkout_return_cancel_title);
        tvHeroSubtitle.setText(R.string.checkout_return_cancel_subtitle);
        tvOrderNumber.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.GONE);
        summaryCard.setVisibility(View.GONE);
        tvLoadFailed.setVisibility(View.GONE);

        btnSecondary.setVisibility(View.GONE);
        btnPrimary.setText(R.string.checkout_return_back_to_cart);
        btnPrimary.setOnClickListener(v -> openMain());
    }

    private void confirmAndLoadOrder() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PENDING_PREFS, MODE_PRIVATE);
        String orderId = prefs.getString(K_ORDER, null);
        String paymentIntentId = prefs.getString(K_PI, null);

        if (sessionManager.isLoggedIn()) {
            String token = sessionManager.getToken();
            if (token != null && !token.isEmpty()) {
                ApiClient.getInstance().setToken(token);
            }
        }

        ActivityLogger.log(this, sessionManager, "PAYMENT_SUCCESS", "Stripe checkout completed");
        CartManager.getInstance(this).clearCart();

        if (orderId == null || paymentIntentId == null) {
            renderLoadFailure();
            return;
        }

        ApiService api = ApiClient.getInstance().getService();
        ConfirmStripePaymentRequest body = new ConfirmStripePaymentRequest();
        body.paymentIntentId = paymentIntentId;
        api.confirmStripePayment(orderId, body).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (isFinishing() || isCancelState) return;
                OrderDto order = response.body();
                if (response.isSuccessful() && order != null) {
                    PendingStripeConfirm.clear(CheckoutReturnActivity.this);
                    renderOrder(order);
                } else {
                    renderLoadFailure();
                }
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                if (isFinishing() || isCancelState) return;
                renderLoadFailure();
            }
        });
    }

    private void renderOrder(OrderDto order) {
        loadingContainer.setVisibility(View.GONE);
        summaryCard.setVisibility(View.VISIBLE);

        String orderLabel = order.orderNumber != null && !order.orderNumber.isEmpty()
                ? order.orderNumber
                : (order.id != null ? order.id : "");
        if (!orderLabel.isEmpty()) {
            tvOrderNumber.setText(getString(R.string.checkout_return_order_number_fmt, orderLabel));
            tvOrderNumber.setVisibility(View.VISIBLE);
        }

        tvMethod.setText(formatMethod(order.orderMethod));

        if (order.bakeryName != null && !order.bakeryName.isEmpty()) {
            tvBakery.setText(order.bakeryName);
            bakeryRow.setVisibility(View.VISIBLE);
        } else {
            bakeryRow.setVisibility(View.GONE);
        }

        Date scheduled = OrderHistoryAdapter.parseIsoDate(order.scheduledAt);
        if (scheduled != null) {
            tvScheduled.setText(scheduleFormat.format(scheduled));
            scheduledRow.setVisibility(View.VISIBLE);
        } else {
            scheduledRow.setVisibility(View.GONE);
        }

        renderItems(order.items);

        tvSubtotal.setText(MoneyFormat.formatCad(currency, order.getSubtotalAmount()));
        tvTax.setText(MoneyFormat.formatCad(currency, order.getTaxAmount()));
        tvTotal.setText(MoneyFormat.formatCad(currency, order.getGrandTotalAmount()));
    }

    private void renderItems(@Nullable List<OrderItemDto> items) {
        itemsContainer.removeAllViews();
        if (items == null || items.isEmpty()) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (OrderItemDto item : items) {
            View row = inflater.inflate(
                    android.R.layout.simple_list_item_2, itemsContainer, false);
            ((ViewGroup.MarginLayoutParams) row.getLayoutParams()).bottomMargin = dp(6);

            TextView name = row.findViewById(android.R.id.text1);
            TextView trailing = row.findViewById(android.R.id.text2);

            String displayName = item.productName != null ? item.productName : "";
            String qtyLabel = getString(R.string.checkout_return_item_qty_fmt, item.quantity);
            name.setText(displayName + "  " + qtyLabel);
            name.setTextColor(getResources().getColor(R.color.bakery_text_dark));
            name.setTextSize(14f);

            BigDecimal line = item.lineTotal != null
                    ? item.lineTotal
                    : (item.unitPrice != null
                        ? item.unitPrice.multiply(BigDecimal.valueOf(item.quantity))
                        : BigDecimal.ZERO);
            trailing.setText(MoneyFormat.formatCad(currency, line));
            trailing.setTextColor(getResources().getColor(R.color.bakery_text_secondary));
            trailing.setTextSize(13f);

            itemsContainer.addView(row);
        }
    }

    private String formatMethod(@Nullable String method) {
        if (method == null) return "";
        String lower = method.trim().toLowerCase(Locale.ROOT);
        if (lower.equals("delivery")) return getString(R.string.delivery);
        if (lower.equals("pickup")) return getString(R.string.pickup);
        return method;
    }

    private void renderLoadFailure() {
        loadingContainer.setVisibility(View.GONE);
        summaryCard.setVisibility(View.GONE);
        tvLoadFailed.setVisibility(View.VISIBLE);
    }

    private void openOrderHistory() {
        Intent intent = new Intent(this, OrderHistoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (isCancelState) {
            super.onBackPressed();
        } else {
            openMain();
        }
    }
}
