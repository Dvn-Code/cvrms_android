package com.example.casavista.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.casavista.models.Announcement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminAnnouncementHelper {

    private static final String PREF_NAME = "CasavistaAnnouncements";
    private static final String KEY_ANNOUNCEMENTS_JSON = "announcements_list_json";
    private static final int MAX_ANNOUNCEMENTS = 5;

    // Load Announcements (Reads author property if present)
    public static List<Announcement> getAnnouncements(Context context) {
        List<Announcement> list = new ArrayList<>();
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

    // Overloaded method to support "Super Admin" author attribution
    public static void addAnnouncement(Context context, String title, String body, String date, String imagePath, String author) {
        List<Announcement> list = getAnnouncements(context);

        Announcement newPost = new Announcement(
                String.valueOf(System.currentTimeMillis()),
                title,
                body,
                date,
                imagePath,
                author
        );

        // Keep maximum 5 announcements (newest at top)
        list.add(0, newPost);
        if (list.size() > MAX_ANNOUNCEMENTS) {
            list = new ArrayList<>(list.subList(0, MAX_ANNOUNCEMENTS));
        }

        saveAnnouncements(context, list);
    }

    // Save list back to SharedPreferences
    public static void saveAnnouncements(Context context, List<Announcement> list) {
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