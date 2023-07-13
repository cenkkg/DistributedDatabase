package helperMethods;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
}
