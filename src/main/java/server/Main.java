package server;

import heartbeat.GossipJob;

import java.util.Scanner;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;


public class Main {
    public static void main(String[] args) {
        System.out.println("LOG  : ------server started------");

        // Create a Scanner object
        Scanner scanner = new Scanner(System.in);
        System.out.println("INFO : Enter server ID : ");
        // input user ids
        String serverId = scanner.nextLine();

        ServerState.getInstance().initializeWithConfigs(serverId, 5000); // TODO : change to auto fetch from config

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

            // if (true) {
            //     System.out.println("INFO : Failure Detection is running GOSSIP mode");
            //     startGossip();
            //     startConsensus();
            // }

            while (true) {
                Socket socket = serverSocket.accept();
                Server serverThread = new Server(socket);
                // starting the tread
                ServerState.getInstance().addClientHandlerThreadToList(serverThread);
                serverThread.start();
            }

        } catch (Exception e) {
            System.out.println("Error occured in main " + e.getStackTrace());
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

}