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


public class ECSServer {
    private static Map<List<String>, List<String>> metadata = new HashMap<>();
    private static MessageSendGet messageSendGet = new MessageSendGet();

    // Getting MACROS
    public static MacroDefinitions macroDefinitions = new MacroDefinitions();

    public static class ServerPinger extends Thread {

        @Override
        public void run() {

            while (true) {

                //if there exists a coordinator server and the current ecs is not a coordinator server
                if (macroDefinitions.getCoordiantorServer() != null && !(macroDefinitions.getCoordiantorServer().equals(macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort()))) {
                    System.out.println(macroDefinitions.getCoordiantorServer() + " --> " + macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());
                    try {
                        Socket socketForPrimaryECS = new Socket(macroDefinitions.getCoordiantorServer().split(":")[0], Integer.parseInt(macroDefinitions.getCoordiantorServer().split(":")[1]));
                        try {
                            OutputStream outputStreamForPrimaryECS = socketForPrimaryECS.getOutputStream();
                            InputStream inputStreamForPrimaryECS = socketForPrimaryECS.getInputStream();
                            {
                                messageSendGet.sendMessage(outputStreamForPrimaryECS, "PRIMARYECSSENDMEDATA " + macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());

                                System.out.println("before thread sleep");
                                Thread.sleep(700);

                                int availableBytes = inputStreamForPrimaryECS.available();
                                if (availableBytes == 0) {
                                    System.out.println("couldnt receive response");
                                    //TODO : add logic for ungraceful exit

                                } else {
                                    if (macroDefinitions.getCoordiantorServer() != null && !(macroDefinitions.getCoordiantorServer().equals(macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort()))) {

                                        System.out.println("received response");

                                        String newMetadataFileData = messageSendGet.getMessage(inputStreamForPrimaryECS);
                                        String ecsFileData = messageSendGet.getMessage(inputStreamForPrimaryECS);
                                        // Updating Metadata ----------------------------------------------------------------------------------------
                                        metadata = new HashMap<>();
                                        if (!newMetadataFileData.equals("")) {
                                            for (int eachMetadataIndex = 0; eachMetadataIndex < newMetadataFileData.split(" ").length; eachMetadataIndex++) {
                                                List<String> IPAndPortList = new ArrayList<>();
                                                IPAndPortList.add(newMetadataFileData.split(" ")[eachMetadataIndex].split(":")[0]);
                                                IPAndPortList.add(newMetadataFileData.split(" ")[eachMetadataIndex].split(":")[1]);

                                                List<String> rangeList = new ArrayList<>();
                                                rangeList.add(newMetadataFileData.split(" ")[eachMetadataIndex].split(":")[2]);
                                                rangeList.add(newMetadataFileData.split(" ")[eachMetadataIndex].split(":")[3]);

                                                metadata.put(IPAndPortList, rangeList);
                                            }
                                        }

                                        File file = new File(macroDefinitions.getEcsFilePath() + "/" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + "_metadataFile" + ".txt");
                                        try {
                                            FileWriter fileWriter = new FileWriter(file);
                                            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                                            if (!newMetadataFileData.equals("")) {

                                                String totalMetadataToFile = "";
                                                for (Map.Entry<List<String>, List<String>> entry : metadata.entrySet()) {
                                                    List<String> serverAddressAndPort = entry.getKey();
                                                    List<String> rangeOfServer = entry.getValue();
                                                    totalMetadataToFile += serverAddressAndPort.get(0) + ":" + serverAddressAndPort.get(1) + ":" + rangeOfServer.get(0) + ":" + rangeOfServer.get(1) + " ";
                                                }
                                                String totalMetadataToFile2 = totalMetadataToFile.substring(0, totalMetadataToFile.length() - 1);
                                                bufferedWriter.write(totalMetadataToFile2);
                                            } else {
                                                bufferedWriter.write("");
                                            }

                                            bufferedWriter.flush();
                                            bufferedWriter.close();
                                        } catch (Exception e) {
                                        }
                                        // Updating ECS Servers ----------------------------------------------------------------------------------------
                                        File fileForECSServers = new File(macroDefinitions.getEcsFilePath() + "/" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + "_ecsServers" + ".txt");
                                        FileWriter fileWriterForECSServers = new FileWriter(fileForECSServers);
                                        BufferedWriter bufferedWriterForECSServers = new BufferedWriter(fileWriterForECSServers);
                                        bufferedWriterForECSServers.write(ecsFileData);
                                        bufferedWriterForECSServers.flush();
                                    }

                                }
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException ex) {
                        //TODO : add logic for ungraceful exit
                    }
                }

                /*

                for (Map.Entry<List<String>, List<String>> entry : metadata.entrySet()) {
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
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                 */
            }
        }
    }

    public static void main(String[] args) {
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
                    case "-ecsFilePath":
                        macroDefinitions.setEcsFilePath(value);
                        continue;
                }
            }

            // ****************************************************************************************************
            // START HOOK
            File newECSServersFile = new File(macroDefinitions.getEcsFilePath() + "/" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + "_ecsServers" + ".txt");
            File newMetadataFile = new File(macroDefinitions.getEcsFilePath() + "/" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + "_metadataFile" + ".txt");
            newECSServersFile.createNewFile();
            newMetadataFile.createNewFile();

            FileWriter fileWriteForECSServersFile = new FileWriter(newECSServersFile);
            BufferedWriter bufferedWriterForECSServersFile = new BufferedWriter(fileWriteForECSServersFile);
            bufferedWriterForECSServersFile.write(macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());
            bufferedWriterForECSServersFile.close();

            if (!(macroDefinitions.getCoordiantorServer().split(":")[0].equals(macroDefinitions.getListenAddress()) &&
                    macroDefinitions.getCoordiantorServer().split(":")[1].equals(Integer.toString(macroDefinitions.getServerPort())))) {
                try (Socket socketForCoordinatorServer = new Socket(macroDefinitions.getCoordiantorServer().split(":")[0], Integer.valueOf(macroDefinitions.getCoordiantorServer().split(":")[1]));
                     OutputStream outputStreamForCoordinatorServer = socketForCoordinatorServer.getOutputStream()) {
                    messageSendGet.sendMessage(outputStreamForCoordinatorServer, "JOINECS " + macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());
                }
            }

            // START HOOK
            // ****************************************************************************************************

            // ****************************************************************************************************
            // SHUTDOWN HOOK
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (macroDefinitions.getListenAddress().equals(macroDefinitions.getCoordiantorServer().split(":")[0]) &&
                        (Integer.toString(macroDefinitions.getServerPort())).equals(macroDefinitions.getCoordiantorServer().split(":")[1])) {
                    String targetECS = "";

                    try {
                        File fileForECSServers1 = new File(macroDefinitions.getEcsFilePath() + "/" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + "_ecsServers" + ".txt");
                        FileReader fileReaderForECSServers1 = new FileReader(fileForECSServers1);
                        BufferedReader bufferedReaderForECSServers1 = new BufferedReader(fileReaderForECSServers1);
                        String lineForECSServers1;

                        lineForECSServers1 = bufferedReaderForECSServers1.readLine();
                        for (int eachECSServer = 0; eachECSServer < lineForECSServers1.split(" ").length; eachECSServer++) {
                            if (!(lineForECSServers1.split(" ")[eachECSServer].split(":")[0].equals(macroDefinitions.getListenAddress()) &&
                                    lineForECSServers1.split(" ")[eachECSServer].split(":")[1].equals(Integer.toString(macroDefinitions.getServerPort())))) {
                                targetECS = lineForECSServers1.split(" ")[eachECSServer].split(":")[0] + ":" + lineForECSServers1.split(" ")[eachECSServer].split(":")[1];
                                break;
                            }
                        }

                        File fileForECSServers2 = new File(macroDefinitions.getEcsFilePath() + "/" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + "_ecsServers" + ".txt");
                        FileReader fileReaderForECSServers2 = new FileReader(fileForECSServers2);
                        BufferedReader bufferedReaderForECSServers2 = new BufferedReader(fileReaderForECSServers2);
                        String lineForECSServers2 = bufferedReaderForECSServers2.readLine();
                        String allECSServers = ""; // To send
                        for (int eachECSServer = 0; eachECSServer < lineForECSServers2.split(" ").length; eachECSServer++) {
                            if (!(lineForECSServers2.split(" ")[eachECSServer].split(":")[0].equals(macroDefinitions.getListenAddress()) &&
                                    lineForECSServers2.split(" ")[eachECSServer].split(":")[1].equals(Integer.toString(macroDefinitions.getServerPort())))) {
                                allECSServers += lineForECSServers2.split(" ")[eachECSServer] + " ";
                            }
                        }
                        allECSServers = allECSServers.trim();
                        System.out.println("allecsservers " + allECSServers);

                        System.out.println("make ecs new coord " + targetECS);
                        try (Socket socketForFirstReplicaServer = new Socket(targetECS.split(":")[0], Integer.valueOf(targetECS.split(":")[1]));
                             OutputStream outputStreamForTargetECS = socketForFirstReplicaServer.getOutputStream()) {

                            messageSendGet.sendMessage(outputStreamForTargetECS, "YOUARENEWCOORDINATOR"); // Send message to new coordiantor

                            File fileForMetadataFile = new File(macroDefinitions.getEcsFilePath() + "/" + macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + "_metadataFile" + ".txt");
                            FileReader fileReaderForMetadataFile = new FileReader(fileForMetadataFile);
                            BufferedReader bufferedReaderForMetadataFile = new BufferedReader(fileReaderForMetadataFile);

                            String line;
                            String lineOfMetadata = "";
                            while ((line = bufferedReaderForMetadataFile.readLine()) != null) {
                                lineOfMetadata += line;
                            }
                            bufferedReaderForMetadataFile.close();

                            messageSendGet.sendMessage(outputStreamForTargetECS, lineOfMetadata);
                            messageSendGet.sendMessage(outputStreamForTargetECS, allECSServers);
                        }
                    } catch (Exception e) {
                    }
                }
            }));
            // SHUTDOWN HOOK
            // ****************************************************************************************************


            // Create ServerSocker and Socket. Get InputStream and OutputStream
            ServerSocket serverSocket = new ServerSocket(macroDefinitions.getServerPort());

            List<String> serverIpAddresses = new ArrayList<>(); // IP1:PORT1, IP2:PORT2 ....
            boolean firstConnection = true;

            ServerPinger serverPinger = new ServerPinger();
            serverPinger.start();

            while (true) {
                Socket clientServerSocket = serverSocket.accept();
                if (firstConnection) {
                    ServerConnection serverConnection = new ServerConnection(clientServerSocket, macroDefinitions, serverIpAddresses, metadata);
                    serverConnection.start();
                    firstConnection = false;
                } else {
                    ServerConnection serverConnection = new ServerConnection(clientServerSocket, macroDefinitions, null, null);
                    serverConnection.start();
                }
            }
        } catch (Exception exception) {
        }
    }
}
