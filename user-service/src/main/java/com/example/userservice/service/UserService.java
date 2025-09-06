package com.example.userservice.service;

import com.example.userservice.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> findAllUsers();
    Optional<User> findUserByUsername(String username);
    User saveUser(User user);
    void deleteUserByUsername(String username);
}
