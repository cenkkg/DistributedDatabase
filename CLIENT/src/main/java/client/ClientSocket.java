package client;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

public class ClientSocket {

    private static final Logger logger = Logger.getLogger (ClientSocket.class.getName());
    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        ServerCommunication serverCommunication = new ServerCommunication();

        BufferedReader cons = new BufferedReader(new InputStreamReader(System.in));
        // connection revoke flag
        boolean isConnected = false;
        boolean quit = false;

        while(!quit){
            System.out.print("EchoClient> ");
            String input = cons.readLine();
            String tokens[] = input.trim().split("\\s+");

            switch(tokens[0]) {
                case "connect":
                    try {
                        serverCommunication.createSocket(tokens[1], Integer.parseInt(tokens[2]));
                        isConnected = true; // Set connection status to true
                        // Get the local port used by the client-side socket

                    }
                    catch (Exception e) {System.out.print("See command 'help' \n");}
                    continue;
                case "disconnect":
                    serverCommunication.disconnectServer();
                    isConnected = false; // Set connection status to false
                    continue;
                case "send":
                    if (tokens.length == 1) {
                        System.out.print("Unknown command \n");
                        serverCommunication.getHelp();
                       }
                    else {
                        // Drop 1st element
                        for (int i = 0; i < tokens.length - 1; i++) {
                            tokens[i] = tokens[i + 1];
                        }
                        tokens = Arrays.copyOf(tokens, tokens.length - 1);
                        serverCommunication.sendMessage(String.join(" ", tokens).getBytes());
                    }
                    continue;
                case "put":
                    if (tokens.length == 2) {
                        System.out.print("EchoClient> Unknown command \n");
                        serverCommunication.getHelp();
                    }
                    else if (isConnected == false){
                        System.out.print("EchoClient> You are not connected \n");
                        serverCommunication.getHelp();
                    }
                    else {
                        String newValue = "";
                        for (int i = 0; i < tokens.length; i++) {
                            if(i == 0 || i == 1){
                                continue;
                            } else if (i == tokens.length - 1) {
                                newValue = newValue + tokens[i];
                            } else{
                                newValue = newValue + tokens[i] + " ";
                            }
                        }
                        serverCommunication.putData(tokens[1], newValue);
                    }
                    continue;
                case "get":
                    if (tokens.length != 2) {
                        System.out.print("EchoClient> Unknown command \n");
                        serverCommunication.getHelp();
                    }
                    else if (isConnected == false){
                        System.out.print("EchoClient> You are not connected \n");
                        serverCommunication.getHelp();
                    }
                    else {
                        System.out.println(tokens[1]);
                        String response = serverCommunication.encryptData(tokens[1]);
                        serverCommunication.getData(response);
                    }
                    continue;
                case "delete":
                    if (tokens.length != 2) {
                        System.out.print("EchoClient> Unknown command \n");
                        serverCommunication.getHelp();
                    }
                    else if (isConnected == false){
                        System.out.print("EchoClient> You are not connected \n");
                        serverCommunication.getHelp();
                    }
                    else {
                        System.out.println(tokens[1]);
                        String response = serverCommunication.encryptData(tokens[1]);
                        serverCommunication.deleteData(response);
                    }
                    continue;
                case "logLevel":
                    if (tokens.length > 1) {
                        String logLevel = tokens[1];
                        serverCommunication.setLogLevel(logLevel);
                    }
                    continue;
                case "help":
                    serverCommunication.getHelp();
                    continue;
                case "quit":
                    serverCommunication.quitProgram();
                    quit = true;
                    break;
                case "keyrange":
                    serverCommunication.getKeyrange();
                case "encrypt":
                    if (tokens.length != 6) {
                        System.out.print("EchoClient> Unknown command \n");
                    }
                    else if (isConnected == false){
                        System.out.print("EchoClient> You are not connected \n");
                        serverCommunication.getHelp();
                    }
                    else {
                        String newValue = "";
                        for (int i = 0; i < tokens.length; i++) {
                            if(i == 0){
                                continue;
                            } else if (i == tokens.length - 1) {
                                newValue = newValue + tokens[i];
                            } else{
                                newValue = newValue + tokens[i] + " ";
                            }
                        }
                        serverCommunication.connectEncryptionServer(newValue);
                    }
                    if (isConnected == false){
                        System.out.print("EchoClient> You are not connected \n");
                    }
                    continue;
                default:
                    System.out.print("Unknown command \n");
                    serverCommunication.getHelp();
            }
        }
    }
}