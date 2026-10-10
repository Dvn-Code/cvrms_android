package com.example.casavista.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;
    private final Handler mainHandler;

    public interface AuthCallback {
        void onSuccess(String customerId, String email, JSONObject userData);
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

    public void syncGoogleUser(String email, String firstName, String lastName, AuthCallback callback) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", email);
                payload.put("first_name", (firstName != null && !firstName.trim().isEmpty()) ? firstName : "Guest");
                payload.put("last_name", (lastName != null && !lastName.trim().isEmpty()) ? lastName : "User");
                payload.put("phone", "09000000000"); // Standard placeholder for initial Google OAuth
                payload.put("address", "Cebu City, Philippines");

                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/customers/sync-google";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

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
                    try {
                        JSONObject resObj = new JSONObject(respStr);
                        String customerId = resObj.optString("customer_id", "CUST-" + System.currentTimeMillis());
                        mainHandler.post(() -> callback.onSuccess(customerId, email, resObj));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onSuccess("CUST-" + System.currentTimeMillis(), email, payload));
                    }
                } else {
                    mainHandler.post(() -> callback.onSuccess("CUST-" + System.currentTimeMillis(), email, payload));
                }
            } catch (Exception e) {
                Log.e(TAG, "Google User Sync Exception: " + e.getMessage());
                // Fallback to local identifier if network is unreachable
                mainHandler.post(() -> {
                    try {
                        JSONObject fallbackPayload = new JSONObject();
                        fallbackPayload.put("email", email);
                        fallbackPayload.put("first_name", (firstName != null && !firstName.trim().isEmpty()) ? firstName : "Guest");
                        fallbackPayload.put("last_name", (lastName != null && !lastName.trim().isEmpty()) ? lastName : "User");
                        callback.onSuccess("local-" + System.currentTimeMillis(), email, fallbackPayload);
                    } catch (Exception ex) {
                        callback.onError("Google Sync Exception: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    public interface DataCallback {
        void onSuccess(String jsonArrayString);
        void onError(String errorMessage);
    }

    public void fetchRooms(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/rooms";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

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
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

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
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/bookings";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = bookingPayload.toString().getBytes(StandardCharsets.UTF_8);
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

    public void fetchBookings(DataCallback callback) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/bookings";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

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
                String endpoint = SupabaseConfig.FASTAPI_BACKEND_URL + "/bookings/" + bookingId + "/status?new_status=" + newStatus;
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

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
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

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
}