package server;

public class Main
{
    public static void main( String[] args ) {
        System.out.println("hii");

        // start sending thread
        Server serverThread = new Server();
        Thread sendThread = new Thread(serverThread);
        sendThread.start();
    }
}