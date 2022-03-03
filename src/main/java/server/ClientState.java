package server;

public class ClientState {

    private String id;
    private String roomID;
    private Integer port;

    public ClientState(String id, String roomID, Integer port){
        this.id = id;
        this.roomID = roomID;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getIDbyPort(Integer port){
        if (this.port==port){
            return this.id;
        } else {
            return null;
        }
    }
}