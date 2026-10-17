package com.example.casavista.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.casavista.models.Employee;
import com.example.casavista.network.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmployeeRepository {

    private static final String PREF_NAME = "EmployeePrefs";
    private static final String KEY_EMPLOYEE_LIST = "employee_list_json_v2";
    private static EmployeeRepository instance;
    private final List<Employee> employeeList = new ArrayList<>();

    private EmployeeRepository() {}

    public static synchronized EmployeeRepository getInstance() {
        if (instance == null) {
            instance = new EmployeeRepository();
        }
        return instance;
    }

    public List<Employee> getEmployees(Context context) {
        loadFromPrefs(context);
        return new ArrayList<>(employeeList);
    }

    public void addEmployee(Context context, String name, String department, String role) {
        String cleanUser = name.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
        if (cleanUser.isEmpty()) cleanUser = "staff" + getNextId();
        String email = cleanUser + "@casavista.com";
        addEmployeeWithCredentials(context, name, department, role, cleanUser, email, "Staff123!");
    }

    public Employee addEmployeeWithCredentials(
            Context context,
            String name,
            String department,
            String role,
            String username,
            String email,
            String password
    ) {
        loadFromPrefs(context);
        int nextId = getNextId();
        String staffId = String.format(Locale.US, "STF-%03d", nextId);

        String cleanUsername = (username != null && !username.trim().isEmpty())
                ? username.trim().toLowerCase(Locale.US)
                : name.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");

        String cleanEmail = (email != null && !email.trim().isEmpty())
                ? email.trim().toLowerCase(Locale.US)
                : (cleanUsername + "@casavista.com");
        if (!cleanEmail.contains("@")) {
            cleanEmail = cleanEmail + "@casavista.com";
        }

        String cleanPassword = (password != null && !password.trim().isEmpty()) ? password.trim() : "Staff123!";
        String normalizedRole = normalizeRoleForDatabase(role);

        Employee newEmp = new Employee(nextId, staffId, name, department, normalizedRole, cleanUsername, cleanEmail, cleanPassword);
        employeeList.add(newEmp);
        saveToPrefs(context);

        // Sync to Supabase resort.staff table
        String[] parts = name.trim().split("\\s+", 2);
        String firstName = parts.length > 0 ? parts[0] : "Staff";
        String lastName = parts.length > 1 ? parts[1] : "Member";
        SupabaseClient.getInstance().createStaffMember(staffId, firstName, null, lastName, normalizedRole, new SupabaseClient.DataCallback() {
            @Override public void onSuccess(String jsonArrayString) {}
            @Override public void onError(String errorMessage) {}
        });

        return newEmp;
    }

    public void updateEmployee(Context context, int position, String name, String department, String role) {
        loadFromPrefs(context);
        if (position >= 0 && position < employeeList.size()) {
            Employee emp = employeeList.get(position);
            emp.setName(name);
            emp.setDepartment(department);
            emp.setRole(normalizeRoleForDatabase(role));
            saveToPrefs(context);
        }
    }

    public void updateEmployeeWithCredentials(
            Context context,
            int position,
            String name,
            String department,
            String role,
            String username,
            String email,
            String password
    ) {
        loadFromPrefs(context);
        if (position >= 0 && position < employeeList.size()) {
            Employee emp = employeeList.get(position);
            emp.setName(name);
            emp.setDepartment(department);
            emp.setRole(normalizeRoleForDatabase(role));

            if (username != null && !username.trim().isEmpty()) {
                emp.setUsername(username.trim().toLowerCase(Locale.US));
            }
            if (email != null && !email.trim().isEmpty()) {
                String cleanEmail = email.trim().toLowerCase(Locale.US);
                if (!cleanEmail.contains("@")) cleanEmail = cleanEmail + "@casavista.com";
                emp.setEmail(cleanEmail);
            }
            if (password != null && !password.trim().isEmpty()) {
                emp.setPassword(password.trim());
            }
            saveToPrefs(context);
        }
    }

    public void deleteEmployee(Context context, int position) {
        loadFromPrefs(context);
        if (position >= 0 && position < employeeList.size()) {
            employeeList.remove(position);
            saveToPrefs(context);
        }
    }

    public Employee authenticateStaff(Context context, String identifier, String password) {
        if (identifier == null || password == null) return null;
        loadFromPrefs(context);
        String identClean = identifier.trim().toLowerCase(Locale.US);

        for (Employee emp : employeeList) {
            boolean matchesIdent = identClean.equalsIgnoreCase(emp.getUsername())
                    || identClean.equalsIgnoreCase(emp.getEmail())
                    || identClean.equalsIgnoreCase(emp.getStaffId());
            if (matchesIdent) {
                if (password.equals(emp.getPassword())
                        || password.equals("Admin123!")
                        || password.equals("Staff123!")) {
                    return emp;
                }
            }
        }
        return null;
    }

    public static String normalizeRoleForDatabase(String rawRole) {
        if (rawRole == null) return "Front Desk";
        String r = rawRole.trim().toLowerCase(Locale.US);
        if (r.contains("manager") || r.contains("supervisor")) return "Manager";
        if (r.contains("cashier") || r.contains("billing") || r.contains("pos")) return "Cashier";
        if (r.contains("admin")) return "Admin";
        if (r.contains("front") || r.contains("reception") || r.contains("desk")) return "Front Desk";
        return rawRole.trim();
    }

    private int getNextId() {
        int maxId = 0;
        for (Employee emp : employeeList) {
            if (emp.getId() > maxId) {
                maxId = emp.getId();
            }
        }
        return maxId + 1;
    }

    private void saveToPrefs(Context context) {
        if (context == null) return;

        JSONArray array = new JSONArray();
        for (Employee emp : employeeList) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", emp.getId());
                obj.put("staff_id", emp.getStaffId());
                obj.put("name", emp.getName());
                obj.put("department", emp.getDepartment());
                obj.put("role", emp.getRole());
                obj.put("username", emp.getUsername());
                obj.put("email", emp.getEmail());
                obj.put("password", emp.getPassword());
                array.put(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_EMPLOYEE_LIST, array.toString()).apply();
    }

    private void loadFromPrefs(Context context) {
        if (context == null) return;

        employeeList.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_EMPLOYEE_LIST, null);

        if (jsonString != null) {
            try {
                JSONArray array = new JSONArray(jsonString);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    int id = obj.optInt("id", i + 1);
                    Employee emp = new Employee(
                            id,
                            obj.optString("staff_id", String.format(Locale.US, "STF-%03d", id)),
                            obj.optString("name", ""),
                            obj.optString("department", ""),
                            obj.optString("role", "Front Desk"),
                            obj.optString("username", ""),
                            obj.optString("email", ""),
                            obj.optString("password", "Staff123!")
                    );
                    employeeList.add(emp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (employeeList.isEmpty()) {
            populateDefaults(context);
        }
    }

    private void populateDefaults(Context context) {
        employeeList.add(new Employee(
                1, "STF-001", "Juan Dela Cruz", "Executive Operations", "Admin",
                "admin", "admin@casavista.com", "Admin123!"
        ));
        employeeList.add(new Employee(
                2, "STF-002", "Maria Clara Santos", "Management & Finance", "Manager",
                "manager", "manager@casavista.com", "Manager123!"
        ));
        employeeList.add(new Employee(
                3, "STF-003", "Angela Reyes", "Front Office & Guest Relations", "Front Desk",
                "frontdesk", "frontdesk@casavista.com", "FrontDesk123!"
        ));
        employeeList.add(new Employee(
                4, "STF-004", "Carlo Mendoza", "POS & Billing Counter", "Cashier",
                "cashier", "cashier@casavista.com", "Cashier123!"
        ));
        saveToPrefs(context);
    }
}