package com.example.casavista.admin.fragments;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.R;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminPosFragment extends Fragment {

    private ImageView imgPosHeaderAvatar;
    private TextView tvPosHeaderInitials;

    private MaterialButton btnCatCourts, btnCatCottage, btnCatRentals;
    private MaterialButton btnChargePos;
    private TextView tvPosTotal;
    private TextView tvClearOrder;
    private LinearLayout containerOrderItems;

    private TextView tvItem1Title, tvItem1Price;
    private TextView tvItem2Title, tvItem2Price;
    private TextView tvItem3Title, tvItem3Price;
    private TextView tvItem4Title, tvItem4Price;

    private final Map<String, Integer> cart = new LinkedHashMap<>();
    private final Map<String, PosProduct> availableProducts = new LinkedHashMap<>();

    private static class PosProduct {
        String id;
        String name;
        double price;
        String category;

        PosProduct(String id, String name, double price, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.category = category;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_pos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgPosHeaderAvatar = view.findViewById(R.id.imgPosHeaderAvatar);

        View cardAvatar = view.findViewById(R.id.cardAdminPosAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                com.example.casavista.admin.AdminMainActivity adminActivity = (com.example.casavista.admin.AdminMainActivity) getActivity();
                if (adminActivity != null) {
                    adminActivity.loadFragment(new AdminProfileFragment());
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = adminActivity.findViewById(R.id.adminBottomNavigationView);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.admin_nav_profile);
                    }
                }
            });
        }
        tvPosHeaderInitials = view.findViewById(R.id.tvPosHeaderInitials);

        btnCatCourts = view.findViewById(R.id.btnCatCourts);
        btnCatCottage = view.findViewById(R.id.btnCatCottage);
        btnCatRentals = view.findViewById(R.id.btnCatRentals);

        containerOrderItems = view.findViewById(R.id.containerOrderItems);
        tvPosTotal = view.findViewById(R.id.tvPosTotal);
        tvClearOrder = view.findViewById(R.id.tvClearOrder);
        btnChargePos = view.findViewById(R.id.btnChargePos);

        tvItem1Title = view.findViewById(R.id.tvItem1Title);
        tvItem1Price = view.findViewById(R.id.tvItem1Price);
        tvItem2Title = view.findViewById(R.id.tvItem2Title);
        tvItem2Price = view.findViewById(R.id.tvItem2Price);
        tvItem3Title = view.findViewById(R.id.tvItem3Title);
        tvItem3Price = view.findViewById(R.id.tvItem3Price);
        tvItem4Title = view.findViewById(R.id.tvItem4Title);
        tvItem4Price = view.findViewById(R.id.tvItem4Price);

        loadCourtsCategory();

        bindItemClickListener(view, R.id.btnAddItem1, "ITEM_1");
        bindItemClickListener(view, R.id.btnAddItem2, "ITEM_2");
        bindItemClickListener(view, R.id.btnAddItem3, "ITEM_3");
        bindItemClickListener(view, R.id.btnAddItem4, "ITEM_4");

        btnCatCourts.setOnClickListener(v -> {
            animateSelection(v);
            selectCategoryTab(btnCatCourts);
            loadCourtsCategory();
        });

        btnCatCottage.setOnClickListener(v -> {
            animateSelection(v);
            selectCategoryTab(btnCatCottage);
            loadCottageCategory();
        });

        btnCatRentals.setOnClickListener(v -> {
            animateSelection(v);
            selectCategoryTab(btnCatRentals);
            loadRentalsCategory();
        });

        if (tvClearOrder != null) {
            tvClearOrder.setOnClickListener(v -> {
                animateSelection(v);
                clearCart();
            });
        }

        btnChargePos.setOnClickListener(v -> {
            animateSelection(v);
            processPayment();
        });

        updateOrderSummaryUI();
    }

    private void bindItemClickListener(View parentView, int resId, String itemKey) {
        View itemBtn = parentView.findViewById(resId);
        if (itemBtn != null) {
            itemBtn.setOnClickListener(v -> {
                animateSelection(v);
                addItemToCart(itemKey);
            });
        }
    }

    private void loadCourtsCategory() {
        availableProducts.clear();
        availableProducts.put("ITEM_1", new PosProduct("ITEM_1", "Pickleball · Daytime (1hr)", 400.0, "Courts"));
        availableProducts.put("ITEM_2", new PosProduct("ITEM_2", "Pickleball · Nighttime (1hr)", 600.0, "Courts"));
        availableProducts.put("ITEM_3", new PosProduct("ITEM_3", "Paddle rental (1hr)", 100.0, "Courts"));
        availableProducts.put("ITEM_4", new PosProduct("ITEM_4", "Bottled water", 35.0, "Courts"));

        updateItemCards();
    }

    private void loadCottageCategory() {
        availableProducts.clear();
        availableProducts.put("ITEM_1", new PosProduct("ITEM_1", "Small Cottage (Day Use)", 800.0, "Cottage"));
        availableProducts.put("ITEM_2", new PosProduct("ITEM_2", "Large Cottage (Day Use)", 1500.0, "Cottage"));
        availableProducts.put("ITEM_3", new PosProduct("ITEM_3", "VIP Gazebo (Day Use)", 2200.0, "Cottage"));
        availableProducts.put("ITEM_4", new PosProduct("ITEM_4", "Extra Table & Chairs", 200.0, "Cottage"));

        updateItemCards();
    }

    private void loadRentalsCategory() {
        availableProducts.clear();
        availableProducts.put("ITEM_1", new PosProduct("ITEM_1", "Life Vest Rental", 100.0, "Rentals"));
        availableProducts.put("ITEM_2", new PosProduct("ITEM_2", "Towel Rental", 75.0, "Rentals"));
        availableProducts.put("ITEM_3", new PosProduct("ITEM_3", "Locker Rental", 50.0, "Rentals"));
        availableProducts.put("ITEM_4", new PosProduct("ITEM_4", "Pool Float / Ring", 120.0, "Rentals"));

        updateItemCards();
    }

    private void updateItemCards() {
        PosProduct p1 = availableProducts.get("ITEM_1");
        PosProduct p2 = availableProducts.get("ITEM_2");
        PosProduct p3 = availableProducts.get("ITEM_3");
        PosProduct p4 = availableProducts.get("ITEM_4");

        if (p1 != null) {
            tvItem1Title.setText(p1.name);
            tvItem1Price.setText(String.format("₱%.0f", p1.price));
        }
        if (p2 != null) {
            tvItem2Title.setText(p2.name);
            tvItem2Price.setText(String.format("₱%.0f", p2.price));
        }
        if (p3 != null) {
            tvItem3Title.setText(p3.name);
            tvItem3Price.setText(String.format("₱%.0f", p3.price));
        }
        if (p4 != null) {
            tvItem4Title.setText(p4.name);
            tvItem4Price.setText(String.format("₱%.0f", p4.price));
        }
    }

    private void selectCategoryTab(MaterialButton selectedBtn) {
        int activeGold = ContextCompat.getColor(requireContext(), R.color.accent_gold);
        int cardBg = ContextCompat.getColor(requireContext(), R.color.bg_card);
        int mainBg = ContextCompat.getColor(requireContext(), R.color.bg_main);
        int primaryText = ContextCompat.getColor(requireContext(), R.color.text_primary);

        btnCatCourts.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cardBg));
        btnCatCourts.setTextColor(primaryText);

        btnCatCottage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cardBg));
        btnCatCottage.setTextColor(primaryText);

        btnCatRentals.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cardBg));
        btnCatRentals.setTextColor(primaryText);

        selectedBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeGold));
        selectedBtn.setTextColor(mainBg);
    }

    private void addItemToCart(String itemKey) {
        PosProduct product = availableProducts.get(itemKey);
        if (product == null) return;

        int currentQty = cart.containsKey(product.name) ? cart.get(product.name) : 0;
        cart.put(product.name, currentQty + 1);

        updateOrderSummaryUI();
    }

    private void clearCart() {
        cart.clear();
        updateOrderSummaryUI();
    }

    private void updateOrderSummaryUI() {
        if (containerOrderItems == null) return;

        containerOrderItems.removeAllViews();
        double totalAmount = 0.0;

        if (cart.isEmpty()) {
            TextView emptyTv = new TextView(getContext());
            emptyTv.setText("No items added to order yet.");
            emptyTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            emptyTv.setTextSize(12);
            emptyTv.setPadding(0, 8, 0, 8);
            containerOrderItems.addView(emptyTv);
        } else {
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String name = entry.getKey();
                int qty = entry.getValue();
                double itemUnitPrice = getItemUnitPrice(name);
                double itemSubtotal = itemUnitPrice * qty;
                totalAmount += itemSubtotal;

                RelativeLayout row = new RelativeLayout(getContext());
                RelativeLayout.LayoutParams rowParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 10, 0, 10);
                row.setLayoutParams(rowParams);

                TextView tvName = new TextView(getContext());
                tvName.setText(String.format("%s × %d", name, qty));
                tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                tvName.setTextSize(13);
                tvName.setTypeface(null, Typeface.BOLD);

                RelativeLayout.LayoutParams nameParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                nameParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                tvName.setLayoutParams(nameParams);

                TextView tvPrice = new TextView(getContext());
                tvPrice.setText(String.format("₱%.2f", itemSubtotal));
                tvPrice.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                tvPrice.setTextSize(13);
                tvPrice.setTypeface(null, Typeface.BOLD);

                RelativeLayout.LayoutParams priceParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                priceParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                tvPrice.setLayoutParams(priceParams);

                row.addView(tvName);
                row.addView(tvPrice);

                row.setOnClickListener(v -> {
                    animateSelection(v);
                    if (qty > 1) {
                        cart.put(name, qty - 1);
                    } else {
                        cart.remove(name);
                    }
                    updateOrderSummaryUI();
                });

                containerOrderItems.addView(row);
            }
        }

        tvPosTotal.setText(String.format("₱%.2f", totalAmount));
        btnChargePos.setText(String.format("CHARGE ORDER: ₱%.2f", totalAmount));
        btnChargePos.setEnabled(totalAmount > 0);
    }

    private double getItemUnitPrice(String name) {
        for (PosProduct p : availableProducts.values()) {
            if (p.name.equalsIgnoreCase(name)) return p.price;
        }
        if (name.contains("Daytime")) return 400.0;
        if (name.contains("Nighttime")) return 600.0;
        if (name.contains("Paddle")) return 100.0;
        if (name.contains("water")) return 35.0;
        if (name.contains("Small Cottage")) return 800.0;
        if (name.contains("Large Cottage")) return 1500.0;
        if (name.contains("VIP Gazebo")) return 2200.0;
        if (name.contains("Table")) return 200.0;
        if (name.contains("Life Vest")) return 100.0;
        if (name.contains("Towel")) return 75.0;
        if (name.contains("Locker")) return 50.0;
        if (name.contains("Float")) return 120.0;

        return 0.0;
    }

    private void processPayment() {
        double finalTotal = calculateCartTotal();
        if (finalTotal <= 0) {
            Toast.makeText(getContext(), "Please add items to order first", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pos_payment, null);
        TextView tvDialogTotal = dialogView.findViewById(R.id.tvPosDialogTotal);
        android.widget.Spinner spinnerPayment = dialogView.findViewById(R.id.spinnerPosPaymentMethod);

        if (tvDialogTotal != null) {
            tvDialogTotal.setText(String.format("Total Amount: ₱%.2f", finalTotal));
        }

        java.util.List<String> paymentMethods = new java.util.ArrayList<>();
        paymentMethods.add("GCash (Cashless E-Wallet)");
        paymentMethods.add("Cash at Front Desk / Counter");
        paymentMethods.add("Credit / Debit Card");
        paymentMethods.add("Bank Transfer (Online)");

        com.google.android.material.textfield.TextInputLayout layoutPosMobile = dialogView.findViewById(R.id.layoutPosAccountMobile);
        com.google.android.material.textfield.TextInputEditText etPosMobile = dialogView.findViewById(R.id.etPosAccountMobileNumber);

        android.widget.ArrayAdapter<String> paymentAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.item_payment_spinner_selected, paymentMethods);
        paymentAdapter.setDropDownViewResource(R.layout.item_payment_spinner_dropdown);
        if (spinnerPayment != null) {
            spinnerPayment.setAdapter(paymentAdapter);

            spinnerPayment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selected = paymentAdapter.getItem(position);
                    if (selected != null && (selected.contains("GCash") || selected.contains("Card") || selected.contains("Bank"))) {
                        if (layoutPosMobile != null) {
                            layoutPosMobile.setVisibility(View.VISIBLE);
                            if (selected.contains("GCash")) {
                                layoutPosMobile.setHint("GCash / Mobile Number");
                            } else if (selected.contains("Card")) {
                                layoutPosMobile.setHint("Cardholder Mobile / Account Number");
                            } else {
                                layoutPosMobile.setHint("Bank Account / Ref Mobile Number");
                            }
                        }
                    } else {
                        if (layoutPosMobile != null) {
                            layoutPosMobile.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Process POS Payment")
                .setView(dialogView)
                .setPositiveButton("Confirm & Charge", (dialog, which) -> {
                    String chosenPayment = spinnerPayment != null ? spinnerPayment.getSelectedItem().toString() : "GCash (Cashless E-Wallet)";
                    String mobileNum = etPosMobile != null ? etPosMobile.getText().toString().trim() : "";

                    if (layoutPosMobile != null && layoutPosMobile.getVisibility() == View.VISIBLE && mobileNum.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter the account / mobile number for cashless payment.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!mobileNum.isEmpty()) {
                        chosenPayment += " (" + mobileNum + ")";
                    }

                    Toast.makeText(getContext(), String.format("Payment of ₱%.2f via %s successful!", finalTotal, chosenPayment), Toast.LENGTH_LONG).show();
                    clearCart();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private double calculateCartTotal() {
        double total = 0.0;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            total += getItemUnitPrice(entry.getKey()) * entry.getValue();
        }
        return total;
    }

    private void animateSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null && imgPosHeaderAvatar != null && tvPosHeaderInitials != null) {
            AvatarHelper.loadAvatar(getContext(), imgPosHeaderAvatar, tvPosHeaderInitials, "Juan Dela Cruz");
        }
    }
}