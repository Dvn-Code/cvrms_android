package com.example.casavista.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiHelper {

    public static final String VERCEL_BASE_URL = "https://cvrms-api.vercel.app";
    public static final String PREFS_NAME = "CasavistaProfilePrefs";

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public static void syncCustomerToDatabase(
            Context context,
            String email,
            String firstName,
            String lastName,
            String phone,
            String address,
            ApiCallback callback
    ) {
        new Thread(() -> {
            try {
                URL url = new URL(VERCEL_BASE_URL + "/customers/sync-google");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", email);
                jsonParam.put("first_name", firstName != null && !firstName.trim().isEmpty() ? firstName : "Test");
                jsonParam.put("last_name", lastName != null && !lastName.trim().isEmpty() ? lastName : "User");
                jsonParam.put("phone", phone != null && !phone.trim().isEmpty() ? phone : "09123456789");
                jsonParam.put("address", address != null && !address.trim().isEmpty() ? address : "Cebu City");

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
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

                JSONObject resObj = new JSONObject(response.toString());

                if (code >= 200 && code < 300) {
                    if (context != null) {
                        String custId = resObj.optString("customer_id", "");
                        String resEmail = resObj.optString("email", email);
                        String resFirst = resObj.optString("first_name", firstName);
                        String resLast = resObj.optString("last_name", lastName);
                        String resPhone = resObj.optString("phone", phone);
                        String resAddr = resObj.optString("address", address);

                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("customer_id", custId)
                                .putString("user_email", resEmail)
                                .putString("user_first_name", resFirst)
                                .putString("user_last_name", resLast)
                                .putString("user_name", resFirst + " " + resLast)
                                .putString("user_phone", resPhone)
                                .putString("user_address", resAddr)
                                .apply();
                    }

                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(resObj));
                    }
                } else {
                    String msg = resObj.optString("detail", "Error code " + code);
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(msg));
                    }
                }

            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getLocalizedMessage()));
                }
            }
        }).start();
    }

    public static void sendVerificationCode(Context context, String email, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(VERCEL_BASE_URL + "/auth/send-verification-code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", email);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
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

                JSONObject resObj = new JSONObject(response.toString());

                if (code >= 200 && code < 300) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(resObj));
                    }
                } else {
                    String msg = resObj.optString("detail", "Error sending verification code.");
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(msg));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getLocalizedMessage()));
                }
            }
        }).start();
    }

    public static void verifyCode(Context context, String email, String codeStr, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(VERCEL_BASE_URL + "/auth/verify-code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", email);
                jsonParam.put("code", codeStr);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
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

                JSONObject resObj = new JSONObject(response.toString());

                if (code >= 200 && code < 300) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(resObj));
                    }
                } else {
                    String msg = resObj.optString("detail", "Invalid or expired code. Please try again.");
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(msg));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getLocalizedMessage()));
                }
            }
        }).start();
    }
}
