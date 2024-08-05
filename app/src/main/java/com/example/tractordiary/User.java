package com.example.tractordiary;

import com.google.firebase.database.Exclude;

public class User {
    @Exclude
    private String key;
    private String name, date, stime, etime, description, amt, amt2;

    public User() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public User(String name, String date, String stime, String etime, String amt, String amt2, String description) {
        this.name = name;
        this.date = date;
        this.stime = stime;
        this.etime = etime;
        this.description = description;
        this.amt = amt;
        this.amt2 = amt2;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getStime() {
        return stime;
    }

    public String getEtime() {
        return etime;
    }

    public String getDescription() {
        return description;
    }

    public String getAmt() {
        return amt;
    }

    public String getAmt2() {
        return amt2;
    }
}