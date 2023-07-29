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
    static  Data[] cache;
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
    public ClientConnection(Socket clientSocket, Data[] cache, MacroDefinitions macroDefinitions, Map<List<String>, List<String>> metadata) {
        this.clientSocket = clientSocket;
        this.isOpen = true;
        this.macroDefinitions = macroDefinitions;

        if(cache == null){} else{this.cache = cache;}
        if(metadata == null){} else{this.metadata = metadata;}
    }

    // Helper Methods -----------------------------------------------------------------------------------------
    /**
     * Slide cache 1 cell from index. To delete data from cache
     *
     * @param indexInp
     * @return
     */
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


    /**
     * Update memory Add & Delete data from memory
     *
     * @param data
     * @return
     */
    public synchronized void updateMemory(Data data) {
        String ourMD5 = helper.calculateMD5(data.getKey());
        Gson gson = new Gson();
        try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
            Data[] jsonArray = gson.fromJson(reader, Data[].class);
            List<Data> newDataArray = new ArrayList<>();

            // Key update???
            boolean insertedOrNot = false;
            for (Data dataInMemory : jsonArray) {
                if (!dataInMemory.getKey().equals(data.getKey())) {
                    if(helper.calculateMD5(dataInMemory.getKey()).compareTo(ourMD5) > 0 && !insertedOrNot){
                        System.out.println("UPDATEMEMORY: " + data.getKey() + " " + data.getValue());
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
            Data newData = new Data(key, value, String.valueOf(System.currentTimeMillis()), 1);
            for(int eachIndex = 0; eachIndex < macroDefinitions.getCacheSize(); eachIndex++){
                if(cache[eachIndex] == null){
                    cache[eachIndex] = newData;
                    updateMemory(newData);
                    return ("put_success " + key);
                }
                else if (cache[eachIndex] != null && cache[eachIndex].getKey().equals(key)) {
                    if (macroDefinitions.getCachePolicy().equals("LRU")) {
                        newData.setFrequency(cache[eachIndex].getFrequency() + 1);
                        slideCache(eachIndex);
                        for(int eachElementInCache = 0; eachElementInCache < macroDefinitions.getCacheSize(); eachElementInCache++){
                            if(cache[eachElementInCache] == null){
                                cache[eachElementInCache] = newData;
                                updateMemory(cache[eachElementInCache]);
                                return ("put_update " + key);
                            }
                        }
                    }
                    else {
                        newData.setFrequency(cache[eachIndex].getFrequency() + 1);
                        cache[eachIndex] = newData;
                        updateMemory(cache[eachIndex]);
                        return ("put_update " + key);
                    }
                }
            }

            // Find in memory and change cache ++++++++++++++++++++++++++++++++++++++++++++++++++++++
            Gson gson = new Gson();
            try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                Data requestedData = null;
                for (Data data : jsonArray) {
                    if(data.getKey().equals(key)){
                        requestedData = data;
                    }
                }

                // Find data for delete from cache and put into memory
                if((macroDefinitions.getCachePolicy().equals("FIFO")) || (macroDefinitions.getCachePolicy().equals("LRU"))){
                    slideCache(0);
                    if(requestedData == null){
                        newData.setFrequency(1);
                        updateMemory(cache[macroDefinitions.getCacheSize() - 1]);
                        return ("put_success " + key);
                    }
                    else{
                        newData.setFrequency(requestedData.getFrequency() + 1);
                        updateMemory(cache[macroDefinitions.getCacheSize() - 1]);
                        return ("put_update " + key);
                    }
                }
                else if (macroDefinitions.getCachePolicy().equals("LFU")) {
                    int minCount = Integer.MAX_VALUE;
                    int slideFromIndex = 0;
                    for (int i = 0; i < macroDefinitions.getCacheSize(); i++) {
                        if (cache[i].getFrequency() < minCount) {
                            minCount = cache[i].getFrequency();
                            slideFromIndex = i;
                        }
                    }
                    slideCache(slideFromIndex);
                    if(requestedData == null){
                        newData.setFrequency(1);
                        updateMemory(cache[macroDefinitions.getCacheSize() - 1]);
                        return ("put_success " + key);
                    }
                    else{
                        newData.setFrequency(requestedData.getFrequency() + 1);
                        updateMemory(cache[macroDefinitions.getCacheSize() - 1]);
                        return ("put_update " + key);
                    }
                }
            }
        } catch (Exception exception){
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
    public synchronized String getData(String key) {
        try{
            for(int eachIndex = 0; eachIndex < macroDefinitions.getCacheSize(); eachIndex++){
                if(cache[eachIndex] == null){
                    return ("get_error " + key);
                }
                else if(cache[eachIndex] != null && cache[eachIndex].getKey().equals(key)){
                    if(macroDefinitions.getCachePolicy().equals("LRU")){
                        String currentValue = cache[eachIndex].getValue();
                        int currentFrequency = cache[eachIndex].getFrequency() + 1;
                        slideCache(eachIndex);
                        for(int eachElementInCache = 0; eachElementInCache < macroDefinitions.getCacheSize(); eachElementInCache++){
                            if(cache[eachElementInCache] == null){
                                cache[eachElementInCache] = new Data(key, currentValue, String.valueOf(System.currentTimeMillis()), currentFrequency);
                                updateMemory(cache[eachElementInCache]);
                                return ("get_success " + key + " " + cache[eachElementInCache].getValue());
                            }
                        }
                    }
                    else {
                        Data newData = new Data(key, cache[eachIndex].getValue(), String.valueOf(System.currentTimeMillis()), cache[eachIndex].getFrequency() + 1);
                        cache[eachIndex] = newData;
                        updateMemory(cache[eachIndex]);
                        return ("get_success " + key + " " + cache[eachIndex].getValue());
                    }
                }
            }

            // Search in memory ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            Gson gson = new Gson();
            try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                Data requestedData = null;
                requestedData = helper.findDataInMemoryBinarySearch(jsonArray, key);
                if(!(requestedData == null)) {
                    // Find data for delete from cache and put into memory
                    if (macroDefinitions.getCachePolicy().equals("FIFO") || macroDefinitions.getCachePolicy().equals("LRU")) {
                        slideCache(0);
                        cache[macroDefinitions.getCacheSize() - 1] = new Data(requestedData.getKey(), requestedData.getValue(), String.valueOf(System.currentTimeMillis()), requestedData.getFrequency() + 1);
                        updateMemory(cache[macroDefinitions.getCacheSize() - 1]);
                        return ("get_success " + key + " " + requestedData.getValue());
                    } else if (macroDefinitions.getCachePolicy().equals("LFU")) {
                        int minCount = Integer.MAX_VALUE;
                        int index = 0;
                        for (int i = 0; i < cache.length; i++) {
                            if (cache[i].getFrequency() < minCount) {
                                minCount = cache[i].getFrequency();
                                index = i;
                            }
                        }
                        slideCache(index);
                        cache[macroDefinitions.getCacheSize() - 1] = new Data(requestedData.getKey(), requestedData.getValue(), String.valueOf(System.currentTimeMillis()), requestedData.getFrequency() + 1);
                        updateMemory(cache[macroDefinitions.getCacheSize() - 1]);
                        return ("get_success " + key + " " + requestedData.getValue());
                    }
                }
            }
        } catch(Exception exception){
            return ("get_error " + key);
        }
        return ("get_error " + key);
    }

    /**
     * Delete data from cache or memory.
     *
     * @param key
     * @return
     */
    public synchronized String deleteData(String key) {
        try {
            for (int eachIndex = 0; eachIndex < macroDefinitions.getCacheSize(); eachIndex++) {
                if (cache[eachIndex] == null) {
                    return ("delete_error " + key);
                }
                else if (cache[eachIndex] != null && cache[eachIndex].getKey().equals(key)) {
                    String deletedValue = cache[eachIndex].getValue();

                    // Delete data from cache
                    slideCache(eachIndex);

                    // Find data from memory to put here.
                    Gson gson = new Gson();
                    try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                        Data[] jsonArray = gson.fromJson(reader, Data[].class);
                        List<Data> newDataArray = new ArrayList<>();
                        for (Data data : jsonArray) {
                            if(!data.getKey().equals(key)){
                                newDataArray.add(data);
                            }
                        }
                        if(newDataArray.size() == 0){
                            String jsonToWriteFile = "[]";
                            helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());
                            return ("delete_success " + key + " " + deletedValue);
                        }
                        else{
                            Data addDataToCache = newDataArray.get(0);
                            for(int eachIndexInCache = 0; eachIndexInCache < macroDefinitions.getCacheSize(); eachIndexInCache++){
                                if(cache[eachIndexInCache] == null){
                                    cache[eachIndexInCache] = addDataToCache;
                                }
                            }
                            String jsonToWriteFile = gson.toJson(newDataArray);
                            helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());
                            return ("delete_success " + key + " " + deletedValue);
                        }
                    }
                }
            }

            // Look at the memory or swap, data is not in cache
            Gson gson = new Gson();
            try (Reader reader = new FileReader(macroDefinitions.getMemoryFilePath())) {
                Data[] jsonArray = gson.fromJson(reader, Data[].class);
                Data requestedData = helper.findDataInMemoryBinarySearch(jsonArray, key);
                List<Data> newDataArray = helper.deleteFromMemoryAndCreateNewMemory(jsonArray, key);

                if (requestedData == null) {
                    return ("delete_error " + key);
                } else {
                    String jsonToWriteFile = gson.toJson(newDataArray);
                    helper.writeToFile(jsonToWriteFile, macroDefinitions.getMemoryFilePath());
                    return ("delete_success " + key + " " + requestedData.getValue());
                }
            }
        } catch (Exception e) {
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
                            if(helper.dataInRangeOrNotChecker(getMessage.split(" ")[1], metadata, macroDefinitions.getListenAddress(), macroDefinitions.getServerPort())){
                                String valueFromRequest = helper.extractValue(getMessage);
                                if(valueFromRequest.equals("null")){
                                    updateReplicas(getMessage.split(" ")[1], null, "delete");
                                    messageSendGet.sendMessage(outputStream, deleteData(getMessage.split(" ")[1]));
                                }
                                else{
                                    String returnOfPutData = putData(getMessage.split(" ")[1], valueFromRequest);
                                    updateReplicas(getMessage.split(" ")[1], valueFromRequest, "put");
                                    messageSendGet.sendMessage(outputStream, returnOfPutData);
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
                            if(!getData(getMessage.split(" ")[1]).split(" ")[0].equals("get_error")){
                                messageSendGet.sendMessage(outputStream, getData(getMessage.split(" ")[1]));
                            }
                            else{
                                messageSendGet.sendMessage(outputStream, "server_not_responsible");
                            }
                            continue;
                        case "delete":
                            if(helper.dataInRangeOrNotChecker(getMessage.split(" ")[1], metadata, macroDefinitions.getListenAddress(), macroDefinitions.getServerPort())){
                                updateReplicas(getMessage.split(" ")[1], null, "delete");
                                messageSendGet.sendMessage(outputStream, deleteData(getMessage.split(" ")[1]));
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
                            ObjectInputStream objectInputStream1 = new ObjectInputStream(inputStream);
                            String coordinatorECSAddress = (String) objectInputStream1.readObject();
                            macroDefinitions.setBootstrapServerIP(coordinatorECSAddress.split(":")[0]);
                            macroDefinitions.setBootstrapServerPort(Integer.parseInt(coordinatorECSAddress.split(":")[1]));
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
