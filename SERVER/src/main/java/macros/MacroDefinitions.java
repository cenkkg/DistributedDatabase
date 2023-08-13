package macros;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.logging.Level;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    public int encryptionServerPort = 8878;

    public String encryptionServer = "";


}
