package messaging;

import java.util.List;

public class ClientMessageContext {
    public enum CLIENT_MSG_TYPE {
        NEW_ID,
        LIST,//roomlist
        WHO,//roomcontents
        MESSAGE,
        CREATE_ROOM,
        JOIN_ROOM,//roomchange
        BROADCAST_JOIN_ROOM,//roomchangeall
        ROUTE,
        MOVE_JOIN,
        DELETE_ROOM,
        QUIT
    }

    public CLIENT_MSG_TYPE messageType;

    public String clientID;
    public String roomID;
    public String formerRoomID;
    public String newRoomID;

    public String currentServerID;

    public String body;

    //flags
    public String isNewClientIdApproved;
    public String isNewRoomIdApproved;
    public String isDeleteRoomApproved;

    public String targetHost;
    public String targetPort;

    public List<String> participantsList;
    public List<String> roomsList;


    public ClientMessageContext setMessageType(CLIENT_MSG_TYPE messageType) {
        this.messageType = messageType;
        return this;
    }

    public ClientMessageContext setClientID(String clientID) {
        this.clientID = clientID;
        return this;
    }

    public ClientMessageContext setRoomID(String roomID) {
        this.roomID = roomID;
        return this;
    }

    public ClientMessageContext setFormerRoomID(String formerRoomID) {
        this.formerRoomID = formerRoomID;
        return this;
    }

    public ClientMessageContext setCurrentServerID(String currentServerID) {
        this.currentServerID = currentServerID;
        return this;
    }

    public ClientMessageContext setBody(String body) {
        this.body = body;
        return this;
    }

    public ClientMessageContext setIsNewClientIdApproved(String isNewClientIdApproved) {
        this.isNewClientIdApproved = isNewClientIdApproved;
        return this;
    }

    public ClientMessageContext setIsNewRoomIdApproved(String isNewRoomIdApproved) {
        this.isNewRoomIdApproved = isNewRoomIdApproved;
        return this;
    }

    public ClientMessageContext setIsDeleteRoomApproved(String isDeleteRoomApproved) {
        this.isDeleteRoomApproved = isDeleteRoomApproved;
        return this;
    }

    public ClientMessageContext setTargetHost(String targetHost) {
        this.targetHost = targetHost;
        return this;
    }

    public ClientMessageContext setTargetPort(String targetPort) {
        this.targetPort = targetPort;
        return this;
    }

    public ClientMessageContext setParticipantsList(List<String> participantsList) {
        this.participantsList = participantsList;
        return this;
    }

    public ClientMessageContext setRoomsList(List<String> roomsList) {
        this.roomsList = roomsList;
        return this;
    }
}