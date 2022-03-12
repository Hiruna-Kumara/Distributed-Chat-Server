package client;

import java.net.Socket;

public class ClientState {

    private String clientID;
    private String roomID;
    private Socket socket;
    private boolean isRoomOwner = false;

    public ClientState(String clientID, String roomID, Socket socket) {
        this.clientID = clientID;
        this.roomID = roomID;
        this.socket = socket;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }


    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isRoomOwner() {
        return isRoomOwner;
    }

    public void setRoomOwner(boolean roomOwner) {
        isRoomOwner = roomOwner;
    }
}