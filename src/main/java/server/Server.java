package server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class Server extends Thread {

    private DataOutputStream out;
    private ServerSocket Server;
    private Socket clientSocket;

    private PrintWriter output;

    // client list
    private static HashMap<String, Integer> clientList = new HashMap<String, Integer>();
    private static HashMap<Integer, String> reverseClientList = new HashMap<Integer, String>();
    // global room list and ids
    private static HashMap<String, String> globalRoomList = new HashMap<String, String>();
    private String serverID;
    // maintain room object list clientID:clientObject
    private static HashMap<String, ClientState> clientObjectList = new HashMap<String, ClientState>();
    // maintain room object list roomID:roomObject
    private static HashMap<String, Room> roomObjectList = new HashMap<String, Room>();
    // Main hall
    private Room mainhall;

    public Server(Socket clientSocket) {
        this.serverID = ServerState.getInstance().getServerID();
        mainhall = new Room("default-" + serverID, "MainHall-" + serverID);
        roomObjectList.put("MainHall-" + serverID, mainhall);
        globalRoomList.put("MainHall-" + serverID, "default-" + serverID);

        this.clientSocket = clientSocket;
    }

    // check whether the id exist
    private boolean hasKey(JSONObject jsonObject, String key) {
        return (jsonObject != null && jsonObject.get(key) != null);
    }

    // Id validation
    private boolean checkID(String id) {
        return (Character.toString(id.charAt(0)).matches("[a-zA-Z]+") && id.matches("[a-zA-Z0-9]+") && id.length() >= 3
                && id.length() <= 16);
    }

    // massage to client
    private void send(JSONObject obj) throws IOException {
        // System.out.println(obj.toJSONString()+"\n");
        out.write((obj.toJSONString() + "\n").getBytes("UTF-8"));
        out.flush();
    }

    private void messageSend(Socket socket, String msg, List<String> msgList) throws IOException {
        JSONObject sendToClient = new JSONObject();
        String[] array = msg.split(" ");
        if (array[0].equals("newid")) {
            sendToClient = Message.getApprovalNewID(array[1]);
            send(sendToClient);
        }
        if (array[0].equals("roomchange")) {
            sendToClient = Message.getRoomChange(array[1], array[2]);
            send(sendToClient);
        }
        if (array[0].equals("createroom")) {
            sendToClient = Message.getCreateRoom(array[1], array[2]);
            send(sendToClient);
        }
        if (array[0].equals("createroomchange")) {
            sendToClient = Message.getCreateRoomChange(array[1], array[2], array[3]);
            send(sendToClient);
        }
        if (array[0].equals("roomcontents")) {
            sendToClient = Message.getWho(array[1], msgList, array[2]);
            send(sendToClient);
        }
        if (array[0].equals("roomlist")) {
            sendToClient = Message.getList(msgList);
            send(sendToClient);
        }
    }

    private void newID(String id, Socket connected, String fromclient) throws IOException {
        if (checkID(id) && !clientObjectList.containsKey(id)) {
            System.out.println("Recieved correct ID ::" + fromclient);

            ClientState client = new ClientState(id, mainhall.getRoomId(), connected.getPort());
            mainhall.addParticipants(client);
            clientObjectList.put(id, client);

            clientList.put(id, connected.getPort());
            reverseClientList.put(connected.getPort(), id);
            synchronized (connected) {
                messageSend(connected, "newid true", null);
                messageSend(connected, "roomchange " + id + " MainHall-" + serverID, null);
            }
        } else {
            System.out.println("Recieved wrong ID type or ID already in use");
            messageSend(connected, "newid false", null);
        }
    }

    // create room
    private void createRoom(String roomID, Socket connected, String fromclient) throws IOException {
        String id = reverseClientList.get(connected.getPort());
        if (checkID(roomID) && !roomObjectList.containsKey(roomID) && !globalRoomList.containsValue(id)) {
            System.out.println("Recieved correct room ID ::" + fromclient);

            ClientState client = clientObjectList.get(id);
            String former = client.getRoomID();
            roomObjectList.get(former).removeParticipants(client);

            Room newRoom = new Room(id, roomID);
            roomObjectList.put(roomID, newRoom);
            globalRoomList.put(roomID, id);

            client.setRoomID(roomID);

            newRoom.addParticipants(client);

            synchronized (connected) {
                messageSend(connected, "createroom " + roomID + " true", null);
                messageSend(connected, "createroomchange " + id + " " + former + " " + roomID, null);
            }
        } else {
            System.out.println("Recieved wrong room ID type or room ID already in use");
            messageSend(connected, "createroom " + roomID + " false", null);
        }
    }

    // who-list of clients in a room
    private void who(Socket connected, String fromclient) throws IOException {
        String id = reverseClientList.get(connected.getPort());
        ClientState client = clientObjectList.get(id);
        String roomID = client.getRoomID();
        Room room = roomObjectList.get(roomID);
        List<ClientState> clients = room.getParticipants();

        List<String> participants = new ArrayList<String>();
        System.out.println("room contains :");
        for (int i = 0; i < clients.size(); i++) {
            participants.add(clients.get(i).getId());
            System.out.println(clients.get(i).getId());
        }
        String owner = room.getOwnerIdentity();
        messageSend(connected, "roomcontents " + roomID + " " + owner, participants);
    }

    // list of users
    private void list(Socket connected, String fromclient) throws IOException {
        List<String> rooms = new ArrayList<>();
        System.out.println("rooms in the system :");
        for (String r : roomObjectList.keySet()) {
            rooms.add(r);
            System.out.println(r);
        }

        messageSend(connected, "roomlist ", rooms);
    }

    @Override
    public void run() {
        // String fromclient;

        try {
            // Server = new ServerSocket(5000);
            // System.out.println(Server.getInetAddress());
            // System.out.println(Server.getLocalSocketAddress());
            // System.out.println(Server.getLocalPort());
            System.out.println(" THE CLIENT" + " " + clientSocket.getInetAddress()
                    + ":" + clientSocket.getPort() + " IS CONNECTED ");

            BufferedReader inFromClient = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

            out = new DataOutputStream(clientSocket.getOutputStream());

            // System.out.println("TCPServer Waiting for client on port 5000"); //client
            // should use 5000 as port

            while (true) {
                String fromclient = inFromClient.readLine();

                if (fromclient.equalsIgnoreCase("exit")) {
                    break;
                }

                try {
                    // convert received message to json object
                    Object object = null;
                    JSONParser jsonParser = new JSONParser();
                    object = jsonParser.parse(fromclient);
                    JSONObject j_object = (JSONObject) object;

                    if (hasKey(j_object, "type")) {
                        // check new identity format
                        if (j_object.get("type").equals("newidentity") && j_object.get("identity") != null) {
                            String id = j_object.get("identity").toString();
                            newID(id, clientSocket, fromclient);
                        } // check create room
                        if (j_object.get("type").equals("createroom") && j_object.get("roomid") != null) {
                            String roomID = j_object.get("roomid").toString();
                            createRoom(roomID, clientSocket, fromclient);
                        } // check who
                        if (j_object.get("type").equals("who")) {
                            who(clientSocket, fromclient);
                        } // check list
                        if (j_object.get("type").equals("list")) {
                            list(clientSocket, fromclient);
                        }
                    } else {
                        System.out.println("Something went wrong");
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void printToAllClients(String fromclient) {
        for (Server thread : ServerState.getInstance().getServerList()) {
            thread.output.println(fromclient);
        }
    }
}