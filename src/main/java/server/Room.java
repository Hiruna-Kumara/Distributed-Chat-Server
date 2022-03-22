package Server;

import Client.Client;

import java.util.HashMap;

public class Room {

    private final String roomID;
    private final String serverID;
    private final String ownerClientID;

    private final HashMap<String,Client> clientList = new HashMap<>();

    public Room(String roomID, String serverID, String ownerClientID){
        this.roomID = roomID;
        this.serverID = serverID;
        this.ownerClientID = ownerClientID;
    }

    public String getRoomID() {
        return roomID;
    }

    public String getServerID() {
        return serverID;
    }

    public String getOwnerClientID() {
        return ownerClientID;
    }

    public HashMap<String, Client> getClientList() {
        return clientList;
    }

    public void addClient(Client client){
        clientList.put(client.getClientID(), client);
    }

    public void removeClient(String clientID){
        clientList.remove(clientID);
    }

}
