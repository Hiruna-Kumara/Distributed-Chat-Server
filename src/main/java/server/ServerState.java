package server;

// import client.ClientHandlerThread;
// import client.ClientState;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerState {

    private String serverID;
    private int selfID;
    private String serverAddress = null;
    private int coordinationPort;
    private int clientsPort;
    private int numberOfServersWithHigherIds;

    private AtomicBoolean ongoingConsensus = new AtomicBoolean(false);

    private final ConcurrentHashMap<Integer, String> suspectList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> heartbeatCountList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> voteSet = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, Server> servers = new ConcurrentHashMap<>(); // list of other servers

    private Room mainHall;

    // maintain client handler thread map <threadID, thread>
    private final ConcurrentHashMap<Long, ClientHandlerThread> clientHandlerThreadMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Room> roomMap = new ConcurrentHashMap<>();  // maintain local room object list <roomID,roomObject>

    //singleton
    private static ServerState serverStateInstance;

    private ServerState() {
    }

    public static ServerState getInstance() {
        if (serverStateInstance == null) {
            synchronized (ServerState.class) {
                if (serverStateInstance == null) {
                    serverStateInstance = new ServerState();//instance will be created at request time
                }
            }
        }
        return serverStateInstance;
    }

    public void initializeWithConfigs(String serverID, String serverConfPath) {
        this.serverID = serverID;
        try {
            File conf = new File(serverConfPath); // read configuration
            Scanner myReader = new Scanner(conf);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] params = data.split(" ");
                if (params[0].equals(serverID)) {
                    this.serverAddress = params[1];
                    this.clientsPort = Integer.parseInt(params[2]);
                    this.coordinationPort = Integer.parseInt(params[3]);
                    this.selfID = Integer.parseInt(params[0].substring(1, 2));
                }
                // add all servers to hash map
                Server s = new Server(Integer.parseInt(params[0].substring(1, 2)),
                        Integer.parseInt(params[3]),
                        Integer.parseInt(params[2]),
                        params[1]);
                servers.put(s.getServerID(), s);
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("Configs file not found");
            e.printStackTrace();
        }
        // set number of servers with higher ids
        numberOfServersWithHigherIds = servers.size() - selfID;

        this.mainHall = new Room("", getMainHallID(), selfID);
        this.roomMap.put(getMainHallID(), mainHall);

    }

    public void addClientHandlerThreadToMap(ClientHandlerThread clientHandlerThread) {
        clientHandlerThreadMap.put(clientHandlerThread.getId(), clientHandlerThread);
    }

    public ClientHandlerThread getClientHandlerThread(Long threadID) {
        return clientHandlerThreadMap.get(threadID);
    }

    public boolean isClientIDAlreadyTaken(String clientID) {
        for (Map.Entry<String, Room> entry : this.getRoomMap().entrySet()) {
            Room room = entry.getValue();
            if (room.getClientStateMap().containsKey(clientID)) return true;
        }
        return false;
    }

    // used for updating leader client list when newly elected
    public List<String> getClientIdList() {
        List<String> clientIdList = new ArrayList<>();
        roomMap.forEach((roomID, room) -> {
            clientIdList.addAll(room.getClientStateMap().keySet());
        });
        return clientIdList;
    }

    // used for updating leader chat room list when newly elected
    public List<List<String>> getChatRoomList() {
        // [ [clientID, roomID, serverID] ]
        List<List<String>> chatRoomList = new ArrayList<>();
        for (Room room: roomMap.values()) {
            List<String> roomInfo = new ArrayList<>();
            roomInfo.add( room.getOwnerIdentity() );
            roomInfo.add( room.getRoomID() );
            roomInfo.add( String.valueOf(room.getServerID()) );

            chatRoomList.add( roomInfo );
        }
        return chatRoomList;
    }

    public void removeClient (String clientID, String formerRoom, Long threadID){
        this.roomMap.get(formerRoom).removeParticipants(clientID);
        this.clientHandlerThreadMap.remove(threadID);
    }

    public String getServerID() {
        return serverID;
    }

    public int getClientsPort() {
        return clientsPort;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getCoordinationPort() {
        return coordinationPort;
    }

    public int getSelfID() {
        return selfID;
    }

    public int getNumberOfServersWithHigherIds() {
        return numberOfServersWithHigherIds;
    }

    public ConcurrentHashMap<Integer, Server> getServers() {
        return servers;
    }

    public void removeServer(Integer serverId) {
        servers.remove(serverId);
    }

    public Room getMainHall() {
        return mainHall;
    }

    public ConcurrentHashMap<String, Room> getRoomMap() {
        return roomMap;
    }

    public String getMainHallID() {
        return getMainHallIDbyServerInt(this.selfID);
    }

    public static String getMainHallIDbyServerInt(int server) {
        return "MainHall-s" + server;
    }

    public synchronized void removeServerInSuspectList(Integer serverId) {
        suspectList.remove(serverId);
    }

    public ConcurrentHashMap<Integer, String> getSuspectList() {
        return suspectList;
    }

    public synchronized void removeServerInCountList(Integer serverId) {
        heartbeatCountList.remove(serverId);
    }

    public ConcurrentHashMap<Integer, Integer> getHeartbeatCountList() {
        return heartbeatCountList;
    }

    public AtomicBoolean onGoingConsensus() {
        return ongoingConsensus;
    }

    public ConcurrentHashMap<String, Integer> getVoteSet() {
        return voteSet;
    }

}