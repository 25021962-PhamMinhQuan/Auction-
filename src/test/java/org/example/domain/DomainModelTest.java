package org.example.domain;

import org.example.domain.auction.BidTransaction;
import org.example.domain.item.*;
import org.example.domain.user.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Model Tests")
public class DomainModelTest {

    private static final LocalDateTime START = LocalDateTime.now().plusMinutes(5);
    private static final LocalDateTime END   = START.plusHours(2);

    // ================================================================
    // ITEM
    // ================================================================

    @Test
    @DisplayName("Electronics khởi tạo đúng các thuộc tính")
    void testElectronicsInit() {
        Electronics e = new Electronics("iPad Pro", "Apple tablet", 15_000_000, START, END, "ipad.jpg");
        assertEquals("iPad Pro",     e.getName());
        assertEquals("Apple tablet", e.getDescription());
        assertEquals(15_000_000,     e.getStartPrice(),   0.001);
        assertEquals(15_000_000,     e.getCurrentPrice(), 0.001);
        assertEquals(START,          e.getStartTime());
        assertEquals(END,            e.getEndTime());
        assertEquals("ipad.jpg",     e.getImageUrl());
        assertEquals("ELECTRONICS",  e.getType());
        assertNotNull(e.getId());
        assertTrue(e.getId().startsWith("ITEM_"));
    }

    @Test
    @DisplayName("Art getType() trả về ART")
    void testArtType() {
        Art art = new Art("Starry Night", "Van Gogh", 200_000_000, START, END, "art.jpg");
        assertEquals("ART", art.getType());
    }

    @Test
    @DisplayName("Item.setCurrentPrice() cập nhật đúng currentPrice")
    void testSetCurrentPrice() {
        Electronics e = new Electronics("Phone", "Desc", 5_000_000, START, END, "img.jpg");
        e.setCurrentPrice(7_000_000);
        assertEquals(7_000_000, e.getCurrentPrice(), 0.001);
        // startPrice không đổi
        assertEquals(5_000_000, e.getStartPrice(), 0.001);
    }

    @Test
    @DisplayName("Item.extendTime() gia hạn đúng số giây")
    void testExtendTime() {
        Electronics e = new Electronics("TV", "Smart TV", 3_000_000, START, END, "tv.jpg");
        LocalDateTime before = e.getEndTime();
        e.extendTime(60);
        assertEquals(before.plusSeconds(60), e.getEndTime());
    }

    @Test
    @DisplayName("Item.setName() và setDescription() cập nhật đúng")
    void testItemSetters() {
        Electronics e = new Electronics("Old Name", "Old Desc", 1_000_000, START, END, "img.jpg");
        e.setName("New Name");
        e.setDescription("New Desc");
        assertEquals("New Name", e.getName());
        assertEquals("New Desc", e.getDescription());
    }

    @Test
    @DisplayName("Item.setStartPrice() cập nhật cả startPrice và currentPrice")
    void testSetStartPrice() {
        Electronics e = new Electronics("Laptop", "Desc", 10_000_000, START, END, "img.jpg");
        e.setStartPrice(12_000_000);
        assertEquals(12_000_000, e.getStartPrice(),   0.001);
        assertEquals(12_000_000, e.getCurrentPrice(), 0.001);
    }

    @Test
    @DisplayName("Item từ DB constructor giữ nguyên ID được truyền vào")
    void testItemFromDatabaseConstructor() {
        String existingId = "ITEM_existing-123";
        Electronics e = new Electronics(existingId, "Laptop", "Desc", 5_000_000, START, END, "img.jpg");
        assertEquals(existingId, e.getId());
    }

    @Test
    @DisplayName("Item.setEndTime() và setStartTime() hoạt động đúng")
    void testItemSetTimes() {
        Electronics e = new Electronics("Phone", "Desc", 1_000_000, START, END, "img.jpg");
        LocalDateTime newStart = START.plusDays(1);
        LocalDateTime newEnd   = END.plusDays(1);
        e.setStartTime(newStart);
        e.setEndTime(newEnd);
        assertEquals(newStart, e.getStartTime());
        assertEquals(newEnd,   e.getEndTime());
    }

    // ================================================================
    // USER
    // ================================================================

    @Test
    @DisplayName("Bidder khởi tạo với role BIDDER")
    void testBidderRole() {
        Bidder b = new Bidder("alice_01", "pass");
        assertEquals("BIDDER", b.getRole());
        assertNotNull(b.getId());
        assertTrue(b.getId().startsWith("USER_"));
    }

    @Test
    @DisplayName("Seller khởi tạo với role SELLER")
    void testSellerRole() {
        Seller s = new Seller("seller_01", "pass");
        assertEquals("SELLER", s.getRole());
    }

    @Test
    @DisplayName("Admin khởi tạo với role ADMIN")
    void testAdminRole() {
        Admin a = new Admin("admin_01", "pass");
        assertEquals("ADMIN", a.getRole());
    }

    @Test
    @DisplayName("User khởi tạo với balance mặc định 1_000_000_000")
    void testUserDefaultBalance() {
        Bidder b = new Bidder("alice_01", "pass");
        assertEquals(1_000_000_000.0, b.getBalance(), 0.001);
    }

    @Test
    @DisplayName("User setBalance() cập nhật đúng")
    void testSetBalance() {
        Bidder b = new Bidder("alice_01", "pass");
        b.setBalance(500_000);
        assertEquals(500_000, b.getBalance(), 0.001);
    }

    @Test
    @DisplayName("User setFullName(), setEmail(), setPhone(), setAvatarUrl() hoạt động đúng")
    void testUserProfileSetters() {
        Bidder b = new Bidder("alice_01", "pass");
        b.setFullName("Alice Nguyen");
        b.setEmail("alice@email.com");
        b.setPhone("0912345678");
        b.setAvatarUrl("avatar.png");

        assertEquals("Alice Nguyen", b.getFullName());
        assertEquals("alice@email.com", b.getEmail());
        assertEquals("0912345678", b.getPhone());
        assertEquals("avatar.png", b.getAvatarUrl());
    }

    @Test
    @DisplayName("Bidder.cloneWithNewPassword() tạo ra Bidder mới với password mới, giữ nguyên id và username")
    void testBidderCloneWithNewPassword() {
        Bidder original = new Bidder("USER_123", "alice_01", "OldPass@1");
        User cloned = original.cloneWithNewPassword(original, "NewPass@2");

        assertNotNull(cloned);
        assertEquals("USER_123",   cloned.getId());
        assertEquals("alice_01",   cloned.getUsername());
        assertEquals("NewPass@2",  cloned.getPassword());
        assertInstanceOf(Bidder.class, cloned);
    }

    @Test
    @DisplayName("Seller.cloneWithNewPassword() tạo Seller mới với password mới")
    void testSellerCloneWithNewPassword() {
        Seller original = new Seller("USER_456", "seller_01", "OldPass@1");
        User cloned = original.cloneWithNewPassword(original, "NewPass@2");

        assertNotNull(cloned);
        assertInstanceOf(Seller.class, cloned);
        assertEquals("NewPass@2", cloned.getPassword());
    }

    @Test
    @DisplayName("Admin.cloneWithNewPassword() tạo Admin mới với password mới")
    void testAdminCloneWithNewPassword() {
        Admin original = new Admin("ADMIN_001", "admin_01", "OldPass@1");
        User cloned = original.cloneWithNewPassword(original, "NewPass@2");

        assertNotNull(cloned);
        assertInstanceOf(Admin.class, cloned);
        assertEquals("NewPass@2", cloned.getPassword());
    }

    @Test
    @DisplayName("User constructor đầy đủ (có balance) khởi tạo đúng")
    void testUserFullConstructorWithBalance() {
        Bidder b = new Bidder("USER_001", "alice_01", "pass");
        b.setBalance(50_000_000);
        assertEquals(50_000_000, b.getBalance(), 0.001);
        assertEquals("alice_01", b.getUsername());
    }

    // ================================================================
    // BID TRANSACTION
    // ================================================================

    @Test
    @DisplayName("BidTransaction khởi tạo đúng các thuộc tính")
    void testBidTransactionInit() {
        Bidder bidder = new Bidder("USER_A", "alice", "pass");
        BidTransaction tx = new BidTransaction(bidder, 5_000_000, BidTransaction.BidType.MANUAL);

        assertEquals(bidder,        tx.getBidder());
        assertEquals(5_000_000,     tx.getAmount(), 0.001);
        assertEquals(BidTransaction.BidType.MANUAL, tx.getType());
        assertNotNull(tx.getTime());
    }

    @Test
    @DisplayName("BidTransaction với BidType AUTO khởi tạo đúng")
    void testBidTransactionAutoType() {
        Bidder bidder = new Bidder("USER_B", "bob", "pass");
        BidTransaction tx = new BidTransaction(bidder, 6_000_000, BidTransaction.BidType.AUTO);
        assertEquals(BidTransaction.BidType.AUTO, tx.getType());
    }

    @Test
    @DisplayName("BidTransaction.toString() chứa thông tin bidder và amount")
    void testBidTransactionToString() {
        Bidder bidder = new Bidder("USER_A", "alice", "pass");
        BidTransaction tx = new BidTransaction(bidder, 5_000_000, BidTransaction.BidType.MANUAL);
        String str = tx.toString();
        assertTrue(str.contains("alice"));
        assertTrue(str.contains("5000000.0"));
    }

    // ================================================================
    // DEPOSIT REQUEST
    // ================================================================

    @Test
    @DisplayName("DepositRequest khởi tạo với status PENDING (constructor ngắn)")
    void testDepositRequestNewInit() {
        DepositRequest req = new DepositRequest("USER_A", "alice", 1_000_000, "Nạp tiền");

        assertEquals("USER_A",            req.getUserId());
        assertEquals("alice",             req.getUsername());
        assertEquals(1_000_000,           req.getAmount(), 0.001);
        assertEquals("Nạp tiền",          req.getNote());
        assertEquals(DepositRequest.Status.PENDING, req.getStatus());
        assertNotNull(req.getCreatedAt());
        assertNull(req.getResolvedAt());
    }

    @Test
    @DisplayName("DepositRequest constructor đầy đủ khởi tạo đúng")
    void testDepositRequestFullConstructor() {
        LocalDateTime created  = LocalDateTime.now().minusDays(1);
        LocalDateTime resolved = LocalDateTime.now();
        DepositRequest req = new DepositRequest(
                1, "USER_A", "alice", 2_000_000, "note",
                DepositRequest.Status.APPROVED, created, resolved);

        assertEquals(1,    req.getId());
        assertEquals(DepositRequest.Status.APPROVED, req.getStatus());
        assertEquals(created,  req.getCreatedAt());
        assertEquals(resolved, req.getResolvedAt());
    }

    @Test
    @DisplayName("DepositRequest.setStatus() và setResolvedAt() hoạt động đúng")
    void testDepositRequestSetters() {
        DepositRequest req = new DepositRequest("USER_A", "alice", 500_000, "note");
        LocalDateTime now = LocalDateTime.now();

        req.setStatus(DepositRequest.Status.REJECTED);
        req.setResolvedAt(now);

        assertEquals(DepositRequest.Status.REJECTED, req.getStatus());
        assertEquals(now, req.getResolvedAt());
    }
}