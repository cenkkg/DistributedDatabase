package ecs;

import com.sun.source.tree.Tree;
import macros.MacroDefinitions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Extension;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerConnection extends Thread {

    // Get logger
    private static final Logger logger = Logger.getLogger(ECSServer.class.getName());

    static Map<List<String>, List<String>> metadata;
    static List<String> serverIpAddresses;

    MacroDefinitions macroDefinitions;
    Socket clientServerSocket;
    boolean isOpen;
    MessageSendGet messageSendGet = new MessageSendGet();


    // Constructor of ClientConnection ------------------------------------------------------------------------
    /**
     * Create Client Connection - Constructor
     *
     * @param clientServerSocket, macroDefinitions, macroDefinitions
     * @return
     */
    public ServerConnection(Socket clientServerSocket, MacroDefinitions macroDefinitions, List<String> serverIpAddressesInp, Map<List<String>, List<String>> metadataInp) {
        this.isOpen = true;
        this.clientServerSocket = clientServerSocket;
        this.macroDefinitions = macroDefinitions;

        if(serverIpAddressesInp != null && metadataInp != null){
            this.serverIpAddresses = serverIpAddressesInp;
            this.metadata = metadataInp;
        }
    }

    /**
     * Get macroDefinitions - testing purpose
     *
     * @param
     * @return MacroDefinitions
     */
    public MacroDefinitions getMacroDefinitions() {
        return macroDefinitions;
    }

    /**
     * Get open or not information from ECS server
     *
     * @param
     * @return isOpen
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Creating Hash value from given string
     *
     * @param input
     * @return
     */
    public String hashFunction(String input) throws NoSuchAlgorithmException {

        // Create an instance of MessageDigest with MD5 algorithm
        MessageDigest md = MessageDigest.getInstance("MD5");

        // Convert the input string to bytes and update the digest
        md.update(input.getBytes());

        // Get the hash bytes
        byte[] hashBytes = md.digest();

        // Convert the hash bytes to a hexadecimal string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hash = sb.toString();
        return hash;
    }

    /**
     * A server wants to join cluster
     *
     * @param ipAddressAndPort
     * @return
     */
    public synchronized String joinServer(String ipAddressAndPort) throws NoSuchAlgorithmException, IOException {

        String hashOfNewServer = hashFunction(ipAddressAndPort);
        String result = "-";
        int insertIndex = -1;

        if (serverIpAddresses.isEmpty()) {
            serverIpAddresses.add(ipAddressAndPort);
        }
        else {
            for (int i = 0; i < serverIpAddresses.size(); i++) {
                if (hashOfNewServer.compareTo(hashFunction(serverIpAddresses.get(i))) < 0) {
                    result = serverIpAddresses.get(i);
                    insertIndex = i;
                    break;
                }
            }

            if (insertIndex != -1) {
                serverIpAddresses.add(insertIndex, ipAddressAndPort);

            } else {
                serverIpAddresses.add(ipAddressAndPort);
            }
        }

        if(serverIpAddresses.size() == 1){
            return result;
        } else{
            if (insertIndex == serverIpAddresses.size() - 1) {
                return serverIpAddresses.get(0);
            }
            return serverIpAddresses.get(insertIndex + 1);
        }
    }

    /**
     * Creating new metadata, and update current one with newer version
     *
     * @param
     * @return
     */
    public synchronized void createMetaData() throws NoSuchAlgorithmException {
        String maxHex = "ffffffffffffffffffffffffffffffffffff";
        String minHex = "000000000000000000000000000000000000";
        if (serverIpAddresses.size() == 1) {
            List<String> key   = new ArrayList<>(); // (IP1, PORT1)
            List<String> value = new ArrayList<>(); // (start1, end1)

            key.add(serverIpAddresses.get(0).split(":")[0]);
            key.add(serverIpAddresses.get(0).split(":")[1]);

            value.add(minHex);
            value.add(maxHex);

            metadata = new HashMap<>();
            metadata.put(key, value);
        }
        else {
            metadata = new HashMap<>();
            for (int eachDestination = 0; eachDestination < serverIpAddresses.size(); eachDestination++) {
                List<String> key   = new ArrayList<>(); // (IP1, PORT1)
                List<String> value = new ArrayList<>(); // (start1, end1)

                if (eachDestination == 0) {
                    key.add(serverIpAddresses.get(eachDestination).split(":")[0]);
                    key.add(serverIpAddresses.get(eachDestination).split(":")[1]);
                    value.add(hashFunction(serverIpAddresses.get(serverIpAddresses.size() - 1)));
                    value.add(hashFunction(serverIpAddresses.get(eachDestination)));
                }
                else {
                    key.add(serverIpAddresses.get(eachDestination).split(":")[0]);
                    key.add(serverIpAddresses.get(eachDestination).split(":")[1]);
                    value.add(hashFunction(serverIpAddresses.get(eachDestination - 1)));
                    value.add(hashFunction(serverIpAddresses.get(eachDestination)));
                }
                metadata.put(key, value);
            }
        }

        for(int eachDestination = 0; eachDestination < serverIpAddresses.size(); eachDestination++){
            try (Socket socketForDestination = new Socket(serverIpAddresses.get(eachDestination).split(":")[0], Integer.parseInt(serverIpAddresses.get(eachDestination).split(":")[1]));
                 OutputStream outputStreamForDestination = socketForDestination.getOutputStream();
                 InputStream inputStreamForDestination = socketForDestination.getInputStream()) {
                messageSendGet.sendMessage(outputStreamForDestination, "ECSSENDMETADATA");
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStreamForDestination);
                objectOutputStream.writeObject(metadata);
            }
            catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Remove server, server is exiting
     *
     * @param ipAddressAndPort
     * @return exitedServerAddress
     */
    public synchronized String removeServerFromMetaData(String ipAddressAndPort) {
        int indexToRemove = 0;
        for(int i = 0; i < serverIpAddresses.size(); i++) {
            if(serverIpAddresses.get(i).equals(ipAddressAndPort)) {
                indexToRemove = i;
                break;
            }
        }

        // serverIpAddresses.remove(indexToRemove);
        if (serverIpAddresses.size() == 1) {
            serverIpAddresses.remove(indexToRemove);
            return "-";
        }
        else {
            if(serverIpAddresses.size() - 1 == indexToRemove){
                serverIpAddresses.remove(indexToRemove);
                return serverIpAddresses.get(0);
            } else{
                serverIpAddresses.remove(indexToRemove);
                return serverIpAddresses.get(indexToRemove);
            }
        }
    }

    /**
     * Log message
     *
     * @param logMessage
     * @return
     */
    public synchronized void logMethod(String logMessage) throws IOException {
        logger.log(macroDefinitions.getLoglevel(), logMessage);
    }

    /**
     * Overwrite metadata file
     *
     * @param
     * @return
     */
    public synchronized void updateMetadataFile() {
        File file = new File(macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort() + ".txt");
        try {
            String totalMetadataToFile = "";
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (Map.Entry<List<String>, List<String>> entry : metadata.entrySet()) {
                List<String> serverAddressAndPort = entry.getKey();
                totalMetadataToFile += serverAddressAndPort.get(0) + ":" + serverAddressAndPort.get(1) + " ";
            }
            String totalMetadataToFile2 = totalMetadataToFile.substring(0, totalMetadataToFile.length() - 1);
            bufferedWriter.write(totalMetadataToFile2);
        } catch (Exception e){}
    }

    /**
     * Run method, which is running when thread starts. It is selecting command and call one of the suitable put-get-delete methods.
     *
     * @param
     * @return
     */
    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            inputStream = clientServerSocket.getInputStream();
            outputStream = clientServerSocket.getOutputStream();

            while(isOpen){
                try{
                    String getMessage = messageSendGet.getMessage(inputStream);
                    String keywordCommand = getMessage.split(" ")[0];
                    switch (keywordCommand){
                        case "JOIN":
                            String s = joinServer(getMessage.split(" ")[1]);
                            messageSendGet.sendMessage(outputStream, s);
                            updateMetadataFile();
                            continue;
                        case "DATATRANSFERISDONE":
                            createMetaData();
                            Thread.sleep(500);
                            if (serverIpAddresses.size() >= 3) {
                                for(int eachDestination = 0; eachDestination < serverIpAddresses.size(); eachDestination++){
                                    try (Socket socketForDestination = new Socket(serverIpAddresses.get(eachDestination).split(":")[0], Integer.parseInt(serverIpAddresses.get(eachDestination).split(":")[1]));
                                         OutputStream outputStreamForDestination = socketForDestination.getOutputStream();
                                         InputStream inputStreamForDestination = socketForDestination.getInputStream()) {
                                        messageSendGet.sendMessage(outputStreamForDestination, "STARTREPLICATION");
                                    }
                                }
                            }
                            updateMetadataFile();
                            continue;
                        case "EXIT":
                            messageSendGet.sendMessage(outputStream, removeServerFromMetaData(getMessage.split(" ")[1]));
                            updateMetadataFile();
                            continue;
                        case "YOUARENEWCOORDINATOR":
                            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                            metadata = (Map<List<String>, List<String>>) objectInputStream.readObject();

                            File file = new File(macroDefinitions.getListenAddress() + "_" + macroDefinitions.getServerPort() + ".txt");
                            try {
                                String totalMetadataToFile = "";
                                FileWriter fileWriter = new FileWriter(file);
                                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                                for (Map.Entry<List<String>, List<String>> entry : metadata.entrySet()) {
                                    List<String> serverAddressAndPort = entry.getKey();
                                    totalMetadataToFile += serverAddressAndPort.get(0) + ":" + serverAddressAndPort.get(1) + " ";

                                    try (Socket socketForFirstReplicaServer = new Socket(serverAddressAndPort.get(0), Integer.valueOf(serverAddressAndPort.get(1)));
                                         OutputStream outputStreamForTargetServer = socketForFirstReplicaServer.getOutputStream()){
                                        messageSendGet.sendMessage(outputStreamForTargetServer, "NEWECSCOORDINATOR " + macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());
                                    }
                                }
                                String totalMetadataToFile2 = totalMetadataToFile.substring(0, totalMetadataToFile.length() - 1);
                                bufferedWriter.write(totalMetadataToFile2);
                            } catch (Exception e){}
                                continue;
                        default:
                            messageSendGet.sendMessage(outputStream, "error unknown command!");
                    }
                }
                catch (Exception exception){
                    isOpen = false;
                    break;
                }
            }
        }
        catch (Exception exception){
            try {
                logMethod("Exception");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        finally {
            try {
                inputStream.close();
                outputStream.close();
                clientServerSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
