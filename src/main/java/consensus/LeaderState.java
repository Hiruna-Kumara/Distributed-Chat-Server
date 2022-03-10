package consensus;

import server.ServerState;

import java.util.HashMap;

public class LeaderState
{
    private int leaderID;
    private final HashMap<String, String> pendingClients = new HashMap<>(); // <clientID, serverID>
    private final HashMap<String, String> activeClients = new HashMap<>();
    private final HashMap<String, String> pendingRooms = new HashMap<>();
    private final HashMap<String, String> activeRooms = new HashMap<>();

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

    public static boolean isLeader() {
        return BullyAlgorithm.leaderFlag &&
                ( ServerState.getInstance().getSelfID() == LeaderState.getInstance().getLeaderID() );
    }

    public boolean isClientIDAlreadyTaken(String clientID){
        for ( String key: pendingClients.keySet()) {
            if (key.equals( clientID ))
                return true;
        }
        return false;
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