package heartbeat;

import consensus.BullyAlgorithm;
import org.json.simple.JSONObject;
import server.ServerState;
import messaging.ServerMessage;
import server.Server;
import consensus.LeaderState;
import messaging.MessageTransfer;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class GossipJob implements Job{

    private ServerState serverState = ServerState.getInstance();
    private LeaderState leaderState = LeaderState.getInstance();
    private ServerMessage serverMessage = ServerMessage.getInstance();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException{

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String aliveErrorFactor = dataMap.get("aliveErrorFactor").toString();

        // first work on heart beat vector and suspect failure server list

        for (Server serverInfo : serverState.getServers().values()){
            Integer serverId = serverInfo.getServerID();
            Integer myServerId = serverState.getSelfID();

            // get current heart beat count of a server
            Integer count = serverState.getHeartbeatCountList().get(serverId);

            // first update heart beat count vector
            if (serverId.equals(myServerId)) {
                serverState.getHeartbeatCountList().put(serverId, 0); // reset my own vector always
            } else {
                // up count all others
                if (count == null) {
                    serverState.getHeartbeatCountList().put(serverId, 1);
                } else {
                    serverState.getHeartbeatCountList().put(serverId, count + 1);
                }
            }

            // FIX get the fresh updated current count again
            count = serverState.getHeartbeatCountList().get(serverId);

            if (count != null) {
                // if heart beat count is more than error factor
                if (count > Integer.parseInt(aliveErrorFactor)) {
                    serverState.getSuspectList().put(serverId, "SUSPECTED");
                } else {
                    serverState.getSuspectList().put(serverId, "NOT_SUSPECTED");
                }
            }

        }

        // finally gossip about heart beat vector to a next peer

        int numOfServers = serverState.getServers().size();

        if (numOfServers > 1) { // Gossip required at least 2 servers to be up

            // after updating the heartbeatCountList, randomly select a server and send
            Integer serverIndex = ThreadLocalRandom.current().nextInt(numOfServers - 1);
            ArrayList<Server> remoteServer = new ArrayList<>();
            for (Server server : serverState.getServers().values()) {
                Integer serverId = server.getServerID();
                Integer myServerId = serverState.getSelfID();
                if (!serverId.equals(myServerId)) {
                    remoteServer.add(server);
                }
            }
//            Collections.shuffle(remoteServer, new Random(System.nanoTime())); // another way of randomize the list

            // change concurrent hashmap to hashmap before sending
            HashMap<Integer, Integer> heartbeatCountList = new HashMap<>(serverState.getHeartbeatCountList());
            JSONObject gossipMessage = new JSONObject();
            gossipMessage = serverMessage.gossipMessage(serverState.getSelfID(), heartbeatCountList);
            try {
                MessageTransfer.sendServer(gossipMessage,remoteServer.get(serverIndex));
                System.out.println("INFO : Gossip heartbeat info to next peer s"+remoteServer.get(serverIndex).getServerID());
            } catch (Exception e){
                System.out.println("WARN : Server s"+remoteServer.get(serverIndex).getServerID() +
                        " has failed");
            }

        }

    }

    public static void receiveMessages(JSONObject j_object) {

        ServerState serverState = ServerState.getInstance();

        HashMap<String, Long> gossipFromOthers = (HashMap<String, Long>) j_object.get("heartbeatCountList");
        Integer fromServer = (int) (long)j_object.get("serverId");

        System.out.println(("Receiving gossip from server: [" + fromServer.toString() + "] gossipping: " + gossipFromOthers));

        //update the heartbeatcountlist by taking minimum
        for (String serverId : gossipFromOthers.keySet()) {
            Integer localHeartbeatCount = serverState.getHeartbeatCountList().get(Integer.parseInt(serverId));
            Integer remoteHeartbeatCount = (int) (long)gossipFromOthers.get(serverId);
            if (localHeartbeatCount != null && remoteHeartbeatCount < localHeartbeatCount) {
                serverState.getHeartbeatCountList().put(Integer.parseInt(serverId), remoteHeartbeatCount);
            }
        }

        System.out.println(("Current cluster heart beat state is: " + serverState.getHeartbeatCountList()));

        if (LeaderState.getInstance().isLeaderElected() && LeaderState.getInstance().getLeaderID().equals(serverState.getSelfID())) {
            if (serverState.getHeartbeatCountList().size() < gossipFromOthers.size()) {
                for (String serverId : gossipFromOthers.keySet()) {
                    if (!serverState.getHeartbeatCountList().containsKey(serverId)) {
                        serverState.getSuspectList().put(Integer.parseInt(serverId), "SUSPECTED");
                    }
                }
            }
        }

    }
}

