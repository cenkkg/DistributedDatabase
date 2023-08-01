package bootstrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

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


    public static String generateEncryptionKey(String hostSource, String portSource, String hostTarget, String portTarget) throws NoSuchAlgorithmException {
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


    public static String generateDecryptionKey(String hostSource, String portSource, String hostTarget, String portTarget) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeySpecException {
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
        System.out.println(bytesToHexString(aesKeyBytes));
        return bytesToHexString(aesKeyBytes);
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
                InputStream inputStream = clientSocket .getInputStream();
                OutputStream outputStream = clientSocket .getOutputStream();
                String message = messageSendGet.getMessage(inputStream);

                // Split the input data by spaces
                String[] parts = message.split(" ");
                System.out.println(parts);

                // check if the message received correctly for encryption
                if (parts.length == 5) {
                    String sourceIP = parts[0];
                    String sourcePort = parts[1];
                    String targetIP = parts[2];
                    String targetPort = parts[3];
                    String type = parts[4];
                    System.out.println(sourceIP);
                    System.out.println(sourcePort);
                    System.out.println(targetIP);
                    System.out.println(targetPort);
                    System.out.println(type);

                    // create the encryption keys and send to source
                    if (type == "ENC") {
                        String encryptionKey = generateEncryptionKey(sourceIP,sourcePort,targetIP,targetPort);
                        System.out.println(encryptionKey);
                        messageSendGet.sendMessage(outputStream,encryptionKey);
                        clientSocket.close();
                    }
                    // create the decryption keys and send to source
                    else{

                        String decryptionKey = generateDecryptionKey(sourceIP,sourcePort,targetIP,targetPort);
                        messageSendGet.sendMessage(outputStream,decryptionKey);
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
