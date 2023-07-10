package macros;

import java.util.logging.Level;

public class MacroDefinitions {
    public int serverPort = 46795;
    public int cacheSize = 50;
    public String cachePolicy = "FIFO";

    public String memoryFilePath = "./data.json";

    public String logDirectory = "./client.log";

    public Level loglevel = Level.ALL;

    public String bootstrapServerIP = "127.0.0.1";

    public int bootstrapServerPort = 8888;

    public String listenAddress = "127.0.0.1";


    // Getter and Setter ------------------------------------------------------------------------------

    /**
     * get Server Port
     *
     * @param
     * @return serverPort
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * get Cache Size
     *
     * @param
     * @return getCacheSize
     */
    public int getCacheSize() {
        return cacheSize;
    }

    /**
     * get Cache Policy
     *
     * @param
     * @return getCachePolicy
     */
    public String getCachePolicy() {
        return cachePolicy;
    }

    /**
     * set Server Port
     *
     * @param serverPort
     * @return
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * set Cache Size
     *
     * @param cacheSize
     * @return
     */
    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    /**
     * set Cache Policy
     *
     * @param cachePolicy
     * @return
     */
    public void setCachePolicy(String cachePolicy) {
        this.cachePolicy = cachePolicy;
    }

    /**
     * set Memory File Path
     *
     * @param memoryFilePath
     * @return
     */
    public void setMemoryFilePath(String memoryFilePath) {
        this.memoryFilePath = memoryFilePath;
    }


    /**
     * get Memory File Path
     *
     * @param
     * @return memoryFilePath
     */
    public String getMemoryFilePath() {
        return memoryFilePath;
    }


    /**
     * get Log File Disrectory
     *
     * @param
     * @return logDirectory
     */
    public String getLogDirectory() {
        return logDirectory;
    }


    /**
     * get Log Level
     *
     * @param
     * @return loglevel
     */
    public Level getLoglevel() {
        return loglevel;
    }


    /**
     * set Log Directory
     *
     * @param logDirectory
     * @return
     */
    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    /**
     * set Log Level
     *
     * @param loglevel
     * @return
     */
    public void setLoglevel(Level loglevel) {
        this.loglevel = loglevel;
    }

    /**
     * get Bootstrap Server Port
     *
     * @param
     * @return bootstrapServerPort
     */
    public int getBootstrapServerPort() {
        return bootstrapServerPort;
    }

    /**
     * set Bootstrap Server Port
     *
     * @param bootstrapServerPort
     * @return
     */
    public void setBootstrapServerPort(int bootstrapServerPort) {
        this.bootstrapServerPort = bootstrapServerPort;
    }

    /**
     * get Listen Address
     *
     * @param
     * @return listenAddress
     */
    public String getListenAddress() {
        return listenAddress;
    }

    /**
     * set Listen Address
     *
     * @param listenAddress
     * @return
     */
    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    /**
     * get ootstrapServerIP
     *
     * @param
     * @return
     */
    public String getBootstrapServerIP() {
        return bootstrapServerIP;
    }

    /**
     * set ootstrapServerIP
     *
     * @param bootstrapServerIP
     * @return
     */
    public void setBootstrapServerIP(String bootstrapServerIP) {
        this.bootstrapServerIP = bootstrapServerIP;
    }
}
