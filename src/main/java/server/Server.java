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

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class Server implements Runnable{

    private DataOutputStream out;
    private ServerSocket Server;

//    client list
    private HashMap<String, Integer> clientList = new HashMap<String, Integer>();
    private HashMap<Integer, String> reverseClientList = new HashMap<Integer, String>();
//    global room list and ids
    private HashMap<String, String> globalRoomList = new HashMap<String, String>();
    private String serverID;
//    maintain room object list  clientID:clientObject
    private HashMap<String, ClientState> clientObjectList = new HashMap<String, ClientState>();
//    maintain room object list roomID:roomObject
    private HashMap<String, Room> roomObjectList = new HashMap<String, Room>();
//    Main hall
    private Room mainhall;

    public Server(String id){
        this.serverID = id;
        mainhall = new Room("default-" + serverID, "MainHall-" + serverID);
        roomObjectList.put("MainHall-" + serverID, mainhall);
        globalRoomList.put("MainHall-" + serverID, "default-" + serverID);
    }

//    check whether the id exist
    private boolean hasKey(JSONObject jsonObject, String key) {
        return (jsonObject != null && jsonObject.get(key) != null);
    }

//    Id validation
    private boolean checkID(String id) {
        return (Character.toString(id.charAt(0)).matches("[a-zA-Z]+") && id.matches("[a-zA-Z0-9]+") && id.length() >= 3 && id.length() <= 16);
    }

//    massage to client
    private void send(JSONObject obj) throws IOException {
//        System.out.println(obj.toJSONString()+"\n");
        out.write((obj.toJSONString() + "\n").getBytes("UTF-8"));
        out.flush();
    }

    private void messageSend(Socket socket, String msg) throws IOException {
        JSONObject sendToClient = new JSONObject();
        String[] array = msg.split(" ");
        if (array[0].equals("newid")){
            sendToClient = Message.getApprovalNewID(array[1]);
            send(sendToClient);
        } if (array[0].equals("roomchange")) {
            sendToClient = Message.getRoomChange(array[1], array[2]);
            send(sendToClient);
        } if (array[0].equals("createroom")) {
            sendToClient = Message.getCreateRoom(array[1], array[2]);
            send(sendToClient);
        } if (array[0].equals("createroomchange")) {
            sendToClient = Message.getCreateRoomChange(array[1], array[2], array[3]);
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
                messageSend(connected, "newid true");
                messageSend(connected, "roomchange " + id + " MainHall-" + serverID);
            }
        } else {
            System.out.println("Recieved wrong ID type or ID already in use");
            messageSend(connected, "newid false");
        }
    }

//    create room
    private void createRoom(String roomID, Socket connected, String fromclient) throws IOException {
        String id = reverseClientList.get(connected.getPort());
        if (checkID(roomID) && !roomObjectList.containsKey(roomID) && !globalRoomList.containsValue(id)) {
            System.out.println("Recieved correct room ID ::" + fromclient);

            ClientState client = clientObjectList.get(id);
            String former = client.getRoomID();
            roomObjectList.get(former).removeParticipants(client);

            Room newRoom = new Room(id,roomID);
            roomObjectList.put(roomID,newRoom);
            globalRoomList.put(roomID, id);

            client.setRoomID(roomID);

            synchronized (connected) {
                messageSend(connected, "createroom " + roomID + " true");
                messageSend(connected, "createroomchange " + id + " "+ former +" " + roomID);
            }
        } else {
            System.out.println("Recieved wrong room ID type or room ID already in use");
            messageSend(connected, "createroom " + roomID + " false");
        }
    }


    @Override
    public void run() {
        String fromclient;

        try {
            Server = new ServerSocket(5000);
            System.out.println(Server.getInetAddress());
            System.out.println(Server.getLocalSocketAddress());
            System.out.println(Server.getLocalPort());

            System.out.println("TCPServer Waiting for client on port 5000"); //client should use 5000 as port

            while (true) {
                Socket connected = Server.accept();
                System.out.println(" THE CLIENT" + " " + connected.getInetAddress()
                        + ":" + connected.getPort() + " IS CONNECTED ");

                BufferedReader inFromClient = new BufferedReader(
                        new InputStreamReader(connected.getInputStream(), StandardCharsets.UTF_8));

                out = new DataOutputStream(connected.getOutputStream());

                boolean close = false;

                while (!close) {

                    fromclient = inFromClient.readLine();

                    //convert received message to json object

                    try {
                        Object object = null;
                        JSONParser jsonParser = new JSONParser();
                        object = jsonParser.parse(fromclient);
                        JSONObject j_object = (JSONObject) object;

                        if (hasKey(j_object, "type")) {
                            //check new identity format
                            if (j_object.get("type").equals("newidentity") && j_object.get("identity") != null) {
                                String id = j_object.get("identity").toString();
                                newID(id, connected, fromclient);
                            } if (j_object.get("type").equals("createroom") && j_object.get("roomid") != null) {
                                String roomID = j_object.get("roomid").toString();
                                createRoom(roomID, connected, fromclient);
                            }
                        } else {
                            System.out.println("Something went wrong");
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                connected.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}