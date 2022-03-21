package consensus;

import client.ClientState;
import messaging.MessageTransfer;
import messaging.ServerMessage;
import server.Room;
import server.Server;
import server.ServerState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LeaderState {
    private Integer leaderID;

    private final List<String> activeClientsList = new ArrayList<>();
    private final HashMap<String, Room> activeChatRooms = new HashMap<>(); // <roomID, room obj>

    // singleton
    private static LeaderState leaderStateInstance;

    private LeaderState() {
    }

    public static LeaderState getInstance() {
        if (leaderStateInstance == null) {
            synchronized (LeaderState.class) {
                if (leaderStateInstance == null) {
                    leaderStateInstance = new LeaderState(); // instance will be created at request time
                    // leaderStateInstance.addServerDefaultMainHalls();
                }
            }
        }
        return leaderStateInstance;
    }

    public boolean isLeader() {
        return ServerState.getInstance().getSelfID() == LeaderState.getInstance().getLeaderID();
    }

    public boolean isLeaderElected() {
        // return (BullyAlgorithm.leaderFlag && BullyAlgorithm.leaderUpdateComplete);
        return ServerState.getInstance().getLeaderUpdateComplete();
    }

    // public boolean isLeaderElectedAndIamLeader() {
    // return (BullyAlgorithm.leaderFlag
    // && ServerState.getInstance().getSelfID() ==
    // LeaderState.getInstance().getLeaderID());
    // }

    // public boolean isLeaderElectedAndMessageFromLeader(int serverID) {
    // return (BullyAlgorithm.leaderFlag && serverID ==
    // LeaderState.getInstance().getLeaderID());
    // }

    public boolean isClientIDAlreadyTaken(String clientID) {
        return activeClientsList.contains(clientID);
    }

    public void resetLeader() {
        activeClientsList.clear();
        activeChatRooms.clear();
    }

    public void addClient(ClientState client) {
        activeClientsList.add(client.getClientID());
        activeChatRooms.get(client.getRoomID()).addParticipants(client);
    }

    public void addClientLeaderUpdate(String clientID) {
        activeClientsList.add(clientID);
    }

    public void removeClient(String clientID, String formerRoomID) {
        activeClientsList.remove(clientID);
        activeChatRooms.get(formerRoomID).removeParticipants(clientID);
    }

    public void localJoinRoomClient(ClientState clientState, String formerRoomID) {
        removeClient(clientState.getClientID(), formerRoomID);
        addClient(clientState);
    }

    public boolean isRoomCreationApproved(String roomID) {
        return !(activeChatRooms.containsKey(roomID));
    }

    public void addApprovedRoom(String clientID, String roomID, int serverID) {
        Room room = new Room(clientID, roomID, serverID);
        activeChatRooms.put(roomID, room);

        // add client to the new room
        ClientState clientState = new ClientState(clientID, roomID, null);
        clientState.setRoomOwner(true);
        room.addParticipants(clientState);
    }

    public void addApprovedRoom(Room room) {
        activeChatRooms.put(room.getRoomID(), room);

        // add client to the new room
        ClientState clientState = new ClientState(room.getOwnerIdentity(), room.getRoomID(), null);
        clientState.setRoomOwner(true);
        room.addParticipants(clientState);
    }

    public void removeRoom(String roomID, String mainHallID, String ownerID) {
        HashMap<String, ClientState> formerClientStateMap = this.activeChatRooms.get(roomID).getClientStateMap();
        Room mainHall = this.activeChatRooms.get(mainHallID);

        // update client room to main hall , add clients to main hall
        formerClientStateMap.forEach((clientID, clientState) -> {
            clientState.setRoomID(mainHallID);
            mainHall.getClientStateMap().put(clientState.getClientID(), clientState);
        });

        // set to room owner false, remove room from map
        formerClientStateMap.get(ownerID).setRoomOwner(false);
        this.activeChatRooms.remove(roomID);
    }

    public void addServerDefaultMainHalls() {
        ServerState.getInstance().getOtherServers()
                .forEach((serverID, server) -> {
                    String roomID = ServerState.getMainHallIDbyServerInt(Integer.parseInt(serverID));
                    this.activeChatRooms.put(roomID, new Room("", roomID, Integer.parseInt(serverID)));
                });
    }

    public void removeApprovedRoom(String roomID) {
        // TODO : move clients already in room (update server state) on delete
        activeChatRooms.remove(roomID);
    }

    public int getServerIdIfRoomExist(String roomID) {
        if (this.activeChatRooms.containsKey(roomID)) {
            Room targetRoom = activeChatRooms.get(roomID);
            return targetRoom.getServerID();
        } else {
            return -1;
        }
    }

    public Integer getLeaderID() {
        return leaderID;
    }

    public void setLeaderID(int leaderID) {
        this.leaderID = leaderID;
    }

    public ArrayList<String> getRoomIDList() {
        return new ArrayList<>(this.activeChatRooms.keySet());
    }

    public List<String> getClientIDList() {
        return this.activeClientsList;
    }

    public void updateLeader(String serverID, List<String> clientIDList, List<Room> roomList) {
        synchronized (LeaderState.getInstance()) {
            for (String clientID : clientIDList) {
                addClientLeaderUpdate(clientID);
            }
            for (Room chatRoom : roomList) {
                addApprovedRoom(chatRoom);
            }
            if (!serverID.equals(ServerState.getInstance().getServerIDNum())) {
                Server destServer = ServerState.getInstance().getOtherServers().get(serverID);
                try {
                    MessageTransfer.sendServer(
                            ServerMessage.getLeaderStateUpdateComplete(
                                    String.valueOf(ServerState.getInstance().getServerID())),
                            destServer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void handleRequest(String serverID, List<String> clientIDList, List<Room> roomList) {
        updateLeader(serverID, clientIDList, roomList);
    }

    // remove all rooms and clients by server ID
    public void removeRemoteChatRoomsClientsByServerId(Integer serverId) {
        for (String entry : activeChatRooms.keySet()) {
            Room remoteRoom = activeChatRooms.get(entry);
            if (remoteRoom.getServerID() == serverId) {
                for (String client : remoteRoom.getClientStateMap().keySet()) {
                    activeClientsList.remove(client);
                }
                activeChatRooms.remove(entry);
            }
        }

    }

}