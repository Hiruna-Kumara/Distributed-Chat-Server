package server;

import client.ClientHandlerThread;
import consensus.BullyAlgorithm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {

        System.out.println("INFO : Enter server ID (s1)[default]:  ");
        Scanner scanner = new Scanner(System.in);
        String serverID = scanner.nextLine();  // Read user input

        ServerState.getInstance().initializeWithConfigs(serverID, args[1]);

        System.out.println("LOG  : ------server started------");

        try {
            // throw exception if invalid server id provided
            if (ServerState.getInstance().getServerAddress() == null) {
                throw new IllegalArgumentException();
            }
            /**
             Coordination socket
             **/
            // server socket for coordination
            ServerSocket serverCoordinationSocket = new ServerSocket();

            // bind SocketAddress with inetAddress and port
            SocketAddress endPointCoordination = new InetSocketAddress(
                    ServerState.getInstance().getServerAddress(),
                    ServerState.getInstance().getCoordinationPort()
            );
            serverCoordinationSocket.bind(endPointCoordination);
            System.out.println(serverCoordinationSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for coordination on port " +
                    serverCoordinationSocket.getLocalPort()); // port open for coordination

            /**
             Client socket
             **/

            // server socket for clients
            ServerSocket serverClientsSocket = new ServerSocket();

            // bind SocketAddress with inetAddress and port
            SocketAddress endPointClient = new InetSocketAddress(
                    ServerState.getInstance().getServerAddress(),
                    ServerState.getInstance().getClientsPort());
            serverClientsSocket.bind(endPointClient);
            System.out.println(serverClientsSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for clients on port " +
                    serverClientsSocket.getLocalPort()); // port open for clients
            /**
             Handle coordination
             **/
            ServerHandlerThread serverHandlerThread = new ServerHandlerThread(serverCoordinationSocket);


            // starting the thread
            serverHandlerThread.start();

            /**
             * Maintain consensus using Bully Algorithm
             **/
            BullyAlgorithm.initialize();

            Runnable heartbeat = new BullyAlgorithm("Heartbeat");
            new Thread(heartbeat).start();

            /**
             * Handle clients
             **/
            while (true) {
                Socket clientSocket = serverClientsSocket.accept();
                ClientHandlerThread clientHandlerThread = new ClientHandlerThread(clientSocket);
                // starting the thread
                ServerState.getInstance().addClientHandlerThreadToMap(clientHandlerThread);
                clientHandlerThread.start();
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR : invalid server ID");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("ERROR : server arguments not provided");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("ERROR : occurred in main " + Arrays.toString(e.getStackTrace()));
        }
    }
}