package server;

// import client.ClientHandlerThread;
// import consensus.BullyAlgorithm;
// import heartbeat.GossipJob;
// import heartbeat.ConsensusJob;
// import model.Constant;

// import org.quartz.*;
// import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    private static Integer alive_interval = 3;
    private static Integer alive_error_factor = 5;
    private static Boolean isGossip = true;
    private static Integer consensus_interval=10;
    private static Integer consensus_vote_duration=5;

    public static void main(String[] args) {

        //System.out.println("INFO : Enter server ID (ex- s1):  ");
        //Scanner scanner = new Scanner(System.in);
        //String serverID = scanner.nextLine();  // Read user input
        System.out.println("LOG  : ARG[0] = " + args[0] + " ARG[1] = '" + args[1] + "'");
        ServerState.getInstance().initializeWithConfigs(args[0], args[1]);

        System.out.println("LOG  : ------server started------");

        try {
            // throw exception if invalid server id provided
            if( ServerState.getInstance().getServerAddress() == null ) {
                throw new IllegalArgumentException();
            }

            /**
             Coordination socket
             **/
            // server socket for coordination
            ServerSocket serverCoordinationSocket = new ServerSocket();

            // bind SocketAddress with inetAddress and port
            SocketAddress endPointCoordination = new InetSocketAddress(
                    "0.0.0.0",//ServerState.getInstance().getServerAddress()
                    ServerState.getInstance().getCoordinationPort()
            );
            serverCoordinationSocket.bind( endPointCoordination );
            System.out.println( serverCoordinationSocket.getLocalSocketAddress() );
            System.out.println( "LOG  : TCP Server waiting for coordination on port " +
                                        serverCoordinationSocket.getLocalPort() ); // port open for coordination

            /**
             Client socket
             **/
            // server socket for clients
            ServerSocket serverClientsSocket = new ServerSocket();

            // bind SocketAddress with inetAddress and port
            SocketAddress endPointClient = new InetSocketAddress(
                    "0.0.0.0",//ServerState.getInstance().getServerAddress()
                    ServerState.getInstance().getClientsPort()
            );
            serverClientsSocket.bind(endPointClient);
            System.out.println(serverClientsSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for clients on port "+
                    serverClientsSocket.getLocalPort()); // port open for clients

            /**
             Handle coordination
             **/
            ServerHandler serverHandlerThread = new ServerHandler( serverCoordinationSocket );
            // starting the thread
            serverHandlerThread.start();

            /**
             Maintain consensus using Bully Algorithm
             **/
            // BullyAlgorithm.initialize();

            // Runnable heartbeat = new BullyAlgorithm("Heartbeat");
            // new Thread(heartbeat).start();

            /**
             Heartbeat detection using gossiping
             **/
//            startGossip();
//            Runnable gossip = new GossipJob();
//            new Thread(gossip).start();

            // if (isGossip) {
            //     System.out.println("INFO : Failure Detection is running GOSSIP mode");
            //     startGossip();
            //     startConsensus();
            // }


            /**
             Handle clients
             **/
            // while (true) {
            //     Socket clientSocket = serverClientsSocket.accept();
            //     ClientHandlerThread clientHandlerThread = new ClientHandlerThread( clientSocket );
            //     // starting the thread
            //     ServerState.getInstance().addClientHandlerThreadToMap( clientHandlerThread );
            //     clientHandlerThread.start();
            // }
        }
        catch( IllegalArgumentException e ) {
            System.out.println("ERROR : invalid server ID");
        }
        catch ( IndexOutOfBoundsException e) {
            System.out.println("ERROR : server arguments not provided");
            e.printStackTrace();
        }
        catch ( IOException e) {
            System.out.println("ERROR : occurred in main " + Arrays.toString(e.getStackTrace()));
        }
    }

    // private static void startGossip() {
    //     try {

    //         JobDetail gossipJob = JobBuilder.newJob(GossipJob.class)
    //                 .withIdentity(Constant.GOSSIP_JOB, "group1").build();

    //         gossipJob.getJobDataMap().put("aliveErrorFactor", alive_error_factor);

    //         Trigger gossipTrigger = TriggerBuilder
    //                 .newTrigger()
    //                 .withIdentity(Constant.GOSSIP_JOB_TRIGGER, "group1")
    //                 .withSchedule(
    //                         SimpleScheduleBuilder.simpleSchedule()
    //                                 .withIntervalInSeconds(alive_interval).repeatForever())
    //                 .build();

    //         Scheduler scheduler = new StdSchedulerFactory().getScheduler();
    //         scheduler.start();
    //         scheduler.scheduleJob(gossipJob, gossipTrigger);

    //     } catch (SchedulerException e) {
    //         System.out.println("ERROR : Error in starting gossiping");
    //     }
    // }

    // private static void startConsensus() {
    //     try {

    //         JobDetail consensusJob = JobBuilder.newJob(ConsensusJob.class)
    //                 .withIdentity(Constant.CONSENSUS_JOB, "group1").build();

    //         consensusJob.getJobDataMap().put("consensusVoteDuration", consensus_vote_duration);

    //         Trigger consensusTrigger = TriggerBuilder
    //                 .newTrigger()
    //                 .withIdentity(Constant.CONSENSUS_JOB_TRIGGER, "group1")
    //                 .withSchedule(
    //                         SimpleScheduleBuilder.simpleSchedule()
    //                                 .withIntervalInSeconds(consensus_interval).repeatForever())
    //                 .build();

    //         Scheduler scheduler = new StdSchedulerFactory().getScheduler();
    //         scheduler.start();
    //         scheduler.scheduleJob(consensusJob, consensusTrigger);

    //     } catch (SchedulerException e) {
    //         System.out.println("ERROR : Error in starting consensus");
    //     }
    // }

}
