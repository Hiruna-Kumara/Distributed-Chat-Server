package consensus;

import messaging.MessageTransfer;
import messaging.ServerMessage;
import org.json.simple.JSONObject;
import server.Server;
import server.ServerState;

import java.io.IOException;

public class BullyAlgorithm implements Runnable{
    String operation;
    String reqType;
    static int sourceID=-1;
    static volatile boolean receivedOk = false;
    static volatile boolean leaderFlag = false;
    static volatile boolean electionInProgress = false;

    public static volatile boolean leaderUpdateComplete = false;

    public BullyAlgorithm( String operation) {
        this.operation = operation;
    }

    public BullyAlgorithm( String operation, String reqType ) {
        this.operation = operation;
        this.reqType = reqType;
    }

    /**
     * The run() method has the required logic for handling the receiver, sender, timer and heartbeat thread.
     * The timer thread waits for 7 seconds to receive a response. If it receives an OK but doesn't receive a leader
     * then it starts an election process again.
     * The receiver thread accepts all the incoming requests.
     */
    public void run() {

        switch( operation )
        {
            case "Timer":
                System.out.println( "INFO : Timer started" );
                try {
                    // wait 7 seconds
                    Thread.sleep( 7000 );
                    if( !receivedOk )
                    {
                        // OK not received. Set self as leader
                        LeaderState.getInstance().setLeaderID( ServerState.getInstance().getSelfID() );
                        electionInProgress = false; // allow another election request to come in
                        leaderFlag = true;
                        System.out.println( "INFO : Server s" + LeaderState.getInstance().getLeaderID()
                                                    + " is selected as leader! " );

                        LeaderState.getInstance().resetLeader(); // reset leader lists when newly elected

                        Runnable sender = new BullyAlgorithm( "Sender", "coordinator" );
                        new Thread( sender ).start();
                    }

                    if( receivedOk && !leaderFlag )
                    {
                        System.out.println( "INFO : Received OK but coordinator message was not received" );

                        electionInProgress = false;
                        receivedOk = false;

                        Runnable sender = new BullyAlgorithm( "Sender", "election" );
                        new Thread( sender ).start();
                    }
                }
                catch( Exception e ) {
                    System.out.println( "INFO : Exception in timer thread" );
                }
                break;

            case "Heartbeat":
                while( true ) {
                    try {
                        Thread.sleep(10);
                        if( leaderFlag && ServerState.getInstance().getSelfID() != LeaderState.getInstance().getLeaderID() ) {
                            Thread.sleep( 1500 );
                            Server destServer = ServerState.getInstance().getServers()
                                                           .get( LeaderState.getInstance().getLeaderID() );

                            MessageTransfer.sendServer(
                                    ServerMessage.getHeartbeat( String.valueOf(ServerState.getInstance().getSelfID()) ),
                                    destServer
                            );
                            //System.out.println( "INFO : Sent heartbeat to leader s" + destServer.getServerID() );
                        }
                    }

                    catch( Exception e ) {
                        leaderFlag = false;
                        leaderUpdateComplete = false;
                        System.out.println( "WARN : Leader has failed!" );
                        // send election request
                        Runnable sender = new BullyAlgorithm( "Sender", "election" );
                        new Thread( sender ).start();
                    }
                }

            case "Sender":
                switch( reqType ) {
                    case "election":
                        try {
                            sendElectionRequest();
                        } catch( Exception e ) {
                            System.out.println( "WARN : Server has failed, election request cannot be processed" );
                        }
                        break;

                    case "ok":
                        try {
                            sendOK();
                        } catch( Exception e ) {
                            e.printStackTrace();
                        }
                        break;

                    case "coordinator":
                        try {
                            sendCoordinatorMsg();
                        } catch( Exception e ) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
        }
    }
    /**
     * The sendCoordinatorMsg() method broadcasts the leader to all the servers.
     * If the server has failed then a message is displayed to indicate the failure.
     */
    public static void sendCoordinatorMsg() {
        for ( int key : ServerState.getInstance().getServers().keySet() ) {
            if ( key != ServerState.getInstance().getSelfID() ){
                Server destServer = ServerState.getInstance().getServers().get(key);

                try {
                    MessageTransfer.sendServer(
                            ServerMessage.getCoordinator( String.valueOf(ServerState.getInstance().getSelfID()) ),
                            destServer
                    );
                    System.out.println("INFO : Sent leader ID to s"+destServer.getServerID());
                }
                catch(Exception e) {
                    System.out.println("WARN : Server s"+destServer.getServerID()+
                                               " has failed, it will not receive the leader");
                }
            }
        }

    }
    /**
     * The sendOK() method sends OK message to the incoming server which has requested an election
     */
    public static void sendOK() {
        try {
            Server destServer = ServerState.getInstance().getServers().get(sourceID);
            MessageTransfer.sendServer(
                    ServerMessage.getOk( String.valueOf(ServerState.getInstance().getSelfID()) ),
                    destServer
            );
            System.out.println("INFO : Sent OK to s"+destServer.getServerID());
        }
        catch(Exception e) {
            System.out.println("INFO : Server s"+sourceID+" has failed. OK message cannot be sent");
        }
    }

    /**
     * The sendElectionRequest() method sends an election request to all the servers with higher IDs
     */
    public static void sendElectionRequest()
    {
        System.out.println("INFO : Election initiated");
        int numberOfFailedRequests = 0;
        for ( int key : ServerState.getInstance().getServers().keySet() ) {
            if( key > ServerState.getInstance().getSelfID() ){
                Server destServer = ServerState.getInstance().getServers().get(key);
                try {
                    MessageTransfer.sendServer(
                            ServerMessage.getElection( String.valueOf(ServerState.getInstance().getSelfID()) ),
                            destServer
                    );
                    System.out.println("INFO : Sent election request to s"+destServer.getServerID());
                }
                catch(Exception e){
                    System.out.println("WARN : Server s"+destServer.getServerID() +
                                               " has failed, cannot send election request");
                    numberOfFailedRequests++;
                }
            }

        }
        if (numberOfFailedRequests == ServerState.getInstance().getNumberOfServersWithHigherIds()) {
            if(!electionInProgress){
                //startTime=System.currentTimeMillis();
                electionInProgress = true;
                receivedOk = false;
                Runnable timer = new BullyAlgorithm("Timer");
                new Thread(timer).start();
            }
        }
    }

    public static void receiveMessages(JSONObject j_object) {
        String option = j_object.get( "option" ).toString();
        switch( option ) {
            case "election":
                // {"option": "election", "source": 1}
                sourceID = Integer.parseInt(j_object.get( "source" ).toString());
                System.out.println( "INFO : Received election request from s" + sourceID );

                if( ServerState.getInstance().getSelfID() > sourceID ) {
                    Runnable sender = new BullyAlgorithm( "Sender", "ok" );
                    new Thread( sender ).start();
                }
                if( !electionInProgress ) {
                    Runnable sender = new BullyAlgorithm( "Sender", "election" );
                    new Thread( sender ).start();
                    //startTime = System.currentTimeMillis();
                    electionInProgress = true;

                    Runnable timer = new BullyAlgorithm( "Timer" );
                    new Thread( timer ).start();
                    System.out.println( "INFO : Election started");
                }
                break;
            case "ok": {
                // {"option": "ok", "sender": 1}
                receivedOk = true;
                int senderID = Integer.parseInt(j_object.get( "sender" ).toString());
                System.out.println( "INFO : Received OK from s" + senderID );
                break;
            }
            case "coordinator":
                // {"option": "coordinator", "leader": 1}
                LeaderState.getInstance().setLeaderID(
                        Integer.parseInt(j_object.get( "leader" ).toString()) );
                leaderFlag = true;
                leaderUpdateComplete = false;
                electionInProgress = false;
                receivedOk = false;
                System.out.println( "INFO : Server s" + LeaderState.getInstance().getLeaderID()
                                            + " is selected as leader! " );

                // send local client list and chat room list to leader
                try
                {
                    MessageTransfer.sendToLeader(
                            ServerMessage.getLeaderStateUpdate(
                                    ServerState.getInstance().getClientIdList(),
                                    ServerState.getInstance().getChatRoomList()
                            )
                    );
                } catch( IOException e ) {
                    System.out.println("WARN : Leader state update message could not be sent");
                }
                break;
            case "heartbeat": {
                // {"option": "heartbeat", "sender": 1}
                int senderID = Integer.parseInt(j_object.get( "sender" ).toString());
                //System.out.println( "INFO : Heartbeat received from s" + senderID );
                break;
            }
        }
    }

    /**
     * The initialize() method makes a newly started or recovered server
     * to initiate an election to update the leader
     */
    public static void initialize()
    {
        // Initiate election
        Runnable sender = new BullyAlgorithm("Sender","election");
        new Thread(sender).start();
    }
}