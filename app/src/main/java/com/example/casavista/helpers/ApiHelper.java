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

    private static JSONObject safeParseJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(raw.trim());
        } catch (Exception e) {
            JSONObject fallback = new JSONObject();
            try {
                fallback.put("detail", raw.trim());
            } catch (Exception ignored) {}
            return fallback;
        }
    }

    private static void saveCustomerResponseToPrefs(Context context, JSONObject resObj, String fallbackEmail) {
        if (context == null || resObj == null) return;

        String custId = resObj.optString("customer_id", "");
        String resEmail = resObj.optString("email", fallbackEmail != null ? fallbackEmail : "");
        String resFirst = resObj.optString("first_name", "Guest");
        String resMiddle = resObj.optString("middle_name", "");
        String resLast = resObj.optString("last_name", "User");
        String resPhone = resObj.optString("phone", "");
        String resAddress = resObj.optString("address", "");
        if (resAddress.contains("||CVAUTH:")) {
            resAddress = resAddress.split("\\|\\|CVAUTH:")[0].trim();
        }
        String resBirthdate = resObj.optString("birthdate", resObj.optString("birth_date", ""));
        String resGender = resObj.optString("gender", "");
        String resUsername = resObj.optString("username", "");

        String fullName = (resMiddle != null && !resMiddle.trim().isEmpty() && !"null".equalsIgnoreCase(resMiddle))
                ? resFirst + " " + resMiddle + " " + resLast
                : resFirst + " " + resLast;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String emailCleanKey = resEmail.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");

        SharedPreferences.Editor editor = prefs.edit()
                .putString("user_email", resEmail)
                .putString("user_first_name", resFirst)
                .putString("user_last_name", resLast)
                .putString("user_name", fullName.trim())
                .putString("user_name_" + emailCleanKey, fullName.trim())
                .putBoolean("is_logged_in", true)
                .putBoolean("has_agreed_terms", true);

        if (!custId.isEmpty() && !custId.startsWith("CUST-17")) {
            editor.putString("user_id", custId);
            editor.putString("customer_id", custId);
            editor.putString("customer_id_" + emailCleanKey, custId);
        }
        if (!resUsername.isEmpty() && !"null".equalsIgnoreCase(resUsername)) {
            editor.putString("user_username", resUsername);
            editor.putString("user_username_" + emailCleanKey, resUsername);
        }
        if (!resPhone.trim().isEmpty() && !"Not Set".equalsIgnoreCase(resPhone.trim()) && !"null".equalsIgnoreCase(resPhone.trim())) {
            editor.putString("user_phone", resPhone.trim());
            editor.putString("user_phone_" + emailCleanKey, resPhone.trim());
        }
        if (!resAddress.trim().isEmpty() && !"Not Set".equalsIgnoreCase(resAddress.trim()) && !"null".equalsIgnoreCase(resAddress.trim())) {
            editor.putString("user_address", resAddress.trim());
            editor.putString("user_address_" + emailCleanKey, resAddress.trim());
        }
        if (!resBirthdate.trim().isEmpty() && !"Not Set".equalsIgnoreCase(resBirthdate.trim()) && !"null".equalsIgnoreCase(resBirthdate.trim())) {
            editor.putString("user_birthdate", resBirthdate.trim());
            editor.putString("user_birthdate_" + emailCleanKey, resBirthdate.trim());
        }
        if (!resGender.trim().isEmpty() && !"Not Set".equalsIgnoreCase(resGender.trim()) && !"null".equalsIgnoreCase(resGender.trim())) {
            editor.putString("user_gender", resGender.trim());
            editor.putString("user_gender_" + emailCleanKey, resGender.trim());
        }
        editor.apply();
    }

    public static void registerCustomerAccount(
            Context context,
            String firstName,
            String middleName,
            String lastName,
            String email,
            String phone,
            String address,
            String birthdate,
            String gender,
            String username,
            String password,
            ApiCallback callback
    ) {
        new Thread(() -> {
            try {
                URL url = new URL(VERCEL_BASE_URL + "/auth/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject payload = new JSONObject();
                payload.put("first_name", firstName);
                if (middleName != null && !middleName.trim().isEmpty()) {
                    payload.put("middle_name", middleName.trim());
                }
                payload.put("last_name", lastName);
                payload.put("email", email.trim().toLowerCase());
                payload.put("phone", phone.trim());
                payload.put("address", address.trim());
                payload.put("username", username.trim());
                payload.put("password", password);
                if (birthdate != null && !birthdate.trim().isEmpty()) {
                    payload.put("birthdate", birthdate.trim());
                }
                if (gender != null && !gender.trim().isEmpty()) {
                    payload.put("gender", gender.trim());
                }

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                JSONObject resObj = safeParseJson(response.toString());
                if (code >= 200 && code < 300) {
                    saveCustomerResponseToPrefs(context, resObj, email);
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(resObj));
                    }
                } else {
                    String msg = resObj.optString("detail", "Registration failed (HTTP " + code + ")");
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(msg));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Network error: " + e.getLocalizedMessage()));
                }
            }
        }).start();
    }

    public static void loginCustomerAccount(
            Context context,
            String identifier,
            String password,
            ApiCallback callback
    ) {
        new Thread(() -> {
            try {
                URL url = new URL(VERCEL_BASE_URL + "/auth/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject payload = new JSONObject();
                payload.put("identifier", identifier.trim());
                payload.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                JSONObject resObj = safeParseJson(response.toString());
                if (code >= 200 && code < 300) {
                    saveCustomerResponseToPrefs(context, resObj, identifier);
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(resObj));
                    }
                } else {
                    String msg = resObj.optString("detail", "Invalid username/email or password.");
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(msg));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Network error: " + e.getLocalizedMessage()));
                }
            }
        }).start();
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
        syncCustomerToDatabase(context, email, firstName, lastName, phone, address, null, null, callback);
    }

    public static void syncCustomerToDatabase(
            Context context,
            String email,
            String firstName,
            String lastName,
            String phone,
            String address,
            String birthdate,
            String gender,
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
                jsonParam.put("first_name", firstName != null && !firstName.trim().isEmpty() ? firstName : "Guest");
                jsonParam.put("last_name", lastName != null && !lastName.trim().isEmpty() ? lastName : "User");
                if (phone != null && !phone.trim().isEmpty() && !"Not Set".equalsIgnoreCase(phone.trim())) {
                    jsonParam.put("phone", phone.trim());
                }
                if (address != null && !address.trim().isEmpty() && !"Not Set".equalsIgnoreCase(address.trim())) {
                    jsonParam.put("address", address.trim());
                }
                if (birthdate != null && !birthdate.trim().isEmpty() && !"Not Set".equalsIgnoreCase(birthdate.trim())) {
                    jsonParam.put("birthdate", birthdate.trim());
                }
                if (gender != null && !gender.trim().isEmpty() && !"Not Set".equalsIgnoreCase(gender.trim())) {
                    jsonParam.put("gender", gender.trim());
                }

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

                JSONObject resObj = safeParseJson(response.toString());

                if (code >= 200 && code < 300) {
                    saveCustomerResponseToPrefs(context, resObj, email);
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

                JSONObject resObj = safeParseJson(response.toString());

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

                JSONObject resObj = safeParseJson(response.toString());

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