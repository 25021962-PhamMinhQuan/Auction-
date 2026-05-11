package org.example.server;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.coordinator.BiddingCoordinator;
import org.example.domain.user.Bidder;
import org.example.domain.user.User;
import org.example.observer.AuctionObserver;
import org.example.service.AuctionService;
import org.example.factory.ServiceFactory;
import org.example.service.UserService;
import org.example.util.AutoBid;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable, AuctionObserver {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    private AuctionService auctionService = ServiceFactory.getInstance().getAuctionService();
    public ClientHandler(Socket socket){
        this.socket=socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String line;
        try {
            while ((line = in.readLine()) != null) {
                System.out.println("System handle: " + line);
                handleCommand(line);
            }
        } catch (IOException e) {
            System.out.println("Client dissconected: "+socket.getInetAddress());
        }finally {
            AuctionServer.connectClient.remove(this);
            closeConnection();
        }
    }

    public void update(Auction auction, BidTransaction bid, double minIncrement) {
        String updateMessage = "UPDATE|" + auction.getId() + "|"
                + bid.getAmount() + "|"
                + bid.getBidder().getUsername() + "|"
                + bid.getType();
        sendMessage(updateMessage);
    }

    // xac dinh xem hanh dong la chi mo
    private void handleCommand(String rawline) {
        String[] parts = rawline.split("\\|");
        String command = parts[0];
        switch (command){
            case "BID" : {
                try {
                    int auctionId = Integer.parseInt(parts[1]);
                    double amount = Double.parseDouble(parts[2]);

                    BiddingCoordinator coordinator = auctionService.getCoordinator(auctionId);
                    if (coordinator == null) {
                        sendMessage("ERROR|Auction not found");
                        return;
                    }

                    Auction auction = coordinator.getAuction();

                    auctionService.placeBid(auction, (Bidder) currentUser, amount);
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }
            case "AUTOBID":{ //AUTOBID|auctionId|maxbid|increment
                try {
                    int auctionId = Integer.parseInt(parts[1]);
                    double maxBid = Double.parseDouble(parts[2]);
                    double increment = Double.parseDouble(parts[3]);

                    BiddingCoordinator coordinator = auctionService.getCoordinator(auctionId);
                    if (coordinator == null) {
                        sendMessage("ERROR|Auction not found");
                        return;
                    }

                    Auction auction = coordinator.getAuction();
                    AutoBid autoBid = new AutoBid((Bidder) currentUser, maxBid, increment);
                    auctionService.registerAutoBid(auction, autoBid);

                    // Tự động watch khi đăng ký AutoBid
                    coordinator.getNotifier().addObserver(this);
                    sendMessage("SUCCESS|AutoBid registered");

                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }
            case "STATUS":{
                int auctionId = Integer.parseInt(parts[1]);
                Auction auction = findbyId(auctionId);
                if (auction != null) {
                    sendMessage("STATUS|" + auction.getStatus()
                            + "|" + auction.getCurrentPrice());
                } else {
                    sendMessage("ERROR|Auction not found");
                }
                break;
            }
            case "LOGIN": { // xu ly auth
                // "LOGIN|username|password"  (xử lý auth)
                String username = parts[1];
                String password = parts[2];

                UserService userService = ServiceFactory.getInstance().getUserService();
                User user = userService.findUser(username); // tìm trong DB

                if (user == null) {
                    sendMessage("ERROR|Không tìm thấy tài khoản");
                } else if (!BCrypt.checkpw(password, user.getPassword())) {
                    sendMessage("ERROR|Sai mật khẩu");
                } else {
                    currentUser = user; // lưu lại user đang đăng nhập trên kết nối này
                    sendMessage("OK|" + user.getRole() + "|" + user.getUsername());
                    // gửi về ROLE để biết là BIDDER, SELLER hay ADMIN
                }
                break;
            }

            default:
                sendMessage("ERROR|Unknown command: " + command);
        }

    }
    public void sendMessage(String msg){
        System.out.println(msg);
    }
    private void closeConnection(){
        try {
            if(in!=null){
                in.close();
            }
            if(out!=null){
                out.close();
            }if(socket!=null){
                socket.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        }
        private Auction findbyId(int id){      // server nhan bid kieu BID|id|rpice nen nma placebid dung kieu la auction nen ph tim id => auction
        return auctionService.findbyId(id);
        }
}
