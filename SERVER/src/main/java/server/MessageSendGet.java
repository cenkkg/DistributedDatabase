package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MessageSendGet {
    // Methods for send and get message.

    /**
     * It gets message from client, messages such as GET - PUT - DELETE
     *
     * @param inputStream
     * @return
     */
    public String getMessage(InputStream inputStream) throws IOException {
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
        String returnString = new String(byteArray2, StandardCharsets.UTF_8);
        return returnString;
    }

    /**
     * Send message to client. It convert string to byte array at the beginning and send it.
     *
     * @param outputStream, messageToSend
     * @return
     */
    public void sendMessage(OutputStream outputStream, String messageToSend) throws IOException {
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