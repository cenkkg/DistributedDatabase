package ecs;

import macros.MacroDefinitions;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ECSServer {
    private static Map<List<String>, List<String>> metadata = new HashMap<>();
    private static MessageSendGet messageSendGet = new MessageSendGet();

    // Getting MACROS
    public static MacroDefinitions macroDefinitions = new MacroDefinitions();

    public static class ServerPinger extends Thread{

        @Override
        public void run() {
            while(true) {
                if (macroDefinitions.getCoordiantorServer() != null && !macroDefinitions.getCoordiantorServer().equals(macroDefinitions.getListenAddress() + ":"  + macroDefinitions.getServerPort())) {
                    File fileForECSs = new File("./" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + ".txt");

                    try {
                        FileReader fileReader = new FileReader(fileForECSs);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                        String line;
                        Socket socketForPrimaryECS = new Socket(macroDefinitions.getCoordiantorServer().split(":")[0], Integer.parseInt(macroDefinitions.getCoordiantorServer().split(":")[1]));
                        while ((line = bufferedReader.readLine()) != null) {
                            if(!line.equals(macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort())){
                                Socket currentSocketForECS = new Socket(macroDefinitions.getListenAddress(), macroDefinitions.getServerPort());
                                try {
                                    OutputStream outputStreamForPrimaryECS = socketForPrimaryECS.getOutputStream();
                                    InputStream inputStreamForCurrenctECS = currentSocketForECS.getInputStream();{
                                        outputStreamForPrimaryECS.write("PRIMARYECSSENDMETADATA".getBytes());

                                        Thread.sleep(700);

                                        int availableBytes = inputStreamForCurrenctECS.available();
                                        if (availableBytes == 0) {
                                            try {
                                                File fileForCurrentECSMetadata = new File("./" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + ".txt");

                                                FileReader fileReader2 = new FileReader(fileForCurrentECSMetadata);
                                                BufferedReader bufferedReader2 = new BufferedReader(fileReader);
                                                String lineToRead;
                                                while ((lineToRead = bufferedReader.readLine()) != null) {
                                                    if(!lineToRead.equals(macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort())){
                                                        try (Socket socketForFirstReplicaServer = new Socket(lineToRead.split(":")[0], Integer.valueOf(line.split(":")[1]));
                                                             OutputStream outputStreamForTargetECS = socketForFirstReplicaServer.getOutputStream()){
                                                            outputStreamForTargetECS.write("YOUARENEWCOORDINATOR".getBytes());
                                                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStreamForTargetECS);
                                                            objectOutputStream.writeObject(metadata);
                                                            break;
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) {}
                                        }
                                    }
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (Exception e) {}
                }

                for (Map.Entry<List<String>, List<String>> entry: metadata.entrySet()) {
                    String address = entry.getKey().get(0);
                    int port = Integer.parseInt(entry.getKey().get(1));
                    try (Socket socketForDestination = new Socket(address, port);
                         OutputStream outputStreamForDestination = socketForDestination.getOutputStream();
                         InputStream inputStreamForDestination = socketForDestination.getInputStream()) {
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStreamForDestination);
                        messageSendGet.sendMessage(objectOutputStream, "ISREACHABLE");

                        Thread.sleep(700);

                        int availableBytes = inputStreamForDestination.available();
                        if (availableBytes == 0) {
                            System.out.println("Server at " + address + ":" + port + " is unreachable.");
                        }
                    }
                    catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted.");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void main(String[] args)  {
        try {


            for (int i = 0; i < args.length; i += 2) {
                String flag = args[i];
                String value = args[i + 1];
                switch (flag) {
                    case "-p":
                        macroDefinitions.setServerPort(Integer.parseInt(value));
                        continue;
                    case "-a":
                        macroDefinitions.setListenAddress(value);
                        continue;
                    case "-c":
                        macroDefinitions.setCoordiantorServer(value);
                        continue;
                }
            }

            File file = new File("./" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + ".txt");

            // ****************************************************************************************************
            // SHUTDOWN HOOK
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    FileReader fileReader = new FileReader(file);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if(!line.equals(macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort())){
                            try (Socket socketForFirstReplicaServer = new Socket(line.split(":")[0], Integer.valueOf(line.split(":")[1]));
                                 OutputStream outputStreamForTargetECS = socketForFirstReplicaServer.getOutputStream()){
                                outputStreamForTargetECS.write("YOUARENEWCOORDINATOR".getBytes());
                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStreamForTargetECS);
                                objectOutputStream.writeObject(metadata);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {} {}
            }));
            // SHUTDOWN HOOK
            // ****************************************************************************************************


            // Create ServerSocker and Socket. Get InputStream and OutputStream
            ServerSocket serverSocket = new ServerSocket(macroDefinitions.getServerPort());

            List<String> serverIpAddresses = new ArrayList<>(); // IP1:PORT1, IP2:PORT2 ....
            boolean firstConnection = true;

            ServerPinger serverPinger = new ServerPinger();
            serverPinger.start();

            while(true){
                Socket clientServerSocket = serverSocket.accept();
                if(firstConnection){
                    ServerConnection serverConnection = new ServerConnection(clientServerSocket, macroDefinitions, serverIpAddresses, metadata);
                    serverConnection.start();
                    firstConnection = false;
                } else{
                    ServerConnection serverConnection = new ServerConnection(clientServerSocket, macroDefinitions, null, null);
                    serverConnection.start();
                }
            }
        } catch (Exception exception) {}
    }
}
