package org.example.repository;

import org.example.util.AutoBid;

import java.util.PriorityQueue;

public interface AutoBidRepository {
    void save(AutoBid autoBid, int auctionId);
    public PriorityQueue<AutoBid> findActiveByAuction(int auctionId);
    public void deactivateByAuction(int auctionId);

}
