package server;

import heartbeat.GossipJob;
import heartbeat.ConsensusJob;

import java.util.Scanner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;


public class Main {
    public static void main(String[] args) {
        System.out.println("LOG  : ------server started------");

        ServerState.getInstance().initializeWithConfigs(args[0], args[1]);

        try {
            ServerSocket serverSocket = new ServerSocket(ServerState.getInstance().getServerPort());
            System.out.println(serverSocket.getInetAddress());
            System.out.println(serverSocket.getLocalSocketAddress());
            System.out.println(serverSocket.getLocalPort());
            System.out.println("TCPServer Waiting for client on port 5000"); // client should use 5000 as port


            /**
             Heartbeat detection using gossiping
             **/
            // if (isGossip) {
            //     System.out.println("INFO : Failure Detection is running GOSSIP mode");
            //     Runnable gossip = new GossipJob();
            //     new Thread(gossip).start();
            //     startConsensus();
            // }

            if (true) {
                System.out.println("INFO : Failure Detection is running GOSSIP mode");
                startGossip();
                startConsensus();
            }

            // throw exception if invalid server id provided
            if (ServerState.getInstance().getServerAddress() == null) {
                throw new IllegalArgumentException();
            }
            // server socket for coordination
            ServerSocket serverCoordinationSocket = new ServerSocket();

            // bind SocketAddress with inetAddress and port
            SocketAddress endPointCoordination = new InetSocketAddress(
                    ServerState.getInstance().getServerAddress(),
                    ServerState.getInstance().getCoordinationPort());
            serverCoordinationSocket.bind(endPointCoordination);
            System.out.println(serverCoordinationSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for coordination on port " +
                    serverCoordinationSocket.getLocalPort()); // port open for coordination

            // server socket for clients
            ServerSocket serverClientsSocket = new ServerSocket();

            // bind SocketAddress with inetAddress and port
            SocketAddress endPointClient = new InetSocketAddress(
                    ServerState.getInstance().getServerAddress(),
                    ServerState.getInstance().getClientsPort());
            serverClientsSocket.bind(endPointClient);
            System.out.println(serverClientsSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for clients on port " +
                    serverClientsSocket.getLocalPort()); // port open for clients
            while (true) {
                Socket clientSocket = serverClientsSocket.accept();
                Server clientHandlerThread = new Server(clientSocket);
                // starting the tread
                ServerState.getInstance().addClientHandlerThreadToList(clientHandlerThread);
                clientHandlerThread.start();
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR : invalid server ID");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("ERROR : server arguments not provided ");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error occured in main " + e.getStackTrace());
        }
    }

    private static void startGossip() {
        try {

            JobDetail gossipJob = JobBuilder.newJob(GossipJob.class)
                    .withIdentity("GOSSIPJOB", "group1").build();

            gossipJob.getJobDataMap().put("aliveErrorFactor", 5);

            Trigger gossipTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity("GOSSIPJOBTRIGGER", "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(3).repeatForever())
                    .build();

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(gossipJob, gossipTrigger);

        } catch (SchedulerException e) {
            System.out.println("ERROR : Error in starting gossiping");
        }
    }

    private static void startConsensus() {
        try {

            JobDetail consensusJob = JobBuilder.newJob(ConsensusJob.class)
                    .withIdentity("CONSENSUSJOB", "group1").build();

            consensusJob.getJobDataMap().put("consensusVoteDuration", 5);

            Trigger consensusTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity("CONSENSUSJOBTRIGGER", "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(10).repeatForever())
                    .build();

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(consensusJob, consensusTrigger);

        } catch (SchedulerException e) {
            System.out.println("ERROR : Error in starting consensus");
        }
    }

}