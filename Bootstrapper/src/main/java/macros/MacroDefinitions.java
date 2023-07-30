package macros;

import java.util.logging.Level;

public class MacroDefinitions {
    public int serverPort = 38887;

    public Level loglevel = Level.ALL;
    public String listenAddress;


    // Getter and Setter ------------------------------------------------------------------------------

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }



    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }
}
