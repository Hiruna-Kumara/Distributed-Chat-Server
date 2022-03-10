package server;

import client.ClientHandlerThread;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ServerState {

    private String serverID;
    private int selfID;
    private String serverAddress = null;
    private int coordinationPort;
    private int clientsPort;
    private int numberOfServersWithHigherIds;

    private final HashMap<Integer, Server> servers = new HashMap<>(); // list of other servers

    private Room mainHall;

    // maintain client handler thread map <threadID, thread>
    private final HashMap<Long, ClientHandlerThread> clientHandlerThreadMap = new HashMap<>();


    private final HashMap<String, Room> roomMap = new HashMap<>(); // maintain room object list <roomID,roomObject>

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

        this.mainHall = new Room("default-" + serverID, "MainHall-" + serverID);
        this.roomMap.put("MainHall-" + serverID, mainHall);

    }

        public void addClientHandlerThreadToMap(ClientHandlerThread clientHandlerThread) {
        clientHandlerThreadMap.put( clientHandlerThread.getId(), clientHandlerThread );
    }

    public ClientHandlerThread getClientHandlerThread(Long threadID) {
        return clientHandlerThreadMap.get( threadID );
    }

    public boolean isClientIDAlreadyTaken(String clientID) {
        for (Map.Entry<String, Room> entry : this.getRoomMap().entrySet()) {
            Room room = entry.getValue();
            if (room.getClientStateMap().containsKey(clientID))
                return true;
        }
        return false;
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

    public HashMap<Integer, Server> getServers() {
        return servers;
    }

    public Room getMainHall() {
        return mainHall;
    }

    public HashMap<String, Room> getRoomMap() {
        return roomMap;
    }

}