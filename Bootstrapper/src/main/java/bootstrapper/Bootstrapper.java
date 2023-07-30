package bootstrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import macros.MacroDefinitions;
public class Bootstrapper {

    Socket bootstrapperSocket;
    private String serverIP;
    private int serverPort;
    MacroDefinitions macroDefinitions;

    private static final Logger logger = Logger.getLogger(Bootstrapper.class.getName());


    public Bootstrapper(Socket bootstrapperSocket, String serverIP, int serverPort, MacroDefinitions macroDefinitions) {
        this.bootstrapperSocket = bootstrapperSocket;
        this.macroDefinitions = macroDefinitions;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        try {
            MacroDefinitions macroDefinitions = new MacroDefinitions();

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
            MessageSendGet messageSendGet = new MessageSendGet();

            // Create connection socket and create the encrypted data
            ServerSocket bootstrapSocket = new ServerSocket(macroDefinitions.getServerPort());
            while (true) {
                // Get the input stream from the source socket and create the encrypted data
                System.out.println("Bootstrap Socket is listening on port " + macroDefinitions.getServerPort());

                Socket sourceSocket = bootstrapSocket.accept();
                System.out.println("Source is connected to the bootstrap.");
                InputStream inputStream = sourceSocket.getInputStream();
                OutputStream outputStream = sourceSocket.getOutputStream();
                String message = messageSendGet.getMessage(inputStream);

                // Split the input data by spaces
                String[] parts = message.split(" ");
                System.out.println("burdayım 8");

                // check if the message received correctly for encryption
                if (parts.length == 5) {
                    String sourceIP = parts[0];
                    System.out.println("burdayım 9");
                    String sourcePort = parts[1];
                    String targetIP = parts[2];
                    System.out.println("burdayım 10");
                    String targetPort = parts[3];
                    String type = parts[4];

                    // create the encryption keys and send to source
                    if (type == "ENC") {
                        String encryptionKey = messageSendGet.generateEncryptionKey(sourceIP,sourcePort,targetIP,targetPort);
                        System.out.println("Received IP: " + sourceIP + " and Port: " + sourcePort);
                        messageSendGet.sendMessage(outputStream,"Encryption information is received by the bootstrapper");
                        messageSendGet.sendMessage(outputStream,encryptionKey);
                        System.out.println("burdayım 11");
                    }
                    else{
                        // create the decryption keys and send to source
                        String decryptionKey = messageSendGet.generateDecryptionKey(sourceIP,sourcePort,targetIP,targetPort);
                        System.out.println("burdayım 14");
                        System.out.println("Received IP: " + sourceIP + " and Port: " + sourcePort);
                        System.out.println("burdayım 15");
                        messageSendGet.sendMessage(outputStream,"Decryption information is received by the bootstrapper");
                        messageSendGet.sendMessage(outputStream,decryptionKey);

                        System.out.println("burdayım 16");
                        sourceSocket.close();
                    }
                }
                else {
                    System.out.println("Invalid server information received.");
                    sourceSocket.close();
                }
            }
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "888888888 " + e.getMessage());
        }
    }
}
