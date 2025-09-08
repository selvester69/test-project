package com.example.adminservice.service;

import com.example.adminservice.model.Admin;

import java.util.List;
import java.util.Optional;

public interface AdminService {
    List<Admin> findAllAdmins();
    Optional<Admin> findAdminById(Long id);
    Admin saveAdmin(Admin admin);
    void deleteAdminById(Long id);
}
