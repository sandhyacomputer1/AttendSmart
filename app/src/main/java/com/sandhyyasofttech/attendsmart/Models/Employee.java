//package com.sandhyyasofttech.attendsmart.Models;
//
//public class Employee {
//    private String mobile;
//    private String name;
//    private boolean hasSalaryConfig;
//    private String monthlySalary;
//
//    public Employee() {
//        // Required empty constructor for Firebase
//    }
//
//    public Employee(String mobile, String name, boolean hasSalaryConfig, String monthlySalary) {
//        this.mobile = mobile;
//        this.name = name;
//        this.hasSalaryConfig = hasSalaryConfig;
//        this.monthlySalary = monthlySalary;
//    }
//
//    public String getMobile() {
//        return mobile;
//    }
//
//    public void setMobile(String mobile) {
//        this.mobile = mobile;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public boolean isHasSalaryConfig() {
//        return hasSalaryConfig;
//    }
//
//    public void setHasSalaryConfig(boolean hasSalaryConfig) {
//        this.hasSalaryConfig = hasSalaryConfig;
//    }
//
//    public String getMonthlySalary() {
//        return monthlySalary;
//    }
//
//    public void setMonthlySalary(String monthlySalary) {
//        this.monthlySalary = monthlySalary;
//    }
//}


package com.sandhyyasofttech.attendsmart.Models;

public class Employee {
    private String mobile;
    private String name;
    private String department;
    private boolean hasSalaryConfig;
    private String monthlySalary;

    public Employee(String mobile, String name, String department,
                    boolean hasSalaryConfig, String monthlySalary) {
        this.mobile = mobile;
        this.name = name;
        this.department = department;
        this.hasSalaryConfig = hasSalaryConfig;
        this.monthlySalary = monthlySalary;
    }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isHasSalaryConfig() { return hasSalaryConfig; }
    public void setHasSalaryConfig(boolean hasSalaryConfig) { this.hasSalaryConfig = hasSalaryConfig; }

    public String getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(String monthlySalary) { this.monthlySalary = monthlySalary; }
}