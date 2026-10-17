package com.example.casavista.models;

public class Employee {
    private int id;
    private String staffId;
    private String name;
    private String department;
    private String role;
    private String username;
    private String email;
    private String password;

    public Employee(int id, String name, String department, String role) {
        this(id, String.format("STF-%03d", id), name, department, role,
                name != null ? name.toLowerCase().replaceAll("[^a-z0-9]", "") : "staff",
                (name != null ? name.toLowerCase().replaceAll("\\s+", ".") : "staff") + "@casavista.com",
                "Staff123!");
    }

    public Employee(int id, String staffId, String name, String department, String role, String username, String email, String password) {
        this.id = id;
        this.staffId = (staffId != null && !staffId.trim().isEmpty()) ? staffId.trim() : String.format("STF-%03d", id);
        this.name = name;
        this.department = department;
        this.role = role;
        this.username = (username != null && !username.trim().isEmpty()) ? username.trim() : "staff";
        this.email = (email != null && !email.trim().isEmpty()) ? email.trim().toLowerCase() : (this.username + "@casavista.com");
        this.password = (password != null && !password.trim().isEmpty()) ? password : "Staff123!";
    }

    public int getId() { return id; }
    public String getStaffId() { return staffId; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

    public void setStaffId(String staffId) { this.staffId = staffId; }
    public void setName(String name) { this.name = name; }
    public void setDepartment(String department) { this.department = department; }
    public void setRole(String role) { this.role = role; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
}