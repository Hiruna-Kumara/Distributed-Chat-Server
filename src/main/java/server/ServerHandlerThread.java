package server;

import client.ClientHandlerThread;
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

public class ServerHandlerThread extends Thread {

    private final ServerSocket serverCoordinationSocket;

    public ServerHandlerThread( ServerSocket serverCoordinationSocket) {
        this.serverCoordinationSocket = serverCoordinationSocket;
    }

    @Override
    public void run() {
        try {
            while( true ) {
                Socket serverSocket = serverCoordinationSocket.accept();

                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader( serverSocket.getInputStream(), StandardCharsets.UTF_8 )
                );
                String jsonStringFromServer = bufferedReader.readLine();

                // convert received message to json object
                JSONObject j_object = MessageTransfer.convertToJson( jsonStringFromServer );

                if( MessageTransfer.hasKey( j_object, "option" ) ) {
                    // messages with 'option' tag will be handled inside BullyAlgorithm
                    BullyAlgorithm.receiveMessages( j_object );
                }
                else if (MessageTransfer.hasKey( j_object, "type" )) {
                    if (j_object.get("type").equals("clientidapprovalrequest")
                            && j_object.get("clientid") != null && j_object.get( "sender" ) != null) {

                        // process client ID approval request received by leader
                        String clientID = j_object.get("clientid").toString();
                        int sender = Integer.parseInt(j_object.get("sender").toString());
                        String threadID = j_object.get("threadid").toString();

                        boolean approved = !LeaderState.getInstance().isClientIDAlreadyTaken( clientID );
                        if( approved ) {
                            LeaderState.getInstance().addApprovedClient( clientID, sender );
                        }
                        Server destServer = ServerState.getInstance().getServers()
                                .get( sender );
                        try {
                            // send client id approval reply to sender
                            MessageTransfer.send(
                                    ServerMessage.getClientIdApprovalReply( String.valueOf(approved), threadID ),
                                    destServer
                            );
                            System.out.println("INFO : Client ID '"+ clientID + "' from s" + sender + " is " + (approved ? "":"not") + " approved");
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    } else if ( j_object.get("type").equals("clientidapprovalreply")
                            && j_object.get("approved") != null && j_object.get( "threadid" ) != null){

                        // process client ID approval request reply received by non leader
                        int approved = Boolean.parseBoolean( j_object.get("approved").toString() ) ? 1 : 0;
                        Long threadID = Long.parseLong( j_object.get("threadid").toString() );

                        ClientHandlerThread clientHandlerThread = ServerState.getInstance()
                                .getClientHandlerThread( threadID );
                        clientHandlerThread.setApproved( approved );
                    }
                    else {
                        System.out.println( "WARN : Command error, Corrupted JSON from Server" );
                    }
                }
                else {
                    System.out.println( "WARN : Command error, Corrupted JSON from Server" );
                }
                serverSocket.close();
            }
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
    }
}