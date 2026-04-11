package org.example.service;

import org.example.dao.AuctionDAO;
import org.example.dao.BidDAO;
import org.example.dao.ItemDao;
import org.example.model.auction.Auction;
import org.example.model.user.Bidder;
import org.example.dao.AutoBidDao;
import org.example.model.user.User;
import org.example.util.AutoBid;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.*;

public class AuctionService {
    private AuctionDAO auctionDAO = new AuctionDAO();
    private BidDAO bidDAO = new BidDAO();
    private AutoBidDao autoBidDao = new AutoBidDao();
    private ItemDao itemDao = new ItemDao();

    public void StartAuction(Auction auction){
        auction.start();

        auction.setOnBidPersisted(bid -> {
            auctionDAO.update(auction, "RUNNING");
            bidDAO.save(bid, auction.getId());
        });
        auctionDAO.save(auction, "START");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            if(auction.getStatus() != Auction.Status.RUNNING.name()){
                scheduler.shutdown();
                return;
            }

            LocalDateTime endTime = auction.getItem().getEndTime();

            if(LocalDateTime.now().isAfter(endTime)){
                FinishAuction(auction);
                scheduler.shutdown();
            }
                }, 0, 1,TimeUnit.SECONDS);
    }
    public void placeBid(Auction auction, Bidder bidder,double amount){
        if (bidder == null) {
            throw new IllegalArgumentException("Bidder null");
        }

        auction.placeBid(bidder, amount);

    }

    public void registerAutoBid(Auction auction, AutoBid autoBid) {
        auction.addAutoBid(autoBid);
        autoBidDao.save(autoBid, auction.getId());
    }

    public void FinishAuction(Auction auction){
        auction.finish();
        auctionDAO.update(auction, "FINISHED");
        autoBidDao.deactivateByAuction(auction.getId());
    }

    public void cancelAuction(Auction auction, User requester){

        if(requester.getRole().equals("BIDDER")){
            throw new IllegalStateException("Bidder have no right to cancel this auction");
        }

        if(requester.getRole().equals("SELLER") && auction.getStatus() != Auction.Status.OPEN.name()){
            throw  new IllegalStateException("Auction can't be cancelled after started ");
        }

        auction.cancel();
        auctionDAO.updateStatus(auction, "CANCELLED");
        autoBidDao.deactivateByAuction(auction.getId());
    }

    public void markPaid(Auction auction, User requester){
        if(auction.getHighestBidder() == null){
            throw new IllegalStateException("No bidder won this auction");
        }

        boolean isWinner = auction.getWinner().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole().equals("ADMIN");
        boolean isSeller = requester.getRole().equals("SELLER");

        if(!isAdmin && !isWinner){
            throw new IllegalStateException("Unable to paid");
        }

        auction.markPaid();
        auctionDAO.updateStatus(auction, "PAID");
        itemDao.updateStatus(auction.getItem(),"SOLD");
    }
}

