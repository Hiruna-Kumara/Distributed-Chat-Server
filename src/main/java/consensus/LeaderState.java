package consensus;

import server.ServerState;
import server.Room;

import java.util.HashMap;

public class LeaderState
{
    private int leaderID;
    private final HashMap<String, Integer> activeClients = new HashMap<>(); // <clientID, serverID>
    private final HashMap<String, Room> activeChatRooms = new HashMap<>(); // <roomID, room obj>


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
    public boolean isRoomCreationApproved( String roomID ) {
        return !(activeChatRooms.containsKey( roomID ));

    }

    public void addApprovedRoom(String clientID, String roomID, int serverID) {
        Room room = new Room( clientID, roomID, serverID );
        activeChatRooms.put( roomID, room );
    }

    public void removeApprovedRoom(String roomID) {
        activeChatRooms.remove( roomID );
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