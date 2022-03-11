package server;

import client.ClientHandlerThread;
import client.ClientState;
import consensus.BullyAlgorithm;
import consensus.LeaderState;
import messaging.MessageTransfer;
import org.json.simple.JSONObject;
import messaging.ServerMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.*;

public class ServerHandlerThread extends Thread {

    private final ServerSocket serverCoordinationSocket;

    public ServerHandlerThread(ServerSocket serverCoordinationSocket) {
        this.serverCoordinationSocket = serverCoordinationSocket;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket serverSocket = serverCoordinationSocket.accept();

                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(serverSocket.getInputStream(), StandardCharsets.UTF_8)
                );
                String jsonStringFromServer = bufferedReader.readLine();

                // convert received message to json object
                JSONObject j_object = MessageTransfer.convertToJson(jsonStringFromServer);
                int index = 0;

                if (j_object.containsKey("leader") && j_object.containsKey("coordinatoor")) {
                    index = Integer.parseInt(j_object.get("leader").toString());
                    SharedAttributes.setNeighbourIndex(index);
                } else if (j_object.containsKey("sender")){
                    index = Integer.parseInt(j_object.get("sender").toString());
                    SharedAttributes.setNeighbourIndex(index);
                }

                if (MessageTransfer.hasKey( j_object, "room")) {
                    String rooms =  (String) j_object.get("room");
                    SharedAttributes sharedAttributes = new SharedAttributes();
                    sharedAttributes.setRoom(rooms);
                }

                if (MessageTransfer.hasKey( j_object, "delete-room")) {
                    String deletedRoom = (String) j_object.get("delete-room");
                    SharedAttributes sharedAttributes = new SharedAttributes();
                    sharedAttributes.removeRoomFromGlobalRoomList(deletedRoom);
                }

                if (MessageTransfer.hasKey(j_object, "option")) {
                    // messages with 'option' tag will be handled inside BullyAlgorithm
                    BullyAlgorithm.receiveMessages(j_object);
                } else if (MessageTransfer.hasKey(j_object, "type")) {
                    if (j_object.get("type").equals("clientidapprovalrequest")
                            && j_object.get("clientid") != null && j_object.get("sender") != null && j_object.get("threadid") != null) {

                        // leader processes client ID approval request received
                        String clientID = j_object.get("clientid").toString();
                        int sender = Integer.parseInt(j_object.get("sender").toString());
                        String threadID = j_object.get("threadid").toString();

                        boolean approved = !LeaderState.getInstance().isClientIDAlreadyTaken(clientID);
                        if (approved) {
                            LeaderState.getInstance().addApprovedClient(clientID, sender);
                        }
                        Server destServer = ServerState.getInstance().getServers()
                                .get(sender);
                        try {
                            // send client id approval reply to sender
                            MessageTransfer.sendServer(
                                    ServerMessage.getClientIdApprovalReply(String.valueOf(approved), threadID),
                                    destServer
                            );
                            System.out.println("INFO : Client ID '" + clientID +"' from s" + sender + " is" + (approved ? " ":" not ") + "approved");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (j_object.get("type").equals("clientidapprovalreply")
                            && j_object.get("approved") != null && j_object.get("threadid") != null) {

                        // non leader processes client ID approval request reply received
                        int approved = Boolean.parseBoolean( j_object.get("approved").toString() ) ? 1 : 0;
                        Long threadID = Long.parseLong( j_object.get("threadid").toString() );

                        ClientHandlerThread clientHandlerThread = ServerState.getInstance()
                                .getClientHandlerThread( threadID );
                        clientHandlerThread.setApprovedClientID( approved );
                        Object lock = clientHandlerThread.getLock();
                        synchronized( lock ) {
                            lock.notify();
                        }
                    } else if (j_object.get("type").equals("joinroomapprovalrequest")){

                        // leader processes join room approval request received

                        //get params
                        String clientID = j_object.get("clientid").toString();
                        String roomID = j_object.get("roomid").toString();
                        String formerRoomID = j_object.get("former").toString();
                        int sender = Integer.parseInt(j_object.get("sender").toString());
                        String threadID = j_object.get("threadid").toString();
                        boolean isLocalRoomChange = Boolean.parseBoolean(j_object.get("isLocalRoomChange").toString());

                        if (isLocalRoomChange) {
                            //local change
                            LeaderState.getInstance().removeJoinReqApprovedClientFromRoom(clientID, formerRoomID, sender);
                        } else {
                            int serverIDofTargetRoom = LeaderState.getInstance().getServerIdIfRoomExist(roomID);

                            if (serverIDofTargetRoom != -1) {
                                LeaderState.getInstance().removeJoinReqApprovedClientFromRoom(clientID, formerRoomID, sender);
                            }
                            Server destServer = ServerState.getInstance().getServers().get(sender);
                            try {

                                Server serverOfTargetRoom = ServerState.getInstance().getServers().get(serverIDofTargetRoom);

                                String host;
                                String port;
                                if (serverOfTargetRoom != null) {
                                    host = serverOfTargetRoom.getServerAddress();
                                    port = String.valueOf(serverOfTargetRoom.getClientsPort());
                                } else {
                                    host = "_";
                                    port = "_";
                                }

                                MessageTransfer.sendServer(
                                        ServerMessage.getJoinRoomApprovalReply(
                                                String.valueOf(serverIDofTargetRoom),
                                                threadID,
                                                host,
                                                port),
                                        destServer
                                );
                                System.out.println("INFO : Join Room from [" + formerRoomID +
                                        "] to [" + roomID + "] for client " + clientID +
                                        " is" + (serverIDofTargetRoom != -1 ? " " : " not ") + "approved");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if ( j_object.get("type").equals("joinroomapprovalreply") ) {

                        // non leader processes room create approval request reply received
                        int approved = Boolean.parseBoolean(j_object.get("approved").toString()) ? 1 : 0;
                        Long threadID = Long.parseLong(j_object.get("threadid").toString());
                        String host = j_object.get("host").toString();
                        String port = j_object.get("port").toString();

                        ClientHandlerThread clientHandlerThread = ServerState.getInstance()
                                .getClientHandlerThread(threadID);
                        clientHandlerThread.setApprovedJoinRoom(approved);
                        clientHandlerThread.setApprovedJoinRoomServerHostAddress(host);
                        clientHandlerThread.setApprovedJoinRoomServerPort(port);
                        //TODO check if lock required
                        //Object lock = clientHandlerThread.getLock();
                        //synchronized( lock ) {
                        //    lock.notify();
                        //}
                    }else if(j_object.get("type").equals("movejoinack")) {
                        //leader process move join acknowledgement from the target room server after change

                        //parse params
                        String clientID = j_object.get("clientid").toString();
                        String roomID = j_object.get("roomid").toString();
                        String formerRoomID = j_object.get("former").toString();
                        int sender = Integer.parseInt(j_object.get("sender").toString());
                        String threadID = j_object.get("threadid").toString();

                        ClientState client = new ClientState(clientID,roomID,null);
                        LeaderState.getInstance().addApprovedClient(clientID, sender);
                        LeaderState.getInstance().addClientToRoomID(client,roomID);

                        System.out.println("INFO : Moved Client ["+clientID+"] to server s"+sender
                                +" and room ["+roomID+"] is updated as current room");

                    } else {
                        System.out.println("WARN : Command error, Corrupted JSON from Server");
                    }
                } else {
                    System.out.println("WARN : Command error, Corrupted JSON from Server");
                }
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}