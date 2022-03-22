package consensus.election;

import MessagePassing.MessagePassing;
import Model.Constant;
import Server.Server;
import Server.ServerInfo;
import Server.ServerMessage;
import Server.Room;
import Services.Quartz;
import consensus.Leader;
import consensus.election.timeout.*;
import org.json.simple.JSONObject;
import org.quartz.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class FastBullyAlgorithm implements Runnable{

    String option;
    JobExecutionContext jobExecutionContext = null;
    static JSONObject jsonMessage = null;
    static ServerInfo initiatingServerInfo;
    static ServerInfo iAmUpSender;
    static ServerInfo leader;
    protected Scheduler scheduler;

    public FastBullyAlgorithm(String option){
        this.option = option;
        this.scheduler =  Quartz.getInstance().getScheduler();
    }

    public FastBullyAlgorithm(String option, JobExecutionContext jobExecutionContext){
        this.option = option;
        this.scheduler =  Quartz.getInstance().getScheduler();
        this.jobExecutionContext = jobExecutionContext;
    }


    public void startElection(){

        Server.getInstance().initTempCandidateServers();
        Server.getInstance().setAnswerMessageReceived(false);
        Server.getInstance().setOngoingElection(true);
        Server.getInstance().setLeaderUpdateComplete(false);
        Leader.getInstance().reset();

        initiatingServerInfo = Server.getInstance().getSelfServerInfo();
        ConcurrentHashMap<String, ServerInfo> candidateServers = Server.getInstance().getCandidateServers();

        Long electionTimeOut = Server.getInstance().getElectionAnswerTimeout();

        ArrayList<ServerInfo> CandidateServerList = new ArrayList<>();
        for (String serverID : candidateServers.keySet()) {
                CandidateServerList.add(candidateServers.get(serverID));
            }

        MessagePassing.sendServerBroadcast(ServerMessage.electionMessage(initiatingServerInfo.getServerID()),CandidateServerList);
        System.out.println("INFO : election message sent by the server "+Server.getInstance().getServerID());
        startWaitingForAnswerMessage(electionTimeOut);

    }

    public void startWaitingForAnswerMessage(Long timeout) {
        JobDetail answerMsgTimeoutJob =
                JobBuilder.newJob(AnswerMessageTimeout.class).withIdentity
                        ("answer_msg_timeout_job", "group_fast_bully").build();
        startWaitingTimer("group_fast_bully", timeout, answerMsgTimeoutJob);
    }

    public void replyAnswerForElectionMessage(){
        Server.getInstance().setOngoingElection(true);
        Server.getInstance().setLeaderUpdateComplete(false);
        Leader.getInstance().reset();

        String initiatingServerID = jsonMessage.get("serverID").toString();
        System.out.println("INFO : election message from "+ initiatingServerID+" received");
        initiatingServerInfo = Server.getInstance().getOtherServers().get(initiatingServerID);
        ServerInfo selfServerInfo = Server.getInstance().getSelfServerInfo();
        try {
            MessagePassing.sendServer(ServerMessage.answerMessage(selfServerInfo.getServerID()), initiatingServerInfo);
            System.out.println("INFO : answer message sent to "+initiatingServerID);
        } catch (IOException e) {
            System.out.println("WARN : unable to send the answer message to "+initiatingServerID);
//            e.printStackTrace();
        }
        startWaitingForNominationOrCoordinationMessage(Server.getInstance().getElectionNominationTimeout());
    }

    private void startWaitingForNominationOrCoordinationMessage(Long timeout) {
        JobDetail coordinatorMsgTimeoutJob =
                JobBuilder.newJob(NominationMessageTimeout.class).withIdentity
                        ("coordinator_or_nomination_msg_timeout_job", "group_fast_bully").build();
        startWaitingTimer("group_fast_bully", timeout, coordinatorMsgTimeoutJob);
    }

    public void sendNominationMessage(String stroption){
        if(Objects.equals(stroption, "sendNominationAnswerTimeout")){
            ServerInfo highestPriorityCandidate = Server.getInstance().getHighestPriorityCandidate();
            try {
                MessagePassing.sendServer(ServerMessage.nominationMessage(), highestPriorityCandidate);
                System.out.println("INFO : sending nomination to server " + highestPriorityCandidate.getServerID()+" after answer message timeout");
            } catch (IOException e) {
//                e.printStackTrace();
                System.out.println("WARN : unable to send the nomination message to server "+highestPriorityCandidate.getServerID());
                Server.getInstance().removeTempCandidateServer(highestPriorityCandidate);       //NOT SURE WHETHER REMOVING SHOULD BE DONE IN HERE OR GOSSIP?
            }
            startWaitingForCoordinatorMessage(Server.getInstance().getElectionCoordinatorTimeout());
            Server.getInstance().setAnswerMessageReceived(false);
        }
        else if (Objects.equals(stroption, "sendNominationCoordinatorTimeout")){
            ServerInfo highestPriorityCandidate = Server.getInstance().getHighestPriorityCandidate();
            try {
                MessagePassing.sendServer(ServerMessage.nominationMessage(), highestPriorityCandidate);
                System.out.println("INFO : sending nomination to : " + highestPriorityCandidate.getServerID()+" after coordinator or nominator message timeout");
            } catch (IOException e) {
                System.out.println("WARN : unable to send the nomination message to server "+highestPriorityCandidate.getServerID());
                Server.getInstance().removeTempCandidateServer(highestPriorityCandidate);       //NOT SURE WHETHER REMOVING SHOULD BE DONE IN HERE OR GOSSIP?
//                e.printStackTrace();
            }
            resetWaitingForCoordinatorMessageTimer(this.jobExecutionContext, this.jobExecutionContext.getTrigger().getKey(),
                    Server.getInstance().getElectionCoordinatorTimeout());
            this.jobExecutionContext = null;
        }

    }

    public void startWaitingForCoordinatorMessage(Long timeout) {
        JobDetail coordinatorMsgTimeoutJob =
                JobBuilder.newJob(CoordinatorMessageTimeout.class).withIdentity
                        ("coordinator_msg_timeout_job", "group_fast_bully").build();
        startWaitingTimer("group_fast_bully", timeout, coordinatorMsgTimeoutJob);
    }

    private synchronized void sendCoordinatorMessage(String stroption) {
        ServerInfo SelfServerInfo = Server.getInstance().getSelfServerInfo();
        ConcurrentHashMap<String, ServerInfo> lowPriorityServers = Server.getInstance().getLowPriorityServers();
        ArrayList<ServerInfo> lowPriorityServerList = new ArrayList<>();
        for (String serverID : lowPriorityServers.keySet()) {
            lowPriorityServerList.add(lowPriorityServers.get(serverID));
        }
//        Integer serverCount = lowPriorityServerList.size();
        if (Objects.equals(stroption, "coordinator")){
            String leaderServerID = jsonMessage.get("serverID").toString();
            String leaderAddress = jsonMessage.get("address").toString();
            Integer leaderServerPort = Integer.parseInt( jsonMessage.get("serverPort").toString());
            Integer leaderClientPort = Integer.parseInt( jsonMessage.get("clientPort").toString());
            leader = new ServerInfo(leaderServerID, leaderAddress, leaderServerPort, leaderClientPort);

            Server.getInstance().setViewMessageReceived(true);
            Server.getInstance().addTempCandidateServer(leader);
            String selfServerID = SelfServerInfo.getServerID();

            System.out.println("INFO : view message received with leader as "+leaderServerID);
            Integer leadercheck = 0;
            if(Leader.getInstance().getLeaderID() != null){
                leadercheck = Integer.parseInt(Leader.getInstance().getLeaderID());
            }
            if (Integer.parseInt(selfServerID) >= Integer.parseInt(leaderServerID) && Integer.parseInt(selfServerID) >=leadercheck){
                MessagePassing.sendServerBroadcast(ServerMessage.setCoordinatorMessage(SelfServerInfo.getServerID(), SelfServerInfo.getAddress(), SelfServerInfo.getServerPort(), SelfServerInfo.getClientPort()),lowPriorityServerList);
                System.out.println("INFO : coordinator message sent ["+option+"] with leader as "+SelfServerInfo.getServerID());
                acceptNewLeader(SelfServerInfo.getServerID());
            }
            else if (Integer.parseInt(selfServerID) < Integer.parseInt(leaderServerID)){
                acceptNewLeader(leaderServerID);
            }
            stopWaitingForViewMessage();
        }
        else if (Objects.equals(stroption, "coordinatorAnswerTimeout") || Objects.equals(stroption, "coordinatorViewTimeout") || Objects.equals(stroption, "coordinatorFromNomination")){
            MessagePassing.sendServerBroadcast(ServerMessage.setCoordinatorMessage(SelfServerInfo.getServerID(), SelfServerInfo.getAddress(), SelfServerInfo.getServerPort(), SelfServerInfo.getClientPort()),lowPriorityServerList);
            System.out.println("INFO : coordinator message sent ["+stroption+"] with leader as "+SelfServerInfo.getServerID());
            acceptNewLeader(SelfServerInfo.getServerID());
            if (Objects.equals(stroption, "coordinatorAnswerTimeout") || Objects.equals(stroption, "coordinatorFromNomination")){
                stopElection();
            }
            else{
                Server.getInstance().setViewMessageReceived(false);
            }
        }
    }

    public synchronized void acceptNewLeader(String serverID) {
        Leader.getInstance().setLeaderID(serverID);
        Server.getInstance().setOngoingElection(false);
        Server.getInstance().setViewMessageReceived(false);
        Server.getInstance().setAnswerMessageReceived(false);
        System.out.println("INFO : accepting new leader server "+serverID);
//        System.out.println(serverCount);


        ArrayList<Room> roomList = new ArrayList<>();
        roomList.addAll(Server.getInstance().getRoomList().values());
        if(Integer.parseInt(Server.getInstance().getServerID()) == Integer.parseInt(serverID)) {
            Leader.getInstance().updateLeader(Server.getInstance().getServerID(), Server.getInstance().getClientIDList(), roomList);
            Server.getInstance().setLeaderUpdateComplete(true);
        }
        else{
            if(Integer.parseInt(Server.getInstance().getServerID()) < Integer.parseInt(serverID)){
                Leader.getInstance().reset();
            }
            try {
                MessagePassing.sendToLeader(ServerMessage.leaderUpdate(Server.getInstance().getServerID(), Server.getInstance().getClientIDList(), roomList));
                System.out.println("INFO : send information to new leader "+serverID);
//                startWaitingForUpdateCompleteMessage(50L);
            } catch (IOException e) {
                System.out.println("WARN : unable to send information to "+serverID);
//                e.printStackTrace();
            }
        }
//                Leader.getInstance().updateLeader(Server.getInstance().getServerID(), Server.getInstance().getClientIDList(), roomList);
        }

    public void startWaitingForUpdateCompleteMessage(Long timeout) {
        JobDetail updateCompleteTimeoutJob =
                JobBuilder.newJob(UpdateCompleteTimeout.class).withIdentity
                        ("update_complete_timeout_job", "group_fast_bully").build();
        startWaitingTimer("group_fast_bully", timeout, updateCompleteTimeoutJob);
    }

    private void restartElection() {
        stopElection();
        startElection();
    }

    public void stopElection() {

        Server.getInstance().initTempCandidateServers();
        Server.getInstance().setOngoingElection(false);

        stopWaitingForAnswerMessage();
        stopWaitingForCoordinatorMessage();
        stopWaitingForNominationMessage();
        stopWaitingForViewMessage();
    }

    public void stopWaitingForAnswerMessage() {
        JobKey answerMsgTimeoutJobKey = new JobKey("answer_msg_timeout_job", "group_fast_bully");
        stopWaitingTimer(answerMsgTimeoutJobKey);
    }

    public void stopWaitingForNominationMessage() {
        JobKey answerMsgTimeoutJobKey = new JobKey("coordinator_or_nomination_msg_timeout_job", "group_fast_bully");
        stopWaitingTimer(answerMsgTimeoutJobKey);
    }

    public void stopWaitingForCoordinatorMessage() {
        JobKey coordinatorMsgTimeoutJobKey = new JobKey("coordinator_msg_timeout_job", "group_fast_bully");
        stopWaitingTimer(coordinatorMsgTimeoutJobKey);
    }

    public void stopWaitingForViewMessage() {
        JobKey viewMsgTimeoutJobKey = new JobKey("view_msg_timeout_job", "group_fast_bully");
        stopWaitingTimer(viewMsgTimeoutJobKey);
    }

    public void stopWaitingForUpdateCompleteMessage() {
        JobKey answerMsgTimeoutJobKey = new JobKey("update_complete_timeout_job", "group_fast_bully");
        stopWaitingTimer(answerMsgTimeoutJobKey);
    }

    private synchronized void startWaitingTimer(String groupId, Long timeout, JobDetail jobDetail) {
        try {

            System.out.println(String.format("LOG  : Starting the waiting job [%s] : %s",
                    scheduler.getSchedulerName(), jobDetail.getKey()));

            if (scheduler.checkExists(jobDetail.getKey())) {

                System.out.println(String.format("LOG  : Job get trigger again [%s]", jobDetail.getKey().getName()));
                scheduler.triggerJob(jobDetail.getKey());

            } else {
                SimpleTrigger simpleTrigger =
                        (SimpleTrigger) TriggerBuilder.newTrigger()
                                .withIdentity(Constant.ELECTION_TRIGGER, groupId)
                                .startAt(DateBuilder.futureDate(Math.toIntExact(timeout), DateBuilder.IntervalUnit.SECOND))
                                .build();
                scheduler.start();
                scheduler.scheduleJob(jobDetail, simpleTrigger);
            }

        } catch (ObjectAlreadyExistsException oe) {

            try {

                System.out.println(String.format("Job get trigger again [%s]", jobDetail.getKey().getName()));
                scheduler.triggerJob(jobDetail.getKey());

                //System.err.println(Arrays.toString(scheduler.getTriggerKeys(GroupMatcher.anyGroup()).toArray()));
                // [DEFAULT.MT_e8f718prrj3ol, group1.GOSSIPJOBTRIGGER, group1.CONSENSUSJOBTRIGGER, group_fast_bully.ELECTION_TRIGGER]

            } catch (SchedulerException e) {
//                System.out.println("WARN : scheduler exception");
//                e.printStackTrace();
            }

        } catch (SchedulerException e) {
//            System.out.println("WARN : scheduler exception");
//            e.printStackTrace();
        }
    }

    public void stopWaitingTimer(JobKey jobKey) {
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.interrupt(jobKey);
                //scheduler.deleteJob(jobKey);
                System.out.println(String.format("LOG  : Job [%s] get interrupted from [%s]",
                        jobKey, scheduler.getSchedulerName()));
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is Boot time only election timer job. In main
     */
    public void startWaitingForViewMessage(Long electionAnswerTimeout) throws SchedulerException {
        JobDetail coordinatorMsgTimeoutJob =
                JobBuilder.newJob(ViewMessageTimeout.class).withIdentity
                        ("view_msg_timeout_job", "group_fast_bully").build();
        startWaitingTimer("group_fast_bully", electionAnswerTimeout, coordinatorMsgTimeoutJob);
    }

    public void sendIamUpMessage() {
        Server.getInstance().setOngoingElection(true);
        Leader.getInstance().reset();
        ServerInfo selfServerInfo = Server.getInstance().getSelfServerInfo();
        ConcurrentHashMap<String, ServerInfo> otherServers = Server.getInstance().getOtherServers();
        ArrayList<ServerInfo> otherServersList = new ArrayList<>();
        for (String serverID : otherServers.keySet()) {
            otherServersList.add(otherServers.get(serverID));
        }
        MessagePassing.sendServerBroadcast(ServerMessage.iAmUpMessage(selfServerInfo.getServerID(), selfServerInfo.getAddress(),
                selfServerInfo.getServerPort(), selfServerInfo.getClientPort()),otherServersList);
        System.out.println("INFO : IamUp messages sent");
        try {
            startWaitingForViewMessage(Server.getInstance().getElectionAnswerTimeout());
        } catch (SchedulerException e) {
            System.out.println("WARN : error while waiting for the view message at fast bully election: " +
                    e.getLocalizedMessage());
        }

    }

    private synchronized void sendViewMessage() {
        String senderServerID = jsonMessage.get("serverID").toString();
        String senderAddress = jsonMessage.get("address").toString();
        Integer senderServerPort = Integer.parseInt( jsonMessage.get("serverPort").toString());
        Integer senderClientPort = Integer.parseInt( jsonMessage.get("clientPort").toString());
        iAmUpSender = new ServerInfo(senderServerID, senderAddress, senderServerPort, senderClientPort);

        Server.getInstance().addTempCandidateServer(iAmUpSender);
        if(Leader.getInstance().getLeaderID() == null){
            try {
                MessagePassing.sendServer(ServerMessage.viewMessage(senderServerID, senderAddress, senderServerPort, senderClientPort), iAmUpSender);
                System.out.println("INFO : view message sent to "+senderServerID+" with leader as "+senderServerID);
            } catch (IOException e) {
                System.out.println("WARN : unable to send the view message to server "+senderServerID);
//                e.printStackTrace();
            }
        }
        else{
            try {
                if (Objects.equals(Server.getInstance().getSelfServerInfo().getServerID(), Leader.getInstance().getLeaderID())){
                    leader = Server.getInstance().getSelfServerInfo();
                }
                else {
                    leader = Server.getInstance().getOtherServers().get(Leader.getInstance().getLeaderID());
                }
                MessagePassing.sendServer(ServerMessage.viewMessage(leader.getServerID(), leader.getAddress(), leader.getServerPort(), leader.getClientPort()), iAmUpSender);
                System.out.println("INFO : view message sent to "+senderServerID+" with leader as "+leader.getServerID());
            } catch (IOException e) {
                System.out.println("WARN : unable to send the view message to server "+senderServerID);
//                e.printStackTrace();
            }
        }
    }

    public void resetWaitingForCoordinatorMessageTimer(JobExecutionContext context, TriggerKey triggerKey, Long timeout) {
        try {
            JobDetail jobDetail = context.getJobDetail();
            if (scheduler.checkExists(jobDetail.getKey())) {

                System.out.println(String.format("Job get trigger again [%s]", jobDetail.getKey().getName()));
                scheduler.triggerJob(jobDetail.getKey());

            } else {

                Trigger simpleTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("election_trigger", "group_fast_bully")
                        .startAt(DateBuilder.futureDate(Math.toIntExact(timeout), DateBuilder.IntervalUnit.SECOND))
                        .build();
                context.getScheduler().rescheduleJob(triggerKey, simpleTrigger);
            }

        } catch (ObjectAlreadyExistsException oe) {
            System.out.println(oe.getLocalizedMessage());

            try {

                JobDetail jobDetail = context.getJobDetail();
                System.out.println(String.format("Job get trigger again [%s]", jobDetail.getKey().getName()));

                scheduler.triggerJob(jobDetail.getKey());

            } catch (SchedulerException e) {
                e.printStackTrace();
            }

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void updateLeader(){
        stopElection();
        Server.getInstance().setViewMessageReceived(true);
        Server.getInstance().setLeaderUpdateComplete(false);
        String leaderServerID = jsonMessage.get("serverID").toString();
        Integer leadercheck = 0;
        if(Leader.getInstance().getLeaderID() != null){
            leadercheck = Integer.parseInt(Leader.getInstance().getLeaderID());
        }
        if(Integer.parseInt(leaderServerID) >= leadercheck){
            String leaderAddress = jsonMessage.get("address").toString();
            Integer leaderServerPort = Integer.parseInt( jsonMessage.get("serverPort").toString());
            Integer leaderClientPort = Integer.parseInt( jsonMessage.get("clientPort").toString());
            leader = new ServerInfo(leaderServerID, leaderAddress, leaderServerPort, leaderClientPort);
            acceptNewLeader(leaderServerID);
        }
    }

    @Override
    public void run() {
        switch (option){
            case "start_election":
                startElection();
                break;
            case "election":
                replyAnswerForElectionMessage();
                break;
            case "sendNominationAnswerTimeout":
            case "sendNominationCoordinatorTimeout":
                sendNominationMessage(option);
                break;
            case "coordinator":
            case "coordinatorAnswerTimeout":
            case "coordinatorViewTimeout":
            case "coordinatorFromNomination":
                sendCoordinatorMessage(option);
                break;
            case "IamUp":
                sendIamUpMessage();
                break;
            case "sendView":
                sendViewMessage();
                break;
            case "restart_election":
                restartElection();
                break;
            case "updateLeader":
                updateLeader();
                break;
        }

    }

    public static void receiveMessage(JSONObject jsonObject) {
        String msgOption = jsonObject.get("option").toString();
        jsonMessage = jsonObject;
        switch(msgOption){
            case "election":
                FastBullyAlgorithm electionFBA = new FastBullyAlgorithm("election");
                new Thread(electionFBA).start();
//                electionFBA.replyAnswerForElectionMessage();
                break;
            case "answer":
                String answerServerID = jsonObject.get("serverID").toString();
                Server.getInstance().setAnswerMessageReceived(true);
                ServerInfo answerServerInfo = Server.getInstance().getCandidateServers().get(answerServerID);
                Server.getInstance().addTempCandidateServer(answerServerInfo);
                System.out.println("INFO : answer message from "+ answerServerID+" received");
                break;
            case "nomination":
                FastBullyAlgorithm nominationFBA = new FastBullyAlgorithm("coordinatorFromNomination");
                new Thread(nominationFBA).start();
//                nominationFBA.sendCoordinatorMessage(nominationFBA.option);
                break;
            case "IamUp":
                FastBullyAlgorithm sendViewFBA = new FastBullyAlgorithm("sendView");
                new Thread(sendViewFBA).start();
//                sendViewFBA.sendViewMessage();
                break;
            case "view":
//                while(!Server.getInstance().getOngoingElection()){
//                    continue;
//                }
//                if(Server.getInstance().getOngoingElection()){
                FastBullyAlgorithm coordinatorFBA = new FastBullyAlgorithm("coordinator");
                new Thread(coordinatorFBA).start();
//                coordinatorFBA.sendCoordinatorMessage(coordinatorFBA.option);
//                }
                break;
            case "coordinator":
                FastBullyAlgorithm updateLeaderFBA = new FastBullyAlgorithm("updateLeader");
                new Thread(updateLeaderFBA).start();
                break;
        }
    }

    public static void initialize(){
        FastBullyAlgorithm startFBA = new FastBullyAlgorithm("start_election");
        new Thread(startFBA).start();
//        startFBA.startElection();
    }
}
