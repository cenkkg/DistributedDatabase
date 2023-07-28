package macros;

import lombok.Getter;
import lombok.Setter;

import java.util.logging.Level;

@Getter
@Setter
public class MacroDefinitions {
    public int serverPort = 8888;

    public String logDirectory = "/ecs";

    public Level loglevel = null;

    public String listenAddress;

    public String coordiantorServer;
}
