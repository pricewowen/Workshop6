package com.example.workshop6.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;
import com.example.workshop6.ui.profile.CustomerProfileSetupActivity;
import com.example.workshop6.data.model.Cart;
import com.example.workshop6.util.MoneyFormat;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.ProductSpecialState;
import com.example.workshop6.util.TodayDate;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartFragment extends Fragment implements CartAdapter.OnCartItemListener {

    private RecyclerView rvCart;
    private TextView tvEmptyCart;
    private TextView tvSubtotal;
    private Button btnCheckout;
    private CartAdapter adapter;
    private Cart cart;
    private CartManager cartManager;
    private ApiService api;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);

    private ActivityResultLauncher<Intent> customerProfileLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (!isAdded()) {
                        return;
                    }
                    restoreCheckoutButtonEnabledState();
                });
    }

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

        if (sessionManager.isLoggedIn()) {
            ApiClient.getInstance().setToken(sessionManager.getToken());
        } else {
            ApiClient.getInstance().clearToken();
        }
        api = ApiClient.getInstance().getService();

        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CartAdapter(cart.getItems(), this);
        rvCart.setAdapter(adapter);

        btnCheckout.setOnClickListener(v -> attemptProceedToCheckout());

        updateUI();
    }

    private void attemptProceedToCheckout() {
        if (api == null || cart == null || cart.isEmpty()) {
            return;
        }
        SessionManager sessionManager = new SessionManager(requireContext());
        if (sessionManager.isGuestMode()) {
            if (!sessionManager.hasGuestProfile()) {
                Intent i = new Intent(requireContext(), CustomerProfileSetupActivity.class);
                i.putExtra(CustomerProfileSetupActivity.EXTRA_LAUNCHED_FOR_CHECKOUT, true);
                i.putExtra(CustomerProfileSetupActivity.EXTRA_GUEST_MODE, true);
                customerProfileLauncher.launch(
                        i,
                        NavTransitions.forwardLaunchOptions(requireActivity()));
                return;
            }
            startCheckoutActivity();
            return;
        }
        setCheckoutLoading(true);
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (response.code() == 404) {
                        Intent i = new Intent(requireContext(), CustomerProfileSetupActivity.class);
                        i.putExtra(CustomerProfileSetupActivity.EXTRA_LAUNCHED_FOR_CHECKOUT, true);
                        customerProfileLauncher.launch(
                                i,
                                NavTransitions.forwardLaunchOptions(requireActivity()));
                        return;
                    }
                    setCheckoutLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        startCheckoutActivity();
                        return;
                    }
                    // Toast.makeText(requireContext(), R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    setCheckoutLoading(false);
                    // Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startCheckoutActivity() {
        NavTransitions.startActivityWithForward(requireActivity(),
                new Intent(requireContext(), CheckoutActivity.class));
    }

    private void setCheckoutLoading(boolean loading) {
        if (btnCheckout == null) {
            return;
        }
        btnCheckout.setEnabled(!loading && cart != null && !cart.isEmpty());
        btnCheckout.setAlpha(loading ? 0.5f : (cart != null && !cart.isEmpty() ? 1f : 0.5f));
    }

    private void restoreCheckoutButtonEnabledState() {
        setCheckoutLoading(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null || api == null) {
            return;
        }
        // Re-fetch cart in case it was cleared after checkout
        cart = cartManager.getCart();
        adapter.updateItems(cart.getItems());

        api.getTodayProductSpecial(TodayDate.isoLocal()).enqueue(new Callback<ProductSpecialTodayDto>() {
            @Override
            public void onResponse(Call<ProductSpecialTodayDto> call, Response<ProductSpecialTodayDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductSpecialState.updateFromDto(response.body(), TodayDate.isoLocal());
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.updateItems(cart.getItems());
                    updateUI();
                });
            }

            @Override
            public void onFailure(Call<ProductSpecialTodayDto> call, Throwable t) {
                requireActivity().runOnUiThread(() -> {
                    adapter.updateItems(cart.getItems());
                    updateUI();
                });
            }
        });
    }

    private void updateUI() {
        if (cart.isEmpty()) {
            rvCart.setVisibility(View.GONE);
            tvEmptyCart.setVisibility(View.VISIBLE);
            btnCheckout.setEnabled(false);
            btnCheckout.setAlpha(0.5f);

            tvSubtotal.setText(MoneyFormat.formatCad(currencyFormat, 0));
        } else {
            rvCart.setVisibility(View.VISIBLE);
            tvEmptyCart.setVisibility(View.GONE);
            btnCheckout.setEnabled(true);
            btnCheckout.setAlpha(1.0f);

            double estimatedTotal = cart.getTotalPrice();
            tvSubtotal.setText(MoneyFormat.formatCad(currencyFormat, estimatedTotal));
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