package org.example.controller;

import org.example.domain.user.User;
import org.example.factory.ServiceFactory;
import org.example.service.UserService;

public class AuthController {

    private final UserService userService;

    public AuthController(){
        this.userService = ServiceFactory.getInstance().getUserService();
    }
    public String register(User user) {
        return userService.register(user);
    }

    public User login(String username, String password) {
        return userService.login(username, password);
    }
}

