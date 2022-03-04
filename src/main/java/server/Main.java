package server;

import java.util.Scanner;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        System.out.println("------server started------");

        // Create a Scanner object
        Scanner serverIdScanner = new Scanner(System.in);
        System.out.println("Enter serverID");
        // input user ids
        String serverId = serverIdScanner.nextLine();

        ServerState.getInstance().setServerID(serverId);

        try {
            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println(serverSocket.getInetAddress());
            System.out.println(serverSocket.getLocalSocketAddress());
            System.out.println(serverSocket.getLocalPort());
            System.out.println("TCPServer Waiting for client on port 5000"); // client should use 5000 as port
            while (true) {
                Socket socket = serverSocket.accept();
                Server serverThread = new Server(socket);
                // starting the tread
                ServerState.getInstance().getServerList().add(serverThread);
                serverThread.start();
            }

        } catch (Exception e) {
            System.out.println("Error occured in main " + e.getStackTrace());
        }
    }
}