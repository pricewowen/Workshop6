package com.example.workshop6.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.model.Cart;

import java.text.NumberFormat;
import java.util.Locale;

public class CartFragment extends Fragment implements CartAdapter.OnCartItemListener {

    private RecyclerView rvCart;
    private TextView tvEmptyCart;
    private TextView tvSubtotal;
    private TextView tvTax;
    private TextView tvTotal;
    private Button btnCheckout;
    private CartAdapter adapter;
    private Cart cart;
    private CartManager cartManager;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private static final double TAX_RATE = 0.13; // 13% HST

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cartManager = CartManager.getInstance(requireContext());
        cart = cartManager.getCart();

        rvCart = view.findViewById(R.id.rvCart);
        tvEmptyCart = view.findViewById(R.id.tvEmptyCart);
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvTax = view.findViewById(R.id.tvTax);
        tvTotal = view.findViewById(R.id.tvTotal);
        btnCheckout = view.findViewById(R.id.btnCheckout);

        SessionManager sessionManager = new SessionManager(requireContext());
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!isCustomer) {
            tvEmptyCart.setVisibility(View.VISIBLE);
            tvEmptyCart.setText(R.string.staff_purchase_blocked);
            rvCart.setVisibility(View.GONE);
            btnCheckout.setEnabled(false);
            btnCheckout.setAlpha(0.5f);
            return;
        }

        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CartAdapter(cart.getItems(), this);
        rvCart.setAdapter(adapter);

        btnCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            startActivity(intent);
        });

        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh cart when returning to fragment
        if (adapter != null) {
            adapter.updateItems(cart.getItems());
            updateUI();
        }
    }

    private void updateUI() {
        if (cart.isEmpty()) {
            rvCart.setVisibility(View.GONE);
            tvEmptyCart.setVisibility(View.VISIBLE);
            btnCheckout.setEnabled(false);
            btnCheckout.setAlpha(0.5f);

            tvSubtotal.setText(currencyFormat.format(0));
            tvTax.setText(currencyFormat.format(0));
            tvTotal.setText(currencyFormat.format(0));
        } else {
            rvCart.setVisibility(View.VISIBLE);
            tvEmptyCart.setVisibility(View.GONE);
            btnCheckout.setEnabled(true);
            btnCheckout.setAlpha(1.0f);

            double subtotal = cart.getTotalPrice();
            double tax = subtotal * TAX_RATE;
            double total = subtotal + tax;

            // show discount if there
            if (cart.hasDiscount()) {
                 String price = currencyFormat.format(subtotal);
                tvSubtotal.setText(getString(R.string.label_discount_cart) + " " + price);
            } else {
                tvSubtotal.setText(currencyFormat.format(subtotal));
            }
            tvTax.setText(currencyFormat.format(tax));
            tvTotal.setText(currencyFormat.format(total));
        }
    }

    @Override
    public void onQuantityChanged(int productId, int newQuantity) {
        cart.updateQuantity(productId, newQuantity);
        adapter.updateItems(cart.getItems());
        updateUI();
    }

    @Override
    public void onRemoveItem(int productId) {
        cart.removeItem(productId);
        adapter.updateItems(cart.getItems());
        updateUI();
    }
}