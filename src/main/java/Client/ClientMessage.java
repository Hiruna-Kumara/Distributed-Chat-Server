package Client;

import org.json.simple.JSONObject;
import java.util.*;

public class ClientMessage {

    @SuppressWarnings("unchecked")
    public static JSONObject newIdentityReply(String approve) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "newidentity");
        jsonObject.put("approved", approve);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject roomChangeReply(String clientID, String formerRoomID, String newRoomID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity", clientID);
        jsonObject.put("former", formerRoomID);
        jsonObject.put("roomid", newRoomID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject listReply(List<String> rooms) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomlist");
        jsonObject.put("rooms", rooms);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject whoReply(String roomID, List<String> identities, String owner) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcontents");
        jsonObject.put("roomid", roomID);
        jsonObject.put("identities", identities);
        jsonObject.put("owner", owner);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject createRoomReply(String roomID, String approved) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "createroom");
        jsonObject.put("roomid", roomID);
        jsonObject.put("approved", approved);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject routeReply(String roomID, String host, String port) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "route");
        jsonObject.put("roomid", roomID);
        jsonObject.put("host", host);
        jsonObject.put("port", port);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject serverChangeReply(String approved, String serverID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "serverchange");
        jsonObject.put("approved", approved);
        jsonObject.put("serverid", serverID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject deleteRoomReply(String roomID, String approved) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "deleteroom");
        jsonObject.put("roomid", roomID);
        jsonObject.put("approved", approved);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject broadcastMessage(String clientID, String content) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "message");
        jsonObject.put("identity", clientID);
        jsonObject.put("content", content);
        return jsonObject;
    }



}
