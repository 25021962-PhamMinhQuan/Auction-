package org.example.observer;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;

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
            o.update(auction, bid, auction.getMinIncrement());
        }
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

}
