package messaging;

import org.json.simple.JSONObject;

public class ServerMessage
{
    @SuppressWarnings("unchecked")
    public static JSONObject getHeartbeat( String sender) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "heartbeat");
        jsonObject.put("sender", sender);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getElection(String source) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "election");
        jsonObject.put("source", source);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getCoordinator(String leader) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "coordinator");
        jsonObject.put("leader", leader);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getOk(String sender) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "ok");
        jsonObject.put("sender", sender);
        return jsonObject;
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
    public static JSONObject getClientIdApprovalReply(String approved, String threadID) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "clientidapprovalreply");
        jsonObject.put("approved", approved);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getRoomCreateApprovalRequest(String clientID, String roomID, String sender, String threadID) {
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcreateapprovalreply");
        jsonObject.put("approved", approved);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoomRequest(String clientID, String roomID, String formerRoomID, String sender, String threadID, String isLocalRoomChange) {
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
    public static JSONObject getMoveJoinRequest(String clientID, String roomID, String formerRoomID, String sender, String threadID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "movejoinack");
        jsonObject.put("sender", sender);
        jsonObject.put("roomid", roomID);
        jsonObject.put("former", formerRoomID);
        jsonObject.put("clientid", clientID);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }
}