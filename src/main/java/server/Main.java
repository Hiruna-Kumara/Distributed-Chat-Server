package server;

import heartbeat.GossipJob;
import heartbeat.ConsensusJob;

import java.util.Scanner;
import consensus.BullyAlgorithm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        System.out.println("LOG  : ------server started------");

        ServerState.getInstance().initializeWithConfigs(args[0], args[1]);
        try {
            // throw exception if invalid server id provided
            if (ServerState.getInstance().getServerAddress() == null) {
                throw new IllegalArgumentException();
            }

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

            /**
             * Maintain consensus using Bully Algorithm
             **/
            BullyAlgorithm.initialize();

            Runnable receiver = new BullyAlgorithm("Receiver");
            new Thread(receiver).start();

            Runnable heartbeat = new BullyAlgorithm("Heartbeat");
            new Thread(heartbeat).start();

            /**
             Heartbeat detection using gossiping
             **/
            if (true) {
                System.out.println("INFO : Failure Detection is running GOSSIP mode");
                startGossip();
                startConsensus();
            }

            /**
             * Handle clients
             **/
            while (true) {
                Socket clientSocket = serverClientsSocket.accept();
                ClientHandlerThread clientHandlerThread = new ClientHandlerThread(clientSocket);
                // starting the thread
                ServerState.getInstance().addClientHandlerThreadToList(clientHandlerThread);
                clientHandlerThread.start();
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR : invalid server ID");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("ERROR : server arguments not provided");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("ERROR : occurred in main " + Arrays.toString(e.getStackTrace()));
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