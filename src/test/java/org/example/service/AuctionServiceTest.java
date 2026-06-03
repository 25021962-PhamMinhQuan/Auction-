package org.example.service;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.domain.item.Electronics;
import org.example.domain.item.Item;
import org.example.domain.user.*;
import org.example.repository.*;
import org.example.util.AutoBid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionService Tests")
public class AuctionServiceTest {

    // ================================================================
    // MOCK REPOSITORIES (khớp đúng các interface)
    // ================================================================

    private static class MockAuctionRepository implements AuctionRepository {
        final Map<Integer, Auction> store    = new LinkedHashMap<>();
        final Map<Integer, String>  statuses = new HashMap<>();

        @Override public void save(Auction a, String status) {
            store.put(a.getId(), a); statuses.put(a.getId(), status);
        }
        @Override public void update(Auction a, String status)       { statuses.put(a.getId(), status); }
        @Override public void updateStatus(Auction a, String status) { statuses.put(a.getId(), status); }
        @Override public void updateEndTime(Auction a)               {}
        @Override public void updateScheduleAndPrice(Auction a)      {}
        @Override public void delete(int id)                         { store.remove(id); statuses.remove(id); }
        @Override public Auction findById(int id)                    { return store.get(id); }
        @Override public List<Auction> findAll()                     { return new ArrayList<>(store.values()); }
        @Override public List<Auction> findByStatus(String status) {
            List<Auction> r = new ArrayList<>();
            for (Map.Entry<Integer,String> e : statuses.entrySet())
                if (e.getValue().equals(status)) r.add(store.get(e.getKey()));
            return r;
        }
        @Override public List<Auction> findByName(String kw) {
            List<Auction> r = new ArrayList<>();
            store.values().forEach(a -> { if (a.getItem().getName().contains(kw)) r.add(a); });
            return r;
        }
        @Override public List<Auction> findByType(String type)              { return new ArrayList<>(); }
        @Override public Auction findByItemId(String itemId) {
            return store.values().stream()
                    .filter(a -> a.getItem().getId().equals(itemId)).findFirst().orElse(null);
        }
        @Override public List<Auction> findBySellerId(String id)            { return new ArrayList<>(); }
        @Override public List<Auction> findWonByBidderId(String id)         { return new ArrayList<>(); }
    }

    private static class MockBidRepository implements BidRepository {
        final List<BidTransaction> bids = new ArrayList<>();
        @Override public void save(BidTransaction bid, int auctionId) { bids.add(bid); }
        @Override public List<String[]> getBidHistory(int auctionId)  { return new ArrayList<>(); }
    }

    private static class MockItemRepository implements ItemRepository {
        final Map<String,String> sellerMap = new HashMap<>();
        final Map<String,Item>   store     = new HashMap<>();

        @Override public void save(Item item, String sellerId) {
            store.put(item.getId(), item); sellerMap.put(item.getId(), sellerId);
        }
        @Override public Item findById(String id)                             { return store.get(id); }
        @Override public List<Item> findAll()                                 { return new ArrayList<>(store.values()); }
        @Override public List<Item> findBySeller(String sellerId)             { return new ArrayList<>(); }
        @Override public void update(Item item)                               { store.put(item.getId(), item); }
        @Override public void updateStatus(Item item, String status)          {}
        @Override public String findSellerIdByItemId(String itemId)           { return sellerMap.get(itemId); }
        @Override public void delete(String id)                               { store.remove(id); }
    }

    private static class MockAutoBidRepository implements AutoBidRepository {
        @Override public void save(AutoBid ab, int auctionId)                {}
        @Override public void deactivateByAuction(int auctionId)             {}
        @Override public PriorityQueue<AutoBid> findActiveByAuction(int id)  { return new PriorityQueue<>(); }
    }

    private static class MockUserRepository implements UserRepository {
        final Map<String,User> byId       = new HashMap<>();
        final Map<String,User> byUsername = new HashMap<>();

        @Override public void save(User u)               { byId.put(u.getId(),u); byUsername.put(u.getUsername(),u); }
        @Override public User findById(String id)        { return byId.get(id); }
        @Override public User findByUsername(String un)  { return byUsername.get(un); }
        @Override public List<User> findAll()            { return new ArrayList<>(byId.values()); }
        @Override public void delete(String id)          { User u=byId.remove(id); if(u!=null) byUsername.remove(u.getUsername()); }
        @Override public void updateProfile(User u)      { save(u); }
        @Override public void updatePassword(String id, String pw) {}
        @Override public void updateBalance(String id, double bal) {
            User u = byId.get(id); if (u != null) u.setBalance(bal);
        }
    }

    // ================================================================
    // SETUP
    // ================================================================

    private MockAuctionRepository auctionRepo;
    private MockBidRepository     bidRepo;
    private MockItemRepository    itemRepo;
    private MockAutoBidRepository autoBidRepo;
    private MockUserRepository    userRepo;
    private AuctionService        service;

    private Seller     seller;
    private Bidder     bidder;
    private Admin      admin;
    private Auction    auction;
    private Electronics item;
    private int nextId = 1;

    @BeforeEach
    void setUp() {
        auctionRepo = new MockAuctionRepository();
        bidRepo     = new MockBidRepository();
        itemRepo    = new MockItemRepository();
        autoBidRepo = new MockAutoBidRepository();
        userRepo    = new MockUserRepository();

        service = new AuctionService(auctionRepo, bidRepo, itemRepo, autoBidRepo, userRepo);

        seller = new Seller("SELLER_01", "seller1", "pass");
        bidder = new Bidder("BIDDER_01", "bidder1", "pass");
        admin  = new Admin ("ADMIN_01",  "admin1",  "pass");
        bidder.setBalance(100_000_000);

        userRepo.save(bidder);
        userRepo.save(seller);
        userRepo.save(admin);

        item = new Electronics("MacBook Pro", "Apple laptop", 10_000_000,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1), "img.jpg");
        itemRepo.save(item, seller.getId());

        auction = new Auction(item);
        auction.setId(nextId++);
        auctionRepo.save(auction, Auction.Status.OPEN.name());
    }

    /** Tạo một auction đang RUNNING và lưu vào repo */
    private Auction buildRunningAuction() {
        Electronics i = new Electronics("iPhone 15", "New phone", 5_000_000,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1), "phone.jpg");
        itemRepo.save(i, seller.getId());
        Auction a = new Auction(i);
        a.setId(nextId++);
        a.start();
        auctionRepo.save(a, Auction.Status.RUNNING.name());
        return a;
    }

    // ================================================================
    // cancelAuction()
    // ================================================================

    @Test
    @DisplayName("cancelAuction() thành công khi Admin hủy auction OPEN")
    void testCancelAuctionByAdmin() {
        assertDoesNotThrow(() -> service.cancelAuction(auction, admin));
        assertEquals(Auction.Status.CANCELED, auction.getStatus());
    }

    @Test
    @DisplayName("cancelAuction() thành công khi Seller hủy auction OPEN của chính mình")
    void testCancelAuctionByOwnerSeller() {
        assertDoesNotThrow(() -> service.cancelAuction(auction, seller));
        assertEquals(Auction.Status.CANCELED, auction.getStatus());
    }

    @Test
    @DisplayName("cancelAuction() ném lỗi khi Bidder cố hủy")
    void testCancelAuctionByBidderForbidden() {
        assertThrows(IllegalStateException.class, () -> service.cancelAuction(auction, bidder));
    }

    @Test
    @DisplayName("cancelAuction() ném lỗi khi Seller hủy auction của người khác")
    void testCancelAuctionByOtherSeller() {
        Seller other = new Seller("SELLER_02", "seller2", "pass");
        userRepo.save(other);
        assertThrows(IllegalStateException.class, () -> service.cancelAuction(auction, other));
    }

    @Test
    @DisplayName("cancelAuction() ném lỗi khi Seller cố hủy auction đã RUNNING")
    void testCancelAuctionBySellerOnRunningFails() {
        // Tạo running auction có item thuộc seller_02 để tránh trùng sellerId
        Seller seller2 = new Seller("SELLER_02", "seller2", "pass");
        userRepo.save(seller2);
        Electronics i = new Electronics("TV", "Smart TV", 3_000_000,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1), "tv.jpg");
        itemRepo.save(i, seller2.getId()); // item của seller2
        Auction running = new Auction(i);
        running.setId(nextId++);
        running.start();
        auctionRepo.save(running, Auction.Status.RUNNING.name());

        // seller2 cố hủy auction đã RUNNING => phải ném lỗi
        assertThrows(IllegalStateException.class, () -> service.cancelAuction(running, seller2));
    }

    @Test
    @DisplayName("cancelAuction() Admin có thể hủy khi đang RUNNING")
    void testCancelAuctionByAdminOnRunning() {
        Auction running = buildRunningAuction();
        assertDoesNotThrow(() -> service.cancelAuction(running, admin));
        assertEquals(Auction.Status.CANCELED, running.getStatus());
    }

    // ================================================================
    // markPaid()
    // ================================================================

    @Test
    @DisplayName("markPaid() thành công khi winner trả tiền và đủ số dư")
    void testMarkPaidByWinnerSuccess() {
        Auction running = buildRunningAuction();
        double bid = running.getCurrentPrice() + running.getMinIncrement() + 1;
        running.recordBid(bidder, bid, BidTransaction.BidType.MANUAL);
        running.finish();

        assertDoesNotThrow(() -> service.markPaid(running, bidder));
        assertEquals(Auction.Status.PAID, running.getStatus());
    }

    @Test
    @DisplayName("markPaid() thành công khi Admin xác nhận thanh toán")
    void testMarkPaidByAdminSuccess() {
        Auction running = buildRunningAuction();
        double bid = running.getCurrentPrice() + running.getMinIncrement() + 1;
        running.recordBid(bidder, bid, BidTransaction.BidType.MANUAL);
        running.finish();

        assertDoesNotThrow(() -> service.markPaid(running, admin));
        assertEquals(Auction.Status.PAID, running.getStatus());
    }

    @Test
    @DisplayName("markPaid() ném lỗi khi auction chưa FINISHED")
    void testMarkPaidOnRunningThrows() {
        Auction running = buildRunningAuction();
        double bid = running.getCurrentPrice() + running.getMinIncrement() + 1;
        running.recordBid(bidder, bid, BidTransaction.BidType.MANUAL);
        // KHÔNG gọi running.finish()
        assertThrows(IllegalStateException.class, () -> service.markPaid(running, bidder));
    }

    @Test
    @DisplayName("markPaid() ném lỗi khi auction không có người thắng")
    void testMarkPaidNoWinnerThrows() {
        Auction running = buildRunningAuction();
        running.finish(); // không có bid => highestBidder = null
        assertThrows(IllegalStateException.class, () -> service.markPaid(running, admin));
    }

    @Test
    @DisplayName("markPaid() ném lỗi khi winner không đủ số dư")
    void testMarkPaidInsufficientBalance() {
        bidder.setBalance(100); // số dư cực thấp
        Auction running = buildRunningAuction();
        double bid = running.getCurrentPrice() + running.getMinIncrement() + 1;
        running.recordBid(bidder, bid, BidTransaction.BidType.MANUAL);
        running.finish();

        assertThrows(IllegalStateException.class, () -> service.markPaid(running, bidder));
    }

    @Test
    @DisplayName("markPaid() trừ đúng số dư của winner")
    void testMarkPaidDeductsBalance() {
        double initialBalance = bidder.getBalance();
        Auction running = buildRunningAuction();
        double bid = running.getCurrentPrice() + running.getMinIncrement() + 1;
        running.recordBid(bidder, bid, BidTransaction.BidType.MANUAL);
        running.finish();

        service.markPaid(running, bidder);
        assertEquals(initialBalance - bid, bidder.getBalance(), 0.001);
    }

    @Test
    @DisplayName("markPaid() ném lỗi khi người không liên quan cố thanh toán")
    void testMarkPaidByUnrelatedUserThrows() {
        Bidder other = new Bidder("BIDDER_02", "bidder2", "pass");
        other.setBalance(100_000_000);
        userRepo.save(other);

        Auction running = buildRunningAuction();
        double bid = running.getCurrentPrice() + running.getMinIncrement() + 1;
        running.recordBid(bidder, bid, BidTransaction.BidType.MANUAL);
        running.finish();

        assertThrows(IllegalStateException.class, () -> service.markPaid(running, other));
    }

    // ================================================================
    // updateScheduledAuction()
    // ================================================================

    @Test
    @DisplayName("updateScheduledAuction() thành công khi Seller chỉnh sửa auction OPEN của mình")
    void testUpdateScheduledAuctionSuccess() {
        assertDoesNotThrow(() -> service.updateScheduledAuction(auction, seller));
    }

    @Test
    @DisplayName("updateScheduledAuction() ném lỗi khi không phải Seller (Admin gọi)")
    void testUpdateScheduledAuctionByAdminFails() {
        assertThrows(IllegalStateException.class, () -> service.updateScheduledAuction(auction, admin));
    }

    @Test
    @DisplayName("updateScheduledAuction() ném lỗi khi Seller khác cố chỉnh sửa")
    void testUpdateScheduledAuctionByOtherSellerFails() {
        Seller other = new Seller("SELLER_03", "seller3", "pass");
        assertThrows(IllegalStateException.class, () -> service.updateScheduledAuction(auction, other));
    }

    @Test
    @DisplayName("updateScheduledAuction() ném lỗi khi auction không ở trạng thái OPEN")
    void testUpdateScheduledAuctionNotOpenFails() {
        auction.start(); // chuyển sang RUNNING
        assertThrows(IllegalStateException.class, () -> service.updateScheduledAuction(auction, seller));
    }

    // ================================================================
    // searchByName() / getAuctionsByStatus()
    // ================================================================

    @Test
    @DisplayName("searchByName() trả về auction có tên chứa từ khóa")
    void testSearchByNameFound() {
        List<Auction> results = service.searchByName("MacBook");
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getItem().getName().contains("MacBook"));
    }

    @Test
    @DisplayName("searchByName() trả về danh sách rỗng nếu không khớp")
    void testSearchByNameNotFound() {
        assertTrue(service.searchByName("NonExistentItem").isEmpty());
    }

    @Test
    @DisplayName("getAuctionsByStatus(OPEN) chỉ trả về auction đang OPEN")
    void testGetAuctionsByStatusOpen() {
        List<Auction> openAuctions = service.getAuctionsByStatus("OPEN");
        assertFalse(openAuctions.isEmpty());
        openAuctions.forEach(a -> assertEquals(Auction.Status.OPEN, a.getStatus()));
    }

    @Test
    @DisplayName("getAuctionsByStatus(RUNNING) chỉ trả về auction đang RUNNING")
    void testGetAuctionsByStatusRunning() {
        buildRunningAuction();
        List<Auction> running = service.getAuctionsByStatus("RUNNING");
        assertFalse(running.isEmpty());
        running.forEach(a -> assertEquals(Auction.Status.RUNNING, a.getStatus()));
    }

    // ================================================================
    // deleteAuction()
    // ================================================================

    @Test
    @DisplayName("deleteAuction() xóa auction khỏi repository")
    void testDeleteAuction() {
        service.deleteAuction(auction.getId());
        assertNull(auctionRepo.findById(auction.getId()));
    }

    // ================================================================
    // findbyId() / findAllAuctions()
    // ================================================================

    @Test
    @DisplayName("findbyId() trả về auction đúng theo id")
    void testFindById() {
        Auction found = service.findbyId(auction.getId());
        assertNotNull(found);
        assertEquals(auction.getId(), found.getId());
    }

    @Test
    @DisplayName("findbyId() trả về null nếu không tìm thấy")
    void testFindByIdNotFound() {
        assertNull(service.findbyId(9999));
    }

    @Test
    @DisplayName("findAllAuctions() trả về tất cả auction trong repository")
    void testFindAllAuctions() {
        buildRunningAuction();
        assertEquals(2, service.findAllAuctions().size());
    }

    // ================================================================
    // countRunningAuctions() / countOpenAuctions()
    // ================================================================

    @Test
    @DisplayName("countRunningAuctions() đếm đúng số auction RUNNING")
    void testCountRunningAuctions() {
        buildRunningAuction();
        buildRunningAuction();
        assertEquals(2, service.countRunningAuctions());
    }

    @Test
    @DisplayName("countOpenAuctions() đếm đúng số auction OPEN")
    void testCountOpenAuctions() {
        assertEquals(1, service.countOpenAuctions());
    }
}