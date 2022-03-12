package server;

import client.ClientState;

import java.util.HashMap;

public class Room {
    private final String ownerID;
    private final String roomID;
    private final int serverID;

    private final HashMap<String,ClientState> clientStateMap = new HashMap<>();  //<clientID,clientState>

    //TODO : check sync keyword
    public Room(String ownerID, String roomID, int serverID) {
        this.ownerID = ownerID;
        this.roomID = roomID;
        this.serverID = serverID;
    }

    public synchronized String getRoomID() {
        return roomID;
    }

    public synchronized int getServerID() {
        return serverID;
    }

    public synchronized HashMap<String, ClientState> getClientStateMap() {
        return clientStateMap;
    }

    public synchronized void addParticipants(ClientState clientState) {
        this.clientStateMap.put(clientState.getClientID(), clientState);
    }

    public synchronized void removeParticipants(String clientID) {
        this.clientStateMap.remove(clientID);
    }

    public String getOwnerIdentity() {
        return ownerID;
    }

}