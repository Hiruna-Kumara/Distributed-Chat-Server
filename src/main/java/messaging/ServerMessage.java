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
}