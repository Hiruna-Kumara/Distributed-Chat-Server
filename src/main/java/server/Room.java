package server;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String ownerID;
    private String roomId;
    private List<ClientState> participants = new ArrayList<ClientState>();

    public Room(String identity, String roomId) {
        this.ownerID = identity;
        this.roomId = roomId;
    }

    public synchronized String getRoomID() {
        return roomId;
    }

    public synchronized void setRoomID(String roomId) {
        this.roomId = roomId;
    }

    public synchronized List<ClientState> getParticipants() {
        return this.participants;
    }

    public synchronized void addParticipants(ClientState participantID) {
        this.participants.add(participantID);
    }

    public synchronized void removeParticipants(ClientState participantID) {
        this.participants.remove(participantID);
    }

    public String getOwnerIdentity() {
        return ownerID;
    }

}