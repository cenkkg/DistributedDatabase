import client.MessageSendGet;
import client.ServerCommunication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OutputStreamTest {

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @Mock
    private OutputStream outputStreamMock;

    @Mock
    private ObjectInputStream objectInputStreamMock;

    @Mock
    private InputStream inputStreamMock;

    @Mock
    private MessageSendGet messageSendGetMock;

    private ServerCommunication serverCommunication;

    @Captor
    private ArgumentCaptor<byte[]> argumentCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        serverCommunication = new ServerCommunication();
        System.setOut(new PrintStream(outputStreamCaptor));

    }

    @Test
    public void testOutputStreamPutData() throws Exception {

        serverCommunication.setOutputStream(outputStreamMock);

        byte[] putMessage = ("put key value").getBytes(StandardCharsets.UTF_8);

        byte[] byteArray3 = new byte[putMessage.length + 2];
        for (int i = 0; i < (putMessage.length); i++) {
            byteArray3[i] = putMessage[i];
        }
        byteArray3[putMessage.length] = 13;
        byteArray3[putMessage.length + 1] = 10;

        // Set up the mock behavior
        doNothing().when(outputStreamMock).write(byteArray3);

        // Call the method under test
        serverCommunication.putData("key", "value");

        // Verify the arguments passed to the outputStreamMock.write
        verify(outputStreamMock).write(byteArray3);
    }


    @Test
    public void testOutputStreamGetData() throws Exception {

        serverCommunication.setOutputStream(outputStreamMock);

        byte[] getMessage = ("get key").getBytes(StandardCharsets.UTF_8);

        byte[] byteArray3 = new byte[getMessage.length + 2];
        for (int i = 0; i < (getMessage.length); i++) {
            byteArray3[i] = getMessage[i];
        }
        byteArray3[getMessage.length] = 13;
        byteArray3[getMessage.length + 1] = 10;

        // Set up the mock behavior
        doNothing().when(outputStreamMock).write(byteArray3);

        // Call the method under test
        serverCommunication.getData("key");

        // Verify the arguments passed to the outputStreamMock.write
        verify(outputStreamMock).write(byteArray3);
    }

    @Test
    public void testOutputDataDeleteData() throws Exception {

        serverCommunication.setOutputStream(outputStreamMock);

        byte[] deleteMessage = ("delete key").getBytes(StandardCharsets.UTF_8);

        byte[] byteArray3 = new byte[deleteMessage.length + 2];
        for (int i = 0; i < (deleteMessage.length); i++) {
            byteArray3[i] = deleteMessage[i];
        }
        byteArray3[deleteMessage.length] = 13;
        byteArray3[deleteMessage.length + 1] = 10;

        // Set up the mock behavior
        doNothing().when(outputStreamMock).write(byteArray3);

        // Call the method under test
        serverCommunication.deleteData("key");

        // Verify the arguments passed to the outputStreamMock.write
        verify(outputStreamMock).write(byteArray3);
    }


    @Test
    public void testPutDataForServerOutput() throws Exception {
        String key = "key";
        String value = "value";
        String expectedResponse = "server_not_responsible";

        InputStream responseStream = new ByteArrayInputStream(expectedResponse.getBytes());


        when(messageSendGetMock.getMessage(any(InputStream.class))).thenReturn(expectedResponse);
        doNothing().when(messageSendGetMock).sendMessage(any(OutputStream.class), eq("put " + key + " " + value));

        serverCommunication.putData(key, value);

        String actualResponse = messageSendGetMock.getMessage(responseStream);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testGetDataForServerOutput() throws Exception {
        String key = "key";
        String expectedResponse = "server_not_responsible";

        InputStream responseStream = new ByteArrayInputStream(expectedResponse.getBytes());


        when(messageSendGetMock.getMessage(any(InputStream.class))).thenReturn(expectedResponse);
        doNothing().when(messageSendGetMock).sendMessage(any(OutputStream.class), eq("get " + key));

        serverCommunication.getData(key);

        String actualResponse = messageSendGetMock.getMessage(responseStream);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testDeleteDataForServerOutput() throws Exception {
        String key = "key";
        String expectedResponse = "server_not_responsible";

        InputStream responseStream = new ByteArrayInputStream(expectedResponse.getBytes());


        when(messageSendGetMock.getMessage(any(InputStream.class))).thenReturn(expectedResponse);
        doNothing().when(messageSendGetMock).sendMessage(any(OutputStream.class), eq("delete " + key));

        serverCommunication.deleteData(key);


        String actualResponse = messageSendGetMock.getMessage(responseStream);
        assertEquals(expectedResponse, actualResponse);
    }

    /*
    @Test
    public void testCreateSocket() throws IOException {
        // Create mock objects for dependencies
        Socket socketMock = mock(Socket.class);
        InputStream inputStreamMock = mock(InputStream.class);
        OutputStream outputStreamMock = mock(OutputStream.class);

        // Configure the mock objects
        when(socketMock.getInputStream()).thenReturn(inputStreamMock);
        when(socketMock.getOutputStream()).thenReturn(outputStreamMock);

        // Inject the mock Socket into the class under test
        serverCommunication.setSocket(socketMock);

        // Call the method you want to test
        serverCommunication.createSocket("localhost", 46795);

        // Assert that the connection is established
        assertTrue(serverCommunication.getConnected());
    }

    @Test
    public void testServer() throws IOException {
        // Create mock objects for dependencies
        Socket socketMock = mock(Socket.class);
        InputStream inputStreamMock = mock(InputStream.class);
        OutputStream outputStreamMock = mock(OutputStream.class);

        // Configure the mock objects
        when(socketMock.getInputStream()).thenReturn(inputStreamMock);
        when(socketMock.getOutputStream()).thenReturn(outputStreamMock);

        // Inject the mock Socket into the class under test
        serverCommunication.setSocket(socketMock);

        serverCommunication.createSocket("localhost", 46795);

        // Call the method you want to test
        serverCommunication.disconnectServer();

        // Assert that the connection is established
        assertEquals("EchoClient> Connection terminated: localhost / 46795\n", outputStreamCaptor.toString());

        assertFalse(serverCommunication.getConnected());
    }
     */
}
