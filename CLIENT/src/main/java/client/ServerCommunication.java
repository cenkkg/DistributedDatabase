package client;

import lombok.Getter;
import lombok.Setter;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;

@Getter
@Setter
public class ServerCommunication {
    Socket socket;
    String DNS;
    int port;
    InputStream inputStream;
    OutputStream outputStream;
    boolean connected = false;

    // Metadata
    Map<List<String>, List<String>> metadataStore = new HashMap<>();

    // keys and corresponding iv values
    List<String> keyList = new ArrayList<>();


    /**
     * Tries to establish a TCP- connection to
     * the echo server based on the given server
     * address and the port number of the echo service.
     *
     * @param DNS  Hostname or IP address of the echo server.
     * @param port The port of the echo service on the respective server.
     *
     */
    public void createSocket(String DNS, int port) throws ArrayIndexOutOfBoundsException {
        try {
            socket = new Socket(DNS, port);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            this.DNS = DNS;
            this.port = port;

            connected = true;

            int clientPort = socket.getLocalPort();
            System.out.println("Client port: " + clientPort);
        }
        catch (Exception e) {
            System.out.println("Error, connection refused.");
        }
    }

    /**
     * Tries to disconnect from the connected server.
     *
     */
    public void disconnectServer() throws IOException {
        if(connected){
            inputStream.close();
            outputStream.close();
            socket.close();

            connected = false;

            /*
            System.out.print("EchoClient> ");
            System.out.print("Connection terminated: ");
            System.out.print(DNS);
            System.out.print(" / ");
            System.out.print(port);
            System.out.print("\n");
             */
        }
        else{
            System.out.print("You need to be connected first.");
        }
    }

    /**
     * Sends a text message to the echo server
     * according to the communication protocol.
     *
     * @param message  byte array message to be echoed
     *
     */
    public void sendMessage(byte[] message) throws NullPointerException {
        try {
            byte[] byteArray3 = new byte[message.length + 2];
            for (int i = 0; i < (message.length); i++) {
                byteArray3[i] = message[i];
            }
            byteArray3[message.length] = 13;
            byteArray3[message.length + 1] = 10;

            outputStream.write(byteArray3);
            outputStream.flush();
            outputStream.close();

            byte[] byteArray = new byte[128000];
            for (int i = 0; i < (message.length + 128000); i++) {
                byte readByte = (byte) inputStream.read();
                if(readByte == 13){
                    byte newLineByte = (byte) inputStream.read();
                    break;
                }else{
                    byteArray[i] = readByte;
                }
            }
            String responseOfSend = new String(byteArray, StandardCharsets.UTF_8);
            System.out.println(responseOfSend);
        }
        catch(Exception exception) {
            System.out.println("" +exception.getMessage());
            System.out.print("Error! Not connected! \n");
        }
    }

    /**
     * Prints help text.
     *
     */
    public void getHelp() {
        System.out.println("");
        System.out.println("connect <addrs> " + "<port>");
        System.out.println("                     Tries to establish a TCP connection with connection to the echo server based on the given server address and the port number of the echo service.");
        System.out.println("----------------------------");
        System.out.println("disconnect");
        System.out.println("           Tries to disconnect from the connected server.");
        System.out.println("----------------------------");
        System.out.println("send <message>");
        System.out.println("               Sends a text message to the echo server according to the communication protocol.");
        System.out.println("----------------------------");
        System.out.println("logLevel <level>");
        System.out.println("                 Sets the logger to the specified log level.");
        System.out.println("----------------------------");
        System.out.println("help");
        System.out.println("     Prints help text.");
        System.out.println("----------------------------");
        System.out.println("quit");
        System.out.println("     Tears down the active connection to the server and exits the program execution.");
        System.out.println("----------------------------");
    }

    /**
     * Tears down the active connection to the server and exits the
     * program execution.
     *
     */
    public void quitProgram() throws IOException {
        inputStream.close();
        outputStream.close();
        socket.close();

        connected = false;

        System.out.print("EchoClient> Application exit!");
    }

    /**
     * Encrypt String value with key
     * @param message String to encrypt
     * @param sharedKey String to use as key for encryption
     *
     */
    public static byte[] aesEncrypt(String message, String sharedKey) throws Exception {
        byte[] sharedKeyBytes = sharedKey.getBytes(StandardCharsets.UTF_8);
        byte[] aesKeyBytes = new byte[16];
        System.arraycopy(sharedKeyBytes, 0, aesKeyBytes, 0, Math.min(sharedKeyBytes.length, 16));
        SecretKey key = new SecretKeySpec(aesKeyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }


    // MS2 - MS3 -----------------------------------------------------------------------------------------------------
    /**
     * Find hash value (MD5)
     *
     * @param input String to find hash value
     *
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
     * Put data to storage/server
     *
     * @param key String key of saved value
     * @param value String a value for save to server
     *
     */
    public void putData(String key, String value) {
        try{
            MessageSendGet messageSendGet = new MessageSendGet();
            messageSendGet.sendMessage(outputStream, "put " + key + " " + value);
            String responseOfPut = messageSendGet.getMessage(inputStream);

            String retryResponse = "";
            if(responseOfPut.equals("server_stopped")){
                int tempVarForTime = 0;
                while(tempVarForTime < 1000){
                    tempVarForTime++;
                }
                putData(key, value);
            }
            else if (responseOfPut.equals("server_not_responsible")) {
                getKeyrangeHelper();

                // Loop over the second string list in the metadataStore
                List<String> targetServer = null;
                for (List<String> keyForMetadata : metadataStore.keySet()) {
                    List<String> correspondingValueList = metadataStore.get(keyForMetadata);

                    String startHex = correspondingValueList.get(0);
                    String endHex   = correspondingValueList.get(1);
                    String valuesHash = hashFunction(key);

                    if(startHex.compareTo(endHex) <= 0){
                        // start is smaller
                        if(startHex.compareTo(valuesHash) <= 0 && endHex.compareTo(valuesHash) > 0){
                            targetServer = keyForMetadata;
                        } else{
                            continue;
                        }
                    }
                    else{
                        // start is larger
                        String minHex = "000000000000000000000000000000000000";
                        String maxHex = "ffffffffffffffffffffffffffffffffffff";

                        if(maxHex.compareTo(valuesHash) > 0 && startHex.compareTo(valuesHash) <= 0){
                            targetServer = keyForMetadata;
                        } else if(minHex.compareTo(valuesHash) <= 0 && endHex.compareTo(valuesHash) > 0){
                            targetServer = keyForMetadata;
                        } else {
                            continue;
                        }
                    }
                }
                disconnectServer();
                createSocket(targetServer.get(0), Integer.parseInt(targetServer.get(1)));
                messageSendGet.sendMessage(outputStream, "put " + key + " " + value);
                retryResponse = messageSendGet.getMessage(inputStream);
            }
            else if (responseOfPut.equals("server_write_lock")) {
                int tempVarForTime = 0;
                while(tempVarForTime < 1000){
                    tempVarForTime++;
                }
                putData(key,value);
            }
            else{retryResponse = responseOfPut;}
            System.out.println("EchoClient> " + retryResponse);
        } catch (Exception e){
            System.out.println("EchoClient> " + e.getMessage());
        }
    }

    /**
     * Get data from storage/server
     *
     * @param key String key of data in server
     *
     */
    public void getData(String key) {
        try{
            MessageSendGet messageSendGet = new MessageSendGet();
            messageSendGet.sendMessage(outputStream, "get " + key);
            String responseOfPut = messageSendGet.getMessage(inputStream);
            String retryResponse = "";
            if(responseOfPut.equals("server_stopped")){
                int tempVarForTime = 0;
                while(tempVarForTime < 1000){
                    tempVarForTime++;
                }
                getData(key);
            }
            else if (responseOfPut.equals("server_not_responsible")) {
                getKeyrangeHelper();

                // Loop over the second string list in the metadataStore
                List<String> targetServer = null;
                for (List<String> keyForMetadata : metadataStore.keySet()) {
                    List<String> correspondingValueList = metadataStore.get(keyForMetadata);

                    String startHex = correspondingValueList.get(0);
                    String endHex   = correspondingValueList.get(1);
                    String valuesHash = hashFunction(key);

                    if(startHex.compareTo(endHex) <= 0){
                        // start is smaller
                        if(startHex.compareTo(valuesHash) <= 0 && endHex.compareTo(valuesHash) > 0){
                            targetServer = keyForMetadata;
                        } else{
                            continue;
                        }
                    }
                    else{
                        // start is larger
                        String minHex = "000000000000000000000000000000000000";
                        String maxHex = "ffffffffffffffffffffffffffffffffffff";

                        if(maxHex.compareTo(valuesHash) > 0 && startHex.compareTo(valuesHash) <= 0){
                            targetServer = keyForMetadata;
                        } else if(minHex.compareTo(valuesHash) <= 0 && endHex.compareTo(valuesHash) > 0){
                            targetServer = keyForMetadata;
                        } else {
                            continue;
                        }
                    }
                }
                disconnectServer();
                createSocket(targetServer.get(0), Integer.parseInt(targetServer.get(1)));
                messageSendGet.sendMessage(outputStream, "get " + key);
                retryResponse = messageSendGet.getMessage(inputStream);
            }
            else if (responseOfPut.equals("server_write_lock")) {
                int tempVarForTime = 0;
                while(tempVarForTime < 1000){
                    tempVarForTime++;
                }
                getData(key);
            }
            else{retryResponse = responseOfPut;}
            System.out.println("EchoClient> " + retryResponse);
        } catch (Exception e){
            System.out.println("EchoClient> " + e.getMessage());
        }
    }

    /**
     * Delete data from storage/server
     *
     * @param key String key of data to delete
     *
     */
    public void deleteData(String key) {
        try{
            MessageSendGet messageSendGet = new MessageSendGet();
            messageSendGet.sendMessage(outputStream, "delete " + key);
            String responseOfPut = messageSendGet.getMessage(inputStream);

            String retryResponse = "";
            if(responseOfPut.equals("server_stopped")){
                int tempVarForTime = 0;
                while(tempVarForTime < 1000){
                    tempVarForTime++;
                }
                deleteData(key);
            }
            else if (responseOfPut.equals("server_not_responsible")) {
                getKeyrangeHelper();

                // Loop over the second string list in the metadataStore
                List<String> targetServer = null;
                for (List<String> keyForMetadata : metadataStore.keySet()) {
                    List<String> correspondingValueList = metadataStore.get(keyForMetadata);

                    String startHex = correspondingValueList.get(0);
                    String endHex   = correspondingValueList.get(1);
                    String valuesHash = hashFunction(key);

                    if(startHex.compareTo(endHex) <= 0){
                        // start is smaller
                        if(startHex.compareTo(valuesHash) <= 0 && endHex.compareTo(valuesHash) > 0){
                            targetServer = keyForMetadata;
                        } else{
                            continue;
                        }
                    }
                    else{
                        // start is larger
                        String minHex = "000000000000000000000000000000000000";
                        String maxHex = "ffffffffffffffffffffffffffffffffffff";

                        if(maxHex.compareTo(valuesHash) > 0 && startHex.compareTo(valuesHash) <= 0){
                            targetServer = keyForMetadata;
                        } else if(minHex.compareTo(valuesHash) <= 0 && endHex.compareTo(valuesHash) > 0){
                            targetServer = keyForMetadata;
                        } else {
                            continue;
                        }
                    }
                }
                disconnectServer();
                createSocket(targetServer.get(0), Integer.parseInt(targetServer.get(1)));
                messageSendGet.sendMessage(outputStream, "delete " + key);
                retryResponse = messageSendGet.getMessage(inputStream);
            }
            else if (responseOfPut.equals("server_write_lock")) {
                int tempVarForTime = 0;
                while(tempVarForTime < 1000){
                    tempVarForTime++;
                }
                deleteData(key);
            }
            else{retryResponse = responseOfPut;}
            System.out.println("EchoClient> " + retryResponse);
        } catch (Exception e){
            System.out.println("EchoClient> " + e.getMessage());
        }
    }

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    /**
     * Get keyrange from storage/server
     *
     *
     */
    public void getKeyrange() {
        try {
            MessageSendGet messageSendGet = new MessageSendGet();
            messageSendGet.sendMessage(outputStream, "keyrange");
            String keyrangeOutputFromServer = messageSendGet.getMessage(inputStream);
            System.out.println("EchoClient> " + keyrangeOutputFromServer);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper/Second version of keyrange method, it is used in internal client-server communciation.
     *
     *
     */
    public void getKeyrangeHelper() {
        try {
            MessageSendGet messageSendGet = new MessageSendGet();
            messageSendGet.sendMessage(outputStream, "SENDMETADATA");

            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            metadataStore = (Map<List<String>, List<String>>)objectInputStream.readObject();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Used retrive the encryption keys retrieval and divides keys into parts
     *
     *
     */
    public void getKeyInformation(String newValue) {
        try {
            keyList.clear();
            MessageSendGet messageSendGet = new MessageSendGet();
            System.out.println(newValue);
            messageSendGet.sendMessage(outputStream, newValue);

            String requestOutputFromServer = messageSendGet.getMessage(inputStream);


            //***Trial**///
            keyList.add(requestOutputFromServer);
            //***Trial**///
            System.out.println(keyList);

            System.out.println("EchoClient> " + requestOutputFromServer);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}