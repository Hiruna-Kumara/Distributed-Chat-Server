package messaging;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MessageTransfer {
    public static JSONObject convertToJson(String jsonString) {
        JSONObject j_object = null;
        try {
            JSONParser jsonParser = new JSONParser();
            Object object = jsonParser.parse(jsonString);
            j_object = (JSONObject) object;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return j_object;
    }

    // check the existence of a key in json object
    public static boolean hasKey(JSONObject jsonObject, String key) {
        return (jsonObject != null && jsonObject.get(key) != null);
    }

    public static void send(JSONObject obj, Server destServer) throws IOException {
        Socket socket = new Socket(destServer.getServerAddress(),
                destServer.getCoordinationPort());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }
}
