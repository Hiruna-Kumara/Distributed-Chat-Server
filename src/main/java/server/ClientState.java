package server;

import java.net.Socket;

public class ClientState {

    private String clientID;
    private String roomID;
    private Integer port;
    private Socket socket;

    public ClientState(String clientID, String roomID, Integer port, Socket socket) {
        this.clientID = clientID;
        this.roomID = roomID;
        this.port = port;
        this.socket = socket;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
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
}