package bootstrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public class MessageSendGet {

    public String generateEncryptionKey(String hostSource, String portSource, String hostTarget, String portTarget ) throws NoSuchAlgorithmException {
        // concatenate
        String concatenatedInfo = hostSource + portSource + hostTarget + portTarget ;

        // Create a master key using SHA-256 hash function
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] masterKeyBytes = sha256.digest(concatenatedInfo.getBytes());

        // Use the first 128 bits (16 bytes) as the AES key
        byte[] aesKeyBytes = new byte[16];
        System.arraycopy(masterKeyBytes, 0, aesKeyBytes, 0, 16);

        // Convert the AES key bytes to a string
        return bytesToHexString(aesKeyBytes);
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }


    public String generateDecryptionKey(String hostSource, String portSource, String hostTarget, String portTarget) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeySpecException {
        // Concatenate the input parameters in reverse order
        String concatenatedInfo = portTarget + hostTarget + portSource + hostSource;

        // Create the PBKDF2 key
        char[] passwordChars = concatenatedInfo.toCharArray();
        byte[] salt = new byte[16]; // Use a random salt for more security
        int iterations = 10000; // Number of iterations (should be same as in encryption key generation)
        int keyLength = 128; // Key length in bits (AES-128)

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, keyLength);
        SecretKey secretKey = factory.generateSecret(spec);
        byte[] aesKeyBytes = secretKey.getEncoded();

        // Convert the AES key bytes to a string
        return bytesToHexString(aesKeyBytes);
    }


    /**
     * It gets message from client, messages such as GET - PUT - DELETE
     *
     * @param inputStream
     * @return
     */
    public String getMessage(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        byte[] byteArray = new byte[128000];
        int index = 0;
        while(true){
            byte readByte = (byte) inputStream.read();
            if(readByte == 13){
                byte newLineByte = (byte) inputStream.read();
                if(newLineByte == 10){
                    break;
                }
            }else{
                byteArray[index] = readByte;
                index = index + 1;
            }
        }

        byte[] byteArray2 = new byte[index];
        for (int i = 0; i < (index); i++) {
            byteArray2[i] = byteArray[i];
        }

        String message = new String(byteArray2, StandardCharsets.UTF_8);
        return message;
    }


    /**
     * Send message to client. It convert string to byte array at the beginning and send it.
     *
     * @param outputStream, messageToSend
     * @return
     */
    public void sendMessage(OutputStream outputStream, String messageToSend) throws IOException, NoSuchAlgorithmException {
        byte[] byteArayOfMessage = messageToSend.getBytes();
        byte[] byteArray2 = new byte[byteArayOfMessage.length + 2];
        for (int i = 0; i < (byteArayOfMessage.length); i++) {
            byteArray2[i] = byteArayOfMessage[i];
        }
        byteArray2[byteArayOfMessage.length] = (byte) 13;
        byteArray2[byteArayOfMessage.length + 1] = (byte) 10;
        outputStream.write(byteArray2);
        outputStream.flush();
    }
}