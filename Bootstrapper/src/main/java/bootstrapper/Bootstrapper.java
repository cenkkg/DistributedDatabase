package bootstrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import macros.MacroDefinitions;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@AllArgsConstructor
@Getter
@Setter
public class Bootstrapper {

    Socket bootstrapperSocket;
    String serverIP;
    int serverPort;
    MacroDefinitions macroDefinitions;



    public static String generateKey(String targetIP, String targetPort, String thirdKey) throws NoSuchAlgorithmException {
        String concatenatedInfo = targetIP + targetPort + thirdKey;

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(concatenatedInfo.getBytes());
        String key = bytesToHexString(keyBytes);
        return key;
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }



    public static void main(String[] args) {

        try {
            MacroDefinitions macroDefinitions = new MacroDefinitions();
            MessageSendGet messageSendGet = new MessageSendGet();
            for (int k = 0; k < args.length; k += 2) {
                String flag = args[k];
                String value = args[k + 1];
                switch (flag) {
                    case "-p":
                        macroDefinitions.setServerPort(Integer.parseInt(value));
                        continue;
                    case "-a":
                        macroDefinitions.setListenAddress(value);
                        continue;
                    default:
                        System.out.println("Invalid argument: " + flag);
                }
            }

            // Create connection socket
            ServerSocket bootstrapSocket = new ServerSocket(macroDefinitions.getServerPort());
            while (true) {
                // Get the input stream from the source socket and create the encrypted data

                Socket clientSocket = bootstrapSocket.accept();
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                String message = messageSendGet.getMessage(inputStream);

                // Split the input data by spaces
                String[] parts = message.split(" ");

                // check if the message received correctly for encryption
                if (parts.length == 4) {
                    String targetIP = parts[0];
                    String targetPort = parts[1];
                    String thirdKey = parts[2];
                    String type = parts[3];

                    // create the encryption keys and send to source
                    if (type.equals("ENC")) {
                        String encryptionKey =  generateKey(targetIP,targetPort,thirdKey);
                        messageSendGet.sendMessage(outputStream, encryptionKey);
                        clientSocket.close();
                    }
                    // create the decryption keys and send to source
                    else{
                        String decryptionKey = generateKey(targetIP,targetPort,thirdKey);
                        messageSendGet.sendMessage(outputStream, decryptionKey);
                        clientSocket.close();
                    }
                }
                else {
                    System.out.println("Invalid client information received.");
                    clientSocket.close();
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
