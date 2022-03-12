package client;

import consensus.LeaderState;
import messaging.ClientMessageContext;
import messaging.MessageTransfer;
import messaging.ServerMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.*;
import messaging.ClientMessage;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    private List<String> roomsListTemp;

    final Object lock;

    private boolean quitFlag = false;

    public ClientHandlerThread(Socket clientSocket) {
        String serverID = ServerState.getInstance().getServerID();
        this.clientSocket = clientSocket;
        this.lock = new Object();
    }

    public String getClientId()
    {
        return clientState.getClientID();
    }

    public void setApprovedClientID( int approvedClientID ) {
        this.approvedClientID = approvedClientID;
    }

    public void setApprovedRoomCreation( int approvedRoomCreation ) {
        this.approvedRoomCreation = approvedRoomCreation;
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

    public void setRoomsListTemp(List<String> roomsListTemp) {
        this.roomsListTemp = roomsListTemp;
    }

    public Object getLock() {
        return lock;
    }

    //format message before sending it to client
    private void messageSend(ArrayList<Socket> socketList, ClientMessageContext msgCtx) throws IOException {
        JSONObject sendToClient = new JSONObject();
        if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.NEW_ID)) {
            sendToClient = ClientMessage.getApprovalNewID(msgCtx.isNewClientIdApproved);
            sendClient(sendToClient,clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.JOIN_ROOM)) {
            sendToClient = ClientMessage.getJoinRoom(msgCtx.clientID, msgCtx.formerRoomID, msgCtx.roomID);
            if (socketList != null) sendBroadcast(sendToClient, socketList);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.ROUTE)) {
            sendToClient = ClientMessage.getRoute(msgCtx.roomID, msgCtx.targetHost, msgCtx.targetPort);
            sendClient(sendToClient, clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.SERVER_CHANGE)) {
            sendToClient = ClientMessage.getServerChange(msgCtx.isServerChangeApproved, msgCtx.approvedServerID);
            sendClient(sendToClient, clientSocket);
            //TODO: do the coherent functions like room change broadcast in same line
            //if (socketList != null) sendBroadcast(sendToClient, socketList);
        }
        else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.CREATE_ROOM)) {
            sendToClient = ClientMessage.getCreateRoom(msgCtx.roomID, msgCtx.isNewRoomIdApproved);
            sendClient(sendToClient,clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM)) {
            sendToClient = ClientMessage.getCreateRoomChange(msgCtx.clientID, msgCtx.formerRoomID, msgCtx.roomID);
            sendBroadcast(sendToClient, socketList);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.WHO)) {
            sendToClient = ClientMessage.getWho(msgCtx.roomID, msgCtx.participantsList, msgCtx.clientID);//owner
            sendClient(sendToClient,clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.LIST)) {
            sendToClient = ClientMessage.getList(msgCtx.roomsList);
            sendClient(sendToClient,clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.DELETE_ROOM)) {
            sendToClient = ClientMessage.getDeleteRoom(msgCtx.roomID, msgCtx.isDeleteRoomApproved);
            sendClient(sendToClient,clientSocket);
        } else if (msgCtx.messageType.equals(CLIENT_MSG_TYPE.MESSAGE)) {
            sendToClient = ClientMessage.getMessage(msgCtx.clientID,msgCtx.body);
            sendBroadcast(sendToClient, socketList);
        }
    }

    //new identity
    private void newID(String clientID) throws IOException, InterruptedException
    {
        if (checkID(clientID)) {
            // busy wait until leader is elected
            while(!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }
            synchronized( lock ) {
                while( approvedClientID == -1 )
                {
                    // if self is leader get direct approval
                    if( LeaderState.getInstance().isLeader() )
                    {
                        boolean approved = !LeaderState.getInstance().isClientIDAlreadyTaken( clientID );
                        approvedClientID = approved ? 1 : 0;
                        System.out.println("INFO : Client ID '"+ clientID + " is" + (approved ? " ":" not ") + "approved");
                    }
                    else
                    {
                        try
                        {
                            // send client id approval request to leader
                            MessageTransfer.sendToLeader(
                                    ServerMessage.getClientIdApprovalRequest( clientID,
                                            String.valueOf( ServerState.getInstance().getSelfID() ),
                                            String.valueOf( this.getId() )
                                    )
                            );

                            System.out.println( "INFO : Client ID '" + clientID + "' sent to leader for approval" );
                        }
                        catch( Exception e )
                        {
                            e.printStackTrace();
                        }
                        lock.wait(7000);
                    }
                }
            }

            if( approvedClientID == 1 ) {
                System.out.println( "INFO : Received correct ID :" + clientID );
                this.clientState = new ClientState( clientID, ServerState.getInstance().getMainHall().getRoomID(), clientSocket );
                ServerState.getInstance().getMainHall().addParticipants( clientState );

                //update if self is leader
                if (LeaderState.getInstance().isLeader()) {
                    LeaderState.getInstance().addClient(new ClientState( clientID, clientState.getRoomID(), null ));
                }

                // create broadcast list
                String mainHallRoomID = ServerState.getInstance().getMainHall().getRoomID();
                HashMap<String,ClientState> mainHallClientList = ServerState.getInstance()
                        .getRoomMap()
                        .get( mainHallRoomID )
                        .getClientStateMap();

                ArrayList<Socket> socketList = new ArrayList<>();
                for( String each : mainHallClientList.keySet() )
                {
                    socketList.add( mainHallClientList.get( each ).getSocket() );
                }

                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setMessageType(CLIENT_MSG_TYPE.NEW_ID)
                        .setClientID(clientID)
                        .setIsNewClientIdApproved("true")
                        .setFormerRoomID("")
                        .setRoomID(mainHallRoomID);

                synchronized( clientSocket )
                {
                    messageSend( null, msgCtx );
                    messageSend( socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.JOIN_ROOM) );
                }
            }  else if( approvedClientID == 0 ) {
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

    //list
    private void list() throws IOException, InterruptedException {
        //reset Temp room list
        roomsListTemp = null;

        while (!LeaderState.getInstance().isLeaderElected()) {
            Thread.sleep(1000);
        }

        // if self is leader get list direct from leader state
        if (LeaderState.getInstance().isLeader()) {
            roomsListTemp = LeaderState.getInstance().getRoomIDList();
        } else { // send list request to leader
            MessageTransfer.sendToLeader(
                    ServerMessage.getListRequest(
                            clientState.getClientID(),
                            String.valueOf(this.getId()),
                            String.valueOf(ServerState.getInstance().getSelfID()))
            );

            synchronized (lock) {
                while (roomsListTemp == null) {
                    lock.wait(7000);
                }
            }
        }

        if (roomsListTemp != null) {
            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setRoomsList(roomsListTemp);

            System.out.println("INFO : Recieved rooms in the system :" + roomsListTemp);
            messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.LIST));
        }
    }

    //who
    private void who() throws IOException {
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

    //create room
    private void createRoom(String newRoomID) throws IOException, InterruptedException
    {
        if (checkID(newRoomID) && !clientState.isRoomOwner()) {
            // busy wait until leader is elected
            while(!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }

            synchronized( lock )
            {
                while( approvedRoomCreation == -1 )
                {
                    // if self is leader get direct approval
                    if( LeaderState.getInstance().isLeader() )
                    {
                        boolean approved = LeaderState.getInstance().isRoomCreationApproved( newRoomID );
                        approvedRoomCreation = approved ? 1 : 0;
                        System.out.println("INFO : Room '"+ newRoomID +
                                                   "' creation request from client " + clientState.getClientID() +
                                                   " is" + (approved ? " ":" not ") + "approved");
                    }
                    else
                    {
                        try
                        {
                            // send room creation approval request to leader
                            MessageTransfer.sendToLeader(
                                    ServerMessage.getRoomCreateApprovalRequest( clientState.getClientID(),
                                            newRoomID,
                                            String.valueOf( ServerState.getInstance().getSelfID() ),
                                            String.valueOf( this.getId() )
                                    )
                            );

                            System.out.println( "INFO : Room '" + newRoomID + "' create request by '"
                                    + clientState.getClientID() + "' sent to leader for approval" );
                        }
                        catch( Exception e )
                        {
                            e.printStackTrace();
                        }
                        lock.wait( 7000 );
                    }
                }
            }
            if( approvedRoomCreation == 1) {
                System.out.println( "INFO : Received correct room ID :" + newRoomID );

                String formerRoomID = clientState.getRoomID();

                // list of clients inside former room
                HashMap<String,ClientState> clientList = ServerState.getInstance().getRoomMap().get( formerRoomID ).getClientStateMap();

                // create broadcast list
                ArrayList<Socket> formerSocket = new ArrayList<>();
                for( String each : clientList.keySet() )
                {
                    formerSocket.add( clientList.get( each ).getSocket() );
                }

                //update server state
                ServerState.getInstance().getRoomMap().get( formerRoomID ).removeParticipants( clientState.getClientID() );

                Room newRoom = new Room( clientState.getClientID(), newRoomID, ServerState.getInstance().getSelfID() );
                ServerState.getInstance().getRoomMap().put( newRoomID, newRoom );

                clientState.setRoomID( newRoomID );
                clientState.setRoomOwner( true );
                newRoom.addParticipants( clientState );

                //update Leader state if self is leader
                if (LeaderState.getInstance().isLeader()) {
                    LeaderState.getInstance().addApprovedRoom(
                            clientState.getClientID(), newRoomID, ServerState.getInstance().getSelfID());
                }

                synchronized (clientSocket) { //TODO : check sync | lock on out buffer?
                    ClientMessageContext msgCtx = new ClientMessageContext()
                            .setClientID(clientState.getClientID())
                            .setRoomID(newRoomID)
                            .setFormerRoomID(formerRoomID)
                            .setIsNewRoomIdApproved("true");

                    messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.CREATE_ROOM));
                    messageSend(formerSocket, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));
                }

            } else if ( approvedRoomCreation == 0 ) {
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

    //join room
    private void joinRoom(String roomID) throws IOException, InterruptedException {
        String formerRoomID = clientState.getRoomID();

        if (clientState.isRoomOwner()) { //already owns a room
            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setClientID(clientState.getClientID())
                    .setRoomID(formerRoomID)       //same
                    .setFormerRoomID(formerRoomID);//same

            System.out.println("WARN : Join room denied, Client" + clientState.getClientID() + " Owns a room");
            messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.JOIN_ROOM));

        } else if (ServerState.getInstance().getRoomMap().containsKey(roomID)) { //local room change
            //TODO : check sync
            clientState.setRoomID(roomID);
            ServerState.getInstance().getRoomMap().get(formerRoomID).removeParticipants(clientState.getClientID());
            ServerState.getInstance().getRoomMap().get(roomID).addParticipants(clientState);

            System.out.println("INFO : client [" + clientState.getClientID() + "] joined room :" + roomID);

            //create broadcast list
            HashMap<String, ClientState> clientListNew = ServerState.getInstance().getRoomMap().get(roomID).getClientStateMap();
            HashMap<String, ClientState> clientListOld = ServerState.getInstance().getRoomMap().get(formerRoomID).getClientStateMap();
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

            messageSend(SocketList,  msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));

            while (!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }

            // if self is leader update leader state directly
            if (LeaderState.getInstance().isLeader()) {
                LeaderState.getInstance().localJoinRoomClient(clientState, formerRoomID);
            } else {
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
            }

        } else { //global room change

            while (!LeaderState.getInstance().isLeaderElected()) {
                Thread.sleep(1000);
            }

            //reset flag
            approvedJoinRoom = -1;
            //check if room id exist and if init route
            if (LeaderState.getInstance().isLeader()) {
                int serverIDofTargetRoom = LeaderState.getInstance().getServerIdIfRoomExist(roomID);

                approvedJoinRoom = serverIDofTargetRoom != -1 ? 1 : 0;

                if (approvedJoinRoom == 1) {
                    Server serverOfTargetRoom = ServerState.getInstance().getServers().get(serverIDofTargetRoom);
                    approvedJoinRoomServerHostAddress = serverOfTargetRoom.getServerAddress();
                    approvedJoinRoomServerPort = String.valueOf(serverOfTargetRoom.getClientsPort());
                }

                System.out.println("INFO : Received response for route request for join room (Self is Leader)");

            } else {
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

                synchronized (lock) {
                    while (approvedJoinRoom == -1) {
                        System.out.println("INFO : Wait until server approve route on Join room request");
                        lock.wait(7000);
                        //wait for response
                    }
                }

                System.out.println("INFO : Received response for route request for join room");
            }

            if (approvedJoinRoom == 1) {

                //broadcast to former room
                ServerState.getInstance().getRoomMap().get(formerRoomID).removeParticipants(clientState.getClientID());
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

                messageSend(SocketList,  msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));

                //server change : route
                messageSend(SocketList,  msgCtx.setMessageType(CLIENT_MSG_TYPE.ROUTE));
                System.out.println("INFO : Route Message Sent to Client");
                quitFlag = true;

            } else if (approvedJoinRoom == 0) { // Room not found on system
                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setClientID(clientState.getClientID())
                        .setRoomID(formerRoomID)       //same
                        .setFormerRoomID(formerRoomID);//same

                System.out.println("WARN : Received room ID ["+roomID + "] does not exist");
                messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.JOIN_ROOM));
            }

            //reset flag
            approvedJoinRoom = -1;
        }
    }

    //Move join
    private void moveJoin(String roomID, String formerRoomID, String clientID) throws IOException, InterruptedException {
        roomID = (ServerState.getInstance().getRoomMap().containsKey(roomID))? roomID:ServerState.getInstance().getMainHallID();
        this.clientState = new ClientState(clientID, roomID, clientSocket);
        ServerState.getInstance().getRoomMap().get(roomID).addParticipants(clientState);

        //create broadcast list
        HashMap<String, ClientState> clientListNew = ServerState.getInstance().getRoomMap().get(roomID).getClientStateMap();

        ArrayList<Socket> SocketList = new ArrayList<>();
        for (String each : clientListNew.keySet()) {
            SocketList.add(clientListNew.get(each).getSocket());
        }

        ClientMessageContext msgCtx = new ClientMessageContext()
                .setClientID(clientState.getClientID())
                .setRoomID(roomID)
                .setFormerRoomID(formerRoomID)
                .setIsServerChangeApproved("true")
                .setApprovedServerID(ServerState.getInstance().getServerID());

        messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.SERVER_CHANGE));
        messageSend(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));


        //TODO : check sync
        while (!LeaderState.getInstance().isLeaderElected()) {
            Thread.sleep(1000);
        }

        //if self is leader update leader state directly
        if (LeaderState.getInstance().isLeader()) {
            ClientState client = new ClientState(clientID, roomID, null);
            LeaderState.getInstance().addClient(client);
        } else {
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
        }

    }

    //Delete room
    private void deleteRoom(String roomID) throws IOException, InterruptedException {

        String mainHallRoomID = ServerState.getInstance().getMainHall().getRoomID();

        if (ServerState.getInstance().getRoomMap().containsKey(roomID)) {
            //TODO : check sync
            Room room = ServerState.getInstance().getRoomMap().get(roomID);
            if (room.getOwnerIdentity().equals(clientState.getClientID())) {

                // clients in deleted room
                HashMap<String,ClientState> formerClientList = ServerState.getInstance().getRoomMap()
                                                                          .get(roomID).getClientStateMap();
                // former clients in main hall
                HashMap<String,ClientState> mainHallClientList = ServerState.getInstance().getRoomMap()
                                                                            .get(mainHallRoomID).getClientStateMap();
                mainHallClientList.putAll(formerClientList);

                ArrayList<Socket> socketList = new ArrayList<>();
                for (String each : mainHallClientList.keySet()){
                    socketList.add(mainHallClientList.get(each).getSocket());
                }

                ServerState.getInstance().getRoomMap().remove(roomID);
                clientState.setRoomOwner( false );

                // broadcast roomchange message to all clients in deleted room and former clients in main hall
                for(String client:formerClientList.keySet()){
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

                //TODO : check sync
                while (!LeaderState.getInstance().isLeaderElected()) {
                    Thread.sleep(1000);
                }

                //if self is leader update leader state directly
                if (LeaderState.getInstance().isLeader()) {
                    LeaderState.getInstance().removeRoom(roomID, mainHallRoomID, clientState.getClientID());
                } else {
                    //update leader server
                    MessageTransfer.sendToLeader(
                            ServerMessage.getDeleteRoomRequest(clientState.getClientID(), roomID, mainHallRoomID)
                    );
                }

                System.out.println("INFO : room [" + roomID + "] was deleted by : " + clientState.getClientID());

            } else {
                ClientMessageContext msgCtx = new ClientMessageContext()
                        .setRoomID(roomID)
                        .setIsDeleteRoomApproved("false");

                messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.DELETE_ROOM));
                System.out.println("WARN : Requesting client [" + clientState.getClientID() + "] does not own the room ID [" + roomID + "]");
            }
        } else {
            ClientMessageContext msgCtx = new ClientMessageContext()
                    .setRoomID(roomID)
                    .setIsDeleteRoomApproved("false");

            messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.DELETE_ROOM));
            System.out.println("WARN : Received room ID [" + roomID + "] does not exist");
        }
    }

    //quit room
    private void quit() throws IOException, InterruptedException {

        //delete room if room owner
        if (clientState.isRoomOwner()){
            deleteRoom(clientState.getRoomID());
            System.out.println("INFO : Deleted room before " + clientState.getClientID() + " quit");
        }

        //send broadcast with empty target room for quit
        HashMap<String,ClientState> formerClientList = ServerState.getInstance().getRoomMap().get(clientState.getRoomID()).getClientStateMap();

        ArrayList<Socket> socketList = new ArrayList<>();
        for (String each:formerClientList.keySet()){
            socketList.add(formerClientList.get(each).getSocket());
        }
        ClientMessageContext msgCtx = new ClientMessageContext()
                                .setClientID(clientState.getClientID())
                                .setRoomID("")
                                .setFormerRoomID(clientState.getRoomID());
        messageSend(socketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));

        //update Local Server
        ServerState.getInstance().getRoomMap().get(clientState.getRoomID()).removeParticipants(clientState.getClientID());

        // Update global list of Leader
        // send quit message to leader if self is not leader
        if( !LeaderState.getInstance().isLeader() ) {
            MessageTransfer.sendToLeader(
                    ServerMessage.getQuit(clientState.getClientID(), clientState.getRoomID())
            );
        } else {
            // Leader is self , removes client from global list
            LeaderState.getInstance().removeClient(clientState.getClientID(),clientState.getRoomID() );
        }

        if (!clientSocket.isClosed()) clientSocket.close();
        quitFlag = true;
        System.out.println("INFO : " + clientState.getClientID() + " quit");
    }

    //message
    private void message(String content) throws IOException {
        String clientID = clientState.getClientID();
        String roomid = clientState.getRoomID();

        HashMap<String, ClientState> clientList = ServerState.getInstance().getRoomMap().get(roomid).getClientStateMap();

        //create broadcast list
        ArrayList<Socket> socketsList = new ArrayList<>();
        for (String each:clientList.keySet()){
            if (!clientList.get(each).getClientID().equals(clientID)){
                socketsList.add(clientList.get(each).getSocket());
            }
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

            while (!quitFlag) {


                try {
                    String jsonStringFromClient = bufferedReader.readLine();

                    if (jsonStringFromClient==null){
                        continue;
                    }

                    //convert received message to json object
                    Object object = null;
                    JSONParser jsonParser = new JSONParser();
                    object = jsonParser.parse(jsonStringFromClient);
                    JSONObject j_object = (JSONObject) object;

                    if (hasKey(j_object, "type")) {
                        //check new identity format
                        if (j_object.get("type").equals("newidentity") && j_object.get("identity") != null) {
                            String newClientID = j_object.get("identity").toString();
                            newID(newClientID);
                        } //check create room
                        if (j_object.get("type").equals("createroom") && j_object.get("roomid") != null) {
                            String newRoomID = j_object.get("roomid").toString();
                            createRoom(newRoomID);
                        } //check who
                        if (j_object.get("type").equals("who")) {
                            who();
                        } //check list
                        if (j_object.get("type").equals("list")) {
                            list();
                        } //check join room
                        if (j_object.get("type").equals("joinroom")) {
                            String roomID = j_object.get("roomid").toString();
                            joinRoom(roomID);
                        } //check move join
                        if (j_object.get("type").equals("movejoin")) {
                            String roomID = j_object.get("roomid").toString();
                            String formerRoomID = j_object.get("former").toString();
                            String clientID = j_object.get("identity").toString();
                            moveJoin(roomID, formerRoomID, clientID);
                        } //check delete room
                        if (j_object.get("type").equals("deleteroom")) {
                            String roomID = j_object.get("roomid").toString();
                            deleteRoom(roomID);
                        } //check message
                        if (j_object.get("type").equals("message")) {
                            String content = j_object.get("content").toString();
                            message(content);
                        } //check quit
                        if (j_object.get("type").equals("quit")) {
                            quit();
                        }
                    } else {
                        System.out.println("WARN : Command error, Corrupted JSON");
                    }

                } catch (ParseException | InterruptedException | SocketException e) {
                    quit();
                    //e.printStackTrace();
                    System.out.println("WARN : " + clientState.getClientID() + " forced quit on exception : " + e.getMessage());
                }
            }

        } catch (IOException | InterruptedException e) {
            //e.printStackTrace();
            System.out.println("WARN : unhandled quit state exception : " + e.getMessage());
        }
        System.out.println("INFO : " + clientState.getClientID() + " Thread terminated");
    }

}