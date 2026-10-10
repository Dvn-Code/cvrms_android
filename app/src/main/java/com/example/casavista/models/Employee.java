package com.example.casavista.models;

public class Employee {
    private int id;
    private String name;
    private String department;
    private String role;

    public Employee(int id, String name, String department, String role) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.role = role;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }

    public void setName(String name) { this.name = name; }
    public void setDepartment(String department) { this.department = department; }
    public void setRole(String role) { this.role = role; }
}