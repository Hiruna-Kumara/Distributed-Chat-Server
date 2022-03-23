package consensus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import Client.Client;
import MessagePassing.MessagePassing;
import Server.Room;
import Server.Server;
import Server.ServerInfo;
import Server.ServerMessage;

public class Leader {
    private String leaderID;

    private final ConcurrentHashMap<String, List<String>> globalClientList = new ConcurrentHashMap<>(); //server_id, cliient_id list
    private final ConcurrentHashMap<String, List<Room>> globalRoomList = new ConcurrentHashMap<>(); //server_id, room list
    private static Leader leaderInstance;

    private Leader(){

    }

    public static synchronized Leader getInstance(){
        if (leaderInstance == null){
            leaderInstance = new Leader();
        }
        return leaderInstance;
    }

    public synchronized String getLeaderID() {
        return leaderID;
    }

    public synchronized Integer getLeaderIDInt() {
        Integer i=Integer.parseInt(leaderID);
        return i;
    }

    public synchronized void setLeaderID(String leaderID) {
        this.leaderID = leaderID;
    }

    public synchronized ConcurrentHashMap<String, List<String>> getGlobalClientList() {
        return globalClientList;
    }

    public synchronized ConcurrentHashMap<String, List<Room>> getGlobalRoomList() {
        return globalRoomList;
    }

    public void updateLeader(String serverID, List<String> clientIDList, List<Room> roomList) {
        synchronized (Leader.getInstance()){
            globalRoomList.put(serverID,roomList);
            globalClientList.put(serverID,clientIDList);

            if(!serverID.equals(Server.getInstance().getServerID())){
                ServerInfo destServer = Server.getInstance().getOtherServers().get(serverID);
                try {
                    MessagePassing.sendServer(
                            ServerMessage.leaderStateUpdateComplete(String.valueOf(Server.getInstance().getServerID())),
                            destServer
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void handleRequest(String serverID, List<String> clientIDList, List<Room> roomList){
        updateLeader(serverID, clientIDList, roomList);
    }

    public void reset() {
        synchronized (Leader.getInstance()){
            globalRoomList.clear();
            globalClientList.clear();
        }
    }

    public synchronized boolean isClientIDTaken(String identity){
        for(String clientID: globalClientList.keySet()){
            if(globalClientList.get(clientID).contains(identity)){
                return true;
            }
        }
        return false;
    }

    public synchronized void addToGlobalClientAndRoomList(String clientID, String serverID, String roomID){
        globalClientList.get(serverID).add(clientID);
        for(Room room: globalRoomList.get(serverID)){
            if(Objects.equals(room.getRoomID(), roomID)){
                room.addClient(new Client(clientID, roomID, null));
                break;
            }
        }
    }

    // --------newly added
    public String getServerIdIfRoomExist(String roomId){
        for(String serverId: globalRoomList.keySet()){
            List<Room> tempRoomList = globalRoomList.get(serverId);
            for(Room room: tempRoomList){
                if (Objects.equals(room.getRoomID(), roomId)){
                    return serverId;
                }
            }
        }
        return null;
    }

    public void InServerJoinRoomClient(String clientID, String serverID, String formerRoomID, String roomID) {
        removeFromGlobalClientAndRoomList(clientID, serverID, formerRoomID);
        addToGlobalClientAndRoomList(clientID, serverID, roomID);
    }

    public synchronized List<String> getRoomIDList() {
        List<String> roomIDList = new ArrayList<>();
        for(String serverID: globalRoomList.keySet()){
            for(Room room: globalRoomList.get(serverID)){
                roomIDList.add(room.getRoomID());
            }
        }
        return roomIDList;
    }

    public synchronized void removeFromGlobalClientAndRoomList(String clientID, String serverID, String roomID){
        globalClientList.get(serverID).remove(clientID);
        for(Room room: globalRoomList.get(serverID)){
            if(Objects.equals(room.getRoomID(), roomID)){
                room.removeClient(clientID);
                break;
            }
        }

    }


    public boolean isRoomIDTaken(String roomID) {
        return getRoomIDList().contains(roomID);
    }

    public void addToRoomList(String clientID, String serverID, String roomID, String former) {
        Room newRoom = new Room(roomID, serverID, clientID);
        globalRoomList.get(serverID).add(newRoom);
        for(Room room: globalRoomList.get(serverID)){
            if(Objects.equals(room.getRoomID(), former)){
                room.removeClient(clientID);
            }
            else if(Objects.equals(room.getRoomID(), roomID)){
                Client client = new Client(clientID, roomID, null);
                client.setRoomOwner(true);
                room.addClient(client);
            }
        }

    }

    public void removeRoom(String serverID, String roomID, String mainHallRoomID, String ownerID) {
        List<Room> rooms = globalRoomList.get(serverID);
        HashMap<String, Client> formerClientList = null;
        for(Room room:rooms){
            if(Objects.equals(room.getRoomID(), roomID)){
                formerClientList = room.getClientList();
                break;
            }
        }
        globalRoomList.get(serverID).removeIf(room -> Objects.equals(room.getRoomID(), roomID));
        formerClientList.get(ownerID).setRoomOwner(false);
        for(Room room:globalRoomList.get(serverID)) {
            if (Objects.equals(room.getRoomID(), mainHallRoomID)) {
                formerClientList.forEach((clientID, client) -> {
                    client.setRoomID(mainHallRoomID);
                    room.getClientList().put(clientID, client);
                });
            }
        }
    }

    public void removeRemoteChatRoomsClientsByServerId(Integer serverId) {
        for (String entry : globalRoomList.keySet()) {
            List<Room> remoteRoom = globalRoomList.get(entry);
            for(Room room: remoteRoom){
                if(room.getServerIdInt()==serverId){
                    for(String client : room.getClientList().keySet()){
                        globalClientList.remove(client);
                    }
                    globalRoomList.remove(entry);
                }
            }
        }

    }

}
