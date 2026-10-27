package com.example.casavista.superadmin.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.casavista.R;
import com.example.casavista.data.ResortRepository;
import com.example.casavista.helpers.AdminAnnouncementHelper;
import com.example.casavista.helpers.AvatarHelper;
import com.example.casavista.models.CatalogItem;
import com.example.casavista.network.SupabaseClient;
import com.example.casavista.superadmin.SuperAdminMainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SuperAdminDashboardFragment extends Fragment {

    private static final String MAINT_PREFS = "AdminMaintenanceSchedulePrefs";

    private MaterialButton btnDashCreatePost;
    private MaterialButton btnDashUpdateRoomPhoto;
    private TextView tvManageFacilities;
    private ImageView imgTopProfile;

    private MaterialButton btnFilterToday, btnFilterWeek, btnFilterMonth, btnFilterYear;

    private TextView tvRevenueTitle, tvRevenueValue, tvRevenueSubtext;
    private TextView tvPendingPaymentsTitle, tvPendingPaymentsCount, tvPendingPaymentsAmount;
    private TextView tvRoomsBookedTitle, tvRoomsBookedCount, tvRoomsBookedSubtext;
    private TextView tvCourtHoursTitle, tvCourtHoursValue, tvCourtHoursSubtext;

    private Uri selectedImageUri = null;
    private ImageView dialogPreviewImage;
    private LinearLayout dialogUploadPrompt;
    private ImageButton dialogBtnRemoveImage;

    private Uri selectedRoomImageUri = null;
    private ImageView dialogRoomPreviewImage;
    private LinearLayout dialogRoomUploadPrompt;
    private ImageButton dialogRoomBtnRemoveImage;

    private String currentFilterType = "TODAY";
    private boolean isRefreshingData = false;

    private final Map<String, String> liveRoomDbStatuses = new HashMap<>();
    private final Set<String> todayBookedFacilities = new HashSet<>();
    private final Map<String, Integer> upcomingBookingsCountPerRoom = new HashMap<>();

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    updateImagePreviewState();
                }
            });

    private final ActivityResultLauncher<String> roomImagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedRoomImageUri = uri;
                    updateRoomImagePreviewState();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_super_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgTopProfile = view.findViewById(R.id.imgTopProfile);

        View cardAvatar = view.findViewById(R.id.cardSuperAdminAvatar);
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

        btnDashCreatePost = view.findViewById(R.id.btnDashCreatePost);
        btnDashUpdateRoomPhoto = view.findViewById(R.id.btnDashUpdateRoomPhoto);
        tvManageFacilities = view.findViewById(R.id.tvManageFacilities);

        btnFilterToday = view.findViewById(R.id.btnFilterToday);
        btnFilterWeek = view.findViewById(R.id.btnFilterWeek);
        btnFilterMonth = view.findViewById(R.id.btnFilterMonth);
        btnFilterYear = view.findViewById(R.id.btnFilterYear);

        tvRevenueTitle = view.findViewById(R.id.tvRevenueTitle);
        tvRevenueValue = view.findViewById(R.id.tvRevenueValue);
        tvRevenueSubtext = view.findViewById(R.id.tvRevenueSubtext);

        tvPendingPaymentsTitle = view.findViewById(R.id.tvPendingPaymentsTitle);
        tvPendingPaymentsCount = view.findViewById(R.id.tvPendingPaymentsCount);
        tvPendingPaymentsAmount = view.findViewById(R.id.tvPendingPaymentsAmount);

        tvRoomsBookedTitle = view.findViewById(R.id.tvRoomsBookedTitle);
        tvRoomsBookedCount = view.findViewById(R.id.tvRoomsBookedCount);
        tvRoomsBookedSubtext = view.findViewById(R.id.tvRoomsBookedSubtext);

        tvCourtHoursTitle = view.findViewById(R.id.tvCourtHoursTitle);
        tvCourtHoursValue = view.findViewById(R.id.tvCourtHoursValue);
        tvCourtHoursSubtext = view.findViewById(R.id.tvCourtHoursSubtext);

        setupFilterListeners();
        setupFacilityClickListeners(view);

        if (btnDashCreatePost != null) {
            btnDashCreatePost.setOnClickListener(v -> {
                animateSelection(v);
                showCreatePostDialog();
            });
        }

        if (btnDashUpdateRoomPhoto != null) {
            btnDashUpdateRoomPhoto.setOnClickListener(v -> {
                animateSelection(v);
                showUpdateRoomPhotoDialog();
            });
        }

        View cardRevenue = view.findViewById(R.id.cardRevenue);
        if (cardRevenue != null) {
            cardRevenue.setOnClickListener(v -> {
                animateSelection(v);
                String val = tvRevenueValue != null ? tvRevenueValue.getText().toString() : "₱0.00";
                Toast.makeText(getContext(), "Confirmed Revenue (" + currentFilterType + "): " + val, Toast.LENGTH_SHORT).show();
            });
        }

        View cardPending = view.findViewById(R.id.cardPending);
        if (cardPending != null) {
            cardPending.setOnClickListener(v -> {
                animateSelection(v);
                String count = tvPendingPaymentsCount != null ? tvPendingPaymentsCount.getText().toString() : "0";
                String amt = tvPendingPaymentsAmount != null ? tvPendingPaymentsAmount.getText().toString() : "₱0";
                Toast.makeText(getContext(), count + " Pending Bookings: " + amt, Toast.LENGTH_SHORT).show();
            });
        }

        refreshAllDashboardData(currentFilterType);
    }

    private String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String getTomorrowDateString() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    private String[] getDateRangeForFilter(String filterType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String endDate = sdf.format(cal.getTime());
        String startDate = endDate;

        switch (filterType) {
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
        }
        return new String[]{startDate, endDate};
    }

    private void refreshAllDashboardData(String filterType) {
        if (getContext() == null || isRefreshingData) return;
        isRefreshingData = true;
        currentFilterType = filterType;

        fadeAnimateView(tvRevenueValue);
        fadeAnimateView(tvPendingPaymentsCount);
        fadeAnimateView(tvRoomsBookedCount);
        fadeAnimateView(tvCourtHoursValue);

        if (tvRevenueTitle != null) {
            switch (filterType) {
                case "TODAY":
                    tvRevenueTitle.setText("Today's revenue");
                    break;
                case "WEEK":
                    tvRevenueTitle.setText("This week's revenue");
                    break;
                case "MONTH":
                    tvRevenueTitle.setText("This month's revenue");
                    break;
                case "YEAR":
                    tvRevenueTitle.setText("This year's revenue");
                    break;
            }
        }

        final String todayStr = getTodayDateString();
        String[] range = getDateRangeForFilter(filterType);
        final String startDate = range[0];
        final String endDate = range[1];

        SupabaseClient.getInstance().fetchRooms(new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String roomsJson) {
                if (!isAdded() || getContext() == null) {
                    isRefreshingData = false;
                    return;
                }
                try {
                    JSONArray rooms = new JSONArray(roomsJson);
                    for (int i = 0; i < rooms.length(); i++) {
                        JSONObject r = rooms.getJSONObject(i);
                        String roomId = r.optString("room_id", "");
                        String status = r.optString("status", "Ready");
                        String uiName = mapDbIdToUiFacility(roomId);
                        if (uiName != null) {
                            liveRoomDbStatuses.put(uiName, status);
                        }
                    }
                } catch (Exception ignored) {}
                fetchBookingsAndComputeMetrics(todayStr, startDate, endDate);
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) {
                    isRefreshingData = false;
                    return;
                }
                fetchBookingsAndComputeMetrics(todayStr, startDate, endDate);
            }
        });
    }

    private void fetchBookingsAndComputeMetrics(String todayStr, String startDate, String endDate) {
        SupabaseClient.getInstance().fetchBookings(new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                isRefreshingData = false;
                if (!isAdded() || getContext() == null) return;
                try {
                    JSONArray array = new JSONArray(jsonResponse);
                    int pendingCount = 0;
                    double pendingAmount = 0.0;
                    double confirmedBookingsRevenue = 0.0;
                    int confirmedCount = 0;
                    double totalCourtHours = 0.0;
                    int courtBookingsCount = 0;

                    todayBookedFacilities.clear();
                    upcomingBookingsCountPerRoom.clear();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject b = array.getJSONObject(i);
                        String bId = b.optString("booking_id", "");
                        String status = b.optString("status", "Pending");
                        double amount = b.optDouble("total_amount", 0.0);
                        String createdOrBookingDate = b.optString("booking_date", b.optString("check_in_date", ""));
                        if (createdOrBookingDate.length() >= 10) {
                            createdOrBookingDate = createdOrBookingDate.substring(0, 10);
                        }

                        boolean isMaintenanceBlock = bId.startsWith("MNT-");
                        boolean inFilterRange = isDateWithinRange(createdOrBookingDate, startDate, endDate);

                        if (!isMaintenanceBlock) {
                            if ("Pending".equalsIgnoreCase(status)) {
                                pendingCount++;
                                pendingAmount += amount;
                            } else if ("Confirmed".equalsIgnoreCase(status)
                                    || "Checked-In".equalsIgnoreCase(status)
                                    || "Completed".equalsIgnoreCase(status)) {
                                if (inFilterRange) {
                                    confirmedBookingsRevenue += amount;
                                    confirmedCount++;
                                }
                            }
                        }

                        if ("Confirmed".equalsIgnoreCase(status) || "Checked-In".equalsIgnoreCase(status)) {
                            String checkIn = b.optString("check_in_date", "");
                            String checkOut = b.optString("check_out_date", "");

                            if (checkIn.length() >= 10 && checkOut.length() >= 10) {
                                String inDate = checkIn.substring(0, 10);
                                String outDate = checkOut.substring(0, 10);

                                boolean occupiesToday = todayStr.compareTo(inDate) >= 0 && todayStr.compareTo(outDate) < 0;
                                boolean isFutureReservation = inDate.compareTo(todayStr) > 0;

                                JSONArray rIds = b.optJSONArray("room_ids");
                                if (rIds != null && rIds.length() > 0) {
                                    for (int j = 0; j < rIds.length(); j++) {
                                        String uiRoom = mapDbIdToUiFacility(rIds.optString(j));
                                        if (uiRoom != null) {
                                            if (occupiesToday) {
                                                if (isMaintenanceBlock) {
                                                    saveLocalMaintenanceWindow(uiRoom, "Maintenance", inDate, outDate, bId);
                                                } else {
                                                    todayBookedFacilities.add(uiRoom);
                                                }
                                            } else if (isFutureReservation && !isMaintenanceBlock) {
                                                int prev = upcomingBookingsCountPerRoom.containsKey(uiRoom) ? upcomingBookingsCountPerRoom.get(uiRoom) : 0;
                                                upcomingBookingsCountPerRoom.put(uiRoom, prev + 1);
                                            }
                                        }
                                    }
                                } else {
                                    String uiRoom = mapDbIdToUiFacility(b.optString("room_id", ""));
                                    if (uiRoom != null) {
                                        if (occupiesToday) {
                                            if (isMaintenanceBlock) {
                                                saveLocalMaintenanceWindow(uiRoom, "Maintenance", inDate, outDate, bId);
                                            } else {
                                                todayBookedFacilities.add(uiRoom);
                                            }
                                        } else if (isFutureReservation && !isMaintenanceBlock) {
                                            int prev = upcomingBookingsCountPerRoom.containsKey(uiRoom) ? upcomingBookingsCountPerRoom.get(uiRoom) : 0;
                                            upcomingBookingsCountPerRoom.put(uiRoom, prev + 1);
                                        }
                                    }
                                }
                            }

                            String courtId = b.optString("court_id", "");
                            if (!courtId.isEmpty() && !"null".equalsIgnoreCase(courtId)) {
                                if (createdOrBookingDate.equals(todayStr)) {
                                    String uiCourt = mapDbIdToUiFacility(courtId);
                                    if (uiCourt != null) {
                                        todayBookedFacilities.add(uiCourt);
                                    }
                                }
                            }
                        }

                        String courtId = b.optString("court_id", "");
                        if (!isMaintenanceBlock && !courtId.isEmpty() && !"null".equalsIgnoreCase(courtId)
                                && !"Cancelled".equalsIgnoreCase(status) && inFilterRange) {
                            String st = b.optString("start_time", "");
                            String et = b.optString("end_time", "");
                            double hrs = calculateHoursBetween(st, et);
                            if (hrs > 0) {
                                totalCourtHours += hrs;
                                courtBookingsCount++;
                            }
                        }
                    }

                    autoHealStaleRoomStatuses();

                    if (tvPendingPaymentsCount != null) {
                        tvPendingPaymentsCount.setText(String.valueOf(pendingCount));
                    }
                    if (tvPendingPaymentsAmount != null) {
                        tvPendingPaymentsAmount.setText(pendingCount > 0
                                ? String.format(Locale.US, "₱%,.0f awaiting", pendingAmount)
                                : "₱0 pending");
                    }

                    if (tvCourtHoursValue != null) {
                        if (totalCourtHours == (long) totalCourtHours) {
                            tvCourtHoursValue.setText(String.format(Locale.US, "%d hrs", (long) totalCourtHours));
                        } else {
                            tvCourtHoursValue.setText(String.format(Locale.US, "%.1f hrs", totalCourtHours));
                        }
                    }
                    if (tvCourtHoursSubtext != null) {
                        tvCourtHoursSubtext.setText(courtBookingsCount + (courtBookingsCount == 1 ? " reservation" : " reservations"));
                    }

                    updateFacilityStatus();

                    final double fallbackConfirmedRevenue = confirmedBookingsRevenue;
                    final int fallbackConfirmedCount = confirmedCount;
                    fetchActualPnLWithBookingFallback(startDate, endDate, fallbackConfirmedRevenue, fallbackConfirmedCount);

                } catch (Exception e) {
                    e.printStackTrace();
                    updateFacilityStatus();
                }
            }

            @Override
            public void onError(String errorMessage) {
                isRefreshingData = false;
                if (!isAdded() || getContext() == null) return;
                updateFacilityStatus();
                fetchActualPnLWithBookingFallback(startDate, endDate, 0.0, 0);
            }
        });
    }

    private void autoHealStaleRoomStatuses() {
        if (getContext() == null) return;
        for (int i = 1; i <= 3; i++) {
            final String roomKey = "Room " + i;
            boolean hasActiveBookingToday = todayBookedFacilities.contains(roomKey);
            String maintStatus = getActiveMaintenanceStatusForToday(roomKey);
            boolean hasAdminBlock = !"Available".equalsIgnoreCase(maintStatus);

            if (!hasActiveBookingToday && !hasAdminBlock) {
                ResortRepository.getInstance().setFacilityStatusOverride(requireContext(), roomKey, "Available");
                String dbStatus = liveRoomDbStatuses.get(roomKey);
                if ("Occupied".equalsIgnoreCase(dbStatus)) {
                    liveRoomDbStatuses.put(roomKey, "Ready");
                    String dbRoomId = mapUiFacilityToDbRoomId(roomKey);
                    if (dbRoomId != null) {
                        String roomNum = dbRoomId.replace("RM-", "");
                        String roomType = "RM-103".equals(dbRoomId) ? "Deluxe" : "Standard";
                        double rate = "RM-103".equals(dbRoomId) ? 3500.0 : 2500.0;
                        SupabaseClient.getInstance().updateRoomStatus(dbRoomId, roomNum, rate, 4, "Ready", roomType, new SupabaseClient.DataCallback() {
                            @Override public void onSuccess(String s) {}
                            @Override public void onError(String e) {}
                        });
                    }
                }
            }
        }
    }

    private void fetchActualPnLWithBookingFallback(String startDate, String endDate, double confirmedBookingsRev, int confirmedBookingsCount) {
        SupabaseClient.getInstance().fetchRevenuePnL(startDate, endDate, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                if (!isAdded() || getContext() == null) return;
                try {
                    JSONObject obj = new JSONObject(jsonResponse);
                    JSONObject breakdown = obj.optJSONObject("financial_breakdown");
                    int txCount = obj.optInt("total_transactions", 0);

                    if (breakdown != null) {
                        double netRev = breakdown.optDouble("net_operating_revenue", 0.0);
                        double posRev = breakdown.optDouble("gross_pos_revenue", 0.0);
                        double bookingPaymentRev = breakdown.optDouble("gross_booking_revenue", 0.0);

                        double effectiveBookingRev = Math.max(bookingPaymentRev, confirmedBookingsRev);
                        double displayRevenue = Math.max(netRev, effectiveBookingRev + posRev);
                        int displayTxCount = Math.max(txCount, confirmedBookingsCount);

                        if (tvRevenueValue != null) {
                            tvRevenueValue.setText(String.format(Locale.US, "₱%,.2f", displayRevenue));
                        }
                        if (tvRevenueSubtext != null) {
                            tvRevenueSubtext.setText(String.format(Locale.US, "%d confirmed %s (POS: ₱%,.0f)",
                                    displayTxCount,
                                    displayTxCount == 1 ? "sale" : "sales",
                                    posRev));
                        }
                    }
                } catch (Exception e) {
                    if (tvRevenueValue != null) {
                        tvRevenueValue.setText(String.format(Locale.US, "₱%,.2f", confirmedBookingsRev));
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                if (tvRevenueValue != null) {
                    tvRevenueValue.setText(String.format(Locale.US, "₱%,.2f", confirmedBookingsRev));
                }
                if (tvRevenueSubtext != null) {
                    tvRevenueSubtext.setText(confirmedBookingsCount + " confirmed bookings");
                }
            }
        });
    }

    private boolean isDateWithinRange(String targetDate, String startDate, String endDate) {
        if (targetDate == null || targetDate.length() < 10) return true;
        return targetDate.compareTo(startDate) >= 0 && targetDate.compareTo(endDate) <= 0;
    }

    private double calculateHoursBetween(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isEmpty() || endTime.isEmpty()) return 0.0;
        try {
            String[] sParts = startTime.split(":");
            String[] eParts = endTime.split(":");
            double sHours = Integer.parseInt(sParts[0]) + (Integer.parseInt(sParts[1]) / 60.0);
            double eHours = Integer.parseInt(eParts[0]) + (Integer.parseInt(eParts[1]) / 60.0);
            return Math.max(0.0, eHours - sHours);
        } catch (Exception e) {
            return 1.0;
        }
    }

    private String mapDbIdToUiFacility(String dbId) {
        if (dbId == null || dbId.isEmpty() || "null".equalsIgnoreCase(dbId)) return null;
        String clean = dbId.trim().toUpperCase();
        if (clean.contains("101") || clean.equals("ROOM 1") || clean.equals("RM-1")) return "Room 1";
        if (clean.contains("102") || clean.equals("ROOM 2") || clean.equals("RM-2")) return "Room 2";
        if (clean.contains("103") || clean.equals("ROOM 3") || clean.equals("RM-3")) return "Room 3";
        if (clean.contains("CRT-01") || clean.contains("COURT 1") || clean.equals("PB-1")) return "Court 1";
        if (clean.contains("CRT-02") || clean.contains("COURT 2") || clean.equals("PB-2")) return "Court 2";
        if (clean.contains("CRT-03") || clean.contains("COURT 3") || clean.contains("BK") || clean.contains("BASKET")) return "Court 3";
        return null;
    }

    private String mapUiFacilityToDbRoomId(String facilityName) {
        if ("Room 1".equalsIgnoreCase(facilityName)) return "RM-101";
        if ("Room 2".equalsIgnoreCase(facilityName)) return "RM-102";
        if ("Room 3".equalsIgnoreCase(facilityName)) return "RM-103";
        return null;
    }

    private String mapUiFacilityToDbCourtId(String facilityName) {
        if ("Court 1".equalsIgnoreCase(facilityName)) return "CRT-01";
        if ("Court 2".equalsIgnoreCase(facilityName)) return "CRT-02";
        if ("Court 3".equalsIgnoreCase(facilityName)) return "CRT-03";
        return null;
    }

    private void saveLocalMaintenanceWindow(String facility, String status, String startDate, String endDate, String maintBookingId) {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(facility + "_status", status);
        editor.putString(facility + "_start", startDate);
        editor.putString(facility + "_end", endDate);
        if (maintBookingId != null) {
            editor.putString(facility + "_booking_id", maintBookingId);
        }
        editor.apply();

        ResortRepository.getInstance().setFacilityStatusOverride(requireContext(), facility, status);
    }

    private void clearLocalMaintenanceWindow(String facility) {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(facility + "_status")
                .remove(facility + "_start")
                .remove(facility + "_end")
                .remove(facility + "_booking_id")
                .apply();
        ResortRepository.getInstance().setFacilityStatusOverride(requireContext(), facility, "Available");
    }

    private String getActiveMaintenanceStatusForToday(String facility) {
        if (getContext() == null) return "Available";
        SharedPreferences prefs = requireContext().getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);
        String status = prefs.getString(facility + "_status", "");
        String start = prefs.getString(facility + "_start", "");
        String end = prefs.getString(facility + "_end", "");

        if (status != null && !status.isEmpty() && !"Available".equalsIgnoreCase(status)) {
            String today = getTodayDateString();
            if (!start.isEmpty() && !end.isEmpty()) {
                if (today.compareTo(start) >= 0 && today.compareTo(end) <= 0) {
                    return status;
                } else if (today.compareTo(start) < 0) {
                    return "Scheduled:" + status;
                } else {
                    return "Available";
                }
            }
            return status;
        }
        return "Available";
    }

    private void setupFacilityClickListeners(View view) {
        if (view == null) return;
        View.OnClickListener goToRoomsTab = v -> {
            if (getActivity() instanceof SuperAdminMainActivity) {
                SuperAdminMainActivity activity = (SuperAdminMainActivity) getActivity();
                BottomNavigationView navView = activity.findViewById(R.id.superAdminNavView);
                if (navView != null) {
                    navView.setSelectedItemId(R.id.nav_super_rooms);
                } else {
                    activity.loadFragment(new SuperAdminRoomsFragment());
                }
            }
        };

        View tvManage = view.findViewById(R.id.tvManageFacilities);
        if (tvManage != null) tvManage.setOnClickListener(goToRoomsTab);

        View room1 = view.findViewById(R.id.room1Card);
        if (room1 != null) room1.setOnClickListener(goToRoomsTab);
        View room2 = view.findViewById(R.id.room2Card);
        if (room2 != null) room2.setOnClickListener(goToRoomsTab);
        View room3 = view.findViewById(R.id.room3Card);
        if (room3 != null) room3.setOnClickListener(goToRoomsTab);
        View court1 = view.findViewById(R.id.court1Card);
        if (court1 != null) court1.setOnClickListener(goToRoomsTab);
        View court2 = view.findViewById(R.id.court2Card);
        if (court2 != null) court2.setOnClickListener(goToRoomsTab);
        View court3 = view.findViewById(R.id.court3Card);
        if (court3 != null) court3.setOnClickListener(goToRoomsTab);
    }

    private void showManageFacilitiesDialog(String defaultFacility) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manage_facilities, null);
        Spinner spinnerFacility = dialogView.findViewById(R.id.spinnerFacilityList);
        RadioButton radioAvailable = dialogView.findViewById(R.id.radioStatusAvailable);
        RadioButton radioMaintenance = dialogView.findViewById(R.id.radioStatusMaintenance);
        RadioButton radioCleaning = dialogView.findViewById(R.id.radioStatusCleaning);
        RadioButton radioUnavailable = dialogView.findViewById(R.id.radioStatusUnavailable);

        final String[] selectedStartDate = {getTodayDateString()};
        final String[] selectedEndDate = {getTomorrowDateString()};

        View layoutDateRange = dialogView.findViewById(R.id.layoutMaintenanceDates);
        View cardMaintStartDate = dialogView.findViewById(R.id.cardMaintStartDate);
        TextView tvMaintStartDate = dialogView.findViewById(R.id.tvMaintStartDate);
        View cardMaintEndDate = dialogView.findViewById(R.id.cardMaintEndDate);
        TextView tvMaintEndDate = dialogView.findViewById(R.id.tvMaintEndDate);

        if (tvMaintStartDate != null) tvMaintStartDate.setText(selectedStartDate[0]);
        if (tvMaintEndDate != null) tvMaintEndDate.setText(selectedEndDate[0]);

        if (cardMaintStartDate != null) {
            cardMaintStartDate.setOnClickListener(v -> showDatePicker(selectedStartDate[0], picked -> {
                selectedStartDate[0] = picked;
                if (tvMaintStartDate != null) tvMaintStartDate.setText(picked);
                if (selectedEndDate[0].compareTo(picked) <= 0) {
                    selectedEndDate[0] = addOneDay(picked);
                    if (tvMaintEndDate != null) tvMaintEndDate.setText(selectedEndDate[0]);
                }
            }));
        }

        if (cardMaintEndDate != null) {
            cardMaintEndDate.setOnClickListener(v -> showDatePicker(selectedEndDate[0], picked -> {
                if (picked.compareTo(selectedStartDate[0]) <= 0) {
                    Toast.makeText(requireContext(), "End date must be after start date.", Toast.LENGTH_SHORT).show();
                    selectedEndDate[0] = addOneDay(selectedStartDate[0]);
                } else {
                    selectedEndDate[0] = picked;
                }
                if (tvMaintEndDate != null) tvMaintEndDate.setText(selectedEndDate[0]);
            }));
        }

        View.OnClickListener radioToggleListener = v -> {
            boolean isAvailableChecked = radioAvailable != null && radioAvailable.isChecked();
            if (layoutDateRange != null) {
                layoutDateRange.setVisibility(isAvailableChecked ? View.GONE : View.VISIBLE);
            }
        };
        if (radioAvailable != null) radioAvailable.setOnClickListener(radioToggleListener);
        if (radioMaintenance != null) radioMaintenance.setOnClickListener(radioToggleListener);
        if (radioCleaning != null) radioCleaning.setOnClickListener(radioToggleListener);
        if (radioUnavailable != null) radioUnavailable.setOnClickListener(radioToggleListener);

        List<String> facilities = Arrays.asList("Room 1", "Room 2", "Room 3", "Court 1", "Court 2", "Court 3");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, facilities);
        if (spinnerFacility != null) {
            spinnerFacility.setAdapter(adapter);
            if (defaultFacility != null && facilities.contains(defaultFacility)) {
                spinnerFacility.setSelection(facilities.indexOf(defaultFacility));
            }

            spinnerFacility.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String fac = facilities.get(position);
                    populateFacilityDialogState(fac, radioAvailable, radioMaintenance, radioCleaning, radioUnavailable,
                            layoutDateRange, tvMaintStartDate, tvMaintEndDate, selectedStartDate, selectedEndDate);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        String facilityKey = defaultFacility != null ? defaultFacility : "Room 1";
        populateFacilityDialogState(facilityKey, radioAvailable, radioMaintenance, radioCleaning, radioUnavailable,
                layoutDateRange, tvMaintStartDate, tvMaintEndDate, selectedStartDate, selectedEndDate);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage Facility Availability")
                .setView(dialogView)
                .setPositiveButton("Save Status", (dialog, which) -> {
                    String facility = spinnerFacility != null ? spinnerFacility.getSelectedItem().toString() : "Room 1";
                    String selectedReason = "Available";
                    if (radioMaintenance != null && radioMaintenance.isChecked()) selectedReason = "Maintenance";
                    else if (radioCleaning != null && radioCleaning.isChecked()) selectedReason = "Cleaning";
                    else if (radioUnavailable != null && radioUnavailable.isChecked()) selectedReason = "Unavailable";

                    final String finalReason = selectedReason;
                    final String startStr = selectedStartDate[0];
                    final String endStr = selectedEndDate[0];

                    if (todayBookedFacilities.contains(facility) && "Available".equalsIgnoreCase(finalReason)) {
                        Toast.makeText(requireContext(), facility + " has an active guest checked in today and cannot be forced to Available.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    applyFacilityStatusAndSyncSupabase(facility, finalReason, startStr, endStr);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void populateFacilityDialogState(
            String facilityKey,
            RadioButton radioAvailable,
            RadioButton radioMaintenance,
            RadioButton radioCleaning,
            RadioButton radioUnavailable,
            View layoutDateRange,
            TextView tvMaintStartDate,
            TextView tvMaintEndDate,
            String[] selectedStartDate,
            String[] selectedEndDate
    ) {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);
        String savedStatus = prefs.getString(facilityKey + "_status", "");
        String savedStart = prefs.getString(facilityKey + "_start", getTodayDateString());
        String savedEnd = prefs.getString(facilityKey + "_end", getTomorrowDateString());

        if (savedStatus == null || savedStatus.isEmpty()) {
            String dbStatus = liveRoomDbStatuses.get(facilityKey);
            if ("Maintenance".equalsIgnoreCase(dbStatus)) savedStatus = "Maintenance";
            else if ("Cleaning".equalsIgnoreCase(dbStatus)) savedStatus = "Cleaning";
            else savedStatus = "Available";
        }

        selectedStartDate[0] = savedStart;
        selectedEndDate[0] = savedEnd;
        if (tvMaintStartDate != null) tvMaintStartDate.setText(savedStart);
        if (tvMaintEndDate != null) tvMaintEndDate.setText(savedEnd);

        boolean bookedTodayByGuest = todayBookedFacilities.contains(facilityKey);

        if (radioAvailable != null) radioAvailable.setEnabled(true);
        if (radioUnavailable != null) radioUnavailable.setText("Unavailable / Occupied");

        if (bookedTodayByGuest) {
            if (radioUnavailable != null) {
                radioUnavailable.setChecked(true);
                radioUnavailable.setText("Booked Today (Guest Reserved)");
            }
            if (radioAvailable != null) radioAvailable.setEnabled(false);
            if (layoutDateRange != null) layoutDateRange.setVisibility(View.GONE);
        } else if ("Maintenance".equalsIgnoreCase(savedStatus)) {
            if (radioMaintenance != null) radioMaintenance.setChecked(true);
            if (layoutDateRange != null) layoutDateRange.setVisibility(View.VISIBLE);
        } else if ("Cleaning".equalsIgnoreCase(savedStatus)) {
            if (radioCleaning != null) radioCleaning.setChecked(true);
            if (layoutDateRange != null) layoutDateRange.setVisibility(View.VISIBLE);
        } else if ("Unavailable".equalsIgnoreCase(savedStatus)) {
            if (radioUnavailable != null) radioUnavailable.setChecked(true);
            if (layoutDateRange != null) layoutDateRange.setVisibility(View.VISIBLE);
        } else {
            if (radioAvailable != null) radioAvailable.setChecked(true);
            if (layoutDateRange != null) layoutDateRange.setVisibility(View.GONE);
        }
    }

    private void applyFacilityStatusAndSyncSupabase(String facility, String finalReason, String startDate, String endDate) {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(MAINT_PREFS, Context.MODE_PRIVATE);
        String existingMaintBookingId = prefs.getString(facility + "_booking_id", null);

        if ("Available".equalsIgnoreCase(finalReason)) {
            clearLocalMaintenanceWindow(facility);
            liveRoomDbStatuses.put(facility, "Ready");
            updateFacilityStatus();

            if (existingMaintBookingId != null && !existingMaintBookingId.isEmpty()) {
                SupabaseClient.getInstance().updateBookingStatus(existingMaintBookingId, "Cancelled", new SupabaseClient.DataCallback() {
                    @Override public void onSuccess(String s) {}
                    @Override public void onError(String e) {}
                });
            }
        } else {
            String maintBookingId = String.format(Locale.US, "MNT-%s-%04d",
                    facility.replaceAll("\\s+", "").toUpperCase(),
                    (int) (System.currentTimeMillis() % 10000));
            saveLocalMaintenanceWindow(facility, finalReason, startDate, endDate, maintBookingId);

            String mappedDb = "Maintenance".equalsIgnoreCase(finalReason) ? "Maintenance"
                    : ("Cleaning".equalsIgnoreCase(finalReason) ? "Cleaning" : "Occupied");
            liveRoomDbStatuses.put(facility, mappedDb);
            updateFacilityStatus();

            if (existingMaintBookingId != null && !existingMaintBookingId.isEmpty()) {
                SupabaseClient.getInstance().updateBookingStatus(existingMaintBookingId, "Cancelled", new SupabaseClient.DataCallback() {
                    @Override public void onSuccess(String s) {}
                    @Override public void onError(String e) {}
                });
            }

            String dbRoomId = mapUiFacilityToDbRoomId(facility);
            String dbCourtId = mapUiFacilityToDbCourtId(facility);
            SupabaseClient.getInstance().createMaintenanceBlockBooking(
                    maintBookingId,
                    dbRoomId,
                    dbCourtId,
                    startDate,
                    endDate,
                    new SupabaseClient.DataCallback() {
                        @Override
                        public void onSuccess(String response) {
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(requireContext(),
                                        facility + " blocked for " + finalReason + " (" + startDate + " to " + endDate + ")",
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override public void onError(String errorMessage) {}
                    }
            );
        }

        String dbRoomId = mapUiFacilityToDbRoomId(facility);
        if (dbRoomId != null) {
            String dbStatus = "Ready";
            if ("Maintenance".equalsIgnoreCase(finalReason)) dbStatus = "Maintenance";
            else if ("Cleaning".equalsIgnoreCase(finalReason)) dbStatus = "Cleaning";
            else if ("Unavailable".equalsIgnoreCase(finalReason)) dbStatus = "Occupied";

            String roomNum = dbRoomId.replace("RM-", "");
            String roomType = "RM-103".equals(dbRoomId) ? "Deluxe" : "Standard";
            double rate = "RM-103".equals(dbRoomId) ? 3500.0 : 2500.0;

            final String targetDbStatus = dbStatus;
            SupabaseClient.getInstance().updateRoomStatus(dbRoomId, roomNum, rate, 4, targetDbStatus, roomType, new SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    liveRoomDbStatuses.put(facility, targetDbStatus);
                    if (isAdded() && getContext() != null) {
                        updateFacilityStatus();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isAdded() && getContext() != null) {
                        updateFacilityStatus();
                    }
                }
            });
        } else {
            Toast.makeText(requireContext(), "Updated " + facility + " to " + finalReason, Toast.LENGTH_SHORT).show();
        }
    }

    private interface OnDatePickedListener {
        void onDatePicked(String yyyyMmDd);
    }

    private void showDatePicker(String currentDateStr, OnDatePickedListener listener) {
        if (getContext() == null) return;
        Calendar cal = Calendar.getInstance();
        try {
            String[] parts = currentDateStr.split("-");
            if (parts.length == 3) {
                cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
            }
        } catch (Exception ignored) {}

        DatePickerDialog dpd = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    listener.onDatePicked(formatted);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dpd.show();
    }

    private String addOneDay(String yyyyMmDd) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = sdf.parse(yyyyMmDd);
            Calendar cal = Calendar.getInstance();
            if (d != null) cal.setTime(d);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return getTomorrowDateString();
        }
    }

    private void setupFilterListeners() {
        if (btnFilterToday != null) {
            btnFilterToday.setOnClickListener(v -> {
                animateSelection(v);
                selectFilter(btnFilterToday, "TODAY");
            });
        }
        if (btnFilterWeek != null) {
            btnFilterWeek.setOnClickListener(v -> {
                animateSelection(v);
                selectFilter(btnFilterWeek, "WEEK");
            });
        }
        if (btnFilterMonth != null) {
            btnFilterMonth.setOnClickListener(v -> {
                animateSelection(v);
                selectFilter(btnFilterMonth, "MONTH");
            });
        }
        if (btnFilterYear != null) {
            btnFilterYear.setOnClickListener(v -> {
                animateSelection(v);
                selectFilter(btnFilterYear, "YEAR");
            });
        }
    }

    private void selectFilter(MaterialButton selectedButton, String filterType) {
        if (getContext() == null) return;

        List<MaterialButton> buttons = Arrays.asList(btnFilterToday, btnFilterWeek, btnFilterMonth, btnFilterYear);

        int activeBgColor = ContextCompat.getColor(getContext(), R.color.admin_accent_gold);
        int activeTextColor = ContextCompat.getColor(getContext(), R.color.button_bg);
        int inactiveBgColor = ContextCompat.getColor(getContext(), R.color.bg_card);
        int inactiveTextColor = ContextCompat.getColor(getContext(), R.color.admin_text_primary);

        for (MaterialButton btn : buttons) {
            if (btn != null) {
                if (btn == selectedButton) {
                    btn.setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
                    btn.setTextColor(activeTextColor);
                } else {
                    btn.setBackgroundTintList(ColorStateList.valueOf(inactiveBgColor));
                    btn.setTextColor(inactiveTextColor);
                }
            }
        }

        refreshAllDashboardData(filterType);
    }

    private void fadeAnimateView(View view) {
        if (view != null) {
            view.setAlpha(0.3f);
            view.animate().alpha(1.0f).setDuration(200).start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileImage();
        updateFacilityStatus();
        refreshAllDashboardData(currentFilterType);
    }

    private void updateFacilityStatus() {
        if (getView() == null || getContext() == null) return;
        View view = getView();
        int unavailableRoomsToday = 0;
        int totalUpcomingRoomReservations = 0;

        for (int i = 1; i <= 3; i++) {
            String roomKey = "Room " + i;
            View card = view.findViewById(i == 1 ? R.id.room1Card : (i == 2 ? R.id.room2Card : R.id.room3Card));
            String badgeStatus = resolveTodayFacilityBadge(roomKey);
            if (!"Open".equalsIgnoreCase(badgeStatus) && !badgeStatus.startsWith("Sched.")) {
                unavailableRoomsToday++;
            }
            if (upcomingBookingsCountPerRoom.containsKey(roomKey)) {
                totalUpcomingRoomReservations += upcomingBookingsCountPerRoom.get(roomKey);
            }
            updateCardStatusWithBadge(card, roomKey, badgeStatus);
        }

        for (int i = 1; i <= 3; i++) {
            String courtKey = "Court " + i;
            View card = view.findViewById(i == 1 ? R.id.court1Card : (i == 2 ? R.id.court2Card : R.id.court3Card));
            String badgeStatus = resolveTodayFacilityBadge(courtKey);
            updateCardStatusWithBadge(card, courtKey, badgeStatus);
        }

        if (tvRoomsBookedCount != null) {
            tvRoomsBookedCount.setText(unavailableRoomsToday + " / 3");
        }
        if (tvRoomsBookedSubtext != null) {
            int openToday = 3 - unavailableRoomsToday;
            if (openToday > 0 && totalUpcomingRoomReservations > 0) {
                tvRoomsBookedSubtext.setText(openToday + " open today • " + totalUpcomingRoomReservations + " upcoming");
            } else if (openToday > 0) {
                tvRoomsBookedSubtext.setText(openToday + " open today");
            } else {
                tvRoomsBookedSubtext.setText("All 3 occupied/blocked today");
            }
        }
    }

    private String resolveTodayFacilityBadge(String facilityName) {
        if (todayBookedFacilities.contains(facilityName)) {
            return "Booked";
        }

        String maintStatus = getActiveMaintenanceStatusForToday(facilityName);
        if (maintStatus.startsWith("Scheduled:")) {
            return "Sched. Maint";
        }
        if ("Maintenance".equalsIgnoreCase(maintStatus)) {
            return "Maintenance";
        }
        if ("Cleaning".equalsIgnoreCase(maintStatus)) {
            return "Cleaning";
        }
        if ("Unavailable".equalsIgnoreCase(maintStatus)) {
            return "Unavailable";
        }

        String dbStatus = liveRoomDbStatuses.get(facilityName);
        if (dbStatus != null) {
            if ("Maintenance".equalsIgnoreCase(dbStatus)) return "Maintenance";
            if ("Cleaning".equalsIgnoreCase(dbStatus)) return "Cleaning";
        }

        return "Open";
    }

    private void updateCardStatusWithBadge(View cardView, String name, String badgeText) {
        if (!(cardView instanceof MaterialCardView)) return;
        MaterialCardView card = (MaterialCardView) cardView;
        LinearLayout layout = (LinearLayout) card.getChildAt(0);
        if (layout != null && layout.getChildCount() >= 2) {
            TextView tvTitle = (TextView) layout.getChildAt(0);
            TextView tvBadge = (TextView) layout.getChildAt(1);
            tvTitle.setText(name);
            tvBadge.setText(badgeText);

            if ("Open".equalsIgnoreCase(badgeText)) {
                card.setCardBackgroundColor(Color.parseColor("#E2EFE0"));
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4E8D4F")));
                tvBadge.setTextColor(Color.WHITE);
            } else if ("Cleaning".equalsIgnoreCase(badgeText)
                    || "Maintenance".equalsIgnoreCase(badgeText)
                    || "Sched. Maint".equalsIgnoreCase(badgeText)) {
                card.setCardBackgroundColor(Color.parseColor("#FEF3C7"));
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D97706")));
                tvBadge.setTextColor(Color.WHITE);
            } else {
                card.setCardBackgroundColor(Color.parseColor("#F8E3DB"));
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C85A32")));
                tvBadge.setTextColor(Color.WHITE);
            }
        }
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

        AvatarHelper.loadAvatar(getContext(), imgTopProfile, null, "Dr. Lemyel Yap");
    }

    private void showCreatePostDialog() {
        selectedImageUri = null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null);
        EditText etTitle = dialogView.findViewById(R.id.etPostTitle);
        EditText etContent = dialogView.findViewById(R.id.etPostContent);

        View cardImagePicker = dialogView.findViewById(R.id.cardImagePicker);
        dialogPreviewImage = dialogView.findViewById(R.id.imgPostPreview);
        dialogUploadPrompt = dialogView.findViewById(R.id.layoutUploadPrompt);
        dialogBtnRemoveImage = dialogView.findViewById(R.id.btnRemoveImage);

        cardImagePicker.setOnClickListener(v -> {
            animateSelection(v);
            imagePickerLauncher.launch("image/*");
        });

        dialogBtnRemoveImage.setOnClickListener(v -> {
            animateSelection(v);
            selectedImageUri = null;
            updateImagePreviewState();
        });

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Publish", (dialog, which) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

                    if (!title.isEmpty() && !content.isEmpty()) {
                        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

                        String imageBase64 = null;
                        if (selectedImageUri != null) {
                            imageBase64 = com.example.casavista.helpers.ImageBase64Helper.encodeImageToBase64(requireContext(), selectedImageUri);
                        }

                        AdminAnnouncementHelper.addAnnouncement(
                                requireContext(),
                                title,
                                content,
                                currentDate,
                                imageBase64 != null ? imageBase64 : "",
                                "Super Admin"
                        );

                        Toast.makeText(getContext(), "Announcement published and synced to all devices!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Please fill in post title and content", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUpdateRoomPhotoDialog() {
        selectedRoomImageUri = null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_room_photo, null);
        Spinner spinnerCatalogItems = dialogView.findViewById(R.id.spinnerCatalogItems);
        View cardRoomImagePicker = dialogView.findViewById(R.id.cardRoomImagePicker);
        dialogRoomPreviewImage = dialogView.findViewById(R.id.imgRoomPreview);
        dialogRoomUploadPrompt = dialogView.findViewById(R.id.layoutRoomUploadPrompt);
        dialogRoomBtnRemoveImage = dialogView.findViewById(R.id.btnRemoveRoomImage);

        List<CatalogItem> catalogItems = ResortRepository.getInstance().getCatalogItems();
        List<String> itemTitles = new ArrayList<>();
        for (CatalogItem item : catalogItems) {
            itemTitles.add(item.getTitle() + " (" + item.getCategory() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, itemTitles);
        spinnerCatalogItems.setAdapter(adapter);

        cardRoomImagePicker.setOnClickListener(v -> {
            animateSelection(v);
            roomImagePickerLauncher.launch("image/*");
        });

        dialogRoomBtnRemoveImage.setOnClickListener(v -> {
            animateSelection(v);
            selectedRoomImageUri = null;
            updateRoomImagePreviewState();
        });

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save Photo", (dialog, which) -> {
                    int selectedIndex = spinnerCatalogItems.getSelectedItemPosition();
                    if (selectedIndex >= 0 && selectedIndex < catalogItems.size()) {
                        CatalogItem targetItem = catalogItems.get(selectedIndex);

                        if (selectedRoomImageUri != null) {
                            String base64 = com.example.casavista.helpers.ImageBase64Helper.encodeImageToBase64(requireContext(), selectedRoomImageUri);
                            if (base64 != null) {
                                targetItem.setImageUrl(base64);
                                ResortRepository.getInstance().updateCatalogItem(requireContext(), targetItem);
                                com.example.casavista.network.SupabaseClient.getInstance().uploadMedia(
                                        targetItem.getTitle(),
                                        "facility",
                                        base64,
                                        "image/jpeg",
                                        new com.example.casavista.network.SupabaseClient.DataCallback() {
                                            @Override
                                            public void onSuccess(String result) {
                                                if (getContext() != null) {
                                                    Toast.makeText(getContext(), "Cloud photo synced for " + targetItem.getTitle(), Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onError(String error) {
                                                // Retained locally
                                            }
                                        }
                                );
                                Toast.makeText(getContext(), "Updated photo for " + targetItem.getTitle(), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to process photo.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Please select a new photo to save", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String saveImageToInternalStorage(Uri uri, String prefix) {
        if (getContext() == null || uri == null) return null;
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File file = new File(requireContext().getFilesDir(), prefix + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateImagePreviewState() {
        if (dialogPreviewImage == null || dialogUploadPrompt == null || dialogBtnRemoveImage == null) return;

        if (selectedImageUri != null) {
            dialogPreviewImage.setImageURI(selectedImageUri);
            dialogPreviewImage.setVisibility(View.VISIBLE);
            dialogBtnRemoveImage.setVisibility(View.VISIBLE);
            dialogUploadPrompt.setVisibility(View.GONE);
        } else {
            dialogPreviewImage.setImageURI(null);
            dialogPreviewImage.setVisibility(View.GONE);
            dialogBtnRemoveImage.setVisibility(View.GONE);
            dialogUploadPrompt.setVisibility(View.VISIBLE);
        }
    }

    private void updateRoomImagePreviewState() {
        if (dialogRoomPreviewImage == null || dialogRoomUploadPrompt == null || dialogRoomBtnRemoveImage == null) return;

        if (selectedRoomImageUri != null) {
            dialogRoomPreviewImage.setImageURI(selectedRoomImageUri);
            dialogRoomPreviewImage.setVisibility(View.VISIBLE);
            dialogRoomBtnRemoveImage.setVisibility(View.VISIBLE);
            dialogRoomUploadPrompt.setVisibility(View.GONE);
        } else {
            dialogRoomPreviewImage.setImageURI(null);
            dialogRoomPreviewImage.setVisibility(View.GONE);
            dialogRoomBtnRemoveImage.setVisibility(View.GONE);
            dialogRoomUploadPrompt.setVisibility(View.VISIBLE);
        }
    }

    private void animateSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
    }
}