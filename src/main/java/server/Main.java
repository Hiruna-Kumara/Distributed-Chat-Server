package server;

import java.util.Scanner;

public class Main
{
    public static void main( String[] args ) {
        System.out.println("------server started------");

        Scanner serverID = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Enter serverID");
        String serverid = serverID.nextLine();

        // start sending thread
        Server serverThread = new Server(serverid);
        Thread sendThread = new Thread(serverThread);
        sendThread.start();
    }
}