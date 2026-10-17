package com.example.casavista.superadmin.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.adapters.HistoryAdapter;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.models.HistoryLog;
import com.example.casavista.network.SupabaseClient;
import com.example.casavista.network.SupabaseConfig;
import com.example.casavista.superadmin.SuperAdminMainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SuperAdminHistoryFragment extends Fragment {

    private RecyclerView rvHistoryList;
    private HistoryAdapter adapter;
    private EditText etSearchHistory;
    private TextView tvDateHeader;
    private ImageView imgTopProfile;

    private String currentPeriodFilter = "MONTH"; // TODAY, WEEK, MONTH, YEAR, ALL
    private final List<TextView> periodFilterPills = new ArrayList<>();

    private static class RawHistoryEntry {
        String id;
        String dateYyyyMmDd;
        HistoryLog.Type type;
        String guestOrActor;
        String itemOrFacility;
        String statusOrChannel;
        double amount;
        boolean isPositive;
        HistoryLog uiLog;
    }

    private final List<RawHistoryEntry> allLiveEntries = new ArrayList<>();
    private final Map<String, String> customerNames = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_super_admin_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgTopProfile = view.findViewById(R.id.imgTopProfile);
        rvHistoryList = view.findViewById(R.id.rvHistoryList);
        etSearchHistory = view.findViewById(R.id.etSearchHistory);
        tvDateHeader = view.findViewById(R.id.tvDateHeader);

        View cardAvatar = view.findViewById(R.id.cardSuperAdminHistoryAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof SuperAdminMainActivity) {
                    SuperAdminMainActivity superActivity = (SuperAdminMainActivity) getActivity();
                    superActivity.loadFragment(new SuperAdminProfileFragment());
                    BottomNavigationView navView = superActivity.findViewById(R.id.superAdminNavView);
                    if (navView != null) {
                        navView.setSelectedItemId(R.id.nav_super_profile);
                    }
                }
            });
        }

        adapter = new HistoryAdapter(new ArrayList<>());
        if (rvHistoryList != null) {
            rvHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvHistoryList.setAdapter(adapter);
        }

        injectFilterAndExportControls(view);
        setupSearchFilter();
        loadLiveHistoryFromSupabase();
    }

    private void injectFilterAndExportControls(View rootView) {
        if (getContext() == null || tvDateHeader == null) return;
        ViewGroup parent = (ViewGroup) tvDateHeader.getParent();
        if (parent == null) return;

        int headerIndex = parent.indexOfChild(tvDateHeader);
        if (headerIndex < 0) return;

        LinearLayout controlsContainer = new LinearLayout(requireContext());
        controlsContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 20, 0, 4);
        controlsContainer.setLayoutParams(containerParams);

        // Row 1: Period Filter Pills (Daily, Weekly, Monthly, Yearly, All) + Export CSV Button
        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout pillRow = new LinearLayout(requireContext());
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.setGravity(Gravity.CENTER_VERTICAL);

        String[][] periods = {
                {"TODAY", "Daily"},
                {"WEEK", "Weekly"},
                {"MONTH", "Monthly"},
                {"YEAR", "Yearly"},
                {"ALL", "All-Time"}
        };

        periodFilterPills.clear();
        for (String[] p : periods) {
            final String code = p[0];
            String label = p[1];

            TextView pill = new TextView(requireContext());
            pill.setText(label);
            pill.setTag(code);
            pill.setTextSize(12f);
            pill.setTypeface(null, Typeface.BOLD);
            pill.setPadding(32, 16, 32, 16);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(14);
            pill.setLayoutParams(lp);

            pill.setOnClickListener(v -> {
                currentPeriodFilter = code;
                updatePeriodPillStyles();
                applyPeriodAndSearchFilter();
            });

            periodFilterPills.add(pill);
            pillRow.addView(pill);
        }

        // Export CSV Button inside the toolbar row
        MaterialButton btnExportCsv = new MaterialButton(requireContext());
        btnExportCsv.setText("⬇ Export CSV");
        btnExportCsv.setTextSize(11f);
        btnExportCsv.setAllCaps(false);
        btnExportCsv.setCornerRadius(40);
        btnExportCsv.setBackgroundColor(Color.parseColor("#4D391A"));
        btnExportCsv.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams exportLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        exportLp.setMarginStart(8);
        btnExportCsv.setLayoutParams(exportLp);
        btnExportCsv.setOnClickListener(v -> exportFilteredHistoryToCsv());

        pillRow.addView(btnExportCsv);
        hsv.addView(pillRow);
        controlsContainer.addView(hsv);

        parent.addView(controlsContainer, headerIndex);
        updatePeriodPillStyles();
    }

    private void updatePeriodPillStyles() {
        if (getContext() == null) return;
        for (TextView pill : periodFilterPills) {
            String tag = (String) pill.getTag();
            if (tag != null && tag.equalsIgnoreCase(currentPeriodFilter)) {
                pill.setBackgroundResource(R.drawable.bg_btn_pos);
                pill.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_main));
            } else {
                pill.setBackgroundResource(R.drawable.bg_white);
                pill.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            }
        }
    }

    private String[] getDateRangeForPeriod(String period) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String endDate = sdf.format(cal.getTime());
        String startDate = "2000-01-01";

        switch (period) {
            case "TODAY":
                startDate = endDate;
                break;
            case "WEEK":
                cal.add(Calendar.DAY_OF_YEAR, -6);
                startDate = sdf.format(cal.getTime());
                break;
            case "MONTH":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                startDate = sdf.format(cal.getTime());
                break;
            case "YEAR":
                cal.set(Calendar.MONTH, Calendar.JANUARY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                startDate = sdf.format(cal.getTime());
                break;
            case "ALL":
            default:
                startDate = "2000-01-01";
                endDate = "2099-12-31";
                break;
        }
        return new String[]{startDate, endDate};
    }

    private void loadLiveHistoryFromSupabase() {
        if (getContext() == null) return;

        SupabaseClient.getInstance().fetchAllCustomers(new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String custJson) {
                try {
                    JSONArray cArr = new JSONArray(custJson);
                    customerNames.clear();
                    for (int i = 0; i < cArr.length(); i++) {
                        JSONObject c = cArr.getJSONObject(i);
                        String id = c.optString("customer_id", "");
                        String email = c.optString("email", "");
                        String name = (c.optString("first_name", "") + " " + c.optString("last_name", "")).trim();
                        if (!id.isEmpty() && !name.isEmpty()) customerNames.put(id, name);
                        if (!email.isEmpty() && !name.isEmpty()) customerNames.put(email.toLowerCase(Locale.US), name);
                    }
                } catch (Exception ignored) {}
                fetchBookingsAndPaymentsForHistory();
            }

            @Override
            public void onError(String errorMessage) {
                fetchBookingsAndPaymentsForHistory();
            }
        });
    }

    private void fetchBookingsAndPaymentsForHistory() {
        SupabaseClient.getInstance().fetchBookings(new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String bookingsJson) {
                if (!isAdded() || getContext() == null) return;
                List<RawHistoryEntry> builtEntries = new ArrayList<>();

                try {
                    JSONArray bArr = new JSONArray(bookingsJson);
                    for (int i = 0; i < bArr.length(); i++) {
                        JSONObject b = bArr.getJSONObject(i);
                        String bId = b.optString("booking_id", "");
                        if (bId.startsWith("MNT-")) continue;

                        String custId = b.optString("customer_id", "");
                        String custEmail = b.optString("customer_email", "");
                        String status = b.optString("status", "Pending");
                        double amount = b.optDouble("total_amount", 0.0);

                        String rawDate = b.optString("booking_date", b.optString("check_in_date", ""));
                        if (rawDate.length() >= 10) rawDate = rawDate.substring(0, 10);

                        String guestName = customerNames.get(custId);
                        if ((guestName == null || guestName.isEmpty()) && !custEmail.isEmpty()) {
                            guestName = customerNames.get(custEmail.toLowerCase(Locale.US));
                        }
                        if (guestName == null || guestName.isEmpty()) {
                            guestName = !custEmail.isEmpty() && !"null".equalsIgnoreCase(custEmail) ? custEmail : custId;
                        }

                        String courtId = b.optString("court_id", "");
                        String facilityDesc;
                        if (!courtId.isEmpty() && !"null".equalsIgnoreCase(courtId)) {
                            facilityDesc = "Sports Court (" + courtId + ") • "
                                    + formatShortTime(b.optString("start_time", "")) + "-"
                                    + formatShortTime(b.optString("end_time", ""));
                        } else {
                            JSONArray rIds = b.optJSONArray("room_ids");
                            String rLabel = (rIds != null && rIds.length() > 0) ? rIds.optString(0, "RM-101") : b.optString("room_id", "RM-101");
                            facilityDesc = "Villa Suite (" + rLabel + ") • In: " + b.optString("check_in_date", rawDate);
                        }

                        RawHistoryEntry entry = new RawHistoryEntry();
                        entry.id = bId;
                        entry.dateYyyyMmDd = rawDate;
                        entry.guestOrActor = guestName;
                        entry.itemOrFacility = facilityDesc;
                        entry.statusOrChannel = status;
                        entry.amount = amount;

                        String formattedAmt;
                        if ("Confirmed".equalsIgnoreCase(status) || "Checked-In".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                            entry.type = HistoryLog.Type.PAYMENT;
                            entry.isPositive = true;
                            formattedAmt = String.format(Locale.US, "+₱%,.2f", amount);
                        } else if ("Cancelled".equalsIgnoreCase(status)) {
                            entry.type = HistoryLog.Type.REFUND;
                            entry.isPositive = false;
                            formattedAmt = String.format(Locale.US, "₱%,.2f (Cancelled)", amount);
                        } else {
                            entry.type = HistoryLog.Type.BOOKING;
                            entry.isPositive = false;
                            formattedAmt = String.format(Locale.US, "₱%,.2f (Pending)", amount);
                        }

                        String titleHtml = "<b>" + guestName + "</b> — " + facilityDesc;
                        String subText = "Booking #" + bId + " • Status: " + status;
                        String processedBy = "Customer ID: " + custId;

                        entry.uiLog = new HistoryLog(
                                bId,
                                entry.type,
                                titleHtml,
                                subText,
                                formattedAmt,
                                processedBy,
                                rawDate
                        );
                        builtEntries.add(entry);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                fetchPosLedgerAndFinalize(builtEntries);
            }

            @Override
            public void onError(String errorMessage) {
                fetchPosLedgerAndFinalize(new ArrayList<>());
            }
        });
    }

    private void fetchPosLedgerAndFinalize(List<RawHistoryEntry> currentEntries) {
        new Thread(() -> {
            try {
                URL url = new URL(SupabaseConfig.FASTAPI_BACKEND_URL + "/reports/pos-sales-ledger");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("X-Staff-Token", "CVRMS-SECURE-STAFF-TOKEN-2026");
                conn.setRequestProperty("Authorization", "Basic Y3ZybXNfcHJkX3NlcnZpY2VfYWNjb3VudDpTV1VfUm9vdDIwMjYhIQ==");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line.trim());

                    JSONArray posArr = new JSONArray(sb.toString());
                    for (int i = 0; i < posArr.length(); i++) {
                        JSONObject p = posArr.getJSONObject(i);
                        String orderId = p.optString("order_id", "ORD");
                        String rawDate = p.optString("order_date", "");
                        if (rawDate.length() >= 10) rawDate = rawDate.substring(0, 10);

                        String prodName = p.optString("product_name", "POS Item");
                        int qty = p.optInt("quantity", 1);
                        double subtotal = p.optDouble("line_subtotal", 0.0);
                        String cashierName = p.optString("cashier_staff_name", "Cashier Staff");
                        String custName = p.optString("customer_name", "Walk-In Customer");

                        RawHistoryEntry entry = new RawHistoryEntry();
                        entry.id = orderId;
                        entry.dateYyyyMmDd = rawDate;
                        entry.type = HistoryLog.Type.PAYMENT;
                        entry.guestOrActor = custName;
                        entry.itemOrFacility = qty + "x " + prodName + " (POS)";
                        entry.statusOrChannel = "POS Paid";
                        entry.amount = subtotal;
                        entry.isPositive = true;

                        entry.uiLog = new HistoryLog(
                                orderId,
                                HistoryLog.Type.PAYMENT,
                                "<b>" + custName + "</b> purchased " + qty + "x " + prodName,
                                "POS Order #" + orderId,
                                String.format(Locale.US, "+₱%,.2f", subtotal),
                                "Processed by " + cashierName,
                                rawDate
                        );
                        currentEntries.add(entry);
                    }
                }
            } catch (Exception ignored) {}

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null) return;
                    allLiveEntries.clear();
                    allLiveEntries.addAll(currentEntries);
                    applyPeriodAndSearchFilter();
                });
            }
        }).start();
    }

    private void applyPeriodAndSearchFilter() {
        if (getContext() == null || adapter == null) return;

        String[] range = getDateRangeForPeriod(currentPeriodFilter);
        String startDate = range[0];
        String endDate = range[1];

        List<HistoryLog> periodFiltered = new ArrayList<>();
        double totalConfirmedSalesInPeriod = 0.0;
        int confirmedCount = 0;

        for (RawHistoryEntry entry : allLiveEntries) {
            String d = entry.dateYyyyMmDd;
            boolean inRange = (d == null || d.length() < 10)
                    || (d.compareTo(startDate) >= 0 && d.compareTo(endDate) <= 0);
            if (inRange) {
                periodFiltered.add(entry.uiLog);
                if (entry.isPositive) {
                    totalConfirmedSalesInPeriod += entry.amount;
                    confirmedCount++;
                }
            }
        }

        if (tvDateHeader != null) {
            String periodLabel;
            switch (currentPeriodFilter) {
                case "TODAY":
                    periodLabel = "DAILY (" + startDate + ")";
                    break;
                case "WEEK":
                    periodLabel = "WEEKLY (" + startDate + " → " + endDate + ")";
                    break;
                case "MONTH":
                    periodLabel = "MONTHLY (" + startDate + " → " + endDate + ")";
                    break;
                case "YEAR":
                    periodLabel = "YEARLY (" + startDate.substring(0, 4) + ")";
                    break;
                default:
                    periodLabel = "ALL-TIME LEDGER";
                    break;
            }
            tvDateHeader.setText(String.format(Locale.US, "%s • %d RECORDS • CONFIRMED SALES: ₱%,.2f",
                    periodLabel, periodFiltered.size(), totalConfirmedSalesInPeriod));
        }

        adapter.updateData(periodFiltered);
        if (etSearchHistory != null && etSearchHistory.getText() != null) {
            adapter.filter(etSearchHistory.getText().toString());
        }
    }

    private void exportFilteredHistoryToCsv() {
        if (getContext() == null) return;

        String[] range = getDateRangeForPeriod(currentPeriodFilter);
        String startDate = range[0];
        String endDate = range[1];
        String searchQuery = (etSearchHistory != null && etSearchHistory.getText() != null)
                ? etSearchHistory.getText().toString().trim().toLowerCase(Locale.US) : "";

        List<RawHistoryEntry> toExport = new ArrayList<>();
        for (RawHistoryEntry entry : allLiveEntries) {
            String d = entry.dateYyyyMmDd;
            boolean inRange = (d == null || d.length() < 10)
                    || (d.compareTo(startDate) >= 0 && d.compareTo(endDate) <= 0);
            if (!inRange) continue;

            if (!searchQuery.isEmpty()) {
                String combined = (entry.id + " " + entry.guestOrActor + " " + entry.itemOrFacility + " " + entry.statusOrChannel).toLowerCase(Locale.US);
                if (!combined.contains(searchQuery)) continue;
            }
            toExport.add(entry);
        }

        if (toExport.isEmpty()) {
            Toast.makeText(requireContext(), "No records in the current filter to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Reference ID,Date,Category,Guest / Customer,Facility / Item Details,Status,Confirmed Sale Amount (PHP)\n");

        double grandTotalSales = 0.0;
        for (RawHistoryEntry e : toExport) {
            double confirmedAmt = e.isPositive ? e.amount : 0.0;
            grandTotalSales += confirmedAmt;

            csv.append(escapeCsv(e.id)).append(",")
                    .append(escapeCsv(e.dateYyyyMmDd)).append(",")
                    .append(escapeCsv(e.type.name())).append(",")
                    .append(escapeCsv(e.guestOrActor)).append(",")
                    .append(escapeCsv(e.itemOrFacility)).append(",")
                    .append(escapeCsv(e.statusOrChannel)).append(",")
                    .append(String.format(Locale.US, "%.2f", confirmedAmt)).append("\n");
        }
        csv.append(",,,,,TOTAL CONFIRMED SALES (").append(currentPeriodFilter).append("),")
                .append(String.format(Locale.US, "%.2f", grandTotalSales)).append("\n");

        String fileName = "CasaVista_Sales_Bookings_" + currentPeriodFilter + "_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";

        try {
            String savedLocation;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            os.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
                savedLocation = "Downloads/" + fileName;
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File outFile = new File(dir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                }
                savedLocation = outFile.getAbsolutePath();
            }

            final String finalCsvText = csv.toString();
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("CSV Export Complete")
                    .setMessage("Exported " + toExport.size() + " records (" + currentPeriodFilter + " filter)\n"
                            + "Total Confirmed Sales: " + String.format(Locale.US, "₱%,.2f", grandTotalSales)
                            + "\n\nSaved to: " + savedLocation)
                    .setPositiveButton("Share / Open CSV", (dialog, which) -> {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.setType("text/csv");
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, finalCsvText);
                        startActivity(Intent.createChooser(sendIntent, "Share CSV Report"));
                    })
                    .setNegativeButton("Done", null)
                    .show();

        } catch (Exception ex) {
            Toast.makeText(requireContext(), "CSV Export failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        String clean = Html.fromHtml(val, Html.FROM_HTML_MODE_LEGACY).toString().replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }

    private String formatShortTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty() || "null".equalsIgnoreCase(rawTime)) return "";
        return rawTime.length() >= 5 ? rawTime.substring(0, 5) : rawTime;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileImage();
        loadLiveHistoryFromSupabase();
    }

    private void loadProfileImage() {
        if (imgTopProfile == null || getContext() == null) return;

        File avatarFile = new File(requireContext().getFilesDir(), "super_admin_avatar.jpg");
        if (avatarFile.exists()) {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
            if (bitmap != null) {
                imgTopProfile.setImageBitmap(bitmap);
                return;
            }
        }

        AvatarHelper.loadAvatar(getContext(), imgTopProfile, null, "Juan Dela Cruz");
    }

    private void setupSearchFilter() {
        if (etSearchHistory != null) {
            etSearchHistory.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (adapter != null && rvHistoryList != null) {
                        rvHistoryList.setAlpha(0.4f);
                        adapter.filter(s.toString());
                        rvHistoryList.animate().alpha(1.0f).setDuration(150).start();
                    }
                }

                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
}