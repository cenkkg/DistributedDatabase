package macros;

import java.util.logging.Level;

public class MacroDefinitions {
    public int serverPort = 46795;
    public int cacheSize = 50;
    public String cachePolicy = "FIFO";

    public String memoryFilePath = "/data.json";

    public String logDirectory = "/client";

    public Level loglevel = null;

    public int bootstrapServerPort = 8888;

    public String listenAddress;


    // Getter and Setter ------------------------------------------------------------------------------

    public int getServerPort() {
        return serverPort;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public String getCachePolicy() {
        return cachePolicy;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setCachePolicy(String cachePolicy) {
        this.cachePolicy = cachePolicy;
    }

    public void setMemoryFilePath(String memoryFilePath) {
        this.memoryFilePath = memoryFilePath;
    }

    public String getMemoryFilePath() {
        return memoryFilePath;
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

    public int getBootstrapServerPort() {
        return bootstrapServerPort;
    }

    public void setBootstrapServerPort(int bootstrapServerPort) {
        this.bootstrapServerPort = bootstrapServerPort;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }
}
