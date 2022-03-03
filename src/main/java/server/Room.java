package server;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String owner;
    private String roomId;


    public Room(String identity, String roomId) {
        this.owner = identity;
        this.roomId = roomId;
    }

    public synchronized String getRoomId() {
        return roomId;
    }

    public synchronized void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public synchronized String getParticipants() {
        return roomId;
    }


    public String getOwnerIdentity() {
        return owner;
    }

}