package org.example.repository;

import org.example.domain.user.User;

import java.util.List;

public interface UserRepository {
    void save(User user);
    User findByUsername(String name);
    User findById(String id);
    void updateProfile(User user);
    void updatePassword(String userId, String hashedPassword);
    List<User> findAll();
    void delete(String id);
    void updateBalance(String userId, double newBalance);
    void updateStatus(String userId, String status);

}
