package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.simple.JSONObject;

    
class Server {
    public static void main(String argv[]) throws Exception {

        JSONObject obj=new JSONObject();
        obj.put("name","sonoo");
        obj.put("age",new Integer(27));
        obj.put("salary",new Double(600000));
        System.out.print(obj);


        String fromclient;
        String toclient;

        ServerSocket Server = new ServerSocket(4444);

        System.out.println("TCPServer Waiting for client on port 5000"); //client should use 5000 as port

        while (true) {
            Socket connected = Server.accept();
            System.out.println(" THE CLIENT" + " " + connected.getInetAddress()
                    + ":" + connected.getPort() + " IS CONNECTED ");

            BufferedReader inFromUser = new BufferedReader(
                    new InputStreamReader(System.in));

            BufferedReader inFromClient = new BufferedReader(
                    new InputStreamReader(connected.getInputStream()));

            PrintWriter outToClient = new PrintWriter(
                    connected.getOutputStream(), true);

            while (true) {

                System.out.println("SEND(Type Q or q to Quit):");
                toclient = inFromUser.readLine();

                if (toclient.equals("q") || toclient.equals("Q")) {
                    outToClient.println(toclient);
                    connected.close();
                    break;
                } else {
                    outToClient.println(toclient);
                }

                fromclient = inFromClient.readLine();

                if (fromclient.equals("q") || fromclient.equals("Q")) {
                    connected.close();
                    break;
                } else {
                    System.out.println("RECIEVED:" + fromclient);
                }

            }

        }
    }
}