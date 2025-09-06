package com.example.userservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    private String username;
    private String fname;
    private String lname;
    private String address;
    private String phoneNumber;

    public User() {
    }

    public User(String username, String fname, String lname, String address, String phoneNumber) {
        this.username = username;
        this.fname = fname;
        this.lname = lname;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString() {
        return "User{"
                + "username='" + username + "'\n" +
                ", fname='" + fname + "'\n" +
                ", lname='" + lname + "'\n" +
                ", address='" + address + "'\n" +
                ", phoneNumber='" + phoneNumber + "'"
                + "}";
    }
}