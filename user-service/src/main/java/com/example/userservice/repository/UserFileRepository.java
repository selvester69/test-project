
package com.example.userservice.repository;

import com.example.userservice.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserFileRepository {

    private static final String FILE_NAME = "mock-user.csv";
    private List<User> users = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadUsersFromFile();
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    public User save(User user) {
        // Check if user already exists
        Optional<User> existingUser = findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            // Update existing user
            users.remove(existingUser.get());
            users.add(user);
        } else {
            // Add new user
            users.add(user);
        }
        saveUsersToFile();
        return user;
    }

    public boolean deleteByUsername(String username) {
        Optional<User> userToDelete = findByUsername(username);
        if (userToDelete.isPresent()) {
            users.remove(userToDelete.get());
            saveUsersToFile();
            return true;
        }
        return false;
    }

    private void loadUsersFromFile() {
        users.clear();
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            // Create file with headers if it doesn't exist
            try {
                file.createNewFile();
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    writer.println("username,fname,lname,address,phoneNumber");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Skip header line
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1); // -1 to keep empty strings
                if (parts.length == 5) {
                    User user = new User(
                            parts[0].trim(), // username
                            parts[1].trim(), // fname
                            parts[2].trim(), // lname
                            parts[3].trim(), // address
                            parts[4].trim() // phoneNumber
                    );
                    users.add(user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUsersToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            // Write header
            writer.println("username,fname,lname,address,phoneNumber");

            // Write user data
            for (User user : users) {
                writer.println(String.format("%s,%s,%s,%s,%s",
                        user.getUsername(),
                        user.getFname(),
                        user.getLname(),
                        user.getAddress(),
                        user.getPhoneNumber()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}