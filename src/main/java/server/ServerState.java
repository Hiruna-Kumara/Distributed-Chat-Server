package server;

import java.util.ArrayList;

public class ServerState {

    private String serverID;
    private final ArrayList<Server> serverList = new ArrayList<>();

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

    public String getServerID() {
        return serverID;
    }

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public ArrayList<Server> getServerList() {
        return serverList;
    }
}