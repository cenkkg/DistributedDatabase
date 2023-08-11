package client;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientSocket {

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

    public static void main(String[] args) throws Exception {

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
                            if(i == 0 || i == 1){}
                            else if (i == tokens.length - 1) {
                                newValue = newValue + tokens[i];
                            }
                            else{
                                newValue = newValue + tokens[i] + " ";
                            }
                        }

                        String encrytionKey = getKeyFromBootstrapper(serverCommunication.DNS, Integer.toString(serverCommunication.port));
                        byte[] response1 = ServerCommunication.aesEncrypt(tokens[1], encrytionKey);
                        byte[] response2 = ServerCommunication.aesEncrypt(newValue, encrytionKey);
                        serverCommunication.putData(ServerCommunication.byteArrayToHexString(response1), ServerCommunication.byteArrayToHexString(response2));
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
                        String encrytionKey = getKeyFromBootstrapper(serverCommunication.DNS, Integer.toString(serverCommunication.port));
                        byte[] response = ServerCommunication.aesEncrypt(tokens[1], encrytionKey);
                        serverCommunication.getData(ServerCommunication.byteArrayToHexString(response));
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
                        String encrytionKey = getKeyFromBootstrapper(serverCommunication.DNS, Integer.toString(serverCommunication.port));
                        byte[] response = ServerCommunication.aesEncrypt(tokens[1], encrytionKey);
                        serverCommunication.deleteData(ServerCommunication.byteArrayToHexString(response));
                    }
                    continue;
                case "logLevel":
                    if (tokens.length > 1) {}
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
                        serverCommunication.getKeyInformation(newValue);
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