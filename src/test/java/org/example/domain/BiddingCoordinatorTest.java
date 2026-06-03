package org.example.domain;

import org.example.coordinator.BiddingCoordinator;
import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.domain.item.Electronics;
import org.example.domain.user.Bidder;
import org.example.observer.AuctionObserver;
import org.example.util.AutoBid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BiddingCoordinator Tests")
public class BiddingCoordinatorTest {

    private Auction auction;
    private BiddingCoordinator coordinator;
    private Bidder bidder1;
    private Bidder bidder2;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Electronics item = new Electronics("Tesla Model 3", "EV car", 50_000_000,
                now.minusMinutes(5),
                now.plusHours(1), "car.jpg");
        auction = new Auction(item);
        auction = new Auction(item);
        auction.start();
        auction.setId(1);
        coordinator = new BiddingCoordinator(auction);
        bidder1 = new Bidder("USER_A", "alice", "pass");
        bidder2 = new Bidder("USER_B", "bob",   "pass");
    }


    // placeBid() - MANUAL


    @Test
    @DisplayName("placeBid() thành công cập nhật giá và highest bidder")
    void testPlaceBidSuccess() {
        double bid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        assertDoesNotThrow(() -> coordinator.placeBid(bidder1, bid));
        assertEquals(bid, auction.getCurrentPrice(), 0.001);
        assertEquals(bidder1, auction.getHighestBidder());
    }

    @Test
    @DisplayName("placeBid() ghi nhận BidType MANUAL vào lịch sử")
    void testPlaceBidRecordsManualType() {
        double bid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, bid);
        assertEquals(1, auction.getBids().size());
        assertEquals(BidTransaction.BidType.MANUAL, auction.getBids().get(0).getType());
    }

    @Test
    @DisplayName("placeBid() ném lỗi khi giá quá thấp")
    void testPlaceBidTooLow() {
        assertThrows(IllegalArgumentException.class,
                () -> coordinator.placeBid(bidder1, 100));
    }

    @Test
    @DisplayName("placeBid() ném lỗi khi bidder đang giữ giá cao nhất đặt lại thủ công")
    void testPlaceBidSameBidderManualFails() {
        double bid1 = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, bid1);
        double bid2 = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        assertThrows(IllegalArgumentException.class, () -> coordinator.placeBid(bidder1, bid2));
    }

    @Test
    @DisplayName("placeBid() gọi onBidPersisted callback với đúng BidTransaction")
    void testPlaceBidCallsCallback() {
        List<BidTransaction> captured = new ArrayList<>();
        coordinator.setOnBidPersisted(captured::add);

        double bid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, bid);

        assertEquals(1, captured.size());
        assertEquals(bid, captured.get(0).getAmount(), 0.001);
    }

    @Test
    @DisplayName("placeBid() không crash khi onBidPersisted chưa được set (null)")
    void testPlaceBidNoCallbackDoesNotCrash() {
        double bid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        assertDoesNotThrow(() -> coordinator.placeBid(bidder1, bid));
    }

    @Test
    @DisplayName("Hai bidder khác nhau có thể đặt giá lần lượt")
    void testTwoBiddersAlternate() {
        double bid1 = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, bid1);

        double bid2 = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder2, bid2);

        assertEquals(2, auction.getBids().size());
        assertEquals(bidder2, auction.getHighestBidder());
    }


    // registerAutoBid() + triggerAutoBid()


    @Test
    @DisplayName("triggerAutoBid() tạo ít nhất 1 AUTO bid khi có auto-bid hợp lệ")
    void testTriggerAutoBidCreatesAutoBid() {
        double maxBid   = auction.getCurrentPrice() + 5_000_000;
        double increment = auction.getMinIncrement() + 100_000;
        coordinator.registerAutoBid(new AutoBid(bidder1, maxBid, increment));
        coordinator.triggerAutoBid();

        assertTrue(auction.getBids().stream()
                .anyMatch(b -> b.getType() == BidTransaction.BidType.AUTO));
    }

    @Test
    @DisplayName("placeBid() MANUAL kích hoạt AUTO bid của bidder đăng ký trước")
    void testManualBidTriggersRegisteredAutoBid() {
        double maxBid    = auction.getCurrentPrice() + 10_000_000;
        double increment = auction.getMinIncrement() + 100_000;
        coordinator.registerAutoBid(new AutoBid(bidder2, maxBid, increment));

        // bidder1 đặt thủ công → phải kích hoạt auto của bidder2
        double manualBid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, manualBid);

        long autoCount = auction.getBids().stream()
                .filter(b -> b.getType() == BidTransaction.BidType.AUTO).count();
        assertTrue(autoCount >= 1, "Phải có ít nhất 1 AUTO bid sau khi MANUAL bid kích hoạt");
    }

    @Test
    @DisplayName("triggerAutoBid() không làm gì nếu không có auto-bid đăng ký")
    void testTriggerAutoBidWithNoAutoBids() {
        coordinator.triggerAutoBid();
        assertTrue(auction.getBids().isEmpty());
    }
    // OBSERVER PATTERN

    @Test
    @DisplayName("Observer được notify khi có bid mới")
    void testObserverNotifiedOnBid() {
        List<BidTransaction> notifications = new ArrayList<>();
        AuctionObserver observer = (a, bid, inc) -> notifications.add(bid);
        coordinator.getNotifier().addObserver(observer);

        double bid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, bid);

        assertFalse(notifications.isEmpty());
        assertEquals(bid, notifications.get(0).getAmount(), 0.001);
    }

    @Test
    @DisplayName("Observer nhận đúng auction object và minIncrement dương")
    void testObserverReceivesAuctionAndIncrement() {
        List<Auction> auctions    = new ArrayList<>();
        List<Double>  increments  = new ArrayList<>();
        coordinator.getNotifier().addObserver((a, bid, inc) -> { auctions.add(a); increments.add(inc); });

        double bid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, bid);

        assertSame(auction, auctions.get(0));
        assertTrue(increments.get(0) > 0);
    }

    @Test
    @DisplayName("Observer đã xóa không nhận thông báo")
    void testRemoveObserverStopsNotification() {
        List<BidTransaction> notifications = new ArrayList<>();
        AuctionObserver observer = (a, bid, inc) -> notifications.add(bid);

        coordinator.getNotifier().addObserver(observer);
        coordinator.getNotifier().removeObserver(observer);

        double bid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, bid);

        assertTrue(notifications.isEmpty());
    }

    @Test
    @DisplayName("Nhiều observer đều nhận thông báo cùng lúc")
    void testMultipleObserversAllNotified() {
        List<BidTransaction> notif1 = new ArrayList<>();
        List<BidTransaction> notif2 = new ArrayList<>();
        coordinator.getNotifier().addObserver((a, bid, inc) -> notif1.add(bid));
        coordinator.getNotifier().addObserver((a, bid, inc) -> notif2.add(bid));

        double bid = auction.getCurrentPrice() + auction.getMinIncrement() + 1;
        coordinator.placeBid(bidder1, bid);

        assertFalse(notif1.isEmpty());
        assertFalse(notif2.isEmpty());
    }


    // GETTERS


    @Test
    @DisplayName("getAuction() trả về đúng auction")
    void testGetAuction() {
        assertSame(auction, coordinator.getAuction());
    }

    @Test
    @DisplayName("getNotifier() không null")
    void testGetNotifierNotNull() {
        assertNotNull(coordinator.getNotifier());
    }

    @Test
    @DisplayName("getAutoBidManager() không null")
    void testGetAutoBidManagerNotNull() {
        assertNotNull(coordinator.getAutoBidManager());
    }
}
