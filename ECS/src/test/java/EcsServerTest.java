import ecs.ServerConnection;
import macros.MacroDefinitions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class EcsServerTest {


    @Mock
    Socket clientServerSocketMock = new Socket();

    @Mock
    MacroDefinitions macroDefinitionsMock = new MacroDefinitions();



        @Test
        public void testServerConnectionConstructor() {
            // Test to check if the constructor is created with valid inputs
            List<String> serverIpAddresses = Arrays.asList("192.168.0.1", "192.168.0.2", "192.168.0.3");
            Map<List<String>, List<String>> metadata = new HashMap<>();
            metadata.put(Arrays.asList("key1"), Arrays.asList("value1"));
            metadata.put(Arrays.asList("key2"), Arrays.asList("value2"));

            ServerConnection serverConnection = new ServerConnection(clientServerSocketMock, macroDefinitionsMock, serverIpAddresses, metadata);

            assertTrue(serverConnection.isOpen());
            assertEquals(macroDefinitionsMock, serverConnection.getMacroDefinitions());

            ServerConnection serverConnection2 = new ServerConnection(clientServerSocketMock, macroDefinitionsMock, null, null);

            assertTrue(serverConnection2.isOpen());
            assertEquals(macroDefinitionsMock, serverConnection2.getMacroDefinitions());

        }


    @Test
    public void testJoinServer() throws NoSuchAlgorithmException, IOException {

        List<String> serverIpAddresses2 = new ArrayList<>(Arrays.asList("192.168.0.1:8080", "192.168.0.2:8080", "192.168.0.3:8080"));


        Map<List<String>, List<String>> metadata = new HashMap<>();
        metadata.put(Arrays.asList("key1"), Arrays.asList("value1"));
        metadata.put(Arrays.asList("key2"), Arrays.asList("value2"));

        ServerConnection serverConnection = new ServerConnection(clientServerSocketMock, macroDefinitionsMock, serverIpAddresses2, metadata);

        // Test when inserting at the beginning of the list
        String ipAddressAndPort2 = "192.168.0.2:8080";
        String result2 = serverConnection.joinServer(ipAddressAndPort2);
        assertNotEquals(ipAddressAndPort2, result2);
        assertEquals(4, serverIpAddresses2.size());
        assertEquals(ipAddressAndPort2, serverIpAddresses2.get(1));

        // Test when inserting in the middle of the list
        String ipAddressAndPort3 = "192.168.0.3:8080";
        String result3 = serverConnection.joinServer(ipAddressAndPort3);
        assertEquals(ipAddressAndPort2, result3);
        assertEquals(5, serverIpAddresses2.size());
        assertNotEquals(ipAddressAndPort2, serverIpAddresses2.get(0));
        assertEquals(ipAddressAndPort3, serverIpAddresses2.get(1));
    }


}
