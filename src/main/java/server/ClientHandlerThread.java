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
import java.util.Arrays;

public class ClientHandlerThread extends Thread {

    private final Socket clientSocket;
    private ClientState clientState;

    // TODO : check input stream local var
    private DataOutputStream dataOutputStream;

    public ClientHandlerThread(Socket clientSocket) {
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

    // send broadcast message
    private void sendBroadcast(JSONObject obj, ArrayList<Socket> socketList) throws IOException {
        for (Socket each : socketList) {
            Socket TEMP_SOCK = (Socket) each;
            PrintWriter TEMP_OUT = new PrintWriter(TEMP_SOCK.getOutputStream());
            TEMP_OUT.println(obj);
            TEMP_OUT.flush();
        }
    }

    // send message to client
    private void send(JSONObject obj) throws IOException {
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }

    // format message before sending it to client
    private void messageSend(ArrayList<Socket> socketList, String msg, List<String> msgList) throws IOException {
        JSONObject sendToClient = new JSONObject();
        String[] array = msg.split(" ");
        if (array[0].equals("newid")) {
            sendToClient = ServerMessage.getApprovalNewID(array[1]);
            send(sendToClient);
        } else if (array[0].equals("roomchange")) {
            sendToClient = ServerMessage.getJoinRoom(array[1], array[2].replace("_", ""), array[3]);
            sendBroadcast(sendToClient, socketList);
        } else if (array[0].equals("createroom")) {
            sendToClient = ServerMessage.getCreateRoom(array[1], array[2]);
            send(sendToClient);
        } else if (array[0].equals("roomchangeall")) {
            sendToClient = ServerMessage.getCreateRoomChange(array[1], array[2], array[3]);
            sendBroadcast(sendToClient, socketList);
        } else if (array[0].equals("roomcontents")) {
            sendToClient = ServerMessage.getWho(array[1], msgList, array[2]);
            send(sendToClient);
        } else if (array[0].equals("roomlist")) {
            sendToClient = ServerMessage.getList(msgList);
            send(sendToClient);
        } else if (array[0].equals("deleteroom")) {
            sendToClient = ServerMessage.getDeleteRoom(array[1], array[2]);
            send(sendToClient);
        } else if (array[0].equals("message")) {
            sendToClient = ServerMessage.getMessage(array[1],
                    String.join(" ", Arrays.copyOfRange(array, 2, array.length)));
            sendBroadcast(sendToClient, socketList);
        }
    }

    // new identity
    private void newID(String clientID, Socket connected, String jsonStringFromClient) throws IOException {
        if (checkID(clientID) && !ServerState.getInstance().isClientIDAlreadyTaken(clientID)) {
            System.out.println("INFO : Received correct ID ::" + jsonStringFromClient);

            this.clientState = new ClientState(clientID, ServerState.getInstance().getMainHall().getRoomID(),
                    connected.getPort(), connected);
            ServerState.getInstance().getMainHall().addParticipants(clientState);

            //create broadcast list
            String mainHallRoomID = ServerState.getInstance().getMainHall().getRoomID();
            HashMap<String,ClientState> mainHallClientList = ServerState.getInstance().getRoomMap().get(mainHallRoomID).getClientStateMap();

            ArrayList<Socket> socketList = new ArrayList<>();
            for (String each:mainHallClientList.keySet()){
                socketList.add(mainHallClientList.get(each).getSocket());
            }

            synchronized (connected) {
                messageSend(null, "newid true", null);
                messageSend(socketList, "roomchange " + clientID + " _" + " MainHall-" + ServerState.getInstance().getServerID(), null);
            }
        } else {
            System.out.println("WARN : Recieved wrong ID type or ID already in use");
            messageSend(null, "newid false", null);
        }
    }

    // list
    private void list(Socket connected, String jsonStringFromClient) throws IOException {
        List<String> roomsList = new ArrayList<>(ServerState.getInstance().getRoomMap().keySet());

        System.out.println("INFO : rooms in the system :");
        messageSend(null, "roomlist ", roomsList);
    }

    // who
    private void who(Socket connected, String jsonStringFromClient) throws IOException {
        String roomID = clientState.getRoomID();
        Room room = ServerState.getInstance().getRoomMap().get(roomID);
        HashMap<String, ClientState> clientStateMap = room.getClientStateMap();

        List<String> participants = new ArrayList<>(clientStateMap.keySet());

        String owner = room.getOwnerIdentity();
        System.out.println("LOG  : participants in room [" + roomID + "] : " + participants);
        messageSend(null, "roomcontents " + roomID + " " + owner, participants);
    }

    // create room
    private void createRoom(String newRoomID, Socket connected, String jsonStringFromClient) throws IOException {
        if (checkID(newRoomID) && !ServerState.getInstance().getRoomMap().containsKey(newRoomID)) {
            System.out.println("INFO : Received correct room ID ::" + jsonStringFromClient);

            String formerRoomID = clientState.getRoomID();

            String former = clientState.getRoomID();
            HashMap<String, ClientState> clientList = ServerState.getInstance().getRoomMap().get(former)
                    .getClientStateMap();

            // create broadcast list
            ArrayList<Socket> formerSocket = new ArrayList<>();
            for (String each : clientList.keySet()) {
                formerSocket.add(clientList.get(each).getSocket());
            }

            ServerState.getInstance().getRoomMap().get(formerRoomID).removeParticipants(clientState);

            Room newRoom = new Room(clientState.getClientID(), newRoomID);
            ServerState.getInstance().getRoomMap().put(newRoomID, newRoom);

            clientState.setRoomID(newRoomID);
            newRoom.addParticipants(clientState);

            synchronized (connected) { // TODO : check sync | lock on out buffer?
                messageSend(null, "createroom " + newRoomID + " true", null);
                messageSend(formerSocket,
                        "roomchangeall " + clientState.getClientID() + " " + formerRoomID + " " + newRoomID, null);
            }
        } else {
            System.out.println("WARN : Recieved wrong room ID type or room ID already in use");
            messageSend(null, "createroom " + newRoomID + " false", null);
        }
    }

    // join room
    private void joinRoom(String roomID, Socket connected, String jsonStringFromClient) throws IOException {
        String formerRoomId = clientState.getRoomID();

        if (ServerState.getInstance().getRoomMap().containsKey(roomID)) {
            // TODO : check sync
            clientState.setRoomID(roomID);
            ServerState.getInstance().getRoomMap().get(formerRoomId).removeParticipants(clientState);
            ServerState.getInstance().getRoomMap().get(roomID).addParticipants(clientState);

            System.out.println("INFO : client [" + clientState.getClientID() + "] joined room :" + roomID);

            // create broadcast list
            HashMap<String, ClientState> clientList = ServerState.getInstance().getRoomMap().get(roomID)
                    .getClientStateMap();
            HashMap<String, ClientState> clientListOld = ServerState.getInstance().getRoomMap().get(formerRoomId)
                    .getClientStateMap();
            clientList.putAll(clientListOld);

            ArrayList<Socket> SocketList = new ArrayList<>();
            for (String each : clientList.keySet()) {
                SocketList.add(clientList.get(each).getSocket());
            }

            messageSend(SocketList, "roomchangeall " + clientState.getClientID() + " " + formerRoomId + " " + roomID,
                    null);
            // TODO : show for ones already in room

            // TODO : check global, route and server change
            // } else if(inAnotherServer){
        } else {
            System.out.println("WARN : Received room ID does not exist");
            messageSend(null, "roomchange " + clientState.getClientID() + " " + formerRoomId + " " + formerRoomId,
                    null);
        }
    }

    // join room
    private void deleteRoom(String roomID, Socket connected, String jsonStringFromClient) throws IOException {
        String formerRoomID = clientState.getRoomID();

        if (ServerState.getInstance().getRoomMap().containsKey(roomID)) {
            // TODO : check sync
            Room room = ServerState.getInstance().getRoomMap().get(roomID);
            if (room.getOwnerIdentity().equals(clientState.getClientID())) {

                String mainHallRoomID = ServerState.getInstance().getMainHall().getRoomID();

                HashMap<String, ClientState> formerClientList = ServerState.getInstance().getRoomMap().get(roomID)
                        .getClientStateMap();
                HashMap<String, ClientState> mainHallClientList = ServerState.getInstance().getRoomMap()
                        .get(mainHallRoomID).getClientStateMap();
                mainHallClientList.putAll(formerClientList);

                ArrayList<Socket> socketList = new ArrayList<>();
                for (String each : mainHallClientList.keySet()) {
                    socketList.add(mainHallClientList.get(each).getSocket());
                }

                clientState.setRoomID(mainHallRoomID);
                ServerState.getInstance().getRoomMap().remove(roomID);
                ServerState.getInstance().getRoomMap().get(mainHallRoomID).addParticipants(clientState);

                for (String client : formerClientList.keySet()) {
                    String id = formerClientList.get(client).getClientID();
                    messageSend(socketList, "roomchangeall " + id + " " + roomID + " " + mainHallRoomID, null);
                }

                messageSend(null, "deleteroom " + roomID + " true", null);

                System.out.println("INFO : room [" + roomID + "] was deleted by : " + clientState.getClientID());

            } else {
                messageSend(null, "deleteroom " + roomID + " false", null);
                System.out.println("WARN : Requesting client [" + clientState.getClientID()
                        + "] does not own the room ID [" + roomID + "]");
            }
            // TODO : check global, room change all members
            // } else if(inAnotherServer){
        } else {
            System.out.println("WARN : Received room ID [" + roomID + "] does not exist");
            messageSend(null, "deleteroom " + roomID + " false", null);
        }
    }

    // message
    private void message(String content, Socket connected, String fromclient) throws IOException {
        String id = clientState.getClientID();
        String roomid = clientState.getRoomID();

        HashMap<String, ClientState> clientList = ServerState.getInstance().getRoomMap().get(roomid)
                .getClientStateMap();

        // create broadcast list
        ArrayList<Socket> roomList = new ArrayList<>();
        for (String each : clientList.keySet()) {
            if (!clientList.get(each).getClientID().equals(id)) {
                roomList.add(clientList.get(each).getSocket());
            }
        }

        messageSend(roomList, "message " + id + " " + content, null);
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
                        if (j_object.get("type").equals("deleteroom")) {
                            String roomID = j_object.get("roomid").toString();
                            deleteRoom(roomID, clientSocket, jsonStringFromClient);
                        }
                        if (j_object.get("type").equals("message")) {
                            String content = j_object.get("content").toString();
                            message(content, clientSocket, jsonStringFromClient);
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