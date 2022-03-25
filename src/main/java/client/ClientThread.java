package Client;

import MessagePassing.MessagePassing;
import Server.Server;
import Server.ServerInfo;
import Server.ServerMessage;
import Server.Room;
import consensus.Leader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ClientThread implements Runnable {

    private static final Logger LOG = LogManager.getLogger(ClientThread.class);

    private final Socket clientSocket;
    private String serverID;
    private Client client;
    private int isClientApproved = -1;
    private int isJoinRoomApproved = -1;

    private String approvedJoinRoomServerHostAddress;
    private String approvedJoinRoomServerPort;

    private boolean quit = false;

    private int isRoomCreationApproved = -1;

    private List<String> tempRoomList;

    public ClientThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.serverID = Server.getInstance().getServerID();
    }

    public void setIsClientApproved(int isClientApproved) {
        this.isClientApproved = isClientApproved;
    }

    public void setTempRoomList(List<String> tempRoomList) {
        this.tempRoomList = tempRoomList;
    }

    public void setIsRoomApproved(int isRoomApproved) { this.isRoomCreationApproved = isRoomApproved; }

    public void setIsJoinRoomApproved(int isJoinRoomApproved) {
        this.isJoinRoomApproved = isJoinRoomApproved;
    }

    public void setJoinRoomServerHostAddress(String approvedJoinRoomServerHostAddress) {
        this.approvedJoinRoomServerHostAddress = approvedJoinRoomServerHostAddress;
    }

    public void setJoinRoomServerPort(String approvedJoinRoomServerPort) {
        this.approvedJoinRoomServerPort = approvedJoinRoomServerPort;
    }

    private void newIdentity(String identity) throws InterruptedException, IOException {
        // TODO - implement adding a new client
        if ((Character.toString(identity.charAt(0)).matches("[a-zA-Z]+")
                && identity.matches("[a-zA-Z0-9]+") && identity.length() >= 3 && identity.length() <= 16)) {
            while (!Server.getInstance().getLeaderUpdateComplete()) {
                Thread.sleep(1000);
            }
            /////////////// JUST A TEST - MUST REMOVE/////////////////////////////////
            System.out.println(Leader.getInstance().getLeaderID());
            LOG.info(Leader.getInstance().getLeaderID());
            ConcurrentHashMap<String, List<Room>> globalRoomList = Leader.getInstance().getGlobalRoomList(); // server_id,
                                                                                                             // cliient_id
                                                                                                             // list
            ConcurrentHashMap<String, List<String>> globalClientList = Leader.getInstance().getGlobalClientList(); // server_id,
                                                                                                                   // cliient_id
                                                                                                                   // list

            for (String key : globalRoomList.keySet()) {
                List<Room> r_list = globalRoomList.get(key);
                for (Room r : r_list) {
                    System.out.println(key);
                    System.out.println(r.getRoomID());
                    LOG.info(key);
                    LOG.info(r.getRoomID());
                }
            }
            for (String key : globalClientList.keySet()) {
                List<String> c_list = globalClientList.get(key);
                for (String c : c_list) {
                    System.out.println(key);
                    System.out.println(c);
                    LOG.info(key);
                    LOG.info(c);
                }
            }

            System.out.println(Server.getInstance().getLeaderUpdateComplete());
            LOG.info(Server.getInstance().getLeaderUpdateComplete());
            ////////////////////////////////////////////////////////////////////////////
            if (Objects.equals(Server.getInstance().getServerID(), Leader.getInstance().getLeaderID())) {
                boolean clientIDTaken = Leader.getInstance().isClientIDTaken(identity);
                isClientApproved = clientIDTaken ? 0 : 1;
            } else {
                MessagePassing.sendToLeader(
                        ServerMessage.clientIdApprovalRequest(identity, Server.getInstance().getServerID(),
                                String.valueOf(Thread.currentThread().getId())));
                synchronized (this) {
                    while (isClientApproved == -1) {
                        this.wait(7000);
                    }
                }
            }
            // if client is approved
            if (isClientApproved == 1) {
                String mainHallID = Server.getInstance().getMainHallID(Server.getInstance().getServerID());
                this.client = new Client(identity, mainHallID, clientSocket);
                // If I am the leader update the global list.
                if (Objects.equals(Server.getInstance().getServerID(), Leader.getInstance().getLeaderID())) {
                    Leader.getInstance().addToGlobalClientAndRoomList(identity, Server.getInstance().getServerID(),
                            mainHallID);
                }
                // add client to mainhall
                Server.getInstance().getRoomList().get(mainHallID).addClient(client);
                // broadcast to all the clients in mainhall
                HashMap<String, Client> mainHallClientList = Server.getInstance().getRoomList().get(mainHallID)
                        .getClientList();
                ArrayList<Socket> socketList = new ArrayList<>();
                for (String clientID : mainHallClientList.keySet()) {
                    socketList.add(mainHallClientList.get(clientID).getSocket());
                }

                synchronized (clientSocket) {
                    MessagePassing.sendClient(ClientMessage.newIdentityReply("true"), clientSocket);
                    MessagePassing.sendBroadcast(ClientMessage.roomChangeReply(identity, "", mainHallID), socketList);
                }
            }
            // if not approved notify client
            else if (isClientApproved == 0) {
                synchronized (clientSocket) {
                    MessagePassing.sendClient(ClientMessage.newIdentityReply("false"), clientSocket);
                }
            }
            isClientApproved = -1;
        }
        // if client id format does not match notify user
        else {
            synchronized (clientSocket) {
                MessagePassing.sendClient(ClientMessage.newIdentityReply("false"), clientSocket);
            }
        }

    }

    private void list() throws IOException, InterruptedException {

        tempRoomList = null;

        while (!Server.getInstance().getLeaderUpdateComplete()) {
            Thread.sleep(1000);
        }

        if (Objects.equals(Leader.getInstance().getLeaderID(), Server.getInstance().getServerID())) {
            tempRoomList = Leader.getInstance().getRoomIDList();

        } else {
            MessagePassing.sendToLeader(
                    ServerMessage.listRequest(
                            client.getClientID(),
                            String.valueOf(Thread.currentThread().getId()),
                            Server.getInstance().getServerID())

            );

            synchronized (this) {
                while (tempRoomList == null) {
                    this.wait(7000);
                }
            }

        }

        if (tempRoomList != null) {
            System.out.println("INFO : Recieved rooms in the system :" + tempRoomList);
            LOG.info("INFO : Recieved rooms in the system :" + tempRoomList);
            MessagePassing.sendClient(
                    ClientMessage.listReply(tempRoomList),
                    clientSocket);
        }
    }

    private void who() throws IOException {

        String roomID = client.getRoomID();
        Room room = Server.getInstance().getRoomList().get(roomID);

        HashMap<String, Client> clientList = room.getClientList();
        List<String> participantsList = new ArrayList<>(clientList.keySet());
        String ownerClientID = room.getOwnerClientID();

        System.out.println("LOG  : participants in room [" + roomID + "] : " + participantsList);
        LOG.info("LOG  : participants in room [" + roomID + "] : " + participantsList);
        MessagePassing.sendClient(
                ClientMessage.whoReply(
                        roomID,
                        participantsList,
                        ownerClientID),
                clientSocket);
    }

    private void createRoom(String roomid) throws InterruptedException, IOException {
        // TODO - implement creating a new chatroom
        if ((Character.toString(roomid.charAt(0)).matches("[a-zA-Z]+")
                && roomid.matches("[a-zA-Z0-9]+") && roomid.length() >= 3 && roomid.length() <= 16) && !client.isRoomOwner()) {
            while (!Server.getInstance().getLeaderUpdateComplete()) {
                Thread.sleep(1000);
            }

            if (Objects.equals(Leader.getInstance().getLeaderID(), Server.getInstance().getServerID())) {
                boolean roomIDTaken = Leader.getInstance().isRoomIDTaken(roomid);
                isRoomCreationApproved = roomIDTaken ? 0:1;
                System.out.println("INFO : Room '" + roomid +
                        "' creation request from client " + client.getClientID() +
                        " is" + (roomIDTaken ? "not" : " ") + "approved");
                LOG.info("INFO : Room '" + roomid +
                "' creation request from client " + client.getClientID() +
                " is" + (roomIDTaken ? "not" : " ") + "approved");

            }else{
                try{
                    MessagePassing.sendToLeader(ServerMessage.roomCreateApprovalRequest(
                        client.getClientID(),
                        client.getRoomID(),
                        roomid, 
                        Server.getInstance().getServerID(),
                        String.valueOf(Thread.currentThread().getId())));
                }catch(Exception e){
                    e.printStackTrace();
                }
                synchronized(this){
                    while(isRoomCreationApproved==-1){
                        this.wait(7000);
                    }

                }
            }
            if(isRoomCreationApproved==1){
                System.out.println( "INFO : Received correct room ID :" + roomid );
                LOG.info("INFO : Received correct room ID :" + roomid);

                String formerRoomID = client.getRoomID();

                HashMap<String,Client> clientList = Server.getInstance().getRoomList().get( formerRoomID ).getClientList();

                ArrayList<Socket> formerSocket = new ArrayList<>();
                for(String each_client: clientList.keySet()){
                    formerSocket.add(clientList.get(each_client).getSocket());
                }

                //update server state
                Server.getInstance().getRoomList().get(formerRoomID).removeClient(client.getClientID());

                Room newRoom = new Room(roomid, Server.getInstance().getServerID(), client.getClientID());
                Server.getInstance().getRoomList().put(roomid, newRoom);

                client.setRoomID(roomid);
                client.setRoomOwner(true);
                newRoom.addClient(client);

                if (Objects.equals(Leader.getInstance().getLeaderID(), Server.getInstance().getServerID())) {
                    Leader.getInstance().addToRoomList(
                        client.getClientID(),
                        Server.getInstance().getServerID(),
                        roomid,
                        formerRoomID
                    );
                }

                synchronized(clientSocket){
                    MessagePassing.sendClient(ClientMessage.createRoomReply(   
                        roomid,
                        "true"
                    ), clientSocket);

                    MessagePassing.sendBroadcast(ClientMessage.roomChangeReply(
                        client.getClientID(),
                        formerRoomID,
                        roomid
                    ), formerSocket);
                }

            }else if(isRoomCreationApproved==0){
                System.out.println("WARN : Room id [" + roomid + "] already in use");
                LOG.warn("Room id [" + roomid + "] already in use");
                synchronized (clientSocket) {
                    MessagePassing.sendClient(ClientMessage.createRoomReply(
                            roomid,
                            "false"
                    ), clientSocket);
                }
            }
            isRoomCreationApproved = -1;
        }else{
            System.out.println("WARN : Received wrong room ID type or client already owns a room [" + roomid + "]");
            LOG.warn("Received wrong room ID type or client already owns a room [" + roomid + "]");
            synchronized (clientSocket) {
                MessagePassing.sendClient(ClientMessage.createRoomReply(
                        roomid,
                        "false"
                ), clientSocket);
            }
        }
    }

    private void deleteRoom(String roomID) throws IOException, InterruptedException {
//        TODO - implement delete room
        String mainHallID = Server.getInstance().getMainHallID(serverID);
        boolean roomExists = Server.getInstance().getRoomList().containsKey(roomID);
        if(roomExists){
            //check sync
            String serverID = Server.getInstance().getServerID();
            Room room = Server.getInstance().getRoomList().get(roomID);
            if(room.getOwnerClientID().equals(client.getClientID())){
                while(!Server.getInstance().getLeaderUpdateComplete()) {
                    Thread.sleep(1000);
                }

                if(Objects.equals(Leader.getInstance().getLeaderID(), Server.getInstance().getServerID())){
                    Leader.getInstance().removeRoom(serverID, roomID, mainHallID, client.getClientID());
                }else{
                    //update leader server
                    MessagePassing.sendToLeader(ServerMessage.deleteRoomRequest(serverID, client.getClientID(), roomID, mainHallID));
                }
                System.out.println("INFO : room [" + roomID + "] was deleted by : " + client.getClientID());
                LOG.info("room [" + roomID + "] was deleted by : " + client.getClientID());

                HashMap<String, Client> formerClients = Server.getInstance().getRoomList().get(roomID).getClientList();
                HashMap<String, Client> mainHallClients = Server.getInstance().getRoomList().get(mainHallID).getClientList();

                //add clients in deleted room to main hall
                mainHallClients.putAll(formerClients);

                ArrayList<Socket> socketList = new ArrayList<>();
                for(String each_mainHallClient: mainHallClients.keySet()){
                    socketList.add(mainHallClients.get(each_mainHallClient).getSocket());
                }

                Server.getInstance().getRoomList().remove(roomID);
                client.setRoomOwner(false);

                for(String each_formerClient: formerClients.keySet()){
                    String clientID = formerClients.get(each_formerClient).getClientID();
                    formerClients.get(each_formerClient).setRoomID(mainHallID);
                    Server.getInstance().getRoomList().get(mainHallID).addClient(formerClients.get(each_formerClient));

                    MessagePassing.sendBroadcast(ClientMessage.roomChangeReply(clientID, roomID, mainHallID), socketList);
                }
                MessagePassing.sendClient(ClientMessage.deleteRoomReply(roomID, "true"), clientSocket);
            }else{
                MessagePassing.sendClient(ClientMessage.deleteRoomReply(roomID, "false"), clientSocket);
                System.out.println("WARN : Requesting client [" + client.getClientID() + "] does not own the room ID [" + roomID + "]");
                LOG.warn("Requesting client [" + client.getClientID() + "] does not own the room ID [" + roomID + "]");
            }
        }else{
            MessagePassing.sendClient(ClientMessage.deleteRoomReply(roomID, "false"), clientSocket);
            System.out.println("WARN : Received room ID [" + roomID + "] does not exist");
            LOG.warn("WARN : Received room ID [" + roomID + "] does not exist");
        }
    }

    private void message(String content) throws IOException {
        // TODO - implement broadcasting the message to clients in the chatroom
        String clientID = client.getClientID();
        String roomID = client.getRoomID();

        HashMap<String, Client> clientList = Server.getInstance().getRoomList().get(roomID).getClientList();

        ArrayList<Socket> socketsList = new ArrayList<>();
        for (String client : clientList.keySet()) {
            if (!clientList.get(client).getClientID().equals(clientID)) {
                socketsList.add(clientList.get(client).getSocket());
            }
        }
        MessagePassing.sendBroadcast(ClientMessage.broadcastMessage(clientID, content), socketsList);
    }

    private void quit() throws IOException {
        // TODO - Quiting the server
        if (client.isRoomOwner()) {
            try {
                deleteRoom(client.getRoomID());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("INFO : Deleted room before " + client.getClientID() + " quit");
            LOG.info("Deleted room before " + client.getClientID() + " quit");
        }

        HashMap<String, Client> formerClientList = Server.getInstance().getRoomList().get(client.getRoomID())
                .getClientList();

        ArrayList<Socket> socketList = new ArrayList<>();
        for (String each_client : formerClientList.keySet()) {
            socketList.add(formerClientList.get(each_client).getSocket());
        }
        MessagePassing.sendBroadcast(ClientMessage.roomChangeReply(client.getClientID(), client.getRoomID(), ""),
                    socketList);

        // update global list - Leader class
        // send quit message to leader if itself is not leader
        if (!Leader.getInstance().getLeaderID().equals(Server.getInstance().getServerID())) {
            MessagePassing.sendToLeader(ServerMessage.quitMessage(client.getClientID(), client.getRoomID(),
                    Server.getInstance().getServerID()));
        } else {
            Leader.getInstance().removeFromGlobalClientAndRoomList(client.getClientID(),
                        Server.getInstance().getServerID(), client.getRoomID());
        }

        // update local server
        Server.getInstance().removeClient(client.getClientID(), client.getRoomID(), Thread.currentThread().getId());

        if (!clientSocket.isClosed()) {
            clientSocket.close();
        }
        quit = true;
        System.out.println("INFO : " + client.getClientID() + " quit");
        LOG.info(client.getClientID() + " quit");
    }

    private void joinRoom(String roomID) throws IOException, InterruptedException {

        String formerRoomID = client.getRoomID();

        if(client.isRoomOwner()){
            System.out.println("WARN : Join room denied, Client" + client.getClientID() + " Owns a room");
            LOG.warn("Join room denied, Client" + client.getClientID() + " Owns a room");

            MessagePassing.sendClient(
                ClientMessage.roomChangeReply(
                    client.getClientID(),
                    formerRoomID,
                    formerRoomID
                ), 
                clientSocket);

        }else if(Server.getInstance().getRoomList().containsKey(roomID)){ //local room change

            // if self is leader update leader state directly
            if(Server.getInstance().getServerID().equals(Leader.getInstance().getLeaderID())){
                Leader.getInstance().InServerJoinRoomClient(client.getClientID(), serverID, formerRoomID, roomID);

            }else {
                MessagePassing.sendToLeader(
                        ServerMessage.joinRoomRequest(
                                client.getClientID(),
                                serverID,
                                formerRoomID,
                                roomID,
                                String.valueOf(Thread.currentThread().getId()),
                                String.valueOf(true)
                        ));
            }

            client.setRoomID(roomID);
            Server.getInstance().getRoomList().get(formerRoomID).removeClient(client.getClientID());
            Server.getInstance().getRoomList().get(roomID).addClient(client);

            System.out.println("INFO : client [" + client.getClientID() + "] joined room :" + roomID);
            LOG.info("client [" + client.getClientID() + "] joined room :" + roomID);

            // creating broadcast list
            HashMap<String, Client> newClientList = Server.getInstance().getRoomList().get(roomID).getClientList();
            HashMap<String, Client> oldClientList = Server.getInstance().getRoomList().get(formerRoomID).getClientList();
            HashMap<String, Client> clientList = new HashMap<>();
            clientList.putAll(oldClientList);
            clientList.putAll(newClientList);

            ArrayList<Socket> sockets = new ArrayList<>();
            for (String each : clientList.keySet()) {
                sockets.add(clientList.get(each).getSocket());
            }
            
            MessagePassing.sendBroadcast(
                ClientMessage.roomChangeReply(
                    client.getClientID(),
                    formerRoomID,
                        roomID),
                    sockets);

            while(!Server.getInstance().getLeaderUpdateComplete()) {
                Thread.sleep(1000);
            }

        } else {  // global room change

            while(!Server.getInstance().getLeaderUpdateComplete()) {
                Thread.sleep(1000);
            }

            isJoinRoomApproved = -1;

            //check if room id exist and if init route
            if(Leader.getInstance().getLeaderID().equals(Server.getInstance().getServerID())){
                String serverIDofTargetRoom = Leader.getInstance().getServerIdIfRoomExist(roomID);
                isJoinRoomApproved = serverIDofTargetRoom != null ? 1 : 0;

                if(isJoinRoomApproved == 1){
                    Leader.getInstance().removeFromGlobalClientAndRoomList(client.getClientID(), serverID, formerRoomID);//remove before route, later add on move join
                    ServerInfo serverInfoOfTargetRoom = Server.getInstance().getOtherServers().get(serverIDofTargetRoom);
                    approvedJoinRoomServerHostAddress = serverInfoOfTargetRoom.getAddress();
                    approvedJoinRoomServerPort = String.valueOf(serverInfoOfTargetRoom.getClientPort());
                }
                System.out.println("INFO : Received response for route request for join room (Self is Leader)");
                LOG.info("Received response for route request for join room (Self is Leader)");


            } else {
                MessagePassing.sendToLeader(
                    ServerMessage.joinRoomRequest(
                        client.getClientID(),
                        serverID,
                        formerRoomID,
                        roomID,
                        String.valueOf(Thread.currentThread().getId()),
                        String.valueOf(false)
                    ));


                synchronized(this){
                    while (isJoinRoomApproved == -1) {
                        System.out.println("INFO : Wait until server approve route on Join room request");
                        LOG.info("Wait until server approve route on Join room request");
                        this.wait(7000);
                        //wait for response
                    }
                }
                
                System.out.println("INFO : Received response for route request for join room");
                LOG.info("Received response for route request for join room");
            }

            if(isJoinRoomApproved == 1){
                //broadcast to former room
                Server.getInstance().removeClient(client.getClientID(), formerRoomID, Thread.currentThread().getId());
                System.out.println("INFO : client [" + client.getClientID() + "] left room :" + formerRoomID);
                LOG.info("client [" + client.getClientID() + "] left room :" + formerRoomID);
            
                //create broadcast list
                HashMap<String, Client> clientListOld = Server.getInstance().getRoomList().get(formerRoomID).getClientList();
                System.out.println("INFO : Send broadcast to former room in local server");
                LOG.info("Send broadcast to former room in local server");

                ArrayList<Socket> sockets = new ArrayList<>();
                for (String each : clientListOld.keySet()) {
                    sockets.add(clientListOld.get(each).getSocket());
                }

                MessagePassing.sendBroadcast(
                    ClientMessage.roomChangeReply(
                        client.getClientID(),
                        formerRoomID,
                            roomID)
                    , sockets);
    
                //server change : route
                MessagePassing.sendClient(
                    ClientMessage.routeReply(
                            roomID,
                        approvedJoinRoomServerHostAddress,
                        approvedJoinRoomServerPort)
                    , clientSocket);
    
                System.out.println("INFO : Route Message Sent to Client");
                LOG.info("Route Message Sent to Client");
                quit = true;
            
            } else if(isJoinRoomApproved ==0) { // Room not found on system

                System.out.println("WARN : Received room ID ["+ roomID + "] does not exist");
                LOG.warn("Received room ID ["+ roomID + "] does not exist");;

                MessagePassing.sendClient(
                    ClientMessage.roomChangeReply(
                        client.getClientID(),
                        formerRoomID, //same
                        formerRoomID) //same
                    , clientSocket);
                
                isJoinRoomApproved = -1;
            }
        }
    }

    private void moveJoin(String formerRoomID, String roomID, String clientID) throws IOException, InterruptedException {
        // TODO - implement joining a room in another server
        if(!Server.getInstance().getRoomList().containsKey(roomID)){
            roomID = Server.getInstance().getMainHallID(Server.getInstance().getServerID());
        }
        while (!Server.getInstance().getLeaderUpdateComplete()) {
            Thread.sleep(1000);
        }

        //if self is leader update leader state directly
        if (Objects.equals(Leader.getInstance().getLeaderID(), Server.getInstance().getServerID())) {
            Leader.getInstance().addToGlobalClientAndRoomList(clientID, Server.getInstance().getServerID(), roomID);
        } else {
            //update leader server
            MessagePassing.sendToLeader(
                    ServerMessage.moveJoinRequest(
                            clientID,
                            roomID,
                            formerRoomID,
                            Server.getInstance().getServerID(),
                            String.valueOf(Thread.currentThread().getId())
                    )
            );
        }

        this.client = new Client(clientID, roomID, clientSocket);
        Server.getInstance().getRoomList().get(roomID).addClient(client);
        HashMap<String, Client> newClientList = Server.getInstance().getRoomList().get(roomID).getClientList();

        ArrayList<Socket> sockets = new ArrayList<>();
        for (String each : newClientList.keySet()) {
            sockets.add(newClientList.get(each).getSocket());
        }

        MessagePassing.sendClient(ClientMessage.serverChangeReply("true", "s"+Server.getInstance().getServerID()), clientSocket);
        MessagePassing.sendBroadcast(ClientMessage.roomChangeReply(clientID, formerRoomID, roomID), sockets);
    }


    @Override
    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            JSONParser jsonParser = new JSONParser();
            while (!quit) {
                try {
                    String string = bufferedReader.readLine();
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(string);
                    String type = null;
                    if (jsonObject != null && jsonObject.get("type") != null) {
                        type = (String) jsonObject.get("type");
                    } else {
                        // TODO - Add output string
                    }
                    if (Objects.equals(type, "newidentity") && jsonObject.get("identity") != null) {
                        newIdentity((String) jsonObject.get("identity"));
                    } else if (Objects.equals(type, "list")) {
                        list();
                    } else if (Objects.equals(type, "who")) {
                        who();
                    } else if (Objects.equals(type, "createroom") && jsonObject.get("roomid") != null) {
                        createRoom((String) jsonObject.get("roomid"));
                    } else if (Objects.equals(type, "joinroom") && jsonObject.get("roomid") != null) {
                        joinRoom((String) jsonObject.get("roomid"));
                    } else if (Objects.equals(type, "movejoin") && jsonObject.get("former") != null
                            && jsonObject.get("roomid") != null && jsonObject.get("identity") != null) {
                        String formerRoomID = (String) jsonObject.get("former");
                        String newRoomID = (String) jsonObject.get("roomid");
                        String clientID = (String) jsonObject.get("identity");
                        moveJoin(formerRoomID, newRoomID, clientID);
                    } else if (Objects.equals(type, "deleteroom") && jsonObject.get("roomid") != null) {
                        deleteRoom((String) jsonObject.get("roomid"));
                    } else if (Objects.equals(type, "message") && jsonObject.get("content") != null) {
                        message((String) jsonObject.get("content"));
                    } else if (Objects.equals(type, "quit")) {
                        quit();
                    }
                } catch (NullPointerException e) {
                    break;
                }
            }
        } catch (IOException | ParseException | InterruptedException e) {
            // TODO - Add output string
            // e.printStackTrace();
            System.out.println("WARN : client abruptly disconnected");
            LOG.warn("client abruptly disconnected");
        }
    }

}
