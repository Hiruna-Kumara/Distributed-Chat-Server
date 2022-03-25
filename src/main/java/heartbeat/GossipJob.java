package heartbeat;

import consensus.Leader;
import consensus.election.FastBullyAlgorithm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import Server.ServerInfo;
import Server.ServerMessage;
import Server.Server;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import MessagePassing.MessagePassing;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;


public class GossipJob implements Job{

    private Server serverState = Server.getInstance();
    private Leader leaderState = Leader.getInstance();
    private ServerMessage serverMessage = ServerMessage.getInstance();
    //git

    private static final Logger LOG = LogManager.getLogger(GossipJob.class);
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException{

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String aliveErrorFactor = dataMap.get("aliveErrorFactor").toString();

        // Update heartbeat vector and get the suspect list of each server
        for (ServerInfo serverInfo : serverState.getAllServers().values()){
            Integer serverId = serverInfo.getServerIdInt();
            Integer myServerId = serverState.getSelfIdInt();

            // get current heart beat count of a server
            Integer count = serverState.getHeartbeatCountList().get(serverId);

            // first update heart beat count vector
            if (serverId.equals(myServerId)) {
                serverState.getHeartbeatCountList().put(serverId, 0); // reset my own heartbeat count
            } 
            else {
                if (count == null) {
                    serverState.getHeartbeatCountList().put(serverId, 1);
                } 
                else {
                    serverState.getHeartbeatCountList().put(serverId, count + 1);
                }
            }

            // FIX get the fresh updated current count again
            count = serverState.getHeartbeatCountList().get(serverId);

            if (count != null) {
                // if heart beat count is more than error factor
                if (count > Integer.parseInt(aliveErrorFactor)) {
                    serverState.getSuspectList().put(serverId, "SUSPECTED");
                }
                else {
                    serverState.getSuspectList().put(serverId, "NOT_SUSPECTED");
                }
            }
        }

        if (serverState.getLeaderUpdateComplete()){
            
            Integer leaderServerId = leaderState.getLeaderIDInt();
            // System.out.println("Current coordinator is : " + leaderState.getLeaderID().toString());
            LOG.info("Current coordinator is : " + leaderState.getLeaderID().toString());
            
            // if the leader/coordinator server is in suspect list, start the election process
            if (serverState.getSuspectList().get(leaderServerId).equals("SUSPECTED")) {
            
                //initiate an election
                FastBullyAlgorithm.initialize();
            }
        }

        // finally gossip about heart beat vector to a next peer
        int numOfServers = serverState.getAllServers().size();
        // Required at least 2 servers for gossiping
        if (numOfServers > 1) { 

            // after updating the heartbeatCountList, randomly select a server and send
            Integer serverIndex = ThreadLocalRandom.current().nextInt(numOfServers - 1);
            ArrayList<ServerInfo> remoteServer = new ArrayList<>();

            for (ServerInfo server : serverState.getAllServers().values()) {
                Integer serverId = server.getServerIdInt();
                Integer myServerId = serverState.getSelfIdInt();
                if (!serverId.equals(myServerId)) {
                    remoteServer.add(server);
                }
            }

            // change concurrent hashmap to hashmap before sending
            HashMap<Integer, Integer> heartbeatCountList = new HashMap<>(serverState.getHeartbeatCountList());
            JSONObject gossipMessage = new JSONObject();
            gossipMessage = serverMessage.gossipMessage(serverState.getSelfIdInt(), heartbeatCountList);
            try {
                MessagePassing.sendServer(gossipMessage,remoteServer.get(serverIndex));
                // System.out.println("INFO : Gossip heartbeat info to next peer s"+remoteServer.get(serverIndex).getServerID());
                LOG.info("Gossip heartbeat info to next peer s"+remoteServer.get(serverIndex).getServerID());
            } 
            catch (Exception e){
                // System.out.println("WARN : Server s"+remoteServer.get(serverIndex).getServerID() + " has failed");
                LOG.warn("Server s"+ remoteServer.get(serverIndex).getServerID() + " has failed");
            }
        }

    }

    public static void receiveMessages(JSONObject j_object) {

        Server serverState = Server.getInstance();

        HashMap<String, Long> gossipFromOthers = (HashMap<String, Long>) j_object.get("heartbeatCountList");
        Integer fromServer = (int) (long)j_object.get("serverId");

        // System.out.println(("Receiving gossip from server: [" + fromServer.toString() + "] gossipping: " + gossipFromOthers));
        LOG.info("Receiving gossip from server: [" + fromServer.toString() + "] gossipping: " + gossipFromOthers);

        //update the heartbeatcountlist by taking minimum
        for (String serverId : gossipFromOthers.keySet()) {
            Integer localHeartbeatCount = serverState.getHeartbeatCountList().get(Integer.parseInt(serverId));
            Integer remoteHeartbeatCount = (int) (long)gossipFromOthers.get(serverId);
            if (localHeartbeatCount != null && remoteHeartbeatCount < localHeartbeatCount) {
                serverState.getHeartbeatCountList().put(Integer.parseInt(serverId), remoteHeartbeatCount);
            }
        }

        // System.out.println(("Current cluster heart beat state is: " + serverState.getHeartbeatCountList()));
        LOG.info("Current cluster heart beat state is: " + serverState.getHeartbeatCountList());

        if (serverState.getLeaderUpdateComplete() && Leader.getInstance().getLeaderID().equals(serverState.getServerID())) {
            if (serverState.getHeartbeatCountList().size() < gossipFromOthers.size()) {
                for (String serverId : gossipFromOthers.keySet()) {
                    if (!serverState.getHeartbeatCountList().containsKey(Integer.parseInt(serverId))) {
                        serverState.getSuspectList().put(Integer.parseInt(serverId), "SUSPECTED");
                    }
                }
            }
        }

    }
}

