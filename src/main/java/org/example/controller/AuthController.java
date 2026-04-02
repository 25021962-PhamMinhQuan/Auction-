package org.example.controller;

import org.example.model.user.User;
import org.example.service.UserService;

public class AuthController {

    private UserService userService = new UserService();

    // giả lập POST /register
    public String register(User user) {
        return userService.register(user);
    }

    // giả lập POST /login
    public User login(String username, String password) {
        return userService.login(username, password);
    }
}

