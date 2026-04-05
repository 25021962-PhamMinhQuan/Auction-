package org.example.service;

import org.example.dao.UserDAO;
import org.example.model.user.Admin;
import org.example.model.user.Bidder;
import org.example.model.user.Seller;
import org.example.model.user.User;

import org.mindrot.jbcrypt.BCrypt;

public class UserService {
    private User cloneWithNewPassword(User user, String newPassword) {
        switch (user.getRole()) {
            case "ADMIN":
                return new Admin(user.getId(), user.getUsername(), newPassword);
            case "SELLER":
                return new Seller(user.getId(), user.getUsername(), newPassword);
            case "BIDDER":
                return new Bidder(user.getId(), user.getUsername(), newPassword);
            default:
                throw new IllegalArgumentException("Unknown role");
        }
    }
    private boolean isValidPassword(String password){
        if(password.length() < 8){ return false;}
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        for(char c: password.toCharArray()){
            if(Character.isUpperCase(c)){ hasUpper = true;}
            else if(Character.isLowerCase(c)){ hasLower = true;}
            else if(Character.isDigit(c)){ hasNumber = true;}
            else hasSpecial = true;
        }

        return hasLower && hasNumber && hasUpper && hasSpecial;
    }
    UserDAO userDAO = new UserDAO();
    public String register(User user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return "Username cannot be empty";
        }
        if (userDAO.findByUsername(user.getUsername()) != null) {
            return "Username already exists";
        }
        if(!isValidPassword(user.getPassword())){
            return "Password is not strong enough";
        }
        String HashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user = cloneWithNewPassword(user, HashedPassword);

        // sau khi ktra xem tên user này đã tồn tại chx thì sẽ đến đoạn đki và lưu
        userDAO.save(user);
        return "Register success";
    }

    public User login(String username, String password) {
        User user = userDAO.findByUsername(username);
        // lấy cái object user ra nếu ko tồn tại thì là do tài khỏan không tồn tại thôi
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        // nếu password lấy ra từ object user ko = các password được nhập thì sai
        if (!BCrypt.checkpw(password,user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }
        // Nếu qua 2 bước trên mà chương trình ko bị throw mấy cái lỗi ra thì trả về user thôi.
        return user;
    }
}

