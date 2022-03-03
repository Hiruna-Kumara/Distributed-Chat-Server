package server;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String owner;
    private String roomId;
    private List<ClientState> participants = new ArrayList<ClientState>();

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

    public synchronized void addParticipants(ClientState participantID) {
        this.participants.add(participantID);
    }

    public synchronized List<ClientState> getParticipants(List<ClientState> participantID) {
        return this.participants;
    }

    public synchronized void removeParticipants(ClientState participantID) {
        this.participants.remove(participantID);
    }

    public String getOwnerIdentity() {
        return owner;
    }

}