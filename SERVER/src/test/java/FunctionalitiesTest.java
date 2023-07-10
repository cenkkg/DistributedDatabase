import macros.MacroDefinitions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import server.ClientConnection;
import server.Data;
import server.MessageSendGet;

import javax.crypto.Mac;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FunctionalitiesTest {


    Data[] cacheMock = new Data[5];

    @Mock
    Socket clientSocketMock = new Socket();

    @Mock
    InputStream inputStreamMock;

    @Mock
    OutputStream outputStreamMock;

    MacroDefinitions macroDefinitionsMock = new MacroDefinitions();

    @Mock
    MessageSendGet messageSendGetMock = new MessageSendGet();

    Map<List<String>, List<String>> metadata;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        for(int eachCacheElement = 0; eachCacheElement < 5; eachCacheElement++){
            cacheMock[eachCacheElement] = null;
        }
    }




    @Test
    public void testSetValue() throws IOException {

        macroDefinitionsMock.setCacheSize(5);
        ClientConnection clientConnection = new ClientConnection(clientSocketMock, cacheMock, macroDefinitionsMock, metadata);

        ClientConnection mockedClass = spy(clientConnection);
        doNothing().when(mockedClass).updateMemory(any(Data.class));

        clientConnection.putData("key", "value");

        verify(mockedClass, times(0)).updateMemory(any(Data.class));

        assertEquals(cacheMock[0].getKey(), "key");
        assertEquals(cacheMock[0].getValue(), "value");

    }

    @Test
    public void testGetValue() throws IOException {

        macroDefinitionsMock.setCacheSize(5);
        ClientConnection clientConnection = new ClientConnection(clientSocketMock, cacheMock, macroDefinitionsMock,metadata);

        ClientConnection mockedClass = spy(clientConnection);
        doNothing().when(mockedClass).updateMemory(any(Data.class));

        clientConnection.putData("key", "value");

        verify(mockedClass, times(0)).updateMemory(any(Data.class));

        assertEquals(mockedClass.getData("key"), "get_success key value");

    }

    @Test
    public void testUpdateValue() throws IOException {

        macroDefinitionsMock.setCacheSize(5);
        ClientConnection clientConnection = new ClientConnection(clientSocketMock, cacheMock, macroDefinitionsMock,metadata);

        ClientConnection mockedClass = spy(clientConnection);
        doNothing().when(mockedClass).updateMemory(any(Data.class));

        clientConnection.putData("key", "value");
        clientConnection.putData("key", "new_value");

        verify(mockedClass, times(0)).updateMemory(any(Data.class));

        assertEquals(cacheMock[0].getValue(), "new_value");
        assertEquals(mockedClass.getData("key"), "get_success key new_value");

    }

    @Test
    public void testGetNonExistingValue() throws IOException {

        macroDefinitionsMock.setCacheSize(5);
        ClientConnection clientConnection = new ClientConnection(clientSocketMock, cacheMock, macroDefinitionsMock,metadata);

        ClientConnection mockedClass = spy(clientConnection);
        doNothing().when(mockedClass).updateMemory(any(Data.class));

        verify(mockedClass, times(0)).updateMemory(any(Data.class));

        assertEquals( clientConnection.getData("key"), "get_error key");

    }

    @Test
    public void testDeleteValue() throws IOException {

        macroDefinitionsMock.setCacheSize(5);
        ClientConnection clientConnection = new ClientConnection(clientSocketMock, cacheMock, macroDefinitionsMock,metadata);

        ClientConnection mockedClass = spy(clientConnection);
        doNothing().when(mockedClass).updateMemory(any(Data.class));

        verify(mockedClass, times(0)).updateMemory(any(Data.class));

        clientConnection.putData("key", "value");
        clientConnection.deleteData("key");

        assertEquals( clientConnection.getData("key"), "get_error key");
        assertNull(cacheMock[0]);

    }

    @Test
    public void testClientConnectionConstructor() {
        Socket clientSocket = new Socket();
        Data[] cache = new Data[10];
        Map<List<String>, List<String>> metadata = new HashMap<>();
        metadata.put(Arrays.asList("key1"), Arrays.asList("value1"));
        metadata.put(Arrays.asList("key2"), Arrays.asList("value2"));

        ClientConnection clientConnection = new ClientConnection(clientSocket, cache, macroDefinitionsMock, metadata);

        // Check if clientSocket is initialized correctly
        assertEquals(clientSocket, clientConnection.getClientSocket());

        // Check if metadata is initialized correctly
        assertEquals(2, clientConnection.getMetadata().size());
        assertEquals(Arrays.asList("value1"), clientConnection.getMetadata().get(Arrays.asList("key1")));
        assertEquals(Arrays.asList("value2"), clientConnection.getMetadata().get(Arrays.asList("key2")));
    }


    @Test
    public void testRun() throws IOException {
        when(clientSocketMock.getInputStream()).thenReturn(inputStreamMock);
        when(clientSocketMock.getOutputStream()).thenReturn(outputStreamMock);
        when(messageSendGetMock.getMessage(inputStreamMock)).thenReturn("put key value");
        doNothing().when(messageSendGetMock).sendMessage(any(OutputStream.class), anyString());

        ClientConnection clientConnection = new ClientConnection(clientSocketMock, null, macroDefinitionsMock, metadata);

        // Invoke the method
        clientConnection.run();

        verify(clientSocketMock, times(1)).getInputStream();
        verify(clientSocketMock, times(1)).getOutputStream();
    }



}


