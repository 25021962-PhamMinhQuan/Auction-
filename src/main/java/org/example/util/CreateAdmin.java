package org.example.util;

import org.example.domain.user.Admin;
import org.example.factory.ServiceFactory;
import org.example.service.UserService;

public class CreateAdmin {
    public static void main(String[] args) {
        UserService userService = ServiceFactory.getInstance().getUserService();

        String result = userService.register(
                new Admin("admin01", "Admin@123")
        );

        System.out.println(result);
    }
}
