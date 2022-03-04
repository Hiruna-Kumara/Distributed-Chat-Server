package server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Server extends Thread {

    private final Socket clientSocket;
    private ClientState clientState;

    // TODO : check input stream local var
    private DataOutputStream dataOutputStream;

    public Server(Socket clientSocket) {
        String serverID = ServerState.getInstance().getServerID();
        ServerState.getInstance().getRoomMap().put("MainHall-" + serverID, ServerState.getInstance().getMainHall());

        this.clientSocket = clientSocket;
    }

    // check the existence of a key in json object
    private boolean hasKey(JSONObject jsonObject, String key) {
        return (jsonObject != null && jsonObject.get(key) != null);
    }

    // check validity of the ID
    private boolean checkID(String id) {
        return (Character.toString(id.charAt(0)).matches("[a-zA-Z]+") && id.matches("[a-zA-Z0-9]+") && id.length() >= 3
                && id.length() <= 16);
    }

    // send message to client
    private void send(JSONObject obj) throws IOException {
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes("UTF-8"));
        dataOutputStream.flush();
    }

    // format message before sending it to client
    private void messageSend(Socket socket, String msg, List<String> msgList) throws IOException {
        JSONObject sendToClient = new JSONObject();
        String[] array = msg.split(" ");
        if (array[0].equals("newid")) {
            sendToClient = Message.getApprovalNewID(array[1]);
            send(sendToClient);
        } else if (array[0].equals("roomchange")) {
            sendToClient = Message.getJoinRoom(array[1], array[2].replace("_", ""), array[3]);
            send(sendToClient);
        } else if (array[0].equals("createroom")) {
            sendToClient = Message.getCreateRoom(array[1], array[2]);
            send(sendToClient);
        } else if (array[0].equals("createroomchange")) {
            sendToClient = Message.getCreateRoomChange(array[1], array[2], array[3]);
            send(sendToClient);
        } else if (array[0].equals("roomcontents")) {
            sendToClient = Message.getWho(array[1], msgList, array[2]);
            send(sendToClient);
        } else if (array[0].equals("roomlist")) {
            sendToClient = Message.getList(msgList);
            send(sendToClient);
        }
    }

    // new identity
    private void newID(String clientID, Socket connected, String jsonStringFromClient) throws IOException {
        if (checkID(clientID) && !ServerState.getInstance().isClientIDAlreadyTaken(clientID)) {
            System.out.println("INFO : Received correct ID ::" + jsonStringFromClient);

            this.clientState = new ClientState(clientID, ServerState.getInstance().getMainHall().getRoomID(),
                    connected.getPort());
            ServerState.getInstance().getMainHall().addParticipants(clientState);

            synchronized (connected) {
                messageSend(connected, "newid true", null);
                messageSend(connected,
                        "roomchange " + clientID + " _" + " MainHall-" + ServerState.getInstance().getServerID(), null);
            }
        } else {
            System.out.println("WARN : Recieved wrong ID type or ID already in use");
            messageSend(connected, "newid false", null);
        }
    }

    // list
    private void list(Socket connected, String jsonStringFromClient) throws IOException {
        List<String> roomsList = new ArrayList<>(ServerState.getInstance().getRoomMap().keySet());

        System.out.println("INFO : rooms in the system :");
        messageSend(connected, "roomlist ", roomsList);
    }

    // who
    private void who(Socket connected, String jsonStringFromClient) throws IOException {
        String roomID = clientState.getRoomID();
        Room room = ServerState.getInstance().getRoomMap().get(roomID);
        HashMap<String, ClientState> clientStateMap = room.getClientStateMap();

        List<String> participants = new ArrayList<>(clientStateMap.keySet());

        String owner = room.getOwnerIdentity();
        System.out.println("LOG  : participants in room [" + roomID + "] : " + participants);
        messageSend(connected, "roomcontents " + roomID + " " + owner, participants);
    }

    // create room
    private void createRoom(String newRoomID, Socket connected, String jsonStringFromClient) throws IOException {
        if (checkID(newRoomID) && !ServerState.getInstance().getRoomMap().containsKey(newRoomID)) {
            System.out.println("INFO : Received correct room ID ::" + jsonStringFromClient);

            String formerRoomID = clientState.getRoomID();
            ServerState.getInstance().getRoomMap().get(formerRoomID).removeParticipants(clientState);

            Room newRoom = new Room(clientState.getClientID(), newRoomID);
            ServerState.getInstance().getRoomMap().put(newRoomID, newRoom);

            clientState.setRoomID(newRoomID);
            newRoom.addParticipants(clientState);

            synchronized (connected) {
                messageSend(connected, "createroom " + newRoomID + " true", null);
                messageSend(connected,
                        "createroomchange " + clientState.getClientID() + " " + formerRoomID + " " + newRoomID, null);
            }
        } else {
            System.out.println("WARN : Recieved wrong room ID type or room ID already in use");
            messageSend(connected, "createroom " + newRoomID + " false", null);
        }
    }

    // join room
    private void joinRoom(String roomID, Socket connected, String jsonStringFromClient) throws IOException {
        String formerRoomId = clientState.getRoomID();

        if (ServerState.getInstance().getRoomMap().containsKey(roomID)) {

            clientState.setRoomID(roomID);
            ServerState.getInstance().getRoomMap().get(formerRoomId).removeParticipants(clientState);
            ServerState.getInstance().getRoomMap().get(roomID).addParticipants(clientState);
            System.out.println("INFO : client [" + clientState.getClientID() + "] joined room :" + roomID);
            messageSend(connected, "roomchange " + clientState.getClientID() + " " + formerRoomId + " " + roomID, null);
            // TODO : check global, route and server change
            // } else if(inAnotherServer){
        } else {
            System.out.println("WARN : Received room ID does not exist");
            messageSend(connected, "roomchange " + clientState.getClientID() + " " + formerRoomId + " " + formerRoomId,
                    null);
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("INFO : THE CLIENT" + " " + clientSocket.getInetAddress()
                    + ":" + clientSocket.getPort() + " IS CONNECTED ");

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            this.dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            while (true) {
                String jsonStringFromClient = bufferedReader.readLine();
                if (jsonStringFromClient.equalsIgnoreCase("exit")) {
                    break;
                }
                try {
                    // convert received message to json object
                    Object object = null;
                    JSONParser jsonParser = new JSONParser();
                    object = jsonParser.parse(jsonStringFromClient);
                    JSONObject j_object = (JSONObject) object;
                    if (hasKey(j_object, "type")) {
                        // check new identity format
                        if (j_object.get("type").equals("newidentity") && j_object.get("identity") != null) {
                            String newClientID = j_object.get("identity").toString();
                            newID(newClientID, clientSocket, jsonStringFromClient);
                        } // check create room
                        if (j_object.get("type").equals("createroom") && j_object.get("roomid") != null) {
                            String newRoomID = j_object.get("roomid").toString();
                            createRoom(newRoomID, clientSocket, jsonStringFromClient);
                        } // check who
                        if (j_object.get("type").equals("who")) {
                            who(clientSocket, jsonStringFromClient);
                        } // check list
                        if (j_object.get("type").equals("list")) {
                            list(clientSocket, jsonStringFromClient);
                        } // check join room
                        if (j_object.get("type").equals("joinroom")) {
                            String roomID = j_object.get("roomid").toString();
                            joinRoom(roomID, clientSocket, jsonStringFromClient);
                        }
                    } else {
                        System.out.println("WARN : Command error, Corrupted JSON");
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}