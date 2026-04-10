package com.example.workshop6.ui.cart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;

public class CheckoutReturnActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "success".equals(data.getLastPathSegment())) {
            handleSuccess();
        } else {
            handleCancel();
        }
    }

    private void handleSuccess() {
        SessionManager sessionManager = new SessionManager(this);
        ActivityLogger.log(this, sessionManager, "PAYMENT_SUCCESS", "Stripe checkout completed");

        CartManager.getInstance(this).clearCart();

        Toast.makeText(this, R.string.order_placed_success, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void handleCancel() {
        Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_LONG).show();
        finish();
    }
}
