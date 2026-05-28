package org.example.repository;

import org.example.domain.auction.BidTransaction;
import java.util.List;

public interface BidRepository {
    void save(BidTransaction bids, int auctionId);
    List<String[]> getBidHistory(int auctionId);
}
