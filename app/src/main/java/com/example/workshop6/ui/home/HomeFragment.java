package com.example.workshop6.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;

/**
 * Stub Home tab — teammates wire in featured products, loyalty points, etc.
 */
public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView homeMessage = view.findViewById(R.id.text_home_message);
        SessionManager sessionManager = new SessionManager(requireContext());
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        homeMessage.setText(isCustomer
                ? getString(R.string.stub_home_message)
                : getString(R.string.staff_home_message));
    }
}
