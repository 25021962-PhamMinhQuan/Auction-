package org.example.repository;

import org.example.domain.user.User;

public interface UserRepository {
    void save(User user);
    User findByUsername(String name);

}
