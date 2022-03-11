package messaging;

public class ClientMessageContext {
    public enum CLIENT_MSG_TYPE {
        NEW_ID,
        LIST,
        WHO,
        MESSAGE,
        CREATE_ROOM,
        JOIN_ROOM,
        ROUTE,
        MOVE_JOIN,
        DELETE_ROOM,
        QUIT
    }

    public CLIENT_MSG_TYPE messageType;

    public String clientID;
    public String currentRoomID;
    public String formerRoomID;
    public String newRoomID;

    public int currentServerID;

    public String body;

    //flags
    public boolean isNewClientIdApproved;
    public boolean isNewRoomIdApproved;


    public ClientMessageContext setMessageType(CLIENT_MSG_TYPE messageType) {
        this.messageType = messageType;
        return this;
    }

    public ClientMessageContext setClientID(String clientID) {
        this.clientID = clientID;
        return this;
    }

    public ClientMessageContext setCurrentRoomID(String currentRoomID) {
        this.currentRoomID = currentRoomID;
        return this;
    }

    public ClientMessageContext setFormerRoomID(String formerRoomID) {
        this.formerRoomID = formerRoomID;
        return this;
    }

    public ClientMessageContext setNewRoomID(String newRoomID) {
        this.newRoomID = newRoomID;
        return this;
    }

    public ClientMessageContext setCurrentServerID(int currentServerID) {
        this.currentServerID = currentServerID;
        return this;
    }

    public ClientMessageContext setBody(String body) {
        this.body = body;
        return this;
    }

    public ClientMessageContext setNewClientIdApproved(boolean newClientIdApproved) {
        isNewClientIdApproved = newClientIdApproved;
        return this;
    }

    public ClientMessageContext setNewRoomIdApproved(boolean newRoomIdApproved) {
        isNewRoomIdApproved = newRoomIdApproved;
        return this;
    }
}