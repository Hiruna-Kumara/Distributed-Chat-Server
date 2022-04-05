package Server;

import Client.ClientThread;
import MessagePassing.MessagePassing;
import consensus.Leader;
import consensus.election.FastBullyAlgorithm;
import heartbeat.ConsensusJob;
import heartbeat.GossipJob;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerThread implements Runnable{

    private static final Logger LOG = LogManager.getLogger(ServerThread.class);

    private final ServerSocket serverSocket;
//    private LeaderStateUpdate leaderStateUpdate = new LeaderStateUpdate();

    public ServerThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    private void sendToLeaderUpdate(String serverID, JSONArray clientIDListJson, JSONArray chatRoomsListJson) {
        List<String> clientIDList = new ArrayList<>();
        List<Room> roomList = new ArrayList<>();

        for( Object clientID : clientIDListJson ) {
            clientIDList.add( clientID.toString() );
        }

        for( Object chatRoom : chatRoomsListJson ) {
            JSONObject j_room = (JSONObject)chatRoom;
            roomList.add ( new Room(j_room.get("roomID").toString(),
                    j_room.get("serverID").toString(), j_room.get("clientID").toString()) );
        }
        Leader.getInstance().handleRequest(serverID,clientIDList,roomList);
    }

    @Override
    public void run() {
        try{
            while(true){
                Socket serverSocket = this.serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), StandardCharsets.UTF_8));
                JSONParser jsonParser = new JSONParser();
                String string = bufferedReader.readLine();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(string);
                if (jsonObject != null && jsonObject.get("option") != null) {
                    FastBullyAlgorithm.receiveMessage(jsonObject);
                }
                else if (jsonObject != null && jsonObject.get("type") != null) {
                    String type = (String) jsonObject.get("type");
                    if(Objects.equals(type, "leaderupdate")){
                        String serverID = (String) jsonObject.get("serverID");
                        JSONArray clientIDListJson = ( JSONArray ) jsonObject.get( "clients" );
                        JSONArray chatRoomsListJson = ( JSONArray ) jsonObject.get( "chatrooms" );
                        //System.out.println(chatRoomsList);
                        sendToLeaderUpdate(serverID, clientIDListJson, chatRoomsListJson);
                    }
                    else if(Objects.equals(type, "leaderstateupdatecomplete")){
                        String serverID = (String) jsonObject.get("serverID");
                        // System.out.println("INFO : leader server "+serverID+" update done");
                        LOG.info("INFO : leader server "+serverID+" update done");
//                        FastBullyAlgorithm FBA = new FastBullyAlgorithm("");
//                        FBA.stopWaitingForUpdateCompleteMessage();
//                        Thread.sleep(500);
                        Server.getInstance().setLeaderUpdateComplete(true);
                    }
                    else if(Objects.equals(type, "clientidapprovalrequest")){
                        String clientID = jsonObject.get("clientID").toString();
                        String serverID = jsonObject.get("serverID").toString();
                        String threadID = jsonObject.get("threadID").toString();

                        boolean clientIDTaken = Leader.getInstance().isClientIDTaken(clientID);
                        String reply = String.valueOf(clientIDTaken);
                        if(!clientIDTaken){
                            Leader.getInstance().addToGlobalClientAndRoomList(clientID, serverID, Server.getInstance().getMainHallID(serverID));
                        }
                        ServerInfo destServerInfo = Server.getInstance().getOtherServers().get(serverID);
                        MessagePassing.sendServer(ServerMessage.clientIdApprovalReply(reply,threadID), destServerInfo);
                    }
                    else if(Objects.equals(type, "clientidapprovalreply")){
                        int IsClientApproved = Boolean.parseBoolean(jsonObject.get("reply").toString()) ? 0 : 1;
                        Long threadID = Long.parseLong(jsonObject.get("threadID").toString());

                        ClientThread clientThread = Server.getInstance().getClientHandlerThread(threadID);
                        clientThread.setIsClientApproved(IsClientApproved);
                        synchronized (clientThread) {
                            clientThread.notifyAll();
                        }
                    }
                    else if(Objects.equals(type, "listrequest")){
                        String clientID = jsonObject.get("clientID").toString();
                        String threadID = jsonObject.get("threadID").toString();
                        String serverID = jsonObject.get("serverID").toString();

                        ServerInfo destServerInfo = Server.getInstance().getOtherServers().get(serverID);

                        MessagePassing.sendServer(
                                ServerMessage.listResponse(Leader.getInstance().getRoomIDList(), threadID),
                                destServerInfo
                        );
                    }
                    else if(Objects.equals(type, "listresponse")){
                        Long threadID = Long.parseLong(jsonObject.get("threadID").toString());
                        List<String> rooms = new ArrayList((JSONArray) jsonObject.get("rooms"));

                        ClientThread clientThread = Server.getInstance().getClientHandlerThread(threadID);
                        synchronized (clientThread) {
                            clientThread.setTempRoomList(rooms);
                            clientThread.notifyAll();
                        }
                    }
                    else if(Objects.equals(type, "roomcreateapprovalrequest")){
                        String clientID = jsonObject.get("clientID").toString();
                        String formerRoomID = jsonObject.get("former").toString();
                        String roomID = jsonObject.get("roomID").toString();
                        String serverID = jsonObject.get("serverID").toString();
                        String threadID = jsonObject.get("threadID").toString();

                        boolean roomIDTaken = Leader.getInstance().isRoomIDTaken(roomID);
                        String reply = String.valueOf(roomIDTaken);
                        if(!roomIDTaken){
                            Leader.getInstance().addToRoomList(clientID, serverID, roomID, formerRoomID);
                        }
                        ServerInfo destServerInfo = Server.getInstance().getOtherServers().get(serverID);
                        MessagePassing.sendServer(ServerMessage.roomIdApprovalReply(reply,threadID), destServerInfo);
                    }
                    else if(Objects.equals(type, "roomcreateapprovalreply")){
                        int IsRoomApproved = Boolean.parseBoolean(jsonObject.get("reply").toString()) ? 0 : 1;
                        Long threadID = Long.parseLong(jsonObject.get("threadID").toString());

                        ClientThread clientThread = Server.getInstance().getClientHandlerThread(threadID);
                        clientThread.setIsRoomApproved(IsRoomApproved);
                        synchronized (clientThread) {
                            clientThread.notifyAll();
                        }
                    }
                    else if(Objects.equals(type, "deleterequest")){
                        String serverID = jsonObject.get("serverID").toString();
                        String ownerID = jsonObject.get("ownerID").toString();
                        String roomID = jsonObject.get("roomID").toString();
                        String mainHallID = jsonObject.get("mainHallID").toString();
                        // leader removes client from global room list
                        Leader.getInstance().removeRoom(serverID, roomID, mainHallID, ownerID);
                    }
                    else if(Objects.equals(type, "joinroomapprovalrequest")){
                        String formerServerID = jsonObject.get("formerServer").toString();
                        String formerRoomID = jsonObject.get("formerRoom").toString();
                        String roomID = jsonObject.get("roomID").toString();
                        String clientID = jsonObject.get("clientID").toString();
                        String threadID = jsonObject.get("threadID").toString();
                        boolean inServer = Boolean.parseBoolean(jsonObject.get("inServer").toString());

                        if(inServer){
                            Leader.getInstance().InServerJoinRoomClient(clientID,formerServerID,formerRoomID,roomID);
                        }
                        else{
                            String serverIDofTargetRoom = Leader.getInstance().getServerIdIfRoomExist(roomID);
                            ServerInfo formerServerInfo = Server.getInstance().getOtherServers().get(formerServerID);
                            try {
                                boolean approved = serverIDofTargetRoom != null;
                                ServerInfo serverInfo = null;
                                String host = "";
                                String port = "";
                                if (approved) {
                                    if (!Objects.equals(serverIDofTargetRoom, Server.getInstance().getServerID())) {
                                        serverInfo = Server.getInstance().getOtherServers().get(serverIDofTargetRoom);
                                    }
                                    else{
                                        serverInfo = Server.getInstance().getSelfServerInfo();
                                    }
                                    Leader.getInstance().removeFromGlobalClientAndRoomList(clientID, formerServerID, formerRoomID);//remove before route, later add on move join
                                    host = serverInfo.getAddress();
                                    port = serverInfo.getClientPort().toString();
                                }

                                MessagePassing.sendServer(
                                        ServerMessage.joinRoomApprovalReply(
                                                String.valueOf(approved),
                                                threadID, host, port),
                                        formerServerInfo
                                );
                                // System.out.println("INFO : Join Room from [" + formerRoomID +
                                //         "] to [" + roomID + "] for client " + clientID +
                                //         " is" + (serverIDofTargetRoom != null ? " " : " not ") + "approved");
                                LOG.info("Join Room from [" + formerRoomID +
                                        "] to [" + roomID + "] for client " + clientID +
                                        " is" + (serverIDofTargetRoom != null ? " " : " not ") + "approved");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }else if (Objects.equals(type, "joinroomapprovalreply")) {
                        int approved = Boolean.parseBoolean(jsonObject.get("approved").toString()) ? 1 : 0;
                        Long threadID = Long.parseLong(jsonObject.get("threadid").toString());
                        String host = jsonObject.get("host").toString();
                        String port = jsonObject.get("port").toString();

                        ClientThread clientThread = Server.getInstance()
                                .getClientHandlerThread(threadID);

                        synchronized (clientThread) {
                            clientThread.setIsJoinRoomApproved(approved);
                            clientThread.setJoinRoomServerHostAddress(host);
                            clientThread.setJoinRoomServerPort(port);
                            clientThread.notifyAll();
                        }

                    }
                    else if (Objects.equals(type, "movejoinrequest")) {
                        //leader process move join acknowledgement from the target room server after change

                        //parse params
                        String clientID = jsonObject.get("clientID").toString();
                        String roomID = jsonObject.get("roomID").toString();
                        String serverID = jsonObject.get("serverID").toString();
                        String threadID = jsonObject.get("threadID").toString();

                        Leader.getInstance().addToGlobalClientAndRoomList(clientID, serverID, roomID);

                        // System.out.println("INFO : Moved Client [" + clientID + "] to server s" + serverID
                        //         + " and room [" + roomID + "] is updated as current room");
                        LOG.info("INFO : Moved Client [" + clientID + "] to server s" + serverID
                        + " and room [" + roomID + "] is updated as current room");
                    }
                    else if(Objects.equals(type, "quit")){
                        String clientID = jsonObject.get("clientID").toString();
                        String formerRoomID = jsonObject.get("former").toString();
                        String serverID = jsonObject.get("serverID").toString();
                        // leader removes client from global room list
                        Leader.getInstance().removeFromGlobalClientAndRoomList(clientID, serverID, formerRoomID);
                        // System.out.println("INFO : Client '" + clientID + "' deleted by leader");
                        LOG.info("INFO : Client '" + clientID + "' deleted by leader");
                    }
                    else if (Objects.equals(type, "gossip")) {
                        GossipJob.receiveMessages(jsonObject);
                    }
                    else if (Objects.equals(type, "startVote")) {
                        ConsensusJob.startVoteMessageHandler(jsonObject);
                    } 
                    else if (Objects.equals(type, "answervote")) {
                        ConsensusJob.answerVoteHandler(jsonObject);
                    } 
                    else if (Objects.equals(type, "notifyserverdown")) {
                        ConsensusJob.notifyServerDownMessageHandler(jsonObject);
                    }
                }else {
                    // System.out.println("WARN : Command error, Corrupted JSON from Server");
                    LOG.warn("Command error, Corrupted JSON from Server");
                }
                serverSocket.close();
            }
        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
