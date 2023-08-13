package server;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import helperMethods.Helper;
import lombok.Getter;
import lombok.Setter;
import macros.MacroDefinitions;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Getter
@Setter
public class ClientConnection extends Thread{

    // NON-STATIC VARIABLES
    MacroDefinitions macroDefinitions;
    Socket clientSocket;
    boolean isOpen;
    MessageSendGet messageSendGet = new MessageSendGet();
    Helper helper = new Helper();

    // STATIC VARIABLES
    @Getter
    @Setter
    static FixedCapacityMap<String, Data> cache;
    @Getter
    @Setter
    static Map<List<String>, List<String>> metadata;


    // Constructor of ClientConnection ------------------------------------------------------------------------
    /**
     * Create Client Connection - Constructor
     *
     * @param clientSocket, cache, macroDefinitions, macroDefinitions
     * @return
     */
    public ClientConnection(Socket clientSocket, FixedCapacityMap<String, Data> cache, MacroDefinitions macroDefinitions, Map<List<String>, List<String>> metadata) {
        this.clientSocket = clientSocket;
        this.isOpen = true;
        this.macroDefinitions = macroDefinitions;

        if(cache == null){} else{this.cache = cache;}
        if(metadata == null){} else{this.metadata = metadata;}
    }

    /*
    // Helper Methods -----------------------------------------------------------------------------------------
    public synchronized void slideCache(int indexInp){
        for(int index = indexInp; index < (macroDefinitions.getCacheSize() - 1); index++){
            if(cache[index + 1] == null){
                cache[index] = null;
            }else{
                cache[index] = new Data(cache[index + 1].getKey(), cache[index + 1].getValue(), cache[index + 1].getTimestamp(), cache[index + 1].getFrequency());
            }
        }
        cache[macroDefinitions.getCacheSize() - 1] = null;
    }
     */


    public static byte[] hexStringToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Encrypt String value with key
     * @param message String to encrypt
     * @param sharedKey String to use as key for encryption
     *
     */
    public static String encryption(String message, String sharedKey) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            char character = message.charAt(i);
            char keyChar = sharedKey.charAt(i % sharedKey.length());
            result.append((char) (character ^ keyChar));
        }
        return result.toString();
    }

    /**
     * Update memory Add & Delete data from memory
     *
     * @param data
     * @return
     */
    public synchronized void updateMemory(Data data) {

        Gson gson = new Gson();
        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
            Data[] jsonArray = gson.fromJson(reader, Data[].class);
            List<Data> newDataArray = new ArrayList<>();
            for (Data dataInMemory : jsonArray) {
                if (dataInMemory.getKey() != null && dataInMemory.getKey().equals(data.getKey())) {
                    continue;
                } else {
                    newDataArray.add(dataInMemory);
                }
            }
            newDataArray.add(data);
            String jsonToWriteFile = gson.toJson(newDataArray);
            helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /*
        String ourMD5 = helper.calculateMD5(data.getKey());
        Gson gson = new Gson();
        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
            Data[] jsonArray = gson.fromJson(reader, Data[].class);
            List<Data> newDataArray = new ArrayList<>();

            // Key update???
            boolean insertedOrNot = false;
            for (Data dataInMemory : jsonArray) {
                if (dataInMemory.getKey() != null && dataInMemory.getKey().equals(data.getKey())) {
                    if(helper.calculateMD5(dataInMemory.getKey()).compareTo(ourMD5) > 0 && !insertedOrNot){
                        insertedOrNot = true;
                        newDataArray.add(data);
                        newDataArray.add(dataInMemory);
                    }
                    else {
                        newDataArray.add(dataInMemory);
                    }
                }
            }

            if(!insertedOrNot){
                newDataArray.add(data);
                String jsonToWriteFile = gson.toJson(newDataArray);
                helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());
            } else{
                String jsonToWriteFile = gson.toJson(newDataArray);
                helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        */
    }

    // Methods for update-get-delete data in cache&memory ------------------------------------------------------
    /**
     * Put data, save or update data. To cache and memory. Also send one data from cache to memory if cache is full.
     *
     * @param key, value
     * @return
     */
    public synchronized String putData(String key, String value) throws NullPointerException {
        try{
            Data addData = new Data();
            if(cache.containsKey(key)) {
                if (macroDefinitions.getCachePolicy().equals("LRU")) {
                    addData.setKey(key);
                    addData.setValue(value);
                    addData.setTimestamp(String.valueOf(System.currentTimeMillis()));
                    addData.setFrequency(cache.get(key).getFrequency() + 1);
                    cache.put(key, addData);
                }
                else {
                    int newFreq = cache.get(key).getFrequency() + 1;
                    System.out.println("it is here");
                    addData.setKey(key);
                    addData.setValue(value);
                    addData.setTimestamp(String.valueOf(System.currentTimeMillis()));
                    addData.setFrequency(newFreq);
                    cache.get(key).setKey(key);
                    cache.get(key).setValue(value);
                    cache.get(key).setFrequency(newFreq);
                    cache.get(key).setTimestamp(String.valueOf(System.currentTimeMillis()));
                }
                updateMemory(addData);

                for(String keyEl : cache.keySet()) {
                    System.out.println("it is here2");
                    System.out.println(keyEl);
                }
                System.out.println("--------------------");
                return ("put_update " + key);

            }
            else {
                System.out.println("buraya girio");
                for (Data val : cache.values()) {
                    System.out.println("buraya da girio " + val);
                    if (val == null) {
                        addData.setKey(key);
                        addData.setValue(value);
                        addData.setTimestamp(String.valueOf(System.currentTimeMillis()));
                        addData.setFrequency(1);
                        cache.put(key, addData);
                        updateMemory(addData);

                        for(String keyEl : cache.keySet()) {
                            System.out.println("here2");
                            System.out.println(keyEl);
                        }
                        System.out.println("--------------------");
                        return ("put_success " + key);
                    }
                }
            }

            // Find in memory and change cache ++++++++++++++++++++++++++++++++++++++++++++++++++++++
            Gson gson = new Gson();
            try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                Data requestedData = null;
                for (Data data : jsonArray) {
                    if(data.getKey() != null && data.getKey().equals(key)){
                        requestedData = data;
                    }
                }

                // Find data to delete from cache and put into memory ot to update
                if((macroDefinitions.getCachePolicy().equals("FIFO")) || (macroDefinitions.getCachePolicy().equals("LRU"))){
                    //slideCache(0);
                    Data dataToAdd = new Data();
                    dataToAdd.setKey(key);
                    dataToAdd.setValue(value);
                    dataToAdd.setTimestamp(String.valueOf(System.currentTimeMillis()));

                    if(requestedData == null){
                        dataToAdd.setFrequency(1);
                        cache.put(key, dataToAdd);
                        updateMemory(dataToAdd);

                        for(String keyEl : cache.keySet()) {
                            System.out.println("here3");
                            System.out.println(keyEl);
                        }
                        System.out.println("--------------------");
                        return ("put_success " + key);
                    }
                    else{
                        int currFreq = requestedData.getFrequency() + 1;
                        dataToAdd.setFrequency(currFreq);
                        cache.get(key).setFrequency(currFreq);
                        updateMemory(dataToAdd);

                        return ("put_update " + key);
                    }
                }
                else if (macroDefinitions.getCachePolicy().equals("LFU")) {
                    int minCount = Integer.MAX_VALUE;

                    String keyToRemove = "";
                    if (cache.size() == macroDefinitions.getCacheSize()) {
                        for (Map.Entry<String, Data> item : cache.entrySet()) {
                            if (item.getValue().getFrequency() < minCount) {
                                minCount = item.getValue().getFrequency();
                                keyToRemove = item.getKey();
                            }
                        }
                    }

                    if (!keyToRemove.equals("")) {
                        cache.remove(keyToRemove);
                    }

                    Data toPut = new Data();
                    toPut.setKey(key);
                    toPut.setValue(value);
                    toPut.setTimestamp(String.valueOf(System.currentTimeMillis()));
                    cache.put(key, toPut);

                    if(requestedData == null){
                        cache.get(key).setFrequency(1);
                        updateMemory(cache.get(key));

                        for(String keyEl : cache.keySet()) {
                            System.out.println("here4");
                            System.out.println(keyEl);
                        }
                        System.out.println("--------------------");
                        return ("put_success " + key);
                    }
                    else{
                        cache.get(key).setFrequency(cache.get(key).getFrequency() + 1);
                        updateMemory(cache.get(key));

                        for(String keyEl : cache.keySet()) {
                            System.out.println("here5");
                            System.out.println(keyEl);
                        }
                        System.out.println("--------------------");
                        return ("put_update " + key);
                    }
                }

            }
            catch (IOException e) {
                e.printStackTrace(System.out);
                return "put_error";
            }
        } catch (Exception exception){
            exception.printStackTrace(System.out);
            return "put_error";
        }
        return "put_error";
    }

    /**
     * Get data from cache or memory. If data is not in cache it also select one to send memory.
     *
     * @param key
     * @return
     */
    public synchronized String getData(String key) throws IOException {
        System.out.println(key + " gets here");
        try{
            Data addData;
            String resultValue = "";
            if(cache.containsKey(key)) {
                if(macroDefinitions.getCachePolicy().equals("LRU")) {
                    addData = cache.remove(key);
                    addData.setTimestamp(String.valueOf(System.currentTimeMillis()));
                    addData.setFrequency(addData.getFrequency() + 1);
                    resultValue = addData.getValue();

                    cache.put(key, addData);
                }
                else {
                    cache.get(key).setTimestamp(String.valueOf(System.currentTimeMillis()));
                    cache.get(key).setFrequency(cache.get(key).getFrequency() + 1);
                    resultValue = cache.get(key).getValue();
                }

                //updateMemory(addData);

                //for(String keyEl : cache.keySet()) {
                //    System.out.println(keyEl);
                //}
                //System.out.println("--------------------");
                for(String keyEl : cache.keySet()) {
                    System.out.println(keyEl);
                }
                System.out.println("--------------------");
                return ("get_success " + key + " " + resultValue);
            }
            else {
                for (Data val : cache.values()) {
                    if (val == null) {
                        System.out.println("cp1");
                        return ("get_error " + key);
                    }
                }
            }

            // Search in memory ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            Gson gson = new Gson();
            try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                Data requestedData = null;
                for (Data data : jsonArray) {
                    System.out.println(data.getKey() + " " + key);
                    if(data.getKey().equals(key)){
                        requestedData = data;
                    } else{
                        continue;
                    }
                }
                if(requestedData == null){
                    System.out.println("cp2");
                    return ("get_error " + key);
                }
                else{
                    // Find data for delete from cache and put into memory
                    Data dataToUpdate = new Data();
                    dataToUpdate.setKey(requestedData.getKey());
                    dataToUpdate.setValue(requestedData.getValue());
                    dataToUpdate.setTimestamp(String.valueOf(System.currentTimeMillis()));
                    dataToUpdate.setFrequency(requestedData.getFrequency() + 1);

                    if (macroDefinitions.getCachePolicy().equals("LRU") || macroDefinitions.getCachePolicy().equals("FIFO")) {
                        cache.put(key, dataToUpdate);
                    }
                    else {
                        int minCount = Integer.MAX_VALUE;
                        String keyToRemove = "";
                        for(String k : cache.keySet()) {
                            if (cache.get(k).getFrequency() < minCount) {
                                minCount = cache.get(k).getFrequency();
                                keyToRemove = k;
                            }
                        }

                        if (!keyToRemove.equals("")) {
                            cache.remove(keyToRemove);
                        }

                        cache.put(key, dataToUpdate);

                        for(String keyEl : cache.keySet()) {
                            System.out.println(keyEl);
                        }
                        System.out.println("--------------------");
                        return ("get_success " + key + " " + requestedData.getValue());

                    }

                    updateMemory(dataToUpdate);

                    for(String keyEl : cache.keySet()) {
                        System.out.println(keyEl);
                    }
                    System.out.println("--------------------");
                    return ("get_success " + key + " " + requestedData.getValue());
                }
            } catch (IOException e) {
                System.out.println("cp3");
                return ("get_error " + key);
            }
        } catch(Exception exception){
            System.out.println("cp4");
            return ("get_error " + key);
        }
    }

    /**
     * Delete data from cache or memory.
     *
     * @param key
     * @return
     */
    public synchronized String deleteData(String key) {
        try {
            if(cache.containsKey(key)) {
                Data deletedValue = cache.remove(key);
                // Find data from memory to put here.
                Gson gson = new Gson();
                try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                    Data[] jsonArray = gson.fromJson(reader, Data[].class);
                    List<Data> newDataArray = new ArrayList<>();
                    for (Data data : jsonArray) {
                        if(data.getKey().equals(key)){
                            continue;
                        } else{
                            newDataArray.add(data);
                        }
                    }
                    if(newDataArray.size() == 0){
                        String jsonToWriteFile = "[]";
                        helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());

                        for(String keyEl : cache.keySet()) {
                            System.out.println(keyEl);
                        }
                        System.out.println("--------------------");
                        return ("delete_success " + key + " " + deletedValue);
                    }
                    else{
                        Data addDataToCache = newDataArray.get(0);
                        cache.slideAndInsert(addDataToCache.getKey(), addDataToCache);
                        String jsonToWriteFile = gson.toJson(newDataArray);
                        helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());

                        for(String keyEl : cache.keySet()) {
                            System.out.println(keyEl);
                        }
                        System.out.println("--------------------");
                        return ("delete_success " + key + " " + deletedValue);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                //remove also from memory

                for(String keyEl : cache.keySet()) {
                    System.out.println(keyEl);
                }
                System.out.println("--------------------");
                return ("delete_success " + key);
            }
            else {
                for (Data val : cache.values()) {
                    if (val == null) {
                        return ("delete_error " + key);
                    }
                }
            }

            // Look at the memory or swap, data is not in cache
            Gson gson = new Gson();
            try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                Data requestedData = null;
                List<Data> newDataArray = new ArrayList<>();
                for (Data data : jsonArray) {
                    if (data.getKey().equals(key)) {
                        requestedData = data;
                    } else {
                        newDataArray.add(data);
                    }
                }
                if (requestedData == null) {
                    System.out.println("hereitis");
                    return ("delete_error " + key);
                } else {
                    String jsonToWriteFile = gson.toJson(newDataArray);
                    helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());
                    return ("delete_success " + key + " " + requestedData.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.println("hereitis2");
            return "delete_error " + key;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------------------------------------


    // Methods for Distributed Storage ----------------------------------------------------------------

    /**
     * Update internal memory-cache with respect to metadata
     *
     * @param
     * @return
     */
    public synchronized void calculateNumberOfSentDataAndSendDelete(OutputStream outputStream, String targetIPandPORT) throws IOException {

        List<Data> messagesToSend = new ArrayList<>();
        String ourOwnMD5 = helper.calculateMD5(macroDefinitions.getListenAddress() + ":" + macroDefinitions.getServerPort());
        String targetMD5 = helper.calculateMD5(targetIPandPORT);
        // READING DATA FROM MEMORY
        Gson gson = new Gson();
        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
            Data[] jsonArray = gson.fromJson(reader, Data[].class);
            for (Data data : jsonArray) {
                // compare and save or not
                String elementsMD5Value = helper.calculateMD5(data.getKey());
                if(ourOwnMD5.compareTo(targetMD5) > 0){
                    // We are front of target
                    if(!(elementsMD5Value.compareTo(targetMD5) > 0 && elementsMD5Value.compareTo(ourOwnMD5) <= 0)){
                        messagesToSend.add(data);
                    }
                } else{
                    // We are back from target
                    if(elementsMD5Value.compareTo(targetMD5) <= 0 && elementsMD5Value.compareTo(ourOwnMD5) > 0){
                        messagesToSend.add(data);
                    }
                }
            }
        }

        messageSendGet.sendMessage(outputStream, String.valueOf(messagesToSend.size()));

        // SEND AND DELETE DATA ------------------------------------------------------------------------------------
        for(int eachDataToSend = 0; eachDataToSend < messagesToSend.size(); eachDataToSend++){
            messageSendGet.sendMessage(outputStream, messagesToSend.get(eachDataToSend).getKey() + " " + messagesToSend.get(eachDataToSend).getValue() + " " + messagesToSend.get(eachDataToSend).getFrequency());
        }
        for(int eachDataToSend = 0; eachDataToSend < messagesToSend.size(); eachDataToSend++){
            deleteData(messagesToSend.get(eachDataToSend).getKey());
        }
    }

    /**
     * Sae coming data from exiting server
     *
     * @param
     * @return
     */
    public synchronized void saveComingData(InputStream inputStream, String numberOfComingData) throws IOException {
        int numberOfComingDataInt = Integer.valueOf(numberOfComingData);
        for(int eachComingDataFromTarget = 0; eachComingDataFromTarget < numberOfComingDataInt; eachComingDataFromTarget++){
            String dataAsString = messageSendGet.getMessage(inputStream);
            String key   = dataAsString.split(" ")[0];
            String value = "";
            for(int eachWordInValue = 1; eachWordInValue < dataAsString.split(" ").length; eachWordInValue++){
                if(eachWordInValue == dataAsString.split(" ").length - 1){
                    value = value + dataAsString.split(" ")[eachWordInValue];
                } else{
                    value = value + dataAsString.split(" ")[eachWordInValue] + " ";
                }
            }
            putData(key, value);
        }
    }

    /**
     *
     *
     * @param
     * @return
     */
    public synchronized void replicateData() {

        String oneNextServerAddress = null;
        String twoNextServerAddress = null;
        int oneNextServerAddressIndex = -100;
        int twoNextServerAddressIndex = -100;

        // Send replicas
        List<List<String>> newMetadata = new ArrayList<>();
        for (Map.Entry<List<String>, List<String>> eachServer : metadata.entrySet()) {
            List<String> innerList = new ArrayList<>();
            innerList.add(eachServer.getKey().get(0));
            innerList.add(eachServer.getKey().get(1));
            innerList.add(eachServer.getValue().get(0));
            innerList.add(eachServer.getValue().get(1));
            newMetadata.add(innerList);
        }
        newMetadata.sort(Comparator.comparing(entry -> entry.get(3))); // {(IP, PORT, STARTHEX, ENDHEX), (), ()...} ----> SORTED

        int ourIndex = 0;
        for (List<String> eachServer : newMetadata) {
            if(eachServer.get(0).equals(macroDefinitions.getListenAddress()) && eachServer.get(1).equals(Integer.toString(macroDefinitions.getServerPort()))){
                if(ourIndex == (metadata.size() - 1)){
                    // send to 0 and 1
                    oneNextServerAddressIndex = 0;
                    twoNextServerAddressIndex = 1;
                }
                else if (ourIndex == (metadata.size() - 2)) {
                    // send to -1 and 0
                    oneNextServerAddressIndex = metadata.size() - 1;
                    twoNextServerAddressIndex = 0;
                }
                else{
                    oneNextServerAddressIndex = ourIndex + 1;
                    twoNextServerAddressIndex = ourIndex + 2;
                }
                break;
            }
            else{
                ourIndex++;
            }
        }

        int secondIterationCounter = 0;
        for (List<String> eachServer : newMetadata) {
            if(oneNextServerAddressIndex == secondIterationCounter){
                oneNextServerAddress = eachServer.get(0) + ":" + eachServer.get(1);
                secondIterationCounter++;
            }
            else if (twoNextServerAddressIndex == secondIterationCounter) {
                twoNextServerAddress = eachServer.get(0) + ":" + eachServer.get(1);
                secondIterationCounter++;
            }
            else{
                secondIterationCounter++;
            }
        }

        // ---------------------------------------------------------------------------------------------------------

        List<Data> nonBelongsToThisServer = new ArrayList<>();
        Gson gson = new Gson();

        // --------- 1 ----------
        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
            Data[] jsonArray = gson.fromJson(reader, Data[].class);
            for (Data dataInMemory : jsonArray) {
                if (!helper.dataInRangeOrNotChecker(dataInMemory.getKey(), metadata, macroDefinitions.getListenAddress(), macroDefinitions.getServerPort())) {
                    nonBelongsToThisServer.add(dataInMemory);
                }
            }

            // Delete non-belong data from server
            for(int eachToDeleteData = 0; eachToDeleteData < nonBelongsToThisServer.size(); eachToDeleteData++){
                deleteData(nonBelongsToThisServer.get(eachToDeleteData).getKey());
            }
        }
        catch (Exception e) { throw new RuntimeException(e); }

        // --------- 2 ----------
        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
            try (Socket socketForFirstReplicaServer = new Socket(oneNextServerAddress.split(":")[0], Integer.valueOf(oneNextServerAddress.split(":")[1]));
                 OutputStream outputStreamForFirstReplicaServer = socketForFirstReplicaServer.getOutputStream()){
                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                for (Data dataInMemory : jsonArray) {
                    messageSendGet.sendMessage(outputStreamForFirstReplicaServer, "put_replication " + dataInMemory.getKey() + " " + dataInMemory.getValue());
                }
            }
        }
        catch (Exception e) { throw new RuntimeException(e); }

        // --------- 3 ----------
        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
            try (Socket socketForSecondReplicaServer = new Socket(twoNextServerAddress.split(":")[0], Integer.valueOf(twoNextServerAddress.split(":")[1]));
                 OutputStream outputStreamForSecondReplicaServer = socketForSecondReplicaServer.getOutputStream()){
                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                for (Data dataInMemory : jsonArray) {
                    messageSendGet.sendMessage(outputStreamForSecondReplicaServer, "put_replication " + dataInMemory.getKey() + " " + dataInMemory.getValue());
                }
            }
        }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     *
     *
     * @param outputStream outputStream of client to send message
     * @return
     */
    public synchronized void keyrange_read(OutputStream outputStream) throws IOException {

        List<List<String>> newMetadata = new ArrayList<>();
        for (Map.Entry<List<String>, List<String>> eachServer : metadata.entrySet()) {
            List<String> innerList = new ArrayList<>();
            innerList.add(eachServer.getKey().get(0));
            innerList.add(eachServer.getKey().get(1));
            innerList.add(eachServer.getValue().get(0));
            innerList.add(eachServer.getValue().get(1));
            newMetadata.add(innerList);
        }
        newMetadata.sort(Comparator.comparing(entry -> entry.get(3))); // {(IP, PORT, STARTHEX, ENDHEX), (), ()...} ----> SORTED

        String returnValue = "keyrange_success ";
        int indexOfServers2 = 0;
        for (List<String> eachElement : newMetadata) {
            if(indexOfServers2 == 0){
                String rangeFrom = newMetadata.get(newMetadata.size() - 2).get(2);
                String rangeTo   = eachElement.get(3);
                returnValue = returnValue + rangeFrom + "," + rangeTo + "," + eachElement.get(0) + ":" + eachElement.get(1) + ";";
                indexOfServers2++;
            }
            else if (indexOfServers2 == 1) {
                String rangeFrom = newMetadata.get(newMetadata.size() - 1).get(2);
                String rangeTo   = eachElement.get(3);
                returnValue = returnValue + rangeFrom + "," + rangeTo + "," + eachElement.get(0) + ":" + eachElement.get(1) + ";";
                indexOfServers2++;
            }
            else {
                String rangeFrom = newMetadata.get(indexOfServers2 - 2).get(2);
                String rangeTo   = eachElement.get(3);
                returnValue = returnValue + rangeFrom + "," + rangeTo + "," + eachElement.get(0) + ":" + eachElement.get(1) + ";";
                indexOfServers2++;
            }
        }
        messageSendGet.sendMessage(outputStream, returnValue);
    }

    /**
     *
     *
     * @param key, value, operation key/value pair of data and operation to do (update or delete)
     * @return
     */
    public synchronized void updateReplicas(String key, String value, String operation) {
        if(metadata.size() >= 3) {
            String oneNextServerAddress = null;
            String twoNextServerAddress = null;
            int oneNextServerAddressIndex = -100;
            int twoNextServerAddressIndex = -100;

            // --------- 1 ----------
            // Send replicas
            List<List<String>> newMetadata = new ArrayList<>();
            for (Map.Entry<List<String>, List<String>> eachServer : metadata.entrySet()) {
                List<String> innerList = new ArrayList<>();
                innerList.add(eachServer.getKey().get(0));
                innerList.add(eachServer.getKey().get(1));
                innerList.add(eachServer.getValue().get(0));
                innerList.add(eachServer.getValue().get(1));
                newMetadata.add(innerList);
            }
            newMetadata.sort(Comparator.comparing(entry -> entry.get(3))); // {(IP, PORT, STARTHEX, ENDHEX), (), ()...} ----> SORTED

            int ourIndex = 0;
            for (List<String> eachServer : newMetadata) {
                if (eachServer.get(0).equals(macroDefinitions.getListenAddress()) && eachServer.get(1).equals(Integer.toString(macroDefinitions.getServerPort()))) {
                    if (ourIndex == (metadata.size() - 1)) {
                        // send to 0 and 1
                        oneNextServerAddressIndex = 0;
                        twoNextServerAddressIndex = 1;
                    } else if (ourIndex == (metadata.size() - 2)) {
                        // send to -1 and 0
                        oneNextServerAddressIndex = metadata.size() - 1;
                        twoNextServerAddressIndex = 0;
                    } else {
                        oneNextServerAddressIndex = ourIndex + 1;
                        twoNextServerAddressIndex = ourIndex + 2;
                    }
                    break;
                } else {
                    ourIndex++;
                }
            }

            int secondIterationCounter = 0;
            for (List<String> eachServer : newMetadata) {
                if (oneNextServerAddressIndex == secondIterationCounter) {
                    oneNextServerAddress = eachServer.get(0) + ":" + eachServer.get(1);
                    secondIterationCounter++;
                } else if (twoNextServerAddressIndex == secondIterationCounter) {
                    twoNextServerAddress = eachServer.get(0) + ":" + eachServer.get(1);
                    secondIterationCounter++;
                } else {
                    secondIterationCounter++;
                }
            }

            if (operation.equals("put")) {
                // --------- 2 ----------
                try (Socket socketForFirstReplicaServer = new Socket(oneNextServerAddress.split(":")[0], Integer.valueOf(oneNextServerAddress.split(":")[1]));
                     OutputStream outputStreamForFirstReplicaServer = socketForFirstReplicaServer.getOutputStream()) {
                    messageSendGet.sendMessage(outputStreamForFirstReplicaServer, "put_replication " + key + " " + value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // --------- 3 ----------
                try (Socket socketForSecondReplicaServer = new Socket(twoNextServerAddress.split(":")[0], Integer.valueOf(twoNextServerAddress.split(":")[1]));
                     OutputStream outputStreamForSecondReplicaServer = socketForSecondReplicaServer.getOutputStream()) {
                    messageSendGet.sendMessage(outputStreamForSecondReplicaServer, "put_replication " + key + " " + value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (operation.equals("delete")) {
                // --------- 2 ----------
                try (Socket socketForFirstReplicaServer = new Socket(oneNextServerAddress.split(":")[0], Integer.valueOf(oneNextServerAddress.split(":")[1]));
                     OutputStream outputStreamForFirstReplicaServer = socketForFirstReplicaServer.getOutputStream()) {
                    messageSendGet.sendMessage(outputStreamForFirstReplicaServer, "delete_replication " + key);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // --------- 3 ----------
                try (Socket socketForSecondReplicaServer = new Socket(twoNextServerAddress.split(":")[0], Integer.valueOf(twoNextServerAddress.split(":")[1]));
                     OutputStream outputStreamForSecondReplicaServer = socketForSecondReplicaServer.getOutputStream()) {
                    messageSendGet.sendMessage(outputStreamForSecondReplicaServer, "delete_replication " + key);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Wrong operation");
            }
        }
    }

    /**
     *
     *
     * @param
     * @return
     */
    public synchronized void deleteAllReplicas() {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
            Data[] jsonArray = gson.fromJson(reader, Data[].class);
            for (Data dataInMemory : jsonArray) {
                if (!helper.dataInRangeOrNotChecker(dataInMemory.getKey(), metadata, macroDefinitions.getListenAddress(), macroDefinitions.getServerPort())) {
                    deleteData(dataInMemory.getKey());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getKeyFromBootstrapper(String targetIP, String targetPort){
        int IP0 = Integer.parseInt(targetIP.split("\\.")[0]) % 100000;
        int IP1 = Integer.parseInt(targetIP.split("\\.")[1]) % 100000;
        int IP2 = Integer.parseInt(targetIP.split("\\.")[2]) % 100000;
        int IP3 = Integer.parseInt(targetIP.split("\\.")[3]) % 100000;
        int port = Integer.parseInt(targetPort) % 100000;
        int result = (IP0 + IP1 + IP2 + IP3 + port) % 100000;
        result = 100000 - result;

        try (Socket socketForBootstrapper = new Socket("127.0.0.1", 50000);
             OutputStream outputStreamForBootstrapper = socketForBootstrapper.getOutputStream();
             InputStream inputStreamForBootstrapper = socketForBootstrapper.getInputStream()){
            MessageSendGet messageSendGet = new MessageSendGet();
            messageSendGet.sendMessage(outputStreamForBootstrapper,  targetIP + " " + targetPort + " " + result + " ENC");
            return messageSendGet.getMessage(inputStreamForBootstrapper);
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    // Main RUN method for threads ------------------------------------------------------------------------------------
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
            inputStream = clientSocket.getInputStream();
            outputStream = clientSocket.getOutputStream();
            while(isOpen){
                try{
                    String getMessage = messageSendGet.getMessage(inputStream);
                    String keywordCommand = getMessage.split(" ")[0];
                    switch (keywordCommand){
                        case "put":

                            String encryptionKeyPut = getKeyFromBootstrapper(macroDefinitions.getListenAddress(), Integer.toString(macroDefinitions.getServerPort()));
                            String decryptedKeyPut = encryption(getMessage.split(" ")[1], encryptionKeyPut);
                            String decryptedValuePut = encryption(getMessage.split(" ")[2], encryptionKeyPut);

                           if(helper.dataInRangeOrNotChecker(decryptedKeyPut, metadata, macroDefinitions.getListenAddress(), macroDefinitions.getServerPort())){
                                String valueFromRequest = helper.extractValue("put " + decryptedKeyPut + " " + decryptedValuePut);
                                if(valueFromRequest.equals("null")){
                                    updateReplicas(decryptedKeyPut, null, "delete");
                                    messageSendGet.sendMessage(outputStream, encryption(deleteData(decryptedKeyPut), encryptionKeyPut));
                                }
                                else{
                                    updateReplicas(decryptedKeyPut, valueFromRequest, "put");
                                    messageSendGet.sendMessage(outputStream, encryption(putData(decryptedKeyPut, valueFromRequest), encryptionKeyPut));
                                }
                                continue;
                            }
                            else{
                                messageSendGet.sendMessage(outputStream, "server_not_responsible");
                            }
                            continue;
                        case "put_replication":
                            String valueFromRequest = helper.extractValue(getMessage);
                            if(valueFromRequest.equals("null")){
                                deleteData(getMessage.split(" ")[1]);
                            }
                            else{
                                putData(getMessage.split(" ")[1], valueFromRequest);
                            }
                            continue;
                        case "get":
                            String encryptionKey = getKeyFromBootstrapper(macroDefinitions.getListenAddress(), Integer.toString(macroDefinitions.getServerPort()));
                            String decryptedData = encryption(getMessage.split(" ")[1], encryptionKey);

                            if(!getData(decryptedData).split(" ")[0].equals("get_error")){
                                String encryptedValue = encryption(getData(decryptedData), encryptionKey);
                                messageSendGet.sendMessage(outputStream, encryptedValue);
                                updateMemory(cache.get(decryptedData));
                            }
                            else{
                                messageSendGet.sendMessage(outputStream, "server_not_responsible");
                            }

                            continue;
                        case "delete":

                            String encryptionKeyDelete = getKeyFromBootstrapper(macroDefinitions.getListenAddress(), Integer.toString(macroDefinitions.getServerPort()));
                            String decryptedDataDelete = encryption(getMessage.split(" ")[1], encryptionKeyDelete);

                            if(helper.dataInRangeOrNotChecker(decryptedDataDelete, metadata, macroDefinitions.getListenAddress(), macroDefinitions.getServerPort())){
                                updateReplicas(decryptedDataDelete, null, "delete");
                                String encryptedValue = encryption(deleteData(decryptedDataDelete), encryptionKeyDelete);
                                messageSendGet.sendMessage(outputStream, encryptedValue);
                            }
                            else{
                                messageSendGet.sendMessage(outputStream, "server_not_responsible");
                            }
                            continue;
                        case "delete_replication":
                            deleteData(getMessage.split(" ")[1]);
                            continue;
                        case "GIVEMEMYDATA":
                            calculateNumberOfSentDataAndSendDelete(outputStream, getMessage.split(" ")[1]);
                            continue;
                        case "SOMEISEXITING":
                            saveComingData(inputStream, getMessage.split(" ")[1]);
                            continue;
                        case "ECSSENDMETADATA":
                            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                            metadata = (Map<List<String>, List<String>>) objectInputStream.readObject();
                            deleteAllReplicas();
                            continue;
                        case "keyrange":
                            String returnValue = "keyrange_success ";
                            for (List<String> key : metadata.keySet()) {
                                List<String> range = metadata.get(key);
                                returnValue = returnValue + range.get(0) + "," + range.get(1) + "," + key.get(0) + ":" + key.get(1) + ";";
                            }
                            messageSendGet.sendMessage(outputStream, returnValue);
                            continue;
                        case "SENDMETADATA": //change back to SENDMETADATA
                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                            objectOutputStream.writeObject(metadata);
                            continue;
                        case "STARTREPLICATION":
                            replicateData();
                            continue;
                        case "keyrange_read":
                            keyrange_read(outputStream);
                            continue;
                        case "ISREACHABLE":
                            messageSendGet.sendMessage(outputStream, "YES");
                        case "NEWECSCOORDINATOR":
                            macroDefinitions.setBootstrapServerIP(getMessage.split(" ")[1].split(":")[0]);
                            macroDefinitions.setBootstrapServerPort(Integer.parseInt(getMessage.split(" ")[1].split(":")[1]));
                            continue;
                        default:
                            messageSendGet.sendMessage(outputStream, "Unknown command!");
                    }
                }
                catch (Exception exception){
                    isOpen = false;
                    break;
                }
            }
        }
        catch (Exception exception){}
        finally {
            try {
                inputStream.close();
                outputStream.close();
                clientSocket.close();
            } catch (IOException e) { throw new RuntimeException(e); }
        }
    }
}
