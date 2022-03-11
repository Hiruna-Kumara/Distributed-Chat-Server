package messaging;

import org.json.simple.JSONObject;

import java.util.*;

public class ClientMessage {

    @SuppressWarnings("unchecked")
    public static JSONObject getApprovalNewID(String approve) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "newidentity");
        jsonObject.put("approved", approve);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoomOnCreate(String clientID, String MainHall) {
        return getJoinRoom(clientID, "", MainHall);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoom(String clientID, String formerRoomID, String roomID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity", clientID);
        jsonObject.put("former", formerRoomID);
        jsonObject.put("roomid", roomID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getCreateRoom(String roomID, String approve) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "createroom");
        jsonObject.put("roomid", roomID);
        jsonObject.put("approved", approve);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getRoute(String roomID, String host, String port) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "route");
        jsonObject.put("roomid", roomID);
        jsonObject.put("host", host);
        jsonObject.put("port", port);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getCreateRoomChange(String clientID, String former, String roomID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity", clientID);
        jsonObject.put("former", former);
        jsonObject.put("roomid", roomID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getWho(String roomID, List<String> participants, String id) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcontents");
        jsonObject.put("roomid", roomID);
        jsonObject.put("identities", participants);
        jsonObject.put("owner", id);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getList(List<String> rooms) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomlist");
        jsonObject.put("rooms", rooms);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getDeleteRoom(String roomID, String isApproved) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "deleteroom");
        jsonObject.put("roomid", roomID);
        jsonObject.put("approved", isApproved);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getMessage(String id, String content) {
        JSONObject join = new JSONObject();
        join.put("type", "message");
        join.put("identity", id);
        join.put("content", content);
        return join;
    }


    @SuppressWarnings("unchecked")
    public static JSONObject getClientIdApprovalRequest(String clientID, String sender, String threadID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "clientidapprovalrequest");
        jsonObject.put("clientid", clientID);
        jsonObject.put("sender", sender);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getClientIdApprovalReply(String clientID, String approved, String threadID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "clientidapprovalreply");
        jsonObject.put("clientid", clientID);
        jsonObject.put("approved", approved);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

}
