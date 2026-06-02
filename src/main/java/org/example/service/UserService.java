package org.example.service;

import org.example.domain.user.User;

import org.example.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserService {
    private final UserRepository UserRepositoryImpl;

    public UserService(UserRepository UserRepositoryImpl){
        this.UserRepositoryImpl = UserRepositoryImpl;
    }

    private boolean isValidPassword(String password){
        if(password.length() < 6){ return false;}
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


    public String register(User user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return "Username cannot be empty";
        }
        if (UserRepositoryImpl.findByUsername(user.getUsername()) != null) {
            return "Username already exists";
        }
        if(!isValidPassword(user.getPassword())){
            return "Password is not strong enough";
        }
        if(!user.getUsername().matches("^[a-zA-Z0-9_]{6,20}$")){
            return "Tên đăng nhập phải có độ dài từ 6 đến 20 kí tự.\n" +
                    "Tên đăng nhập không được có kí tự đặc biệt ngoại trừ \"_\"";
        }
        String HashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user = user.cloneWithNewPassword(user, HashedPassword);

        // sau khi ktra xem tên user này đã tồn tại chx thì sẽ đến đoạn đki và lưu
        UserRepositoryImpl.save(user);
        return "Register success";
    }

    public User login(String username, String password) {
        User user = UserRepositoryImpl.findByUsername(username);
        // lấy cái object user ra nếu ko tồn tại thì là do tài khỏan không tồn tại thôi
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (isLocked(user)) {
            throw new RuntimeException("Account is locked");
        }
        // nếu password lấy ra từ object user ko = các password được nhập thì sai
        if (!BCrypt.checkpw(password,user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }
        // Nếu qua 2 bước trên mà chương trình ko bị throw mấy cái lỗi ra thì trả về user thôi.
        return user;
    }

    public String updateProfile(User user) {
        if (user.getEmail() != null && !user.getEmail().isEmpty()
                && !user.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return "Invalid email";
        }
        UserRepositoryImpl.updateProfile(user);
        return "Update successful";
    }

    public String changePassword(User user, String oldPassword, String newPassword) {
        if (!BCrypt.checkpw(oldPassword, user.getPassword()))
            return "The old password is incorrect.";
        if (!isValidPassword(newPassword))
            return "The new password is not strong enough.";
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        UserRepositoryImpl.updatePassword(user.getId(), hashed);
        return "Password changed successfully.";
    }

    public User findUser(String name){
        return UserRepositoryImpl.findByUsername(name);
    }
    public User findUserById(String id) {
        return UserRepositoryImpl.findById(id);
    }
    public List<User> findAllUsers() {
        return UserRepositoryImpl.findAll();
    }
    public void deleteUser(String userId) {
        UserRepositoryImpl.delete(userId);
    }

    public void lockUser(String userId) {
        User user = UserRepositoryImpl.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Account not found");
        }
        if (User.UserRole.ADMIN.name().equals(user.getRole())) {
            throw new IllegalStateException("Admin account cannot be locked");
        }
        UserRepositoryImpl.updateStatus(userId, "LOCKED");
    }

    public void unlockUser(String userId) {
        User user = UserRepositoryImpl.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Account not found");
        }
        UserRepositoryImpl.updateStatus(userId, "ACTIVE");
    }

    public boolean isLocked(User user) {
        return user != null && "LOCKED".equalsIgnoreCase(user.getStatus());
    }

    public long countAllUsers() {
        return UserRepositoryImpl.findAll().size();
    }

    public long countLockedUsers() {
        return UserRepositoryImpl.findAll().stream()
                .filter(this::isLocked)
                .count();
    }
}

