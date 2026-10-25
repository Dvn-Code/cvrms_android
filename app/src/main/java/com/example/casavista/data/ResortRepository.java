package com.example.casavista.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.casavista.R;
import com.example.casavista.models.Booking;
import com.example.casavista.models.CatalogItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResortRepository {

    private static final String PREF_CATALOG = "CatalogImagePrefs";
    private static final String PREF_BOOKINGS = "BookingsPrefs";
    private static final String PREF_FACILITY_OVERRIDE = "FacilityStatusOverridePrefs";
    private static final String KEY_BOOKINGS_JSON = "saved_bookings_list";

    private static ResortRepository instance;
    private List<Booking> bookingsList;
    private List<CatalogItem> catalogItems;
    private final Map<String, String> memoryStatusOverrides = new LinkedHashMap<>();
    private final Map<String, String> memoryMaintStartDates = new LinkedHashMap<>();
    private final Map<String, String> memoryMaintEndDates = new LinkedHashMap<>();

    private String currentUserName = "Alex Morgan";
    private String currentUserEmail = "alex.morgan@casavista.com";
    private String currentCustomerId = null;

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

    public String getCurrentCustomerId() { return currentCustomerId; }
    public void setCurrentCustomerId(String currentCustomerId) { this.currentCustomerId = currentCustomerId; }

    public void saveCatalogToPrefs(Context context) {
        if (context == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (CatalogItem item : catalogItems) {
                if (item == null || item.getTitle() == null) continue;
                JSONObject obj = new JSONObject();
                obj.put("title", item.getTitle());
                obj.put("subtitle", item.getSubtitle() != null ? item.getSubtitle() : "");
                obj.put("category", item.getCategory() != null ? item.getCategory() : "Room");
                obj.put("guests", item.getGuests() != null ? item.getGuests() : "4 Guests");
                obj.put("price", item.getPrice() != null ? item.getPrice() : "₱2,500 / day");
                obj.put("badge", item.getBadge() != null ? item.getBadge() : "Standard");
                obj.put("imageResId", item.getImageResId());
                obj.put("imageUrl", item.getImageUrl() != null ? item.getImageUrl() : "");
                arr.put(obj);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_CATALOG, Context.MODE_PRIVATE);
            prefs.edit().putString("persisted_catalog_items_v2", arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadCatalogFromPrefs(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_CATALOG, Context.MODE_PRIVATE);
        String rawJson = prefs.getString("persisted_catalog_items_v2", null);
        if (rawJson != null && !rawJson.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(rawJson);
                if (arr.length() > 0) {
                    List<CatalogItem> loaded = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        CatalogItem item = new CatalogItem(
                                obj.optString("title", ""),
                                obj.optString("subtitle", ""),
                                obj.optString("category", "Room"),
                                obj.optString("guests", "4 Guests"),
                                obj.optString("price", "₱2,500 / day"),
                                obj.optString("badge", "Standard"),
                                obj.optInt("imageResId", R.drawable.ic_casavista_artwork),
                                obj.optString("imageUrl", null)
                        );
                        loaded.add(item);
                    }
                    this.catalogItems = loaded;
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        initDefaultCatalog();
    }

    public List<CatalogItem> getCatalogItems() {
        return catalogItems;
    }

    public void loadSavedImages(Context context) {
        if (context == null) return;
        loadCatalogFromPrefs(context);
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
        if (context != null) saveCatalogToPrefs(context);
    }

    public void addCatalogItem(CatalogItem item) {
        addCatalogItem(null, item);
    }

    public void addCatalogItem(Context context, CatalogItem item) {
        if (item != null) {
            // Check if item already exists
            boolean exists = false;
            for (int i = 0; i < catalogItems.size(); i++) {
                if (catalogItems.get(i).getTitle().equalsIgnoreCase(item.getTitle())) {
                    catalogItems.set(i, item);
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                catalogItems.add(item);
            }
            if (context != null) {
                saveCatalogToPrefs(context);
            }
        }
    }

    public void deleteCatalogItem(Context context, CatalogItem item) {
        if (item != null && item.getTitle() != null) {
            deleteCatalogItemByTitle(context, item.getTitle());
        }
    }

    public void deleteCatalogItemByTitle(Context context, String title) {
        if (title == null || title.trim().isEmpty()) return;
        for (int i = catalogItems.size() - 1; i >= 0; i--) {
            if (title.equalsIgnoreCase(catalogItems.get(i).getTitle())) {
                catalogItems.remove(i);
            }
        }
        if (context != null) {
            saveCatalogToPrefs(context);
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

    public synchronized void saveBookings(Context context) {
        if (context == null) return;
        try {
            JSONArray array = new JSONArray();
            List<String> seenIds = new ArrayList<>();
            for (Booking b : bookingsList) {
                if (b == null) continue;
                String bId = b.getBookingId() != null ? b.getBookingId().trim() : "";
                if (!bId.isEmpty()) {
                    if (seenIds.contains(bId.toUpperCase())) continue;
                    seenIds.add(bId.toUpperCase());
                }
                if (b.getTitle() == null || b.getTitle().trim().isEmpty()) continue;

                JSONObject obj = new JSONObject();
                obj.put("title", b.getTitle());
                obj.put("status", b.getStatus());
                obj.put("date", b.getDate());
                obj.put("detail1", b.getDetail1());
                obj.put("bookingId", bId);
                obj.put("customerId", b.getCustomerId() != null ? b.getCustomerId() : "");
                obj.put("roomId", b.getRoomId() != null ? b.getRoomId() : "");
                obj.put("courtId", b.getCourtId() != null ? b.getCourtId() : "");
                obj.put("checkInDate", b.getCheckInDate() != null ? b.getCheckInDate() : "");
                obj.put("checkOutDate", b.getCheckOutDate() != null ? b.getCheckOutDate() : "");
                obj.put("detail2", b.getDetail2());
                obj.put("price", b.getPrice());
                obj.put("totalAmount", b.getTotalAmount());
                obj.put("imagePath", b.getImagePath() != null ? b.getImagePath() : "");
                obj.put("imageResId", b.getImageResId());
                obj.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt() : "");
                obj.put("holdExpiresAt", b.getHoldExpiresAt() != null ? b.getHoldExpiresAt() : "");
                array.put(obj);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_BOOKINGS, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_BOOKINGS_JSON, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadSavedBookings(Context context) {
        if (context == null) return;
        loadFacilityOverridesIntoMemory(context);
        loadSavedImages(context);
        SharedPreferences prefs = context.getSharedPreferences(PREF_BOOKINGS, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_BOOKINGS_JSON, null);
        if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                Map<String, Booking> uniqueMap = new LinkedHashMap<>();
                int fallbackIdx = 0;

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String title = obj.optString("title", "");
                    if (title.trim().isEmpty()) continue;

                    Booking booking = new Booking(
                            title,
                            obj.optString("status"),
                            obj.optString("date"),
                            obj.optString("detail1"),
                            obj.optString("detail2"),
                            obj.optString("price"),
                            obj.optString("imagePath"),
                            obj.optInt("imageResId")
                    );
                    String bId = obj.optString("bookingId", "").trim();
                    booking.setBookingId(bId);
                    booking.setCustomerId(obj.optString("customerId", ""));
                    booking.setRoomId(obj.optString("roomId", ""));
                    booking.setCourtId(obj.optString("courtId", ""));
                    booking.setCheckInDate(obj.optString("checkInDate", ""));
                    booking.setCheckOutDate(obj.optString("checkOutDate", ""));
                    booking.setTotalAmount(obj.optDouble("totalAmount", 0.0));
                    booking.setCreatedAt(obj.optString("createdAt", ""));
                    booking.setHoldExpiresAt(obj.optString("holdExpiresAt", ""));

                    if (booking.isHoldExpired()) {
                        booking.setStatus("Cancelled");
                        booking.setDetail2("Hold expired (15-min window ended)");
                    }

                    syncBookingCatalogImage(booking);

                    String key = !bId.isEmpty() ? bId.toUpperCase() : ("LOCAL_" + (fallbackIdx++));
                    if (!uniqueMap.containsKey(key)) {
                        uniqueMap.put(key, booking);
                    }
                }
                this.bookingsList = new ArrayList<>(uniqueMap.values());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncBookingCatalogImage(Booking booking) {
        if (booking == null) return;
        String normBookingKey = getNormalizedFacilityKey(
                booking.getRoomId() != null && !booking.getRoomId().isEmpty()
                        ? booking.getRoomId()
                        : (booking.getCourtId() != null && !booking.getCourtId().isEmpty() ? booking.getCourtId() : booking.getTitle())
        );
        for (CatalogItem item : catalogItems) {
            if (item == null || item.getTitle() == null) continue;
            String normItemKey = getNormalizedFacilityKey(item.getTitle());
            if (item.getTitle().equalsIgnoreCase(booking.getTitle())
                    || (!normBookingKey.isEmpty() && normBookingKey.equalsIgnoreCase(normItemKey))) {
                if ((booking.getImagePath() == null || booking.getImagePath().trim().isEmpty())
                        && item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
                    booking.setImagePath(item.getImageUrl());
                }
                if (booking.getImageResId() == 0 || booking.getImageResId() == R.drawable.rooms_icon_img) {
                    if (item.getImageResId() != 0) {
                        booking.setImageResId(item.getImageResId());
                    }
                }
                break;
            }
        }
    }

    private void loadFacilityOverridesIntoMemory(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_FACILITY_OVERRIDE, Context.MODE_PRIVATE);
        String[] keys = {"Room 1", "Room 2", "Room 3", "Court 1", "Court 2", "Court 3"};
        for (String k : keys) {
            String st = prefs.getString(k, null);
            String sDate = prefs.getString(k + "_start", null);
            String eDate = prefs.getString(k + "_end", null);
            if (st != null) memoryStatusOverrides.put(k, st);
            if (sDate != null) memoryMaintStartDates.put(k, sDate);
            if (eDate != null) memoryMaintEndDates.put(k, eDate);
        }
    }

    public synchronized void clearBookings(Context context) {
        // Keep active maintenance blocks when switching user accounts so maintenance still blocks user reservations
        List<Booking> preservedMaintenance = new ArrayList<>();
        for (Booking b : bookingsList) {
            if (b != null && b.getBookingId() != null && b.getBookingId().startsWith("MAINT-")) {
                preservedMaintenance.add(b);
            }
        }
        this.bookingsList = preservedMaintenance;
        if (context != null) {
            saveBookings(context);
        }
    }

    public synchronized List<Booking> getBookings() {
        return getBookings("ALL");
    }

    public synchronized List<Booking> getBookings(String type) {
        List<Booking> nonMaintenance = new ArrayList<>();
        for (Booking b : bookingsList) {
            if (b == null || b.getTitle() == null || b.getTitle().trim().isEmpty()) continue;
            if (b.getBookingId() != null && b.getBookingId().startsWith("MAINT-")) continue;
            nonMaintenance.add(b);
        }

        if (type == null || type.isEmpty() || type.equalsIgnoreCase("ALL")) {
            return nonMaintenance;
        }

        List<Booking> filtered = new ArrayList<>();
        for (Booking booking : nonMaintenance) {
            String status = booking.getStatus() != null ? booking.getStatus() : "";
            if ("Cancelled".equalsIgnoreCase(type)) {
                if ("Cancelled".equalsIgnoreCase(status) || "Declined".equalsIgnoreCase(status)) {
                    filtered.add(booking);
                }
            } else if ("Past".equalsIgnoreCase(type)) {
                if ("Completed".equalsIgnoreCase(status) || "Past".equalsIgnoreCase(status)) {
                    filtered.add(booking);
                }
            } else {
                if (!"Cancelled".equalsIgnoreCase(status) && !"Declined".equalsIgnoreCase(status) && !"Completed".equalsIgnoreCase(status)) {
                    filtered.add(booking);
                }
            }
        }
        return filtered;
    }

    // --- ADMIN FACILITY STATUS & MAINTENANCE DATE RANGE OVERRIDE METHODS ---

    public void setFacilityStatusOverride(Context context, String facilityName, String status) {
        setFacilityStatusOverride(context, facilityName, status, null, null);
    }

    public void setFacilityStatusOverride(Context context, String facilityName, String status, String startDateIso, String endDateIso) {
        if (facilityName == null) return;
        String normKey = getNormalizedFacilityKey(facilityName);
        memoryStatusOverrides.put(normKey, status);

        if ("Available".equalsIgnoreCase(status) || "Open".equalsIgnoreCase(status) || "Ready".equalsIgnoreCase(status)) {
            memoryMaintStartDates.remove(normKey);
            memoryMaintEndDates.remove(normKey);
            removeLocalMaintenanceBookingBlock(context, normKey);
        } else if (startDateIso != null && endDateIso != null && !startDateIso.isEmpty() && !endDateIso.isEmpty()) {
            memoryMaintStartDates.put(normKey, startDateIso);
            memoryMaintEndDates.put(normKey, endDateIso);
            upsertLocalMaintenanceBookingBlock(context, normKey, status, startDateIso, endDateIso);
        }

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_FACILITY_OVERRIDE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(normKey, status);
            editor.putString(facilityName.trim(), status);
            if ("Available".equalsIgnoreCase(status) || "Open".equalsIgnoreCase(status) || "Ready".equalsIgnoreCase(status)) {
                editor.remove(normKey + "_start");
                editor.remove(normKey + "_end");
            } else if (startDateIso != null && endDateIso != null) {
                editor.putString(normKey + "_start", startDateIso);
                editor.putString(normKey + "_end", endDateIso);
            }
            editor.apply();
        }
    }

    private void upsertLocalMaintenanceBookingBlock(Context context, String normFacilityKey, String status, String startIso, String endIso) {
        String maintId = "MAINT-" + normFacilityKey.replaceAll("\\s+", "").toUpperCase();
        String dbRoomId = "Room 1".equalsIgnoreCase(normFacilityKey) ? "RM-101"
                : ("Room 2".equalsIgnoreCase(normFacilityKey) ? "RM-102"
                   : ("Room 3".equalsIgnoreCase(normFacilityKey) ? "RM-103" : ""));

        Booking maintBlock = new Booking(
                normFacilityKey + " (" + status + ")",
                "Confirmed",
                startIso,
                "Maintenance: " + startIso + " to " + endIso,
                "Resort Maintenance",
                "₱0.00"
        );
        maintBlock.setBookingId(maintId);
        maintBlock.setRoomId(dbRoomId);
        maintBlock.setCheckInDate(startIso);
        maintBlock.setCheckOutDate(endIso);
        addBooking(context, maintBlock);
    }

    private void removeLocalMaintenanceBookingBlock(Context context, String normFacilityKey) {
        String maintId = "MAINT-" + normFacilityKey.replaceAll("\\s+", "").toUpperCase();
        for (int i = bookingsList.size() - 1; i >= 0; i--) {
            Booking b = bookingsList.get(i);
            if (b != null && maintId.equalsIgnoreCase(b.getBookingId())) {
                bookingsList.remove(i);
            }
        }
        if (context != null) {
            saveBookings(context);
        }
    }

    public String getFacilityStatusOverride(Context context, String facilityName) {
        if (facilityName == null) return null;
        String normKey = getNormalizedFacilityKey(facilityName);
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_FACILITY_OVERRIDE, Context.MODE_PRIVATE);
            String val = prefs.getString(normKey, null);
            if (val == null) {
                val = prefs.getString(facilityName.trim(), null);
            }
            if (val != null) {
                memoryStatusOverrides.put(normKey, val);
                return val;
            }
        }
        return memoryStatusOverrides.get(normKey);
    }

    public String getFacilityMaintenanceStartDate(Context context, String facilityName) {
        if (facilityName == null) return null;
        String normKey = getNormalizedFacilityKey(facilityName);
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_FACILITY_OVERRIDE, Context.MODE_PRIVATE);
            String s = prefs.getString(normKey + "_start", null);
            if (s != null) {
                memoryMaintStartDates.put(normKey, s);
                return s;
            }
        }
        return memoryMaintStartDates.get(normKey);
    }

    public String getFacilityMaintenanceEndDate(Context context, String facilityName) {
        if (facilityName == null) return null;
        String normKey = getNormalizedFacilityKey(facilityName);
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_FACILITY_OVERRIDE, Context.MODE_PRIVATE);
            String e = prefs.getString(normKey + "_end", null);
            if (e != null) {
                memoryMaintEndDates.put(normKey, e);
                return e;
            }
        }
        return memoryMaintEndDates.get(normKey);
    }

    public boolean isFacilityUnderMaintenanceOnDate(Context context, String facilityName, String targetIsoDate) {
        if (facilityName == null) return false;
        String normKey = getNormalizedFacilityKey(facilityName);
        String status = getFacilityStatusOverride(context, normKey);
        if (status == null || "Available".equalsIgnoreCase(status) || "Open".equalsIgnoreCase(status) || "Ready".equalsIgnoreCase(status)) {
            return false;
        }
        String startIso = getFacilityMaintenanceStartDate(context, normKey);
        String endIso = getFacilityMaintenanceEndDate(context, normKey);
        if (startIso == null || endIso == null || targetIsoDate == null) {
            return true; // Indefinite maintenance until set back to Available
        }
        Date target = parseAnyDate(targetIsoDate);
        Date start = parseAnyDate(startIso);
        Date end = parseAnyDate(endIso);
        if (target == null || start == null || end == null) return true;
        return !target.before(start) && !target.after(end);
    }

    public String getNormalizedFacilityKey(String name) {
        if (name == null) return "";
        String key = name.toLowerCase().trim();
        if (key.contains("room 1") || key.contains("101") || key.contains("suite 101")) return "Room 1";
        if (key.contains("room 2") || key.contains("102") || key.contains("suite 102") || key.contains("deluxe 102")) return "Room 2";
        if (key.contains("room 3") || key.contains("103") || key.contains("201") || key.contains("suite 103") || key.contains("family")) return "Room 3";
        if (key.contains("court 1") || key.contains("crt-01")) return "Court 1";
        if (key.contains("court 2") || key.contains("crt-02")) return "Court 2";
        if (key.contains("court 3") || key.contains("crt-03") || key.contains("basketball") || key.contains("bk")) return "Court 3";
        if (key.contains("buyout") || key.contains("exclusive")) return "Full Resort Buyout";
        return name.trim();
    }

    // --- FACILITY BOOKED CHECK HELPERS ---

    public static Date parseAnyDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || "null".equalsIgnoreCase(dateStr.trim())) {
            return null;
        }
        String clean = dateStr.trim();
        if (clean.length() >= 10 && clean.charAt(4) == '-' && clean.charAt(7) == '-') {
            clean = clean.substring(0, 10);
        }
        String[] formats = {
                "yyyy-MM-dd",
                "EEE, MMM dd, yyyy",
                "EEE, MMM dd yyyy",
                "MMM dd, yyyy",
                "MM/dd/yyyy",
                "yyyy/MM/dd"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setLenient(false);
                Date parsed = sdf.parse(clean);
                if (parsed != null) return stripTime(parsed);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Date stripTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private boolean matchesRoomFacility(String searchKey, String title, String roomId) {
        String normSearch = getNormalizedFacilityKey(searchKey);
        if ("Full Resort Buyout".equalsIgnoreCase(normSearch)) {
            return true; // Whole resort buyout overlaps with any room
        }
        String normTitle = getNormalizedFacilityKey(title);
        String normRoomId = getNormalizedFacilityKey(roomId);
        if ("Full Resort Buyout".equalsIgnoreCase(normTitle)) {
            return true;
        }
        return (!normSearch.isEmpty() && (normSearch.equalsIgnoreCase(normTitle) || normSearch.equalsIgnoreCase(normRoomId)));
    }

    public boolean isRoomBookedForDates(String roomIdOrTitle, String checkInIso, String checkOutIso) {
        if (roomIdOrTitle == null || checkInIso == null || checkOutIso == null) return false;

        try {
            Date targetIn = parseAnyDate(checkInIso);
            Date targetOut = parseAnyDate(checkOutIso);
            if (targetIn == null) return false;

            if (targetOut == null || !targetOut.after(targetIn)) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(targetIn);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                targetOut = cal.getTime();
            }

            // 1. Check Admin Maintenance / Cleaning / Unavailable Date Range first!
            String normKey = getNormalizedFacilityKey(roomIdOrTitle);
            String overrideStatus = memoryStatusOverrides.get(normKey);
            if (overrideStatus != null && !"Available".equalsIgnoreCase(overrideStatus)
                    && !"Open".equalsIgnoreCase(overrideStatus) && !"Ready".equalsIgnoreCase(overrideStatus)) {
                String mStartStr = memoryMaintStartDates.get(normKey);
                String mEndStr = memoryMaintEndDates.get(normKey);
                if (mStartStr != null && mEndStr != null) {
                    Date mStart = parseAnyDate(mStartStr);
                    Date mEnd = parseAnyDate(mEndStr);
                    if (mStart != null && mEnd != null) {
                        if (!mEnd.after(mStart)) {
                            Calendar c = Calendar.getInstance();
                            c.setTime(mStart);
                            c.add(Calendar.DAY_OF_MONTH, 1);
                            mEnd = c.getTime();
                        }
                        if (mStart.before(targetOut) && mEnd.after(targetIn)) {
                            return true; // Blocked by Admin Maintenance dates!
                        }
                    }
                } else {
                    return true; // Blocked indefinitely until set to Available
                }
            }

            // 2. Check active guest bookings
            for (Booking b : bookingsList) {
                if (b == null) continue;
                String status = b.getStatus();
                if ("Confirmed".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status) || "Checked-In".equalsIgnoreCase(status)) {
                    String title = b.getTitle() != null ? b.getTitle() : "";
                    String bRoomId = b.getRoomId() != null ? b.getRoomId() : "";

                    if (!matchesRoomFacility(roomIdOrTitle, title, bRoomId)) {
                        continue;
                    }

                    String exInStr = b.getCheckInDate();
                    String exOutStr = b.getCheckOutDate();

                    if (exInStr == null || exInStr.isEmpty() || "null".equalsIgnoreCase(exInStr)) {
                        exInStr = b.getDate();
                    }
                    if (exOutStr == null || exOutStr.isEmpty() || "null".equalsIgnoreCase(exOutStr)) {
                        exOutStr = exInStr;
                    }

                    if (exInStr != null && !exInStr.isEmpty()) {
                        Date existingIn = parseAnyDate(exInStr);
                        Date existingOut = parseAnyDate(exOutStr);

                        if (existingIn != null) {
                            if (existingOut == null || !existingOut.after(existingIn)) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(existingIn);
                                cal.add(Calendar.DAY_OF_MONTH, 1);
                                existingOut = cal.getTime();
                            }

                            if (existingIn.before(targetOut) && existingOut.after(targetIn)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    public boolean hasActiveGuestBooking(String facilityName) {
        if (facilityName == null || facilityName.isEmpty()) return false;
        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String todayIso = isoFmt.format(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, 1);
        String tomorrowIso = isoFmt.format(cal.getTime());

        // Check only real guest bookings (excluding MAINT- blocks) covering TODAY
        Date today = parseAnyDate(todayIso);
        Date tomorrow = parseAnyDate(tomorrowIso);
        if (today == null || tomorrow == null) return false;

        for (Booking b : bookingsList) {
            if (b == null) continue;
            if (b.getBookingId() != null && b.getBookingId().startsWith("MAINT-")) continue;
            String status = b.getStatus();
            if ("Confirmed".equalsIgnoreCase(status) || "Checked-In".equalsIgnoreCase(status)) {
                if (!matchesRoomFacility(facilityName, b.getTitle(), b.getRoomId())) continue;
                String inStr = b.getCheckInDate() != null && !b.getCheckInDate().isEmpty() ? b.getCheckInDate() : b.getDate();
                String outStr = b.getCheckOutDate() != null && !b.getCheckOutDate().isEmpty() ? b.getCheckOutDate() : inStr;
                Date exIn = parseAnyDate(inStr);
                Date exOut = parseAnyDate(outStr);
                if (exIn != null) {
                    if (exOut == null || !exOut.after(exIn)) {
                        Calendar c = Calendar.getInstance();
                        c.setTime(exIn);
                        c.add(Calendar.DAY_OF_MONTH, 1);
                        exOut = c.getTime();
                    }
                    if (exIn.before(tomorrow) && exOut.after(today)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getFacilityDisplayStatus(Context context, String facilityName) {
        if (facilityName == null || facilityName.isEmpty()) return "Available";

        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayIso = isoFmt.format(new Date());

        if (isFacilityUnderMaintenanceOnDate(context, facilityName, todayIso)) {
            String override = getFacilityStatusOverride(context, facilityName);
            if ("Cleaning".equalsIgnoreCase(override)) return "Cleaning";
            if ("Unavailable".equalsIgnoreCase(override) || "Occupied".equalsIgnoreCase(override)) return "Unavailable";
            return "Maintenance";
        }

        if (hasActiveGuestBooking(facilityName)) {
            return "Booked";
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
        if (isFacilityBooked(context, "Room 1")) count++;
        if (isFacilityBooked(context, "Room 2")) count++;
        if (isFacilityBooked(context, "Room 3")) count++;
        return count;
    }

    public int getConfirmedRoomsCount() {
        return getConfirmedRoomsCount(null);
    }

    // --- BOOKING ADD METHODS (ALL OVERLOADS WITH DEDUPLICATION) ---

    public synchronized void addBooking(Context context, Booking booking) {
        if (booking == null) return;
        if (booking.getTitle() == null || booking.getTitle().trim().isEmpty()) return;

        syncBookingCatalogImage(booking);

        String newId = booking.getBookingId() != null ? booking.getBookingId().trim() : "";
        String newRoom = booking.getRoomId() != null ? booking.getRoomId().trim() : "";
        String newIn = booking.getCheckInDate() != null ? booking.getCheckInDate().trim() : "";
        String newOut = booking.getCheckOutDate() != null ? booking.getCheckOutDate().trim() : "";

        for (int i = 0; i < bookingsList.size(); i++) {
            Booking existing = bookingsList.get(i);
            if (existing == null) continue;
            String exId = existing.getBookingId() != null ? existing.getBookingId().trim() : "";
            String exRoom = existing.getRoomId() != null ? existing.getRoomId().trim() : "";
            String exIn = existing.getCheckInDate() != null ? existing.getCheckInDate().trim() : "";
            String exOut = existing.getCheckOutDate() != null ? existing.getCheckOutDate().trim() : "";

            boolean sameId = !newId.isEmpty() && newId.equalsIgnoreCase(exId);
            boolean sameRoomAndDates = !newRoom.isEmpty() && newRoom.equalsIgnoreCase(exRoom)
                    && !newIn.isEmpty() && newIn.equalsIgnoreCase(exIn)
                    && !newOut.isEmpty() && newOut.equalsIgnoreCase(exOut)
                    && !"Cancelled".equalsIgnoreCase(existing.getStatus())
                    && !"Cancelled".equalsIgnoreCase(booking.getStatus());
            boolean sameDetails = newId.isEmpty() && exId.isEmpty()
                    && existing.getTitle() != null && existing.getTitle().equalsIgnoreCase(booking.getTitle())
                    && existing.getDate() != null && existing.getDate().equalsIgnoreCase(booking.getDate())
                    && existing.getDetail1() != null && existing.getDetail1().equalsIgnoreCase(booking.getDetail1());

            if (sameId || sameRoomAndDates || sameDetails) {
                if (!newId.isEmpty()) {
                    existing.setBookingId(newId);
                }

                // Do not downgrade an already Confirmed local booking back to Pending if remote sync lags
                if (!("Confirmed".equalsIgnoreCase(existing.getStatus()) && "Pending".equalsIgnoreCase(booking.getStatus()))) {
                    existing.setStatus(booking.getStatus());
                }

                if (booking.getTotalAmount() >= existing.getTotalAmount() && booking.getPrice() != null && !booking.getPrice().isEmpty()) {
                    existing.setPrice(booking.getPrice());
                    existing.setTotalAmount(booking.getTotalAmount());
                }
                if (booking.getImagePath() != null && !booking.getImagePath().trim().isEmpty()) {
                    existing.setImagePath(booking.getImagePath());
                }
                if (booking.getImageResId() != 0 && booking.getImageResId() != R.drawable.rooms_icon_img) {
                    existing.setImageResId(booking.getImageResId());
                }
                if (booking.getCustomerId() != null && !booking.getCustomerId().isEmpty()) {
                    existing.setCustomerId(booking.getCustomerId());
                }
                if (booking.getRoomId() != null && !booking.getRoomId().isEmpty()) {
                    existing.setRoomId(booking.getRoomId());
                }
                if (booking.getCourtId() != null && !booking.getCourtId().isEmpty()) {
                    existing.setCourtId(booking.getCourtId());
                }
                if (booking.getCheckInDate() != null && !booking.getCheckInDate().isEmpty()) {
                    existing.setCheckInDate(booking.getCheckInDate());
                }
                if (booking.getCheckOutDate() != null && !booking.getCheckOutDate().isEmpty()) {
                    existing.setCheckOutDate(booking.getCheckOutDate());
                }
                if (booking.getCreatedAt() != null && !booking.getCreatedAt().isEmpty()) {
                    existing.setCreatedAt(booking.getCreatedAt());
                }
                if (booking.getHoldExpiresAt() != null && !booking.getHoldExpiresAt().isEmpty()) {
                    existing.setHoldExpiresAt(booking.getHoldExpiresAt());
                }
                if (existing.isHoldExpired()) {
                    existing.setStatus("Cancelled");
                    existing.setDetail2("Hold expired (15-min window ended)");
                }
                syncBookingCatalogImage(existing);
                if (context != null) {
                    saveBookings(context);
                }
                return;
            }
        }

        if (booking.isHoldExpired()) {
            booking.setStatus("Cancelled");
            booking.setDetail2("Hold expired (15-min window ended)");
        }
        bookingsList.add(0, booking);
        if (context != null) {
            saveBookings(context);
        }
    }

    public void addBooking(Booking booking) {
        addBooking(null, booking);
    }

    public void addBooking(Context context, String title, String status, String date, String detail1, String detail2, String price) {
        addBooking(context, new Booking(title, status, date, detail1, detail2, price));
    }

    public void addBooking(Context context, String title, String status, String date, String detail1, String detail2, String price, String imagePath, int imageResId) {
        addBooking(context, new Booking(title, status, date, detail1, detail2, price, imagePath, imageResId));
    }

    public void addBooking(Context context, String title, String date, String detail1, String detail2, String price) {
        addBooking(context, title, "Confirmed", date, detail1, detail2, price);
    }

    public void addBooking(String title, String status, String date, String detail1, String detail2, String price) {
        addBooking(null, title, status, date, detail1, detail2, price);
    }

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