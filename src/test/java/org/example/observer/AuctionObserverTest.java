package org.example.observer;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.domain.item.Electronics;
import org.example.domain.user.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionNotifier (Observer Pattern) Tests")
public class AuctionObserverTest {

    private Auction auction;
    private AuctionNotifier notifier;
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end   = LocalDateTime.now().plusHours(1);
        Electronics item = new Electronics("Watch", "Luxury watch", 20_000_000, start, end, "watch.jpg");
        auction  = new Auction(item);
        notifier = new AuctionNotifier(auction);
        bidder   = new Bidder("USER_OBS", "observer_user", "pass");
    }

    // ================================================================
    // addObserver() + notifyObservers()
    // ================================================================

    @Test
    @DisplayName("Observer được gọi khi notifyObservers()")
    void testObserverCalledOnNotify() {
        List<BidTransaction> received = new ArrayList<>();
        notifier.addObserver((a, bid, inc) -> received.add(bid));

        BidTransaction tx = new BidTransaction(bidder, 500_000, BidTransaction.BidType.MANUAL);
        notifier.notifyObservers(tx);

        assertEquals(1, received.size());
        assertEquals(tx, received.get(0));
    }

    @Test
    @DisplayName("Nhiều observer đều nhận được đúng bid transaction")
    void testMultipleObserversReceiveSameBid() {
        List<BidTransaction> received1 = new ArrayList<>();
        List<BidTransaction> received2 = new ArrayList<>();
        List<BidTransaction> received3 = new ArrayList<>();

        notifier.addObserver((a, bid, inc) -> received1.add(bid));
        notifier.addObserver((a, bid, inc) -> received2.add(bid));
        notifier.addObserver((a, bid, inc) -> received3.add(bid));

        BidTransaction tx = new BidTransaction(bidder, 1_000_000, BidTransaction.BidType.MANUAL);
        notifier.notifyObservers(tx);

        assertEquals(1, received1.size());
        assertEquals(1, received2.size());
        assertEquals(1, received3.size());
        assertSame(tx, received1.get(0));
        assertSame(tx, received2.get(0));
    }

    @Test
    @DisplayName("Observer nhận được đúng auction object")
    void testObserverReceivesCorrectAuction() {
        List<Auction> auctions = new ArrayList<>();
        notifier.addObserver((a, bid, inc) -> auctions.add(a));

        BidTransaction tx = new BidTransaction(bidder, 500_000, BidTransaction.BidType.MANUAL);
        notifier.notifyObservers(tx);

        assertEquals(1, auctions.size());
        assertSame(auction, auctions.get(0));
    }

    @Test
    @DisplayName("Observer nhận được minIncrement dương")
    void testObserverReceivesPositiveMinIncrement() {
        List<Double> increments = new ArrayList<>();
        notifier.addObserver((a, bid, inc) -> increments.add(inc));

        BidTransaction tx = new BidTransaction(bidder, 500_000, BidTransaction.BidType.MANUAL);
        notifier.notifyObservers(tx);

        assertFalse(increments.isEmpty());
        assertTrue(increments.get(0) > 0, "minIncrement phải là số dương");
    }

    // ================================================================
    // removeObserver()
    // ================================================================

    @Test
    @DisplayName("Observer đã xóa không nhận được thông báo")
    void testRemovedObserverNotCalled() {
        List<BidTransaction> received = new ArrayList<>();
        AuctionObserver observer = (a, bid, inc) -> received.add(bid);

        notifier.addObserver(observer);
        notifier.removeObserver(observer);

        BidTransaction tx = new BidTransaction(bidder, 500_000, BidTransaction.BidType.MANUAL);
        notifier.notifyObservers(tx);

        assertTrue(received.isEmpty(), "Observer đã xóa không được nhận notify");
    }

    @Test
    @DisplayName("Xóa một observer trong nhóm nhiều observer, các observer còn lại vẫn nhận notify")
    void testRemoveOneObserverOthersStillNotified() {
        List<BidTransaction> received1 = new ArrayList<>();
        List<BidTransaction> received2 = new ArrayList<>();

        AuctionObserver obs1 = (a, bid, inc) -> received1.add(bid);
        AuctionObserver obs2 = (a, bid, inc) -> received2.add(bid);

        notifier.addObserver(obs1);
        notifier.addObserver(obs2);
        notifier.removeObserver(obs1); // xóa obs1

        BidTransaction tx = new BidTransaction(bidder, 500_000, BidTransaction.BidType.MANUAL);
        notifier.notifyObservers(tx);

        assertTrue(received1.isEmpty(),   "obs1 đã xóa không được notify");
        assertEquals(1, received2.size(), "obs2 vẫn phải nhận notify");
    }

    // ================================================================
    // EDGE CASES
    // ================================================================

    @Test
    @DisplayName("notifyObservers() không crash khi không có observer nào")
    void testNotifyWithNoObservers() {
        BidTransaction tx = new BidTransaction(bidder, 500_000, BidTransaction.BidType.MANUAL);
        assertDoesNotThrow(() -> notifier.notifyObservers(tx));
    }

    @Test
    @DisplayName("notifyObservers() nhiều lần gọi observer tương ứng nhiều lần")
    void testNotifyMultipleTimes() {
        List<BidTransaction> received = new ArrayList<>();
        notifier.addObserver((a, bid, inc) -> received.add(bid));

        BidTransaction tx1 = new BidTransaction(bidder, 500_000,   BidTransaction.BidType.MANUAL);
        BidTransaction tx2 = new BidTransaction(bidder, 1_000_000, BidTransaction.BidType.MANUAL);
        BidTransaction tx3 = new BidTransaction(bidder, 1_500_000, BidTransaction.BidType.AUTO);

        notifier.notifyObservers(tx1);
        notifier.notifyObservers(tx2);
        notifier.notifyObservers(tx3);

        assertEquals(3, received.size());
        assertEquals(tx1, received.get(0));
        assertEquals(tx2, received.get(1));
        assertEquals(tx3, received.get(2));
    }

    @Test
    @DisplayName("Thêm cùng một observer hai lần => nhận notify hai lần mỗi lần notifyObservers()")
    void testAddSameObserverTwice() {
        List<BidTransaction> received = new ArrayList<>();
        AuctionObserver observer = (a, bid, inc) -> received.add(bid);

        notifier.addObserver(observer);
        notifier.addObserver(observer); // thêm lần 2

        BidTransaction tx = new BidTransaction(bidder, 500_000, BidTransaction.BidType.MANUAL);
        notifier.notifyObservers(tx);

        assertEquals(2, received.size(), "Observer thêm 2 lần phải được gọi 2 lần");
    }
}