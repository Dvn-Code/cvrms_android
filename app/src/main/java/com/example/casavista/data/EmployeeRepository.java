package com.example.casavista.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.casavista.models.Employee;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {

    private static final String PREF_NAME = "EmployeePrefs";
    private static final String KEY_EMPLOYEE_LIST = "employee_list_json";
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
        int nextId = getNextId();
        Employee newEmp = new Employee(nextId, name, department, role);
        employeeList.add(newEmp);
        saveToPrefs(context);
    }

    public void updateEmployee(Context context, int position, String name, String department, String role) {
        if (position >= 0 && position < employeeList.size()) {
            Employee emp = employeeList.get(position);
            emp.setName(name);
            emp.setDepartment(department);
            emp.setRole(role);
            saveToPrefs(context);
        }
    }

    public void deleteEmployee(Context context, int position) {
        if (position >= 0 && position < employeeList.size()) {
            employeeList.remove(position);
            saveToPrefs(context);
        }
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
                obj.put("name", emp.getName());
                obj.put("department", emp.getDepartment());
                obj.put("role", emp.getRole());
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
                    Employee emp = new Employee(
                            obj.optInt("id", 0),
                            obj.optString("name", ""),
                            obj.optString("department", ""),
                            obj.optString("role", "")
                    );
                    employeeList.add(emp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Default initial data if no records exist yet
        if (employeeList.isEmpty()) {
            populateDefaults(context);
        }
    }

    private void populateDefaults(Context context) {
        employeeList.add(new Employee(1, "Jopet Tan", "Front Desk", "Receptionist"));
        employeeList.add(new Employee(2, "Mika Reyes", "Housekeeping", "Staff"));
        employeeList.add(new Employee(3, "Carlo Sison", "Maintenance", "Technician"));
        saveToPrefs(context);
    }
}