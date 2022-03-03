package server;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class Server implements Runnable{

    private DataOutputStream out;
    private ServerSocket Server;
    private static List<String> clientList = new ArrayList<String>();
    private String serverID;

    public Server(String id){
        this.serverID = id;
    }


        private boolean hasKey(JSONObject jsonObject, String key) {
        return (jsonObject != null && jsonObject.get(key) != null);
    }

    private boolean checkID(String id) {
        return (Character.toString(id.charAt(0)).matches("[a-zA-Z]+") && id.matches("[a-zA-Z0-9]+") && id.length() >= 3 && id.length() <= 16);
    }

    private void send(JSONObject obj) throws IOException {
//        System.out.println(obj.toJSONString()+"\n");
        out.write((obj.toJSONString() + "\n").getBytes("UTF-8"));
        out.flush();
    }

    private void messageSend(Socket socket, String msg) throws IOException {
        JSONObject sendToClient = new JSONObject();
        String[] array = msg.split(" ");
        if (array[0].equals("newid")){
            sendToClient = Message.getApprovalNewID(array[1]);
            send(sendToClient);
        } if (array[0].equals("roomchange")){
            sendToClient = Message.getRoomChange(array[1],array[2]);
            send(sendToClient);
        }
    }

    private void newID(String id, Socket connected, String fromclient) throws IOException{
        if (checkID(id) && !clientList.contains(id)) {
            System.out.println("Recieved correct ID type::" + fromclient);
            clientList.add(id);
            synchronized (connected) {
                messageSend(connected,"newid true");
                messageSend(connected,"roomchange "+id+" MainHall-"+serverID);
            }
        } else {
            System.out.println("Recieved wrong ID type:: The identity must be an alphanumeric string starting with an upper or lower case character. It must be at least 3 characters and no more than 16 characters long.");
            messageSend(connected,"newid false");
        }
    }

    @Override
    public void run() {
        String fromclient;

        try {
            Server = new ServerSocket(5000);
            System.out.println(Server.getInetAddress());
            System.out.println(Server.getLocalSocketAddress());
            System.out.println(Server.getLocalPort());

            System.out.println("TCPServer Waiting for client on port 5000"); //client should use 5000 as port

            while (true) {
                Socket connected = Server.accept();
                System.out.println(" THE CLIENT" + " " + connected.getInetAddress()
                        + ":" + connected.getPort() + " IS CONNECTED ");

                BufferedReader inFromClient = new BufferedReader(
                        new InputStreamReader(connected.getInputStream(), StandardCharsets.UTF_8));

                out = new DataOutputStream(connected.getOutputStream());

                boolean close = false;

                while (!close) {

                    fromclient = inFromClient.readLine();

                    //convert received message to json object

                    try {
                        Object object = null;
                        JSONParser jsonParser = new JSONParser();
                        object = jsonParser.parse(fromclient);
                        JSONObject j_object = (JSONObject) object;

                        if (hasKey(j_object, "type")) {
                            //check new identity format
                            if (j_object.get("type").equals("newidentity") && j_object.get("identity") != null) {
                                String id = j_object.get("identity").toString();
                                newID(id, connected, fromclient);
                            }
                        } else {
                            System.out.println("Something went wrong");
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                connected.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}