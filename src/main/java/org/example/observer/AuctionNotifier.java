package org.example.observer;

import org.example.model.auction.Auction;
import org.example.model.auction.BidTransaction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionNotifier {
    private List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    private Auction auction;

    public AuctionNotifier(Auction auction){
        this.auction = auction;
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(BidTransaction bid) {
        for (AuctionObserver o : observers) {
            // đoạn này this có nghĩa là nạp cái auction này vào bâyh thì chx hẳn là có tdung nhma khi thiết kế giao diện thì cnay sẽ cung cấp các thông tin như đang đấu giá sản phẩm nào bla bla
            o.update(auction, bid, auction.getMinIncrement());
        }
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

}
