package com.example.casavista.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.casavista.data.ResortRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;
    private final Handler mainHandler;
    private final Random random = new Random();

    public interface AuthCallback {
        void onSuccess(String customerId, String email, JSONObject userData);
        void onError(String errorMessage);
    }

    public interface DataCallback {
        void onSuccess(String jsonArrayString);
        void onError(String errorMessage);
    }

    private SupabaseClient() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    private void applyStaffHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("X-Staff-Token", "CVRMS-SECURE-STAFF-TOKEN-2026");
        conn.setRequestProperty("Authorization", "Basic Y3ZybXNfcHJkX3NlcnZpY2VfYWNjb3VudDpTV1VfUm9vdDIwMjYhIQ==");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
    }

    public String generateUniqueBookingId(String prefix) {
        String p = (prefix != null && !prefix.isEmpty()) ? prefix : "BK";
        String timeBase36 = Long.toString(System.currentTimeMillis() % 2176782336L, 36).toUpperCase(Locale.US);
        int rand = 10 + random.nextInt(89);
        String id = p + "-" + timeBase36 + rand;
        return id.length() > 15 ? id.substring(0, 15) : id;
    }

    public void syncGoogleUser(String email, String firstName, String lastName, AuthCallback callback) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", email);
                payload.put("first_name", (firstName != null && !firstName.trim().isEmpty()) ? firstName : "Guest");
                payload.put("last_name", (lastName != null && !lastName.trim().isEmpty()) ? lastName : "User");

                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/customers/sync-google";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                applyStaffHeaders(conn);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                Log.d(TAG, "Google Sync HTTP " + code + ": " + respStr);

                if (code >= 200 && code < 300) {
                    JSONObject resObj = new JSONObject(respStr);
                    String customerId = resObj.optString("customer_id", "");
                    if (!customerId.isEmpty()) {
                        mainHandler.post(() -> callback.onSuccess(customerId, email, resObj));
                        return;
                    }
                }

                fetchCustomerProfile(email, new DataCallback() {
                    @Override
                    public void onSuccess(String profileJson) {
                        try {
                            JSONObject profObj = new JSONObject(profileJson);
                            String existingCustId = profObj.optString("customer_id", "");
                            if (!existingCustId.isEmpty()) {
                                callback.onSuccess(existingCustId, email, profObj);
                            } else {
                                callback.onError("Could not retrieve customer ID from server.");
                            }
                        } catch (Exception e) {
                            callback.onError("Invalid server response: " + respStr);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError("Server sync failed (" + code + "): " + respStr);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Google User Sync Exception: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void fetchCustomerProfile(String emailOrId, DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/customers/" + URLEncoder.encode(emailOrId, "UTF-8");
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("Error " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void fetchAllCustomers(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/customers";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("Error " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void fetchRooms(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/rooms";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("Error " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void updateRoomStatus(String roomId, String roomNumber, double ratePerDay, int maxPax, String status, String roomType, DataCallback callback) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("room_number", roomNumber);
                payload.put("rate_per_day", ratePerDay);
                payload.put("max_pax", maxPax);
                payload.put("status", status);
                payload.put("room_type", roomType);

                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/rooms/" + URLEncoder.encode(roomId, "UTF-8");
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                applyStaffHeaders(conn);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("Error " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void fetchCourts(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/courts";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("Error " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void createBooking(JSONObject bookingPayload, DataCallback callback) {
        new Thread(() -> {
            try {
                JSONArray rawRooms = bookingPayload.optJSONArray("room_ids");
                if (rawRooms != null && rawRooms.length() > 0) {
                    Set<String> uniqueRooms = new LinkedHashSet<>();
                    for (int i = 0; i < rawRooms.length(); i++) {
                        String r = rawRooms.optString(i, "").trim();
                        if (!r.isEmpty()) uniqueRooms.add(r);
                    }
                    JSONArray cleanRooms = new JSONArray();
                    for (String r : uniqueRooms) cleanRooms.put(r);
                    bookingPayload.put("room_ids", cleanRooms);
                }

                String currentId = bookingPayload.optString("booking_id", "");
                if (currentId.isEmpty() || currentId.length() > 15) {
                    bookingPayload.put("booking_id", generateUniqueBookingId("BK"));
                }

                StringBuilder respBuilder = new StringBuilder();
                int code = executePostBooking(bookingPayload, respBuilder);
                String firstResp = respBuilder.toString();

                if (code == 409 && firstResp.contains("ERR-409-DUPLICATE")) {
                    bookingPayload.put("booking_id", generateUniqueBookingId("BK"));
                    respBuilder.setLength(0);
                    code = executePostBooking(bookingPayload, respBuilder);
                    firstResp = respBuilder.toString();
                }

                final int finalCode = code;
                final String finalResp = firstResp;
                if (finalCode >= 200 && finalCode < 300) {
                    mainHandler.post(() -> callback.onSuccess(finalResp));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + finalCode + ": " + finalResp));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    private synchronized int executePostBooking(JSONObject bookingPayload, StringBuilder responseTarget) throws Exception {
        responseTarget.setLength(0);
        String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/bookings";
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        applyStaffHeaders(conn);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = bookingPayload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
        );
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            responseTarget.append(responseLine.trim());
        }
        return code;
    }

    public void createMaintenanceBlockBooking(
            String maintBookingId,
            String dbRoomId,
            String dbCourtId,
            String startDate,
            String endDate,
            DataCallback callback
    ) {
        new Thread(() -> {
            try {
                String maintEmail = "maintenance.block@casavista.com";
                JSONObject syncPayload = new JSONObject();
                syncPayload.put("email", maintEmail);
                syncPayload.put("first_name", "Facility");
                syncPayload.put("last_name", "Maintenance");

                URL syncUrl = new URL(SupabaseConfig.FASTAPI_BACKEND_URL + "/customers/sync-google");
                HttpURLConnection sConn = (HttpURLConnection) syncUrl.openConnection();
                sConn.setRequestMethod("POST");
                sConn.setRequestProperty("Content-Type", "application/json; utf-8");
                applyStaffHeaders(sConn);
                sConn.setDoOutput(true);
                try (OutputStream os = sConn.getOutputStream()) {
                    os.write(syncPayload.toString().getBytes(StandardCharsets.UTF_8));
                }
                int sCode = sConn.getResponseCode();
                BufferedReader sBr = new BufferedReader(new InputStreamReader(
                        sCode >= 200 && sCode < 300 ? sConn.getInputStream() : sConn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder sSb = new StringBuilder();
                String line;
                while ((line = sBr.readLine()) != null) sSb.append(line.trim());
                JSONObject custObj = new JSONObject(sSb.toString());
                String custId = custObj.optString("customer_id", "CUST-101");

                String cleanId = maintBookingId.length() > 15 ? maintBookingId.substring(0, 15) : maintBookingId;

                JSONObject bPayload = new JSONObject();
                bPayload.put("booking_id", cleanId);
                bPayload.put("customer_id", custId);
                bPayload.put("customer_email", maintEmail);
                bPayload.put("staff_id", "STF-001");
                bPayload.put("booking_date", startDate);
                bPayload.put("total_amount", 0.0);

                if (dbRoomId != null && !dbRoomId.isEmpty()) {
                    JSONArray rArr = new JSONArray();
                    rArr.put(dbRoomId);
                    bPayload.put("room_id", dbRoomId);
                    bPayload.put("room_ids", rArr);
                    bPayload.put("check_in_date", startDate);
                    bPayload.put("check_out_date", endDate);
                    bPayload.put("exclusive", false);
                    bPayload.put("paddle_count", 0);
                } else if (dbCourtId != null && !dbCourtId.isEmpty()) {
                    bPayload.put("court_id", dbCourtId);
                    bPayload.put("start_time", "06:00:00");
                    bPayload.put("end_time", "22:00:00");
                    bPayload.put("exclusive", false);
                    bPayload.put("paddle_count", 0);
                }

                StringBuilder respBuilder = new StringBuilder();
                int bCode = executePostBooking(bPayload, respBuilder);
                if (bCode >= 200 && bCode < 300) {
                    updateBookingStatus(cleanId, "Confirmed", new DataCallback() {
                        @Override
                        public void onSuccess(String jsonArrayString) {
                            if (callback != null) callback.onSuccess(jsonArrayString);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (callback != null) callback.onError(errorMessage);
                        }
                    });
                } else {
                    if (callback != null) mainHandler.post(() -> callback.onError(respBuilder.toString()));
                }
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void fetchBookings(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/bookings";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void fetchActiveBookings(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/bookings/active";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void updateBookingStatus(String bookingId, String newStatus, DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/bookings/" + URLEncoder.encode(bookingId, "UTF-8")
                        + "/status?new_status=" + URLEncoder.encode(newStatus, "UTF-8");
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void processPayment(JSONObject paymentPayload, DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/payments";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                applyStaffHeaders(conn);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = paymentPayload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void fetchRevenuePnL(DataCallback callback) {
        fetchRevenuePnL(null, null, callback);
    }

    public void fetchRevenuePnL(String startDate, String endDate, DataCallback callback) {
        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(SupabaseConfig.FASTAPI_BACKEND_URL + "/reports/revenue-pnl");
                boolean hasQuery = false;
                if (startDate != null && !startDate.trim().isEmpty()) {
                    urlBuilder.append("?start_date=").append(URLEncoder.encode(startDate.trim(), "UTF-8"));
                    hasQuery = true;
                }
                if (endDate != null && !endDate.trim().isEmpty()) {
                    urlBuilder.append(hasQuery ? "&" : "?").append("end_date=").append(URLEncoder.encode(endDate.trim(), "UTF-8"));
                }

                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void fetchStaffMembers(DataCallback callback) {
        fetchStaffList(callback);
    }

    public void fetchStaffList(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/staff";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void createStaffMember(String staffId, String firstName, String middleName, String lastName, String role, DataCallback callback) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("staff_id", staffId);
                payload.put("first_name", firstName);
                if (middleName != null && !middleName.trim().isEmpty()) {
                    payload.put("middle_name", middleName.trim());
                }
                payload.put("last_name", lastName);
                payload.put("role", role);

                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/staff";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                applyStaffHeaders(conn);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void fetchPosItems(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/pos/items";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                applyStaffHeaders(conn);

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void createPosItem(JSONObject itemPayload, DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/pos/items";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                applyStaffHeaders(conn);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = itemPayload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    public void createPosOrder(JSONObject orderPayload, DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/pos/orders";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                applyStaffHeaders(conn);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = orderPayload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String respStr = response.toString();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> callback.onSuccess(respStr));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + code + ": " + respStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * SAFE SESSION CACHE CLEAR:
     * Previously, this method looped through every booking in Supabase and called
     * PUT /bookings/{id}/status?new_status=Cancelled, which caused all customer bookings
     * to become Cancelled whenever a user logged out or switched accounts!
     * Now it ONLY clears local device cache so remote Supabase bookings remain untouched.
     */
    public void purgeAndResetAllBookingsAndPayments(Context context, DataCallback callback) {
        new Thread(() -> {
            if (context != null) {
                ResortRepository.getInstance().clearBookings(context);

                SharedPreferences overridePrefs = context.getSharedPreferences("FacilityStatusOverridePrefs", Context.MODE_PRIVATE);
                overridePrefs.edit().clear().apply();

                SharedPreferences bookingsPrefs = context.getSharedPreferences("BookingsPrefs", Context.MODE_PRIVATE);
                bookingsPrefs.edit().clear().apply();
            }

            if (callback != null) {
                mainHandler.post(() -> callback.onSuccess("Local session cache cleared safely."));
            }
        }).start();
    }
}