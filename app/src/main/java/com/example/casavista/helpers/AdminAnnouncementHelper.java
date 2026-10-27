package com.example.casavista.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.casavista.models.Announcement;
import com.example.casavista.network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminAnnouncementHelper {

    private static final String PREF_NAME = "CasavistaAnnouncements";
    private static final String KEY_ANNOUNCEMENTS_JSON = "announcements_list_json";
    private static final int MAX_ANNOUNCEMENTS = 10;

    // Load Announcements from local cache
    public static List<Announcement> getAnnouncements(Context context) {
        List<Announcement> list = new ArrayList<>();
        if (context == null) return list;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_ANNOUNCEMENTS_JSON, "[]");

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(new Announcement(
                        obj.optString("id"),
                        obj.optString("title"),
                        obj.optString("body"),
                        obj.optString("date"),
                        obj.optString("imagePath"),
                        obj.optString("author", "Admin")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Standard add method (Defaults author to "Admin")
    public static void addAnnouncement(Context context, String title, String body, String date, String imagePath) {
        addAnnouncement(context, title, body, date, imagePath, "Admin");
    }

    // Overloaded method to support "Super Admin" author attribution & cloud sync
    public static void addAnnouncement(Context context, String title, String body, String date, String imagePath, String author) {
        List<Announcement> list = getAnnouncements(context);

        String id = String.valueOf(System.currentTimeMillis());
        Announcement newPost = new Announcement(
                id,
                title,
                body,
                date,
                imagePath,
                author
        );

        // Keep maximum 10 announcements (newest at top)
        list.add(0, newPost);
        if (list.size() > MAX_ANNOUNCEMENTS) {
            list = new ArrayList<>(list.subList(0, MAX_ANNOUNCEMENTS));
        }

        saveAnnouncements(context, list);

        // Upload to cloud Supabase so all guest and staff phones receive it immediately
        try {
            SupabaseClient.getInstance().createAnnouncement(title, body, imagePath, author, new SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String result) {
                    // Synced to cloud successfully
                }

                @Override
                public void onError(String error) {
                    // Retained in local cache
                }
            });
        } catch (Exception ignored) { }
    }

    // Delete announcement locally and from Supabase
    public static void deleteAnnouncement(Context context, String announcementId, Runnable onComplete) {
        if (context == null) return;
        List<Announcement> list = getAnnouncements(context);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (announcementId != null && announcementId.equals(list.get(i).getId())) {
                list.remove(i);
            }
        }
        saveAnnouncements(context, list);

        try {
            SupabaseClient.getInstance().deleteAnnouncement(announcementId, new SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String result) {
                    if (onComplete != null) onComplete.run();
                }

                @Override
                public void onError(String error) {
                    if (onComplete != null) onComplete.run();
                }
            });
        } catch (Exception e) {
            if (onComplete != null) onComplete.run();
        }
    }

    // Sync cloud announcements down to this device
    public static void syncWithCloud(Context context, Runnable onComplete) {
        if (context == null) return;
        try {
            SupabaseClient.getInstance().fetchAnnouncements(new SupabaseClient.DataCallback() {
                @Override
                public void onSuccess(String jsonResult) {
                    try {
                        JSONArray arr = new JSONArray(jsonResult);
                        List<Announcement> cloudList = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String id = obj.optString("id", String.valueOf(i));
                            String title = obj.optString("title", "");
                            String body = obj.optString("content", "");
                            String date = obj.optString("created_at", "");
                            if (date.contains("T")) {
                                date = date.split("T")[0];
                            }
                            String imageBase64 = obj.optString("image_base64", "");
                            String author = obj.optString("author_name", "Resort Staff");

                            cloudList.add(new Announcement(id, title, body, date, imageBase64, author));
                        }

                        if (!cloudList.isEmpty()) {
                            saveAnnouncements(context, cloudList);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (onComplete != null) onComplete.run();
                }

                @Override
                public void onError(String error) {
                    if (onComplete != null) onComplete.run();
                }
            });
        } catch (Exception e) {
            if (onComplete != null) onComplete.run();
        }
    }

    // Save list back to SharedPreferences
    public static void saveAnnouncements(Context context, List<Announcement> list) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONArray jsonArray = new JSONArray();

        try {
            for (Announcement a : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", a.getId());
                obj.put("title", a.getTitle());
                obj.put("body", a.getBody());
                obj.put("date", a.getDate());
                obj.put("imagePath", a.getImagePath());
                obj.put("author", a.getAuthor());
                jsonArray.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        prefs.edit().putString(KEY_ANNOUNCEMENTS_JSON, jsonArray.toString()).apply();
    }
}