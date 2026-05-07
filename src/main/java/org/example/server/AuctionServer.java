package org.example.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {
    private static final int PORT = 2501;

    //danh client đag connect
    public static List<ClientHandler> connectClient = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("==Auction Server started on port "+PORT+" ==");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connect: " + clientSocket.getInetAddress());

            ClientHandler handler = new ClientHandler(clientSocket);
            connectClient.add(handler);

            new Thread(handler).start();
        }
    }
}


