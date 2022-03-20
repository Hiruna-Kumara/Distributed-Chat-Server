package messaging;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import server.Room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerMessage {
    private static ServerMessage instance = null;

    private ServerMessage() {
    }

    public static synchronized ServerMessage getInstance() {
        if (instance == null)
            instance = new ServerMessage();
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getHeartbeat(String sender) {
        // {"option": "heartbeat", "sender": "s1"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "heartbeat");
        jsonObject.put("sender", sender);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getElection(String source) {
        // {"option": "election", "source": "s1"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "election");
        jsonObject.put("source", source);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject answerMessage(String serverID) {
        // {"option": "ok", "sender": "s1"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "answer");
        jsonObject.put("serverID", serverID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject nominationMessage() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "nomination");
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject setCoordinatorMessage(String serverID, String address, Integer serverPort,
            Integer clientPort) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "coordinator");
        jsonObject.put("serverID", serverID);
        jsonObject.put("address", address);
        jsonObject.put("serverPort", serverPort);
        jsonObject.put("clientPort", clientPort);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject iAmUpMessage(String serverID, String address, Integer serverPort, Integer clientPort) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "IamUp");
        jsonObject.put("serverID", serverID);
        jsonObject.put("address", address);
        jsonObject.put("serverPort", serverPort);
        jsonObject.put("clientPort", clientPort);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject viewMessage(String serverID, String address, Integer serverPort, Integer clientPort) {
        // {"type":"viewelection", "currentcoordinatorid":"1",
        // "currentcoordinatoraddress":"localhost",
        // "currentcoordinatorport":"4444", "currentcoordinatormanagementport":"5555"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "view");
        jsonObject.put("serverID", serverID);
        jsonObject.put("address", address);
        jsonObject.put("serverPort", serverPort);
        jsonObject.put("clientPort", clientPort);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject leaderUpdate(String serverID, List<String> clientIDList, List<Room> roomList) {
        JSONArray clients = new JSONArray();
        clients.addAll(clientIDList);

        JSONArray chatRooms = new JSONArray();
        for (Room room : roomList) {
            // {"clientid" : "Adel", "roomid" : "jokes", "serverid" : "s1"}
            JSONObject chatRoom = new JSONObject();
            chatRoom.put("clientID", room.getRoomID());
            chatRoom.put("roomID", room.getRoomID());
            chatRoom.put("serverID", room.getServerID());
            chatRooms.add(chatRoom);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "leaderupdate");
        jsonObject.put("serverID", serverID);
        jsonObject.put("clients", clients);
        jsonObject.put("chatrooms", chatRooms);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getCoordinator(String leader) {
        // {"option": "coordinator", "leader": "s3"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "coordinator");
        jsonObject.put("leader", leader);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getOk(String sender) {
        // {"option": "ok", "sender": "s1"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "ok");
        jsonObject.put("sender", sender);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getLeaderStateUpdate(List<String> clientIdList, List<List<String>> chatRoomList) {
        // { "type": "leaderstateupdate", "clients": ["Adel", "John", "Daphne"],
        // "chatrooms": [{"clientid" : "Adel", "roomid" : "jokes", "serverid" : "s1"},
        // ..] }

        JSONArray clients = new JSONArray();
        clients.addAll(clientIdList);

        JSONArray chatRooms = new JSONArray();
        for (List<String> chatRoomObj : chatRoomList) {
            // {"clientid" : "Adel", "roomid" : "jokes", "serverid" : "s1"}
            JSONObject chatRoom = new JSONObject();
            chatRoom.put("clientid", chatRoomObj.get(0));
            chatRoom.put("roomid", chatRoomObj.get(1));
            chatRoom.put("serverid", chatRoomObj.get(2));
            chatRooms.add(chatRoom);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "leaderstateupdate");
        jsonObject.put("clients", clients);
        jsonObject.put("chatrooms", chatRooms);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getLeaderStateUpdateComplete(String serverID) {
        // {"type" : "leaderstateupdatecomplete", "serverid" : "s3"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "leaderstateupdatecomplete");
        jsonObject.put("serverid", serverID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getClientIdApprovalRequest(String clientID, String sender, String threadID) {
        // {"type" : "clientidapprovalrequest", "clientid" : "Adel", "sender" : "s2",
        // "threadid" : "10"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "clientidapprovalrequest");
        jsonObject.put("clientid", clientID);
        jsonObject.put("sender", sender);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getClientIdApprovalReply(String approved, String threadID) {
        // {"type" : "clientidapprovalreply", "approved" : "1", "threadid" : "10"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "clientidapprovalreply");
        jsonObject.put("approved", approved);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getRoomCreateApprovalRequest(String clientID, String roomID, String sender,
            String threadID) {
        // {"type" : "roomcreateapprovalrequest", "clientid" : "Adel", "roomid" :
        // "jokes", "sender" : "s2", "threadid" : "10"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcreateapprovalrequest");
        jsonObject.put("clientid", clientID);
        jsonObject.put("roomid", roomID);
        jsonObject.put("sender", sender);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getRoomCreateApprovalReply(String approved, String threadID) {
        // {"type" : "roomcreateapprovalreply", "approved" : "1", "threadid" : "10"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcreateapprovalreply");
        jsonObject.put("approved", approved);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoomRequest(String clientID, String roomID, String formerRoomID, String sender,
            String threadID, String isLocalRoomChange) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "joinroomapprovalrequest");
        jsonObject.put("sender", sender);
        jsonObject.put("roomid", roomID);
        jsonObject.put("former", formerRoomID);
        jsonObject.put("clientid", clientID);
        jsonObject.put("threadid", threadID);
        jsonObject.put("isLocalRoomChange", isLocalRoomChange);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoomApprovalReply(String approved, String threadID, String host, String port) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "joinroomapprovalreply");
        jsonObject.put("approved", approved);
        jsonObject.put("host", host);
        jsonObject.put("port", port);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getMoveJoinRequest(String clientID, String roomID, String formerRoomID, String sender,
            String threadID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "movejoinack");
        jsonObject.put("sender", sender);
        jsonObject.put("roomid", roomID);
        jsonObject.put("former", formerRoomID);
        jsonObject.put("clientid", clientID);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getQuit(String clientID, String formerRoomID) {
        // {"type" : "quit", "clientid" : "Adel"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "quit");
        jsonObject.put("clientid", clientID);
        jsonObject.put("former", formerRoomID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getListRequest(String clientID, String threadID, String sender) {
        // {"type" : "listrequest", "clientid" : "Adel", "sender" : 1, "threadid" : 12 }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "listrequest");
        jsonObject.put("sender", sender);
        jsonObject.put("clientid", clientID);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getListResponse(ArrayList<String> roomIDList, String threadID) {
        // {"type" : "listresponse", "rooms" : ["room-1","MainHall-s1","MainHall-s2"],
        // "threadid" : 12 }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "listresponse");
        jsonObject.put("threadid", threadID);
        jsonObject.put("rooms", roomIDList);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getDeleteRoomRequest(String ownerID, String roomID, String mainHallID) {
        // {"type" : "deleterequest", "owner" : "Adel", "roomid" : "jokes", "mainhall" :
        // "MainHall-s1" }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "deleterequest");
        jsonObject.put("owner", ownerID);
        jsonObject.put("roomid", roomID);
        jsonObject.put("mainhall", mainHallID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject gossipMessage(Integer serverId, HashMap<Integer, Integer> heartbeatCountList) {
        // {"type":"gossip","serverid":"1","heartbeatcountlist":{"1":0,"2":1,"3":1,"4":2}}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "gossip");
        jsonObject.put("serverId", serverId);
        jsonObject.put("heartbeatCountList", heartbeatCountList);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject startVoteMessage(Integer serverId, Integer suspectServerId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "startVote");
        jsonObject.put("serverId", serverId);
        jsonObject.put("suspectServerId", suspectServerId);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject notifyServerDownMessage(Integer serverId) {
        // {"type":"notifyserverdown", "serverid":"s2"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "notifyserverdown");
        jsonObject.put("serverId", serverId);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject answerVoteMessage(Integer suspectServerId, String vote, Integer votedBy) {
        // {"type":"answervote","suspectserverid":"1","vote":"YES", "votedby":"1"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "answervote");
        jsonObject.put("suspectServerId", suspectServerId);
        jsonObject.put("votedBy", votedBy);
        jsonObject.put("vote", vote);
        return jsonObject;
    }
}