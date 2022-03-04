package server;

import java.util.ArrayList;
import java.util.HashMap;

public class ServerState {

    private String serverID;
    private int serverPort;
    private Room mainHall;
    private final ArrayList<Server> clientHandlerList = new ArrayList<>();

    private final HashMap<String, Integer> clientPortMap = new HashMap<String, Integer>(); // client list
                                                                                           // <clientID,port>
    private final HashMap<Integer, String> portClientMap = new HashMap<Integer, String>(); // client list
                                                                                           // <port,clientID>
    private final HashMap<String, String> ownerRoomServerLocalMap = new HashMap<String, String>(); // global rooms with
                                                                                                   // their owners
                                                                                                   // <roomID,ownerID>

    private final HashMap<String, ClientState> clientStateMap = new HashMap<String, ClientState>(); // maintain room
                                                                                                    // object list
                                                                                                    // <clientID,clientState>
    private final HashMap<String, Room> roomMap = new HashMap<String, Room>(); // maintain room object list
                                                                               // <roomID,roomObject>

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
        roomMap.put("MainHall-" + serverID, mainHall);
        ownerRoomServerLocalMap.put("MainHall-" + serverID, "default-" + serverID);
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

    public ArrayList<Server> getServersList() {
        return clientHandlerList;
    }

    public HashMap<String, Integer> getClientPortMap() {
        return clientPortMap;
    }

    public HashMap<Integer, String> getPortClientMap() {
        return portClientMap;
    }

    public HashMap<String, String> getOwnerRoomServerLocalMap() {
        return ownerRoomServerLocalMap;
    }

    public HashMap<String, ClientState> getClientStateMap() {
        return clientStateMap;
    }

    public HashMap<String, Room> getRoomMap() {
        return roomMap;
    }
}