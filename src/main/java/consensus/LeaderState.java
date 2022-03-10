package consensus;

import server.ServerState;

import java.util.HashMap;

public class LeaderState
{
    private int leaderID;
    private final HashMap<String, Integer> activeClients = new HashMap<>(); // <clientID, serverID>
    private final HashMap<String, String> activeClientRooms = new HashMap<>(); // <clientID, roomID>
    private final HashMap<String, Integer> activeServerRooms = new HashMap<>(); // <roomID, serverID>

    // singleton
    private static LeaderState leaderStateInstance;

    private LeaderState() {
    }

    public static LeaderState getInstance() {
        if (leaderStateInstance == null) {
            synchronized (LeaderState.class) {
                if (leaderStateInstance == null) {
                    leaderStateInstance = new LeaderState(); //instance will be created at request time
                }
            }
        }
        return leaderStateInstance;
    }

    public boolean isLeader() {
        return ServerState.getInstance().getSelfID() == LeaderState.getInstance().getLeaderID();
    }

    public boolean isLeaderElected() {
        return BullyAlgorithm.leaderFlag;
    }



    public boolean isClientIDAlreadyTaken(String clientID){
        return activeClients.containsKey( clientID );
    }

    public void addApprovedClient(String clientID, int serverID) {
        activeClients.put( clientID, serverID );
    }

    public void removeApprovedClient(String clientID) {
        activeClients.remove( clientID );
    }
    public boolean isRoomCreationApproved( String clientID, String roomID ) {
        return !(activeServerRooms.containsKey( roomID ) && activeClientRooms.containsKey( clientID ));
    }

    public void addApprovedRoom(String clientID, String roomID, int serverID) {
        activeClientRooms.put( clientID, roomID );
        activeServerRooms.put( roomID, serverID );
    }

    public void removeApprovedRoom(String clientID, String roomID) {
        if( activeClientRooms.get( clientID ).equals( roomID ) ) {
            activeClientRooms.remove( clientID );
            activeServerRooms.remove( roomID );
        }
    }


    public int getLeaderID()
    {
        return leaderID;
    }

    public void setLeaderID( int leaderID )
    {
        this.leaderID = leaderID;
    }
}