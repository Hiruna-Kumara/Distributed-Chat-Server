package client;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.*;
import messaging.ClientMessage;
import messaging.MessageTransfer;
import messaging.ServerMessage;
import consensus.LeaderState;
import messaging.ClientMessageContext;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

import static messaging.ClientMessageContext.CLIENT_MSG_TYPE;
import static messaging.MessageTransfer.*;

public class ClientHandlerThread extends Thread {

    private final Socket clientSocket;
    private ClientState clientState;
    private int approvedClientID = -1;
    private int approvedRoomCreation = -1;
    private int approvedJoinRoom = -1;
    private String approvedJoinRoomServerHostAddress;
    private String approvedJoinRoomServerPort;
    private  int approvedRoomDeletion = -1;

    final Object lock;


    public ClientHandlerThread(Socket clientSocket) {
        String serverID = ServerState.getInstance().getServerID();
        ServerState.getInstance().getRoomMap().put("MainHall-" + serverID, ServerState.getInstance().getMainHall());

        this.clientSocket = clientSocket;
        this.lock = new Object();
    }


    public void setApprovedClientID(int approvedClientID) {
        this.approvedClientID = approvedClientID;
    }

    public void setApprovedJoinRoom(int approvedJoinRoom) {
        this.approvedJoinRoom = approvedJoinRoom;
    }

    public void setApprovedJoinRoomServerHostAddress(String approvedJoinRoomServerHostAddress) {
        this.approvedJoinRoomServerHostAddress = approvedJoinRoomServerHostAddress;
    }

    public void setApprovedJoinRoomServerPort(String approvedJoinRoomServerPort) {
        this.approvedJoinRoomServerPort = approvedJoinRoomServerPort;
    }

    public void setApprovedRoomCreation(int approvedRoomCreation) {
        this.approvedRoomCreation = approvedRoomCreation;
    }

    public Object getLock() {
        return lock;
    }

    // format message before sending it to client
    private void messageSend(ArrayList<Socket> socketList, ClientMessageContext msgCtx) throws IOException {
        JSONObject sendToClient = new JSONObject();

        if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.NEW_ID)) {
            sendToClient = ClientMessage.getApprovalNewID(msgCtx.isNewClientIdApproved);
            sendClient(sendToClient, clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.JOIN_ROOM)) {
            sendToClient = ClientMessage.getJoinRoom(msgCtx.clientID, msgCtx.formerRoomID, msgCtx.roomID);
            if (socketList != null) sendBroadcast(sendToClient, socketList);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.ROUTE)) {
            sendToClient = ClientMessage.getRoute(msgCtx.roomID, msgCtx.targetHost, msgCtx.targetPort);
            sendClient(sendToClient, clientSocket);
            if (socketList != null) sendBroadcast(sendToClient, socketList);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.CREATE_ROOM)) {
            sendToClient = ClientMessage.getCreateRoom(msgCtx.roomID, msgCtx.isNewRoomIdApproved);
            sendClient(sendToClient, clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM)) {
            sendToClient = ClientMessage.getCreateRoomChange(msgCtx.clientID, msgCtx.formerRoomID, msgCtx.roomID);
            sendBroadcast(sendToClient, socketList);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.WHO)) {
            sendToClient = ClientMessage.getWho(msgCtx.roomID, msgCtx.participantsList, msgCtx.clientID);//owner
            sendClient(sendToClient, clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.LIST)) {
            sendToClient = ClientMessage.getList(msgCtx.participantsList);
            sendClient(sendToClient, clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.DELETE_ROOM)) {
            sendToClient = ClientMessage.getDeleteRoom(msgCtx.roomID, msgCtx.isDeleteRoomApproved);
            sendClient(sendToClient, clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.MESSAGE)) {
            sendToClient = ClientMessage.getMessage(msgCtx.clientID, msgCtx.body);
            sendBroadcast(sendToClient, socketList);
        }
    }


    //new identity
    private void newID(String clientID,  String jsonStringFromClient) throws IOException, InterruptedException {
        if (checkID(clientID)) {
            // busy wait until leader is elected
            while (!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }
            synchronized (lock) {
                while (approvedClientID == -1) {
                    // if self is leader get direct approval
                    if (LeaderState.getInstance().isLeader()) {
                        boolean approved = !LeaderState.getInstance().isClientIDAlreadyTaken( clientID );
                        approvedClientID = approved ? 1 : 0;
                        System.out.println("INFO : Client ID '"+ clientID + " is" + (approved ? " ":" not ") + "approved");

                    } else {
                        try {
                            // send client id approval request to leader
                            MessageTransfer.sendToLeader(
                                    ServerMessage.getClientIdApprovalRequest(clientID,
                                            String.valueOf(ServerState.getInstance().getSelfID()),
                                            String.valueOf(this.getId())
                                    )
                            );

                            System.out.println("INFO : Client ID '" + clientID + "' sent to leader for approval");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        lock.wait(7000);

                    }

                }
            }
            if (approvedClientID == 1) {
                System.out.println("INFO : Received correct ID ::" + jsonStringFromClient);
                this.clientState = new ClientState( clientID, ServerState.getInstance().getMainHall().getRoomID(), clientSocket );
                ServerState.getInstance().getMainHall().addParticipants(clientState);

                //create broadcast list
                String mainHallRoomID = ServerState.getInstance().getMainHall().getRoomID();
                HashMap<String, ClientState> mainHallClientList = ServerState.getInstance()
                        .getRoomMap()
                        .get(mainHallRoomID)
                        .getClientStateMap();
                ArrayList<Socket> socketList = new ArrayList<>();
                for (String each : mainHallClientList.keySet()) {
                    socketList.add(mainHallClientList.get(each).getSocket());
                }
                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setMessageType(CLIENT_MSG_TYPE.NEW_ID)
                        .setClientID(clientID)
                        .setIsNewClientIdApproved("true")
                        .setFormerRoomID("")
                        .setRoomID(mainHallRoomID)
                        .setCurrentServerID("MainHall-" + ServerState.getInstance().getServerID());

                synchronized (clientSocket ) {
                    messageSend(null, msgCtx);
                    messageSend(socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.JOIN_ROOM));

                }
            } else if (approvedClientID == 0) {
                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setMessageType(CLIENT_MSG_TYPE.NEW_ID)
                        .setClientID(clientID)
                        .setIsNewClientIdApproved("false");
                System.out.println("WARN : ID already in use");
                messageSend(null, msgCtx);
            }
            approvedClientID = -1;
        } else {
            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setClientID(clientID)
                    .setIsNewClientIdApproved("false");


            System.out.println("WARN : Recieved wrong ID type");
            messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.NEW_ID));
        }
    }


    // list
    private void list( String jsonStringFromClient) throws IOException {

        //TODO : impl wrong, remove shared attr
        ClientMessageContext msgCtx = new ClientMessageContext()
                .setRoomsList(SharedAttributes.getRooms());

        System.out.println("INFO : rooms in the system :" + SharedAttributes.getRooms());
        messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.LIST));
    }

    // who
    private void who(String jsonStringFromClient) throws IOException {
        String roomID = clientState.getRoomID();
        Room room = ServerState.getInstance().getRoomMap().get(roomID);
        HashMap<String, ClientState> clientStateMap = room.getClientStateMap();

        List<String> participantsList = new ArrayList<>(clientStateMap.keySet());
        String ownerID = room.getOwnerIdentity();

        ClientMessageContext msgCtx = new ClientMessageContext()
                .setClientID(ownerID) //Owner
                .setRoomID(clientState.getRoomID())
                .setParticipantsList(participantsList);
        System.out.println("LOG  : participants in room [" + roomID + "] : " + participantsList);
        messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.WHO));
    }

    // create room
    private void createRoom(String newRoomID,  String jsonStringFromClient) throws IOException, InterruptedException {
        if (checkID(newRoomID) && !clientState.isRoomOwner()) {
            // busy wait until leader is elected
            while (!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }
            synchronized (lock) {
                while (approvedRoomCreation == -1) {
                    // if self is leader get direct approval
                    if (LeaderState.getInstance().isLeader()) {
                        boolean approved = LeaderState.getInstance().isRoomCreationApproved( newRoomID );
                        approvedRoomCreation = approved ? 1 : 0;
                        System.out.println("INFO : Room '"+ newRoomID +
                                "' creation request from client " + clientState.getClientID() +
                                " is" + (approved ? " ":" not ") + "approved");


                    } else {
                        try {
                            // send room creation approval request to leader
                            MessageTransfer.sendToLeader(
                                    ServerMessage.getRoomCreateApprovalRequest(clientState.getClientID(),
                                            newRoomID,
                                            String.valueOf(ServerState.getInstance().getSelfID()),
                                            String.valueOf(this.getId())
                                    )
                            );

                            System.out.println("INFO : Room '" + newRoomID + "' create request by '"
                                    + clientState.getClientID() + "' sent to leader for approval");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        lock.wait(7000);
                    }

                }
            }
            if (approvedRoomCreation == 1) {
                System.out.println("INFO : Received correct room ID ::" + jsonStringFromClient);

                String formerRoomID = clientState.getRoomID();

                String former = clientState.getRoomID();

                // list of clients inside MainHall
                HashMap<String, ClientState> clientList = ServerState.getInstance().getRoomMap().get(former).getClientStateMap();

                // create broadcast list
                ArrayList<Socket> formerSocket = new ArrayList<>();
                for (String each : clientList.keySet()) {
                    formerSocket.add(clientList.get(each).getSocket());
                }
                ServerState.getInstance().getRoomMap().get(formerRoomID).removeParticipants(clientState);

                Room newRoom = new Room(clientState.getClientID(), newRoomID, ServerState.getInstance().getSelfID());
                ServerState.getInstance().getRoomMap().put(newRoomID, newRoom);

                clientState.setRoomID(newRoomID);
                clientState.setRoomOwner(true);
                newRoom.addParticipants(clientState);

                SharedAttributes.addNewRoomToGlobalRoomList(newRoomID, SharedAttributes.getRooms());

                synchronized (clientSocket) { //TODO : check sync | lock on out buffer?
                    ClientMessageContext msgCtx = new ClientMessageContext()
                            .setClientID(clientState.getClientID())
                            .setRoomID(newRoomID)
                            .setFormerRoomID(formerRoomID)
                            .setIsNewRoomIdApproved("true");

                    messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.CREATE_ROOM));
                    messageSend(formerSocket, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));
                }

                int index = SharedAttributes.getNeighbourIndex();
                Server destServer = ServerState.getInstance().getServers().get(index);
                JSONObject obj = new JSONObject();
                obj.put("room", newRoomID); //////   use this for delete room function.
                MessageTransfer.sendRooms(obj, destServer);

            } else if (approvedRoomCreation == 0) {
                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setRoomID(newRoomID)
                        .setIsNewRoomIdApproved("false");

                System.out.println("WARN : Room id [" + newRoomID + "] already in use");
                messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.CREATE_ROOM));

            }
            approvedRoomCreation = -1;
        } else {
            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setRoomID(newRoomID)
                    .setIsNewRoomIdApproved("false");

            System.out.println("WARN : Received wrong room ID type or client already owns a room [" + newRoomID + "]");
            messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.CREATE_ROOM));
        }
    }

    // join room
    private void joinRoom(String roomID, String jsonStringFromClient) throws IOException, InterruptedException {
        String formerRoomID = clientState.getRoomID();

        //local room change
        if (!clientState.isRoomOwner() && ServerState.getInstance().getRoomMap().containsKey(roomID)) {
            // TODO : check sync
            clientState.setRoomID(roomID);
            ServerState.getInstance().getRoomMap().get(formerRoomID).removeParticipants(clientState);
            ServerState.getInstance().getRoomMap().get(roomID).addParticipants(clientState);

            System.out.println("INFO : client [" + clientState.getClientID() + "] joined room :" + roomID);

            // create broadcast list
            HashMap<String, ClientState> clientListNew = ServerState.getInstance().getRoomMap().get(roomID)
                    .getClientStateMap();
            HashMap<String, ClientState> clientListOld = ServerState.getInstance().getRoomMap().get(formerRoomID)
                    .getClientStateMap();
            HashMap<String, ClientState> clientList = new HashMap<>();
            clientList.putAll(clientListOld);
            clientList.putAll(clientListNew);

            ArrayList<Socket> SocketList = new ArrayList<>();
            for (String each : clientList.keySet()) {
                SocketList.add(clientList.get(each).getSocket());
            }

            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setClientID(clientState.getClientID())
                    .setRoomID(roomID)
                    .setFormerRoomID(formerRoomID);

            messageSend(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));

            //TODO : check sync
            while (!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }
            //update leader server
            MessageTransfer.sendToLeader(
                    ServerMessage.getJoinRoomRequest(
                            clientState.getClientID(),
                            roomID,
                            formerRoomID,
                            String.valueOf(ServerState.getInstance().getSelfID()),
                            String.valueOf(this.getId()),
                            String.valueOf(true)
                    )
            );

        } else if (!clientState.isRoomOwner()) { //global room change
            //TODO : check sync
            while (!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }

            //reset flag
            approvedJoinRoom = -1;
            //check is room id exists
            MessageTransfer.sendToLeader(
                    ServerMessage.getJoinRoomRequest(
                            clientState.getClientID(),
                            roomID,
                            formerRoomID,
                            String.valueOf(ServerState.getInstance().getSelfID()),
                            String.valueOf(this.getId()),
                            String.valueOf(false)
                    )
            );

            while (approvedJoinRoom == -1) {
                //wait for response
            }

            if (approvedJoinRoom == 1) {
                //update new server : diff route ServerState.getInstance().getRoomMap().get(roomID).addParticipants(clientState);
                //broadcast to both rooms
                ServerState.getInstance().getRoomMap().get(formerRoomID).removeParticipants(clientState);
                System.out.println("INFO : client [" + clientState.getClientID() + "] left room :" + formerRoomID);

                //create broadcast list
                HashMap<String, ClientState> clientListOld = ServerState.getInstance().getRoomMap().get(formerRoomID).getClientStateMap();
                System.out.println("INFO : Send broadcast to former room in local server");

                ArrayList<Socket> SocketList = new ArrayList<>();
                for (String each : clientListOld.keySet()) {
                    SocketList.add(clientListOld.get(each).getSocket());
                }

                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setClientID(clientState.getClientID())
                        .setRoomID(roomID)
                        .setFormerRoomID(formerRoomID)
                        .setTargetHost(approvedJoinRoomServerHostAddress)
                        .setTargetPort(approvedJoinRoomServerPort);

                messageSend(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));

                //server change : route
                messageSend(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.ROUTE));
                System.out.println("INFO : " + msgCtx.toString());

            } else if (approvedJoinRoom == 0) {
                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setClientID(clientState.getClientID())
                        .setRoomID(formerRoomID)       //same
                        .setFormerRoomID(formerRoomID);//same
                System.out.println("WARN : Received room ID does not exist");

                messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.JOIN_ROOM));

            }

            //reset flag
            approvedJoinRoom = -1;

        } else {
            //already owns a room

            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setClientID(clientState.getClientID())
                    .setRoomID(formerRoomID)       //same
                    .setFormerRoomID(formerRoomID);//same

            System.out.println("WARN : Join room denied, Client" + clientState.getClientID() + " Owns a room");
            messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.JOIN_ROOM));
        }
    }

    //Move join
    private void moveJoin(String roomID, String formerRoomID, String clientID, String jsonStringFromClient) throws IOException, InterruptedException {
        if (ServerState.getInstance().getRoomMap().containsKey(roomID)) {
            this.clientState = new ClientState(clientID, roomID, clientSocket);
            ServerState.getInstance().getRoomMap().get(roomID).addParticipants(clientState);

            // TODO on new server :
            //create broadcast list
            HashMap<String, ClientState> clientListNew = ServerState.getInstance().getRoomMap().get(roomID).getClientStateMap();

            ArrayList<Socket> SocketList = new ArrayList<>();
            for (String each : clientListNew.keySet()) {
                SocketList.add(clientListNew.get(each).getSocket());
            }

            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setClientID(clientState.getClientID())
                    .setRoomID(roomID)
                    .setFormerRoomID(formerRoomID);

            messageSend(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));


            //TODO : check sync
            while (!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }
            //update leader server
            MessageTransfer.sendToLeader(
                    ServerMessage.getMoveJoinRequest(
                            clientState.getClientID(),
                            roomID,
                            formerRoomID,
                            String.valueOf(ServerState.getInstance().getSelfID()),
                            String.valueOf(this.getId())
                    )
            );
        } else {
            //room missing : place in main hall
            this.clientState = new ClientState(clientID, "MainHall-" + ServerState.getInstance().getServerID(), clientSocket);
            ServerState.getInstance().getRoomMap().get("MainHall-" + ServerState.getInstance().getServerID()).addParticipants(clientState);

            // TODO on new server :
            //create broadcast list
            HashMap<String, ClientState> clientListNew = ServerState.getInstance().getRoomMap().get("MainHall-" + ServerState.getInstance().getServerID()).getClientStateMap();

            ArrayList<Socket> SocketList = new ArrayList<>();
            for (String each : clientListNew.keySet()) {
                SocketList.add(clientListNew.get(each).getSocket());
            }

            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setClientID(clientState.getClientID())
                    .setRoomID("MainHall-" + ServerState.getInstance().getServerID())
                    .setFormerRoomID(formerRoomID);
            messageSend(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));


            //TODO : check sync
            while (!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }
            //update leader server
            MessageTransfer.sendToLeader(
                    ServerMessage.getMoveJoinRequest(
                            clientState.getClientID(),
                            "MainHall-" + ServerState.getInstance().getServerID(),
                            formerRoomID,
                            String.valueOf(ServerState.getInstance().getSelfID()),
                            String.valueOf(this.getId())
                    )
            );
        }
    }

    // join room
    private void deleteRoom(String roomID, Socket connected, String jsonStringFromClient) throws IOException {
        //        String formerRoomID = clientState.getRoomID();
        String mainHallRoomID = ServerState.getInstance().getMainHall().getRoomID();

        if (ServerState.getInstance().getRoomMap().containsKey(roomID)) {
            // TODO : check sync
            Room room = ServerState.getInstance().getRoomMap().get(roomID);
            if (room.getOwnerIdentity().equals(clientState.getClientID())) {

//                String mainHallRoomID = ServerState.getInstance().getMainHall().getRoomID();

                HashMap<String, ClientState> formerClientList = ServerState.getInstance().getRoomMap().get(roomID)
                        .getClientStateMap();
                HashMap<String, ClientState> mainHallClientList = ServerState.getInstance().getRoomMap()
                        .get(mainHallRoomID).getClientStateMap();
                mainHallClientList.putAll(formerClientList);

                ArrayList<Socket> socketList = new ArrayList<>();
                for (String each : mainHallClientList.keySet()) {
                    socketList.add(mainHallClientList.get(each).getSocket());
                }

//                clientState.setRoomID(mainHallRoomID);
                ServerState.getInstance().getRoomMap().remove(roomID);
//                ServerState.getInstance().getRoomMap().get(mainHallRoomID).addParticipants(clientState);
                clientState.setRoomOwner(false);

                for (String client : formerClientList.keySet()) {
                    String clientID = formerClientList.get(client).getClientID();
                    formerClientList.get(client).setRoomID(mainHallRoomID);
                    ServerState.getInstance().getRoomMap().get(mainHallRoomID).addParticipants(formerClientList.get(client));
                    ClientMessageContext msgCtx = new ClientMessageContext()
                            .setClientID(clientID)
                            .setRoomID(mainHallRoomID)
                            .setFormerRoomID(roomID);

                    messageSend(socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));
                }

                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setRoomID(roomID)
                        .setIsDeleteRoomApproved("true");

                messageSend(socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.DELETE_ROOM));
                //TODO : shared attr impl check

                SharedAttributes.removeRoomFromGlobalRoomList(roomID);
                int index = SharedAttributes.getNeighbourIndex();
                Server destServer = ServerState.getInstance().getServers().get(index);
                JSONObject obj = new JSONObject();
                obj.put("delete-room", roomID);
                MessageTransfer.sendRooms(obj, destServer);

                System.out.println("INFO : room [" + roomID + "] was deleted by : " + clientState.getClientID());

            } else {
                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setRoomID(roomID)
                        .setIsDeleteRoomApproved("false");

                messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.DELETE_ROOM));
                System.out.println("WARN : Requesting client [" + clientState.getClientID()
                        + "] does not own the room ID [" + roomID + "]");
            }
            // TODO : check global, room change all members
            // } else if(inAnotherServer){
        } else {
            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setRoomID(roomID)
                    .setIsDeleteRoomApproved("false");

            messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.DELETE_ROOM));
            System.out.println("WARN : Received room ID [" + roomID + "] does not exist");
        }
    }

    //    quit
    private void quit(Socket connected, String jsonStringFromClient) throws IOException {

        String roomID = clientState.getRoomID();
        String mainHallRoomID = ServerState.getInstance().getMainHall().getRoomID();

        if (ServerState.getInstance().getRoomMap().containsKey(roomID)) {
            //TODO : check sync
            Room room = ServerState.getInstance().getRoomMap().get(roomID);

            //create broadcast list
            HashMap<String, ClientState> formerClientList = ServerState.getInstance().getRoomMap().get(roomID).getClientStateMap();
            HashMap<String, ClientState> mainHallClientList = ServerState.getInstance().getRoomMap().get(mainHallRoomID).getClientStateMap();
            mainHallClientList.putAll(formerClientList);

            ArrayList<Socket> socketList = new ArrayList<>();
            for (String each : mainHallClientList.keySet()) {
                socketList.add(mainHallClientList.get(each).getSocket());
            }

            if (room.getOwnerIdentity().equals(clientState.getClientID())) {

                ServerState.getInstance().getRoomMap().remove(roomID);

                for(String clientID:formerClientList.keySet()){
                    if (clientState.getClientID().equals(clientID)){
                        ClientMessageContext msgCtx = new ClientMessageContext()
                                .setClientID(clientID)
                                .setRoomID(mainHallRoomID)
                                .setFormerRoomID("");
                        messageSend(socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));
                    } else {
                        formerClientList.get(clientID).setRoomID(mainHallRoomID);
                        ServerState.getInstance().getRoomMap().get(mainHallRoomID).addParticipants(formerClientList.get(clientID));

                        ClientMessageContext msgCtx = new ClientMessageContext()
                                .setClientID(clientID)
                                .setRoomID(mainHallRoomID)
                                .setFormerRoomID(roomID);
                        messageSend(socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));
                    }
                }

                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setRoomID(roomID)
                        .setIsDeleteRoomApproved("true");

                messageSend(socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.DELETE_ROOM));

                System.out.println("INFO : " + clientState.getClientID() + " is quit");


            } else {
                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setClientID(clientState.getClientID())
                        .setRoomID("") //exit
                        .setFormerRoomID(roomID)
                        .setIsDeleteRoomApproved("true");

                ServerState.getInstance().getRoomMap().get(roomID).removeParticipants(clientState);
                messageSend(socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));
                System.out.println("INFO : " + clientState.getClientID() + " is quit");

            }

        } else {
            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setRoomID(roomID)
                    .setIsDeleteRoomApproved("false");

            messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.DELETE_ROOM));

            System.out.println("WARN : Received room ID [" + roomID + "] does not exist");
            }
    }

    // message
    private void message(String content, Socket connected, String fromclient) throws IOException {
        String clientID  = clientState.getClientID();
        String roomid = clientState.getRoomID();

        HashMap<String, ClientState> clientList = ServerState.getInstance().getRoomMap().get(roomid)
                .getClientStateMap();

        // create broadcast list
        ArrayList<Socket> socketsList  = new ArrayList<>();
        for (String each : clientList.keySet()) {
            if (!clientList.get(each).getClientID().equals(clientID)){
                socketsList.add(clientList.get(each).getSocket());}
        }

        ClientMessageContext msgCtx = new ClientMessageContext()
                .setClientID(clientID)
                .setBody(content);
        messageSend(socketsList, msgCtx.setMessageType(CLIENT_MSG_TYPE.MESSAGE));

    }

    @Override
    public void run() {
        try {
            System.out.println("INFO : THE CLIENT" + " " + clientSocket.getInetAddress()
                    + ":" + clientSocket.getPort() + " IS CONNECTED ");

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));


            while (true) {

                String jsonStringFromClient = bufferedReader.readLine();



                try {

                    if (jsonStringFromClient==null){
                        continue;
                    }

                    // convert received message to json object
                    Object object = null;
                    JSONParser jsonParser = new JSONParser();
                    object = jsonParser.parse(jsonStringFromClient);
                    JSONObject j_object = (JSONObject) object;

                    if (hasKey(j_object, "type")) {
                        // check new identity format
                        if (j_object.get("type").equals("newidentity") && j_object.get("identity") != null) {
                            String newClientID = j_object.get("identity").toString();
                            newID(newClientID, jsonStringFromClient);
                        } // check create room
                        if (j_object.get("type").equals("createroom") && j_object.get("roomid") != null) {
                            String newRoomID = j_object.get("roomid").toString();
                            createRoom(newRoomID,  jsonStringFromClient);
                        } // check who
                        if (j_object.get("type").equals("who")) {
                            who( jsonStringFromClient);
                        } // check list
                        if (j_object.get("type").equals("list")) {
                            list( jsonStringFromClient);
                        } // check join room
                        if (j_object.get("type").equals("joinroom")) {
                            String roomID = j_object.get("roomid").toString();
                            joinRoom(roomID,  jsonStringFromClient);
                        }
                        //check move join
                        if (j_object.get("type").equals("movejoin")) {
                            String roomID = j_object.get("roomid").toString();
                            String formerRoomID = j_object.get("former").toString();
                            String clientID = j_object.get("identity").toString();
                            moveJoin(roomID, formerRoomID, clientID, jsonStringFromClient);
                        }
                        if (j_object.get("type").equals("deleteroom")) {
                            String roomID = j_object.get("roomid").toString();
                            deleteRoom(roomID, clientSocket, jsonStringFromClient);
                        }
                        if (j_object.get("type").equals("message")) {
                            String content = j_object.get("content").toString();
                            message(content, clientSocket, jsonStringFromClient);
                        }
                        if (j_object.get("type").equals("quit")) {
                            quit(clientSocket, jsonStringFromClient);
                        }
                    } else {
                        System.out.println("WARN : Command error, Corrupted JSON");
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}