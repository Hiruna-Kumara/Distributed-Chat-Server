package server;

public class ClientState {

    private String clientID;
    private String roomID;
    private Integer port;

    public ClientState(String clientID, String roomID, Integer port) {
        this.clientID = clientID;
        this.roomID = roomID;
        this.port = port;
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

    public String getIDbyPort(Integer port) {
        if (this.port == port) {
            return this.clientID;
        } else {
            return null;
        }
    }
}