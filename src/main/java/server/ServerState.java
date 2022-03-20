package server;

import client.ClientHandlerThread;
import client.ClientState;

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

    private AtomicBoolean ongoingElection = new AtomicBoolean(false);
    private AtomicBoolean answerMessageReceived = new AtomicBoolean(false);
    private AtomicBoolean viewMessageReceived = new AtomicBoolean(false);
    private AtomicBoolean leaderUpdateComplete = new AtomicBoolean(false);

    private Long electionAnswerTimeout;
    private Long electionCoordinatorTimeout;
    private Long electionNominationTimeout;

    private final ConcurrentHashMap<Integer, String> suspectList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> heartbeatCountList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> voteSet = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Server> candidateServers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Server> otherServers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Server> lowPriorityServers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Server> tempCandidateServers = new ConcurrentHashMap<>();

    private Room mainHall;

    // maintain client handler thread map <threadID, thread>
    private final ConcurrentHashMap<Long, ClientHandlerThread> clientHandlerThreadMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Room> roomMap = new ConcurrentHashMap<>(); // maintain local room object
                                                                                       // list <roomID,roomObject>

    // singleton
    private static ServerState serverStateInstance;

    private ServerState() {
    }

    public static ServerState getInstance() {
        if (serverStateInstance == null) {
            synchronized (ServerState.class) {
                if (serverStateInstance == null) {
                    serverStateInstance = new ServerState();// instance will be created at request time
                }
            }
        }
        return serverStateInstance;
    }

    public void initializeWithConfigs(String serverID) {
        this.serverID = serverID;
        try {
            String configFile = "src/main/java/config/server_config.txt";
            File conf = new File(configFile); // read configuration
            Scanner myReader = new Scanner(conf);
            // File conf = new File(serverConfPath); // read configuration
            // Scanner myReader = new Scanner(conf);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] params = data.split(" ");

                // if (params[0].equals(serverID)) {
                // this.serverAddress = params[1];
                // this.clientsPort = Integer.parseInt(params[2]);
                // this.coordinationPort = Integer.parseInt(params[3]);
                // this.selfID = Integer.parseInt(params[0].substring(1, 2));
                // }
                // // add all servers to hash map
                // Server s = new Server(Integer.parseInt(params[0].substring(1, 2)),
                // Integer.parseInt(params[3]),
                // Integer.parseInt(params[2]),
                // params[1]);
                // otherServers.put(String.valueOf(s.getServerID()), s);

                Server server = new Server(Integer.parseInt(params[0].substring(1, 2)), Integer.parseInt(params[3]),
                        Integer.parseInt(params[2]), params[1]);
                if (params[0].equals(serverID)) {
                    this.serverAddress = params[1];
                    this.clientsPort = Integer.parseInt(params[2]);
                    this.coordinationPort = Integer.parseInt(params[3]);
                    this.selfID = Integer.parseInt(params[0].substring(1, 2));
                } else {
                    if (selfID < Integer.parseInt(serverID)) {
                        candidateServers.put(serverID, server);
                    } else {
                        lowPriorityServers.put(serverID, server);
                    }
                    otherServers.put(serverID, server);
                }
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("Configs file not found");
            e.printStackTrace();
        }
        // set number of servers with higher ids
        numberOfServersWithHigherIds = otherServers.size() - selfID;

        this.mainHall = new Room("", getMainHallID(), selfID);
        this.roomMap.put(getMainHallID(), mainHall);

    }

    public void addClientHandlerThreadToMap(ClientHandlerThread clientHandlerThread) {
        clientHandlerThreadMap.put(clientHandlerThread.getId(), clientHandlerThread);
    }

    public ClientHandlerThread getClientHandlerThread(Long threadID) {
        return clientHandlerThreadMap.get(threadID);
    }

    public synchronized void initTempCandidateServers() {
        tempCandidateServers = new ConcurrentHashMap<>();
    }

    public synchronized Server getHighestPriorityCandidate() {
        Integer max = 0;
        for (String key : tempCandidateServers.keySet()) {
            if (Integer.parseInt(key) > max) {
                max = Integer.parseInt(key);
            }
        }
        return tempCandidateServers.get(Integer.toString(max));
    }

    public synchronized void addTempCandidateServer(Server serverInfo) {
        Server selfServerInfo = getSelfServerInfo();
        if (null != serverInfo) {
            if (null != selfServerInfo) {
                if (selfServerInfo.getServerID() < serverInfo.getServerID()) {
                    tempCandidateServers.put(String.valueOf(serverInfo.getServerID()), serverInfo);
                }
            }
        }
    }

    public synchronized void removeTempCandidateServer(Server serverInfo) {
        Server selfServerInfo = getSelfServerInfo();
        if (null != serverInfo) {
            if (null != selfServerInfo) {
                tempCandidateServers.remove(String.valueOf(serverInfo.getServerID()));
            }
        }
    }

    public boolean getOngoingElection() {
        return ongoingElection.get();
    }

    public void setOngoingElection(boolean ongoingElection) {
        this.ongoingElection.set(ongoingElection);
    }

    public boolean getAnswerMessageReceived() {
        return answerMessageReceived.get();
    }

    public void setAnswerMessageReceived(boolean answerMessageReceived) {
        this.answerMessageReceived.set(answerMessageReceived);
    }

    public boolean getViewMessageReceived() {
        return viewMessageReceived.get();
    }

    public void setViewMessageReceived(boolean viewMessageReceived) {
        this.viewMessageReceived.set(viewMessageReceived);
    }

    public Long getElectionAnswerTimeout() {
        return electionAnswerTimeout;
    }

    public void setElectionAnswerTimeout(Long electionAnswerTimeout) {
        this.electionAnswerTimeout = electionAnswerTimeout;
    }

    public Long getElectionCoordinatorTimeout() {
        return electionCoordinatorTimeout;
    }

    public void setElectionCoordinatorTimeout(Long electionCoordinatorTimeout) {
        this.electionCoordinatorTimeout = electionCoordinatorTimeout;
    }

    public Long getElectionNominationTimeout() {
        return electionNominationTimeout;
    }

    public void setElectionNominationTimeout(Long electionNominationTimeout) {
        this.electionNominationTimeout = electionNominationTimeout
                * (getOtherServers().size() + 1 - getLowPriorityServers().size());
    }

    public void setLeaderUpdateComplete(boolean updateComplete) {
        this.leaderUpdateComplete.set(updateComplete);
    }

    public boolean getLeaderUpdateComplete() {
        return leaderUpdateComplete.get();
    }

    public boolean isClientIDAlreadyTaken(String clientID) {
        for (Map.Entry<String, Room> entry : this.getRoomMap().entrySet()) {
            Room room = entry.getValue();
            if (room.getClientStateMap().containsKey(clientID))
                return true;
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
        for (Room room : roomMap.values()) {
            List<String> roomInfo = new ArrayList<>();
            roomInfo.add(room.getOwnerIdentity());
            roomInfo.add(room.getRoomID());
            roomInfo.add(String.valueOf(room.getServerID()));

            chatRoomList.add(roomInfo);
        }
        return chatRoomList;
    }

    public void removeClient(String clientID, String formerRoom, Long threadID) {
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

    public Server getSelfServerInfo() {
        return new Server(Integer.parseInt(serverID), coordinationPort, clientsPort, serverAddress);
    }

    public int getNumberOfServersWithHigherIds() {
        return numberOfServersWithHigherIds;
    }

    public ConcurrentHashMap<String, Server> getCandidateServers() {
        return candidateServers;
    }

    // public ConcurrentHashMap<Integer, Server> getServers() {
    // return servers;
    // }

    public ConcurrentHashMap<String, Server> getLowPriorityServers() {
        return lowPriorityServers;
    }

    public ConcurrentHashMap<String, Server> getOtherServers() {
        return otherServers;
    }

    // public void removeServer(Integer serverId) {
    // servers.remove(serverId);
    // }

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

    public ConcurrentHashMap<String, Integer> getVoteSet() {
        return voteSet;
    }

}