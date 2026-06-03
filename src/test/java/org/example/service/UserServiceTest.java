package org.example.service;

import org.example.domain.user.Bidder;
import org.example.domain.user.Seller;
import org.example.domain.user.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserService Tests")
public class UserServiceTest {

    // ---- Mock UserRepository (không cần Mockito) ----
    private static class MockUserRepository implements UserRepository {
        private final java.util.Map<String, User> byUsername = new java.util.HashMap<>();
        private final java.util.Map<String, User> byId = new java.util.HashMap<>();

        @Override
        public void save(User user) {
            byUsername.put(user.getUsername(), user);
            byId.put(user.getId(), user);
        }

        @Override
        public User findByUsername(String username) {
            return byUsername.get(username);
        }

        @Override
        public User findById(String id) {
            return byId.get(id);
        }

        @Override
        public List<User> findAll() {
            return new java.util.ArrayList<>(byId.values());
        }

        @Override
        public void delete(String id) {
            User u = byId.remove(id);
            if (u != null) byUsername.remove(u.getUsername());
        }

        @Override
        public void updateProfile(User user) {
            save(user);
        }

        @Override
        public void updatePassword(String id, String hashed) {
            User u = byId.get(id);
            if (u != null) {
                User updated = u.cloneWithNewPassword(u, hashed);
                save(updated);
            }
        }

        @Override
        public void updateBalance(String id, double balance) {
            User u = byId.get(id);
            if (u != null) u.setBalance(balance);
        }

        @Override
        public void updateStatus(String userId, String status) {
            User u = byId.get(userId);
            if (u != null) u.setStatus(status);
        }
        }


        private MockUserRepository repo;
    private UserService service;

    // Mật khẩu đủ mạnh: chữ hoa, chữ thường, số, ký tự đặc biệt
    private static final String STRONG_PASSWORD = "Pass@1234";

    @BeforeEach
    void setUp() {
        repo    = new MockUserRepository();
        service = new UserService(repo);
    }

    // ============================================================
    // REGISTER
    // ============================================================

    @Test
    @DisplayName("register() thành công với thông tin hợp lệ (Bidder)")
    void testRegisterBidderSuccess() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        String result = service.register(bidder);
        assertEquals("Register success", result);
        assertNotNull(repo.findByUsername("alice_01"));
    }

    @Test
    @DisplayName("register() thành công với Seller")
    void testRegisterSellerSuccess() {
        Seller seller = new Seller("seller_01", STRONG_PASSWORD);
        String result = service.register(seller);
        assertEquals("Register success", result);
    }

    @Test
    @DisplayName("register() thất bại khi username rỗng")
    void testRegisterEmptyUsername() {
        Bidder bidder = new Bidder("", STRONG_PASSWORD);
        String result = service.register(bidder);
        assertEquals("Username cannot be empty", result);
    }

    @Test
    @DisplayName("register() thất bại khi username đã tồn tại")
    void testRegisterDuplicateUsername() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        service.register(bidder);
        Bidder duplicate = new Bidder("alice_01", STRONG_PASSWORD);
        String result = service.register(duplicate);
        assertEquals("Username already exists", result);
    }

    @Test
    @DisplayName("register() thất bại khi password yếu (thiếu ký tự đặc biệt)")
    void testRegisterWeakPasswordNoSpecial() {
        Bidder bidder = new Bidder("bob_user", "Password1");
        String result = service.register(bidder);
        assertEquals("Password is not strong enough", result);
    }

    @Test
    @DisplayName("register() thất bại khi password quá ngắn (dưới 6 ký tự)")
    void testRegisterPasswordTooShort() {
        Bidder bidder = new Bidder("bob_user", "P@1");
        String result = service.register(bidder);
        assertEquals("Password is not strong enough", result);
    }

    @Test
    @DisplayName("register() thất bại khi password thiếu chữ hoa")
    void testRegisterPasswordNoUpperCase() {
        Bidder bidder = new Bidder("bob_user", "pass@123");
        String result = service.register(bidder);
        assertEquals("Password is not strong enough", result);
    }

    @Test
    @DisplayName("register() thất bại khi username có ký tự đặc biệt ngoài '_'")
    void testRegisterUsernameWithSpecialChar() {
        Bidder bidder = new Bidder("alice@01", STRONG_PASSWORD);
        String result = service.register(bidder);
        assertTrue(result.contains("Tên đăng nhập"));
    }

    @Test
    @DisplayName("register() thất bại khi username quá ngắn (< 6 ký tự)")
    void testRegisterUsernameTooShort() {
        Bidder bidder = new Bidder("ali", STRONG_PASSWORD);
        String result = service.register(bidder);
        assertTrue(result.contains("Tên đăng nhập"));
    }

    @Test
    @DisplayName("register() thất bại khi username quá dài (> 20 ký tự)")
    void testRegisterUsernameTooLong() {
        Bidder bidder = new Bidder("this_username_is_way_too_long_abc", STRONG_PASSWORD);
        String result = service.register(bidder);
        assertTrue(result.contains("Tên đăng nhập"));
    }

    @Test
    @DisplayName("register() lưu password đã được hash (không phải plaintext)")
    void testRegisterPasswordIsHashed() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        service.register(bidder);
        User saved = repo.findByUsername("alice_01");
        assertNotEquals(STRONG_PASSWORD, saved.getPassword(),
                "Mật khẩu được lưu phải là hash, không phải plaintext");
    }

    // ============================================================
    // LOGIN
    // ============================================================

    @Test
    @DisplayName("login() thành công với đúng username và password")
    void testLoginSuccess() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        service.register(bidder);
        User loggedIn = service.login("alice_01", STRONG_PASSWORD);
        assertNotNull(loggedIn);
        assertEquals("alice_01", loggedIn.getUsername());
    }

    @Test
    @DisplayName("login() ném lỗi khi username không tồn tại")
    void testLoginUserNotFound() {
        assertThrows(RuntimeException.class, () -> service.login("ghost_user", STRONG_PASSWORD));
    }

    @Test
    @DisplayName("login() ném lỗi khi password sai")
    void testLoginWrongPassword() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        service.register(bidder);
        assertThrows(RuntimeException.class, () -> service.login("alice_01", "WrongPass@9"));
    }

    // ============================================================
    // UPDATE PROFILE
    // ============================================================

    @Test
    @DisplayName("updateProfile() thành công với email hợp lệ")
    void testUpdateProfileValidEmail() {
        Bidder bidder = new Bidder("USER_001", "alice_01", STRONG_PASSWORD);
        bidder.setEmail("alice@example.com");
        String result = service.updateProfile(bidder);
        assertEquals("Update successful", result);
    }

    @Test
    @DisplayName("updateProfile() thất bại với email không hợp lệ")
    void testUpdateProfileInvalidEmail() {
        Bidder bidder = new Bidder("USER_001", "alice_01", STRONG_PASSWORD);
        bidder.setEmail("not-an-email");
        String result = service.updateProfile(bidder);
        assertEquals("Invalid email", result);
    }

    @Test
    @DisplayName("updateProfile() thành công khi email để trống (không bắt buộc)")
    void testUpdateProfileEmptyEmailAllowed() {
        Bidder bidder = new Bidder("USER_001", "alice_01", STRONG_PASSWORD);
        bidder.setEmail("");
        String result = service.updateProfile(bidder);
        assertEquals("Update successful", result);
    }

    // ============================================================
    // CHANGE PASSWORD
    // ============================================================

    @Test
    @DisplayName("changePassword() thành công với đúng password cũ và password mới đủ mạnh")
    void testChangePasswordSuccess() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        service.register(bidder);
        User saved = repo.findByUsername("alice_01");

        String newPassword = "NewPass@5678";
        String result = service.changePassword(saved, STRONG_PASSWORD, newPassword);
        assertEquals("Password changed successfully.", result);
    }

    @Test
    @DisplayName("changePassword() thất bại khi password cũ sai")
    void testChangePasswordWrongOldPassword() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        service.register(bidder);
        User saved = repo.findByUsername("alice_01");

        String result = service.changePassword(saved, "WrongOld@1", "NewPass@5678");
        assertEquals("The old password is incorrect.", result);
    }

    @Test
    @DisplayName("changePassword() thất bại khi password mới không đủ mạnh")
    void testChangePasswordWeakNewPassword() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        service.register(bidder);
        User saved = repo.findByUsername("alice_01");

        String result = service.changePassword(saved, STRONG_PASSWORD, "weak");
        assertEquals("The new password is not strong enough.", result);
    }

    // ============================================================
    // FIND / DELETE / COUNT
    // ============================================================

    @Test
    @DisplayName("findUser() trả về đúng user theo username")
    void testFindUserByUsername() {
        Bidder bidder = new Bidder("alice_01", STRONG_PASSWORD);
        service.register(bidder);
        User found = service.findUser("alice_01");
        assertNotNull(found);
        assertEquals("alice_01", found.getUsername());
    }

    @Test
    @DisplayName("findUser() trả về null nếu không tồn tại")
    void testFindUserNotFound() {
        assertNull(service.findUser("nobody"));
    }

    @Test
    @DisplayName("findUserById() trả về đúng user theo id")
    void testFindUserById() {
        Bidder bidder = new Bidder("USER_XYZ", "alice_01", STRONG_PASSWORD);
        repo.save(bidder);
        User found = service.findUserById("USER_XYZ");
        assertNotNull(found);
        assertEquals("USER_XYZ", found.getId());
    }

    @Test
    @DisplayName("findAllUsers() trả về tất cả user đã đăng ký")
    void testFindAllUsers() {
        service.register(new Bidder("user_aaa", STRONG_PASSWORD));
        service.register(new Seller("user_bbb", STRONG_PASSWORD));
        List<User> all = service.findAllUsers();
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("deleteUser() xóa user khỏi repository")
    void testDeleteUser() {
        Bidder bidder = new Bidder("USER_DEL", "alice_01", STRONG_PASSWORD);
        repo.save(bidder);
        service.deleteUser("USER_DEL");
        assertNull(repo.findById("USER_DEL"));
    }

    @Test
    @DisplayName("countAllUsers() trả về số lượng user đúng")
    void testCountAllUsers() {
        service.register(new Bidder("user_aaa", STRONG_PASSWORD));
        service.register(new Seller("user_bbb", STRONG_PASSWORD));
        assertEquals(2, service.countAllUsers());
    }

    @Test
    @DisplayName("countAllUsers() trả về 0 khi không có user nào")
    void testCountAllUsersEmpty() {
        assertEquals(0, service.countAllUsers());
    }
}