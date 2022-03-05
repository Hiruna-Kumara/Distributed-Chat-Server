package server;

import java.util.*;

public class ServerState {

    private String serverID;
    private int serverPort;
    private Room mainHall;
    private final ArrayList<Server> clientHandlerList = new ArrayList<>();

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

    // TODO : make private, init with get instance and configs at startup
    public void initializeWithConfigs(String serverID, int serverPort) {
        this.serverID = serverID;
        this.serverPort = serverPort;
        this.mainHall = new Room("default-" + serverID, "MainHall-" + serverID);
        this.roomMap.put("MainHall-" + serverID, mainHall);
    }

    public void addClientHandlerThreadToList(Server clientHandlerThread) {
        clientHandlerList.add(clientHandlerThread);
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

    public int getServerPort() {
        return serverPort;
    }

    public Room getMainHall() {
        return mainHall;
    }

    public HashMap<String, Room> getRoomMap() {
        return roomMap;
    }
}