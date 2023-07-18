package helperMethods;

import server.Data;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public class Helper {
    public String extractValue(String fullText){
        String valueFromRequest = "";
        for(int eachStringInRequest = 0; eachStringInRequest < fullText.split(" ").length; eachStringInRequest++){
            if(eachStringInRequest == 0 || eachStringInRequest == 1){
                continue;
            }
            else if (eachStringInRequest == fullText.split(" ").length - 1) {
                valueFromRequest = valueFromRequest + fullText.split(" ")[eachStringInRequest];
            }
            else{
                valueFromRequest = valueFromRequest + fullText.split(" ")[eachStringInRequest] + " ";
            }
        }
        return valueFromRequest;
    }

    /**
     * Calculate the MD5 value of given string.
     *
     * @param inputToMD5
     * @return
     */
    public String calculateMD5(String inputToMD5) {
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] inputBytes = inputToMD5.getBytes();
            md.update(inputBytes);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            String md5Hash = sb.toString();
            return md5Hash;
        } catch (Exception e){
            return null;
        }
    }

    public Data findDataInMemoryBinarySearch(Data[] dataArr, String key){
        int lastElement = dataArr.length - 1;
        int initialElement = 0;
        int middleElement = (lastElement + initialElement) / 2;
        while(true){
            if(lastElement == 0){
                return null;
            } else if (lastElement == initialElement && dataArr[lastElement].getKey().equals(key)) {
                return dataArr[lastElement];
            } else if (lastElement == initialElement && !dataArr[lastElement].getKey().equals(key)) {
                return null;
            } else if(dataArr[middleElement].getValue().equals(key)){
                return dataArr[middleElement];
            } else if (calculateMD5(dataArr[middleElement].getKey()).compareTo(calculateMD5(key)) > 0) {
                lastElement = middleElement;
                middleElement = (lastElement + initialElement) / 2;
            } else if (calculateMD5(dataArr[middleElement].getKey()).compareTo(calculateMD5(key)) <= 0) {
                initialElement = middleElement;
                middleElement = (lastElement + initialElement) / 2;
            }
        }
    }

    public List<Data> deleteFromMemoryAndCreateNewMemory(Data[] dataArr, String key){
        List<Data> newList = Arrays.asList(dataArr);
        for (Data data : dataArr) {
            if (!data.getKey().equals(key)) {
                newList.add(data);
            }
        }
        return newList;
    }
}
