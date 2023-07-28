package server;

import com.google.gson.Gson;
import macros.MacroDefinitions;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    // Get logger
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static void main(String[] args)  {
        try {

            // Getting MACROS
            MacroDefinitions macroDefinitions = new MacroDefinitions();

            // Create METADATA-MAP
            Map<List<String>, List<String>> metadata = new HashMap<>();

            // Create threadList
            List<Thread> threadList = new ArrayList<>();

            // Boolean Accepting-NotAccepting New Connections
            AtomicBoolean acceptConnection = new AtomicBoolean(true);

            // Create CACHE
            Data[] cache = new Data[macroDefinitions.getCacheSize()];
            Arrays.fill(cache, null);

            for (int i = 0; i < args.length; i += 2) {
                String flag = args[i];
                String value = args[i + 1];
                switch (flag) {
                    case "-p":
                        macroDefinitions.setServerPort(Integer.parseInt(value));
                        continue;
                    case "-d":
                        // Create Directory
                        File directory = new File(value);
                        if (directory.mkdir()) {
                            // First time running server.
                            // Create File - Primary Memory
                            File filePrimary = new File(value + "/data.json");
                            filePrimary.createNewFile();

                            // Write a empty array to file
                            try (BufferedWriter writer = new BufferedWriter(new FileWriter(value + "/data.json"))) {
                                String jsonArray = "[]";
                                writer.write(jsonArray);
                                writer.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            macroDefinitions.setMemoryFilePath(value + "/data.json");
                        }
                        else {
                            // Not First time running server.
                            macroDefinitions.setMemoryFilePath(value + "/data.json");
                            Gson gson = new Gson();
                            try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                                List<Data> newDataArray = new ArrayList<>();
                                for (Data dataInMemory : jsonArray) {
                                    newDataArray.add(dataInMemory);
                                }
                                if(macroDefinitions.getCacheSize() >= newDataArray.size()){
                                    for(int eachDataInMemory = 0; eachDataInMemory < newDataArray.size(); eachDataInMemory++){
                                        cache[eachDataInMemory] = newDataArray.get(eachDataInMemory);
                                    }
                                }else{
                                    for(int eachDataInMemory = 0; eachDataInMemory < macroDefinitions.getCacheSize(); eachDataInMemory++){
                                        cache[eachDataInMemory] = newDataArray.get(eachDataInMemory);
                                    }
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        continue;
                    case "-c":
                        macroDefinitions.setCacheSize(Integer.parseInt(value));
                        continue;
                    case "-a":
                        macroDefinitions.setListenAddress(value);
                        continue;

                    case "-ll":
                        macroDefinitions.setLoglevel(Level.parse(value));
                        continue;
                    case "-l":
                        File directoryForLogs = new File("/logs");
                        if (directoryForLogs.mkdir()) {
                            File filePrimary = new File("/logs/" + value);
                            filePrimary.createNewFile();
                            macroDefinitions.setLogDirectory("/logs/" + value);
                        }
                        else {
                            macroDefinitions.setLogDirectory("/logs/" + value);
                        }
                        continue;
                    case "-s":
                        macroDefinitions.setCachePolicy(value);
                        continue;
                    case "-b":
                        macroDefinitions.setBootstrapServerIP(value.split(":")[0]);
                        macroDefinitions.setBootstrapServerPort(Integer.parseInt(value.split(":")[1]));
                        continue;
                    case "-h":
                        continue;
                }
            }

            // ****************************************************************************************************
            // MS3
            // Connecting to ECS to get metadata
            try (Socket socketForECS = new Socket(macroDefinitions.getBootstrapServerIP(), macroDefinitions.getBootstrapServerPort());
                 OutputStream outputStreamForECS = socketForECS.getOutputStream();
                 InputStream inputStreamForECS = socketForECS.getInputStream()) {

                MessageSendGet messageSendGet = new MessageSendGet();
                messageSendGet.sendMessage(outputStreamForECS, "JOIN " + macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());
                String responseTargetIPandPORT = messageSendGet.getMessage(inputStreamForECS);

                if(!responseTargetIPandPORT.equals("-")){
                    try (Socket socketForTargetServer = new Socket(responseTargetIPandPORT.split(":")[0], Integer.parseInt(responseTargetIPandPORT.split(":")[1]));
                         OutputStream outputStreamForTargetServer = socketForTargetServer.getOutputStream();
                         InputStream inputStreamForTargetServer = socketForTargetServer.getInputStream()) {

                        messageSendGet.sendMessage(outputStreamForTargetServer, "GIVEMEMYDATA " + macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());
                        String getDataCount = messageSendGet.getMessage(inputStreamForTargetServer);
                        List<String> dataGetFromTarget = new ArrayList<>();
                        for(int eachResponse = 0; eachResponse < Integer.parseInt(getDataCount); eachResponse++) {
                            dataGetFromTarget.add(messageSendGet.getMessage(inputStreamForTargetServer));
                        }

                        // Save them to Memory
                        Gson gson = new Gson();
                        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                            List<Data> newDataFromTargetArray = new ArrayList<>();
                            List<Data> newDataArray = new ArrayList<>();
                            List<String> newlyAddedKeys = new ArrayList<>();

                            Data[] jsonArray = gson.fromJson(reader, Data[].class);
                            for (Data dataInMemory : jsonArray) {
                                newDataArray.add(dataInMemory);
                                newlyAddedKeys.add(dataInMemory.getKey());
                            }
                            for (String dataFromTarget : dataGetFromTarget) {
                                String key = dataFromTarget.split(" ")[0];
                                if(newlyAddedKeys.contains(key)){
                                    continue;
                                }

                                // key1 fklfslkfskl fkfsklflkkfs fklklfklfsklfs 45
                                String value = "";
                                for(int eachIndexInValue = 0; eachIndexInValue < dataFromTarget.split(" ").length; eachIndexInValue++){
                                    if(eachIndexInValue == 0 || eachIndexInValue == dataFromTarget.split(" ").length - 1){
                                        continue;
                                    }
                                    value = value + dataFromTarget.split(" ")[eachIndexInValue] + " ";
                                }

                                int frequency = Integer.parseInt(dataFromTarget.split(" ")[dataFromTarget.split(" ").length - 1]);

                                String timestamp = String.valueOf(System.currentTimeMillis());

                                Data saveNewData = new Data(key, value, timestamp, frequency);

                                newDataFromTargetArray.add(saveNewData);
                                newDataArray.add(saveNewData);
                            }

                            String jsonToWriteFile = gson.toJson(newDataArray);
                            try (BufferedWriter writer = new BufferedWriter(new FileWriter(macroDefinitions.getMemoryFilePath(), false))) {
                                writer.write(jsonToWriteFile);
                                writer.flush();
                            } catch (IOException e) { throw new Exception(e.getMessage()); }


                            // Save them to Cache --------------------------------------------------------------------------------
                            int numberOfEmptyInCache = 0;
                            for(int eachElementInCache = 0; eachElementInCache < macroDefinitions.getCacheSize(); eachElementInCache++){
                                if(cache[eachElementInCache] == null){
                                    numberOfEmptyInCache++;
                                }
                            }

                            if((numberOfEmptyInCache != 0) && numberOfEmptyInCache <= newDataFromTargetArray.size()){
                                int firstEmptyIndex = -1;
                                for(int eachElementInCache = 0; eachElementInCache < macroDefinitions.getCacheSize(); eachElementInCache++){
                                    if(cache[eachElementInCache] == null){
                                        firstEmptyIndex = eachElementInCache;
                                        break;
                                    }
                                }

                                int indexFromTarget = 0;
                                for(int eachElementInCache = firstEmptyIndex; eachElementInCache < macroDefinitions.getCacheSize(); eachElementInCache++){
                                    cache[eachElementInCache] = newDataFromTargetArray.get(indexFromTarget);
                                    indexFromTarget++;
                                }
                            }
                            else if(numberOfEmptyInCache != 0){
                                int firstEmptyIndex = -1;
                                for(int eachElementInCache = 0; eachElementInCache < macroDefinitions.getCacheSize(); eachElementInCache++){
                                    if(cache[eachElementInCache] == null){
                                        firstEmptyIndex = eachElementInCache;
                                        break;
                                    }
                                }

                                for(int eachDataItemFromTargetIndex = 0; eachDataItemFromTargetIndex < newDataFromTargetArray.size(); eachDataItemFromTargetIndex++){
                                    cache[firstEmptyIndex] = newDataFromTargetArray.get(eachDataItemFromTargetIndex);
                                    firstEmptyIndex++;
                                }
                            }
                            else{// continue
                            }
                        }
                        catch (Exception e) {throw new RuntimeException(e);}
                    }
                    catch (IOException e) { e.printStackTrace(); }
                }

                // SEND ECS TO SEND NEW METADATA TO ALL SERVERS ------------------------------------------------------------
                messageSendGet.sendMessage(outputStreamForECS, "DATATRANSFERISDONE");

                // open server socket temp
                ServerSocket serverSocket = new ServerSocket(macroDefinitions.getServerPort());
                Socket clientSocket = serverSocket.accept();
                messageSendGet.getMessage(clientSocket.getInputStream()); // "ECSSENDMETADATA"
                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                metadata = (Map<List<String>, List<String>>) objectInputStream.readObject(); // "-" -> 0000000..00/FFFFF....FFF
                serverSocket.close();
            } catch (Exception e) {e.printStackTrace();}
            // ****************************************************************************************************


            // ****************************************************************************************************
            // MS3
            // SHUTDOWN HOOK
            // Register a shutdown hook to perform actions when the JVM is shutting down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try (Socket socketForECS = new Socket(macroDefinitions.getBootstrapServerIP(), macroDefinitions.getBootstrapServerPort());
                     OutputStream outputStreamForECS = socketForECS.getOutputStream();
                     InputStream inputStreamForECS = socketForECS.getInputStream()){
                    MessageSendGet messageSendGet = new MessageSendGet();
                    messageSendGet.sendMessage(outputStreamForECS, "EXIT " + macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());
                    String targetServerIpAndPort = messageSendGet.getMessage(inputStreamForECS);
                    if(!targetServerIpAndPort.equals("-")){
                        try (Socket socketForTargetServer = new Socket(targetServerIpAndPort.split(":")[0], Integer.parseInt(targetServerIpAndPort.split(":")[1]));
                             OutputStream outputStreamForTargetServer = socketForTargetServer.getOutputStream()) {

                            // SEND TARGET a message to take our all data ================================
                            List<Data> newDataArray = new ArrayList<>();
                            Gson gson = new Gson();
                            try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                                for (Data dataInMemory : jsonArray) {
                                    newDataArray.add(dataInMemory);
                                }
                            } catch (Exception e) {throw new RuntimeException(e);}

                            messageSendGet.sendMessage(outputStreamForTargetServer, "SOMEISEXITING " + newDataArray.size());
                            for(int eachDataForSend = 0; eachDataForSend < newDataArray.size(); eachDataForSend++){
                                messageSendGet.sendMessage(outputStreamForTargetServer, newDataArray.get(eachDataForSend).getKey() + " " + newDataArray.get(eachDataForSend).getValue());
                            }

                            // SEND ECS TO SEND NEW METADATA TO ALL SERVERS ------------------------------------------------------------
                            messageSendGet.sendMessage(outputStreamForECS, "DATATRANSFERISDONE");
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                // Stop accepting new connections ------------------------------------------------
                acceptConnection.set(false);
                for (Thread thread : threadList) {
                    thread.interrupt();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }));
            // SHUTDOWN HOOK
            // ****************************************************************************************************


            // Create ServerSocker and Socket. Get InputStream and OutputStream
            boolean firstConnectionOrNot = true;
            ServerSocket serverSocket = new ServerSocket(macroDefinitions.getServerPort());
            while(acceptConnection.get()){
                Socket clientSocket = serverSocket.accept();
                if(firstConnectionOrNot){
                    ClientConnection clientConnection = new ClientConnection(clientSocket, cache, macroDefinitions, metadata);
                    clientConnection.start();
                    threadList.add(clientConnection);
                    firstConnectionOrNot = false;
                }
                else{
                    ClientConnection clientConnection = new ClientConnection(clientSocket, null, macroDefinitions, null);
                    clientConnection.start();
                    threadList.add(clientConnection);
                }
            }
        } catch (Exception exception) { // logger.log(Level.WARNING, "Error, connection can not be established!");
        }
    }
}
