package org.example.server;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.uicontroller.MainScreenController;

import java.io.*;
import java.net.*;
public class AuctionClient {
    private static final String HOST ="localhost";
    private static final int PORT =2501;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String currentUsername;
    private String currentRole;

    private Stage stage; // dung cho FX

    public static AuctionClient instance; // tao 1 server dung chung cho tat ca
    public static AuctionClient getInstance(){
        if (instance==null){
            instance = new AuctionClient();
        }
        return instance;
    }

    private AuctionClient(){}

    public void connect(Stage stage) throws IOException{
        this.stage=stage;
        socket = new Socket(HOST,PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(),true);

        System.out.println("Server connected");

        Thread listener = new Thread(()->{
            try {
                String line;
                while ((line = in.readLine())!=null){
                    handleServerMessage(line);

                }
            } catch (IOException e) {
                System.out.println("Server disconected");
            }
        });
        listener.setDaemon(true);
        listener.start();
    }
    private void handleServerMessage(String line){
        String[] parts = line.split("\\|");
        String type = parts[0];

        switch (type){
            case "CONFIRMED":{
                    currentRole = parts[1];
                    currentUsername=parts[2];

                    Platform.runLater(()->openMainScreen()); // listener thread khon cap nhat UI
                                                            // duoc nen phai gui yeu cau qua cho
                                                            // JavaFx bang runlater
                break;

            }
            case "ERROR": {
                // Server báo lỗi: "ERROR|Sai mật khẩu"
                String errorMsg = parts[1];
                Platform.runLater(() -> showError(errorMsg));
                break;
            }

            case "UPDATE": {
                // broadcast cho tất cả client
                int auctionId = Integer.parseInt(parts[1]);
                double newPrice = Double.parseDouble(parts[2]);
                String bidder = parts[3];

                // Cập nhật UI màn hình chính
                //Platform.runLater(() ->
                        //MainScreenController.getInstance()
                                //.updateAuctionPrice(auctionId, newPrice, bidder)
                //);
                break;
            }
            case "FINISHED": {
                // Phiên đấu giá kết thúc: "FINISHED|auctionId|winnerUsername|finalPrice"
                int    auctionId = Integer.parseInt(parts[1]);
                String winner    = parts[2];
                double finalPrice = Double.parseDouble(parts[3]);

                //Platform.runLater(() ->
                        //MainScreenController.getInstance()
                                //.onAuctionFinished(auctionId, winner, finalPrice)
                //);
                break;
            }
        }
    }
    public void login(String userName, String password){
        sendCommand("LOGIN|"+userName+"|"+password);
    }
    public void placeBid(int auctionId, int ammount){
        sendCommand("BID|"+auctionId+"|"+ammount);
    }
    public void registerAutoBid(int auctionId, double maxBid, double increment) {
        sendCommand("AUTOBID|" + auctionId + "|" + maxBid + "|" + increment);
    }
    public void checkStatus(int auctionId) {
        sendCommand("STATUS|" + auctionId);
    }

    private void sendCommand(String command){
        if(out!=null){
            out.println(command); // gửi command vào socket
        }
    }
    private void openMainScreen() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/view/mainscreen.fxml"));
            Parent root = fxmlLoader.load();

            MainScreenController controller = fxmlLoader.getController();
            //controller.setCurrentUser(currentUsername,currentRole);

            stage.setScene(new Scene(root));
            stage.show();

        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void showError(String msg){}

}
