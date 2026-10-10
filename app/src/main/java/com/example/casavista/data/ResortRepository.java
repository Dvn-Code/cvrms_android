package com.example.casavista.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.casavista.R;
import com.example.casavista.models.Booking;
import com.example.casavista.models.CatalogItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ResortRepository {

    private static final String PREF_CATALOG = "CatalogImagePrefs";
    private static final String PREF_BOOKINGS = "BookingsPrefs";
    private static final String PREF_FACILITY_OVERRIDE = "FacilityStatusOverridePrefs";
    private static final String KEY_BOOKINGS_JSON = "saved_bookings_list";

    private static ResortRepository instance;
    private List<Booking> bookingsList;
    private List<CatalogItem> catalogItems;
    private String currentUserName = "Alex Morgan";
    private String currentUserEmail = "alex.morgan@casavista.com";

    private ResortRepository() {
        bookingsList = new ArrayList<>();
        initDefaultCatalog();
    }

    private void initDefaultCatalog() {
        catalogItems = new ArrayList<>();

        catalogItems.add(new CatalogItem("Standard Villa Suite 101", "Queen Bed • Mountain View • AC", "Room", "4 Guests", "₱2,500 / day", "Standard", R.drawable.ic_casavista_artwork));
        catalogItems.add(new CatalogItem("Standard Villa Suite 102", "Queen Bed • Poolside Access • AC", "Room", "4 Guests", "₱2,500 / day", "Standard", R.drawable.ic_casavista_artwork));
        catalogItems.add(new CatalogItem("Deluxe Family Suite 103", "2 Queen Beds • Balcony • Mini Kitchen", "Room", "6 Guests", "₱3,500 / day", "Family Deluxe", R.drawable.ic_casavista_artwork));
        catalogItems.add(new CatalogItem("Whole-Resort Private Buyout", "Exclusive access to all 3 villas & sports courts", "Buyout", "14 Max", "₱12,000 / day", "Exclusive", R.drawable.ic_casavista_artwork));
        catalogItems.add(new CatalogItem("Pickleball Court 1", "Tournament Hard Court • Outdoor Lighting", "Pickleball", "2-4 Players", "₱400 - ₱600 / hr", "Day/Night", R.drawable.ic_casavista_artwork));
        catalogItems.add(new CatalogItem("Pickleball Court 2", "Tournament Hard Court • Outdoor Lighting", "Pickleball", "2-4 Players", "₱400 - ₱600 / hr", "Day/Night", R.drawable.ic_casavista_artwork));
        catalogItems.add(new CatalogItem("Pickleball Court 3", "Tournament Hard Court • Outdoor Lighting", "Pickleball", "2-4 Players", "₱400 - ₱600 / hr", "Day/Night", R.drawable.ic_casavista_artwork));
        catalogItems.add(new CatalogItem("Basketball Half-Court", "Regulation Pro Flooring • Full Lighting", "Basketball", "2-10 Players", "₱500 - ₱750 / hr", "Day/Night", R.drawable.ic_casavista_artwork));
    }

    public static synchronized ResortRepository getInstance() {
        if (instance == null) {
            instance = new ResortRepository();
        }
        return instance;
    }

    public String getCurrentUserName() { return currentUserName; }
    public void setCurrentUserName(String currentUserName) { this.currentUserName = currentUserName; }

    public String getCurrentUserEmail() { return currentUserEmail; }
    public void setCurrentUserEmail(String currentUserEmail) { this.currentUserEmail = currentUserEmail; }

    public List<CatalogItem> getCatalogItems() {
        return catalogItems;
    }

    public void loadSavedImages(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_CATALOG, Context.MODE_PRIVATE);
        for (CatalogItem item : catalogItems) {
            if (item.getTitle() != null) {
                String savedPath = prefs.getString(item.getTitle(), null);
                if (savedPath != null) {
                    item.setImageUrl(savedPath);
                }
            }
        }
    }

    public void updateCatalogItem(Context context, CatalogItem updatedItem) {
        if (updatedItem == null) return;
        for (int i = 0; i < catalogItems.size(); i++) {
            CatalogItem item = catalogItems.get(i);
            if (item.getTitle() != null && item.getTitle().equalsIgnoreCase(updatedItem.getTitle())) {
                catalogItems.set(i, updatedItem);

                if (context != null && updatedItem.getImageUrl() != null) {
                    SharedPreferences prefs = context.getSharedPreferences(PREF_CATALOG, Context.MODE_PRIVATE);
                    prefs.edit().putString(updatedItem.getTitle(), updatedItem.getImageUrl()).apply();
                }
                break;
            }
        }
    }

    public void addCatalogItem(CatalogItem item) {
        if (item != null) {
            catalogItems.add(item);
        }
    }

    public List<CatalogItem> getCatalogByCategory(String category) {
        List<CatalogItem> allItems = getCatalogItems();
        if (category == null || category.isEmpty() || category.equalsIgnoreCase("ALL")) {
            return allItems;
        }

        List<CatalogItem> filtered = new ArrayList<>();
        for (CatalogItem item : allItems) {
            if (item.getCategory() != null) {
                if (item.getCategory().equalsIgnoreCase(category) ||
                        (category.equalsIgnoreCase("Room") && item.getCategory().equalsIgnoreCase("Rooms")) ||
                        (category.equalsIgnoreCase("Rooms") && item.getCategory().equalsIgnoreCase("Room"))) {
                    filtered.add(item);
                }
            }
        }
        return filtered;
    }

    // --- BOOKING PERSISTENCE METHODS ---

    public void saveBookings(Context context) {
        if (context == null) return;
        try {
            JSONArray array = new JSONArray();
            for (Booking b : bookingsList) {
                JSONObject obj = new JSONObject();
                obj.put("title", b.getTitle());
                obj.put("status", b.getStatus());
                obj.put("date", b.getDate());
                obj.put("detail1", b.getDetail1());
                obj.put("bookingId", b.getBookingId() != null ? b.getBookingId() : "");
                obj.put("detail2", b.getDetail2());
                obj.put("price", b.getPrice());
                obj.put("imagePath", b.getImagePath() != null ? b.getImagePath() : "");
                obj.put("imageResId", b.getImageResId());
                array.put(obj);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_BOOKINGS, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_BOOKINGS_JSON, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadSavedBookings(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_BOOKINGS, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_BOOKINGS_JSON, null);
        if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                List<Booking> loadedList = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Booking booking = new Booking(
                            obj.optString("title"),
                            obj.optString("status"),
                            obj.optString("date"),
                            obj.optString("detail1"),
                            obj.optString("detail2"),
                            obj.optString("price"),
                            obj.optString("imagePath"),
                            obj.optInt("imageResId")
                    );
                    booking.setBookingId(obj.optString("bookingId", ""));
                    loadedList.add(booking);
                }
                this.bookingsList = loadedList;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void clearBookings(Context context) {
        this.bookingsList = new ArrayList<>();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_BOOKINGS, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_BOOKINGS_JSON).apply();
        }
    }

    public List<Booking> getBookings() { return bookingsList; }

    public List<Booking> getBookings(String type) {
        if (type == null || type.isEmpty() || type.equalsIgnoreCase("ALL")) {
            return bookingsList;
        }

        List<Booking> filtered = new ArrayList<>();
        for (Booking booking : bookingsList) {
            String status = booking.getStatus() != null ? booking.getStatus() : "";
            if ("Cancelled".equalsIgnoreCase(type)) {
                if ("Cancelled".equalsIgnoreCase(status) || "Declined".equalsIgnoreCase(status)) {
                    filtered.add(booking);
                }
            } else if ("Past".equalsIgnoreCase(type)) {
                if ("Completed".equalsIgnoreCase(status) || "Past".equalsIgnoreCase(status)) {
                    filtered.add(booking);
                }
            } else { // "Upcoming" / "Pending" / Active bookings
                if (!"Cancelled".equalsIgnoreCase(status) && !"Declined".equalsIgnoreCase(status) && !"Completed".equalsIgnoreCase(status)) {
                    filtered.add(booking);
                }
            }
        }
        return filtered;
    }

    // --- ADMIN FACILITY STATUS OVERRIDE METHODS ---

    public void setFacilityStatusOverride(Context context, String facilityName, String status) {
        if (context == null || facilityName == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_FACILITY_OVERRIDE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getNormalizedFacilityKey(facilityName), status);
        editor.putString(facilityName.trim(), status);
        editor.apply();
    }

    public String getFacilityStatusOverride(Context context, String facilityName) {
        if (context == null || facilityName == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_FACILITY_OVERRIDE, Context.MODE_PRIVATE);
        String val = prefs.getString(getNormalizedFacilityKey(facilityName), null);
        if (val == null) {
            val = prefs.getString(facilityName.trim(), null);
        }
        return val;
    }

    private String getNormalizedFacilityKey(String name) {
        if (name == null) return "";
        String key = name.toLowerCase().trim();
        if (key.contains("room 1") || key.contains("101") || key.contains("villa suite")) return "Room 1";
        if (key.contains("room 2") || key.contains("102") || key.contains("villa deluxe")) return "Room 2";
        if (key.contains("room 3") || key.contains("201") || key.contains("family loft")) return "Room 3";
        if (key.contains("court 1")) return "Court 1";
        if (key.contains("court 2")) return "Court 2";
        if (key.contains("court 3")) return "Court 3";
        if (key.contains("basketball")) return "Basketball Arena";
        if (key.contains("buyout")) return "Full Resort Buyout";
        return name.trim();
    }

    // --- FACILITY BOOKED CHECK HELPERS ---

    public boolean hasActiveGuestBooking(String facilityName) {
        if (facilityName == null || facilityName.isEmpty()) return false;
        String searchKey = facilityName.toLowerCase().trim();

        int parenIdx = searchKey.indexOf('(');
        if (parenIdx > 0) {
            searchKey = searchKey.substring(0, parenIdx).trim();
        }

        for (Booking b : bookingsList) {
            String status = b.getStatus();
            if ("Confirmed".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
                String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                String d1 = b.getDetail1() != null ? b.getDetail1().toLowerCase() : "";
                String d2 = b.getDetail2() != null ? b.getDetail2().toLowerCase() : "";

                if (title.contains(searchKey) || d1.contains(searchKey) || d2.contains(searchKey) ||
                        (!title.isEmpty() && searchKey.contains(title))) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getFacilityDisplayStatus(Context context, String facilityName) {
        if (facilityName == null || facilityName.isEmpty()) return "Available";

        if (hasActiveGuestBooking(facilityName)) {
            return "Booked";
        }

        if (context != null) {
            String override = getFacilityStatusOverride(context, facilityName);
            if (override != null && !override.trim().isEmpty()) {
                if ("Maintenance".equalsIgnoreCase(override)) {
                    return "Maintenance";
                } else if ("Cleaning".equalsIgnoreCase(override)) {
                    return "Cleaning";
                } else if ("Unavailable".equalsIgnoreCase(override) || "Booked".equalsIgnoreCase(override) || "Occupied".equalsIgnoreCase(override)) {
                    return "Unavailable";
                } else if ("Available".equalsIgnoreCase(override) || "Open".equalsIgnoreCase(override)) {
                    return "Available";
                }
            }
        }

        return "Available";
    }

    public boolean isFacilityBooked(Context context, String facilityName) {
        String status = getFacilityDisplayStatus(context, facilityName);
        return !"Available".equalsIgnoreCase(status);
    }

    public boolean isFacilityBooked(String facilityName) {
        return isFacilityBooked(null, facilityName);
    }

    public int getConfirmedRoomsCount(Context context) {
        int count = 0;
        if (isFacilityBooked(context, "Room 1") || isFacilityBooked(context, "Villa Suite 101")) count++;
        if (isFacilityBooked(context, "Room 2") || isFacilityBooked(context, "Villa Deluxe 102")) count++;
        if (isFacilityBooked(context, "Room 3") || isFacilityBooked(context, "Family Loft 201")) count++;
        return count;
    }

    public int getConfirmedRoomsCount() {
        return getConfirmedRoomsCount(null);
    }

    // --- BOOKING ADD METHODS (ALL OVERLOADS) ---

    // 1. Core method with Context
    public void addBooking(Context context, Booking booking) {
        if (booking != null) {
            bookingsList.add(0, booking);
            if (context != null) {
                saveBookings(context);
            }
        }
    }

    // 2. Core method without Context
    public void addBooking(Booking booking) {
        addBooking(null, booking);
    }

    // 3. Full 7-parameter method (Context + status)
    public void addBooking(Context context, String title, String status, String date, String detail1, String detail2, String price) {
        addBooking(context, new Booking(title, status, date, detail1, detail2, price));
    }

    // Full 9-parameter method with image support
    public void addBooking(Context context, String title, String status, String date, String detail1, String detail2, String price, String imagePath, int imageResId) {
        addBooking(context, new Booking(title, status, date, detail1, detail2, price, imagePath, imageResId));
    }

    // 4. 6-parameter method with Context (Defaults to "Confirmed")
    public void addBooking(Context context, String title, String date, String detail1, String detail2, String price) {
        addBooking(context, title, "Confirmed", date, detail1, detail2, price);
    }

    // 5. 6-parameter method without Context (Custom status)
    public void addBooking(String title, String status, String date, String detail1, String detail2, String price) {
        addBooking(null, title, status, date, detail1, detail2, price);
    }

    // 6. 5-parameter method without Context (Defaults to "Confirmed")
    public void addBooking(String title, String date, String detail1, String detail2, String price) {
        addBooking(null, title, "Confirmed", date, detail1, detail2, price);
    }

    public void cancelBooking(Context context, Booking booking) {
        if (booking != null) {
            booking.setStatus("Cancelled");
            saveBookings(context);
        }
    }

    public void cancelBooking(Booking booking) {
        if (booking != null) booking.setStatus("Cancelled");
    }
}