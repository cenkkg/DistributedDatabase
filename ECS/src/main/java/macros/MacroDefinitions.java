package macros;

import java.util.logging.Level;

public class MacroDefinitions {
    public int serverPort = 8888;

    public String logDirectory = "/ecs";

    public Level loglevel = null;

    public String listenAddress;


    // Getter and Setter ------------------------------------------------------------------------------

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public Level getLoglevel() {
        return loglevel;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public void setLoglevel(Level loglevel) {
        this.loglevel = loglevel;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }
}
