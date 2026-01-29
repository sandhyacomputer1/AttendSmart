package com.sandhyyasofttech.attendsmart.Models;

public class CompanyItem {
    public String companyName;
    public String companyKey;
    public String companyEmail;
    public String companyPhone;

    public CompanyItem(String companyName, String companyKey, String companyEmail, String companyPhone) {
        this.companyName = companyName;
        this.companyKey = companyKey;
        this.companyEmail = companyEmail;
        this.companyPhone = companyPhone;
    }
}