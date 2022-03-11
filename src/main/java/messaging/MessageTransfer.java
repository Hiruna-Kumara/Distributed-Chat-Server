package messaging;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.Server;
import consensus.LeaderState;
import server.ServerState;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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

    //check validity of the ID
    public static boolean checkID(String id) {
        return (Character.toString(id.charAt(0)).matches("[a-zA-Z]+")
                && id.matches("[a-zA-Z0-9]+") && id.length() >= 3 && id.length() <= 16);

    }

    //send broadcast message
    public static void sendBroadcast(JSONObject obj, ArrayList<Socket> socketList) throws IOException {
        for (Socket each : socketList) {
            Socket TEMP_SOCK = (Socket) each;
            PrintWriter TEMP_OUT = new PrintWriter(TEMP_SOCK.getOutputStream());
            TEMP_OUT.println(obj);
            TEMP_OUT.flush();
        }
    }
    //send message to client
    public static void sendClient(JSONObject obj, Socket socket) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }


    //send message to server
    public static void sendServer( JSONObject obj, Server destServer) throws IOException{

    Socket socket = new Socket(destServer.getServerAddress(),
                destServer.getCoordinationPort());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }

    //send message to leader server
    public static void sendToLeader(JSONObject obj) throws IOException
    {
        Server destServer = ServerState.getInstance().getServers()
                .get( LeaderState.getInstance().getLeaderID() );
        Socket socket = new Socket(destServer.getServerAddress(),
                destServer.getCoordinationPort());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }

    public static void sendRooms(JSONObject obj, Server destServer) throws IOException {
        Socket socket = new Socket(destServer.getServerAddress(),
                destServer.getCoordinationPort());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }
}
