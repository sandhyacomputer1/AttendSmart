package com.sandhyyasofttech.attendsmart.Models;

public class EmployeeModel {

    private String employeeName;
    private String employeeMobile;
    private String employeeEmail;
    private String employeeRole;
    private String employeeStatus;

    public EmployeeModel() {
        // required for Firebase
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getEmployeeMobile() {
        return employeeMobile;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public String getEmployeeRole() {
        return employeeRole;
    }

    public String getEmployeeStatus() {
        return employeeStatus;
    }
}
