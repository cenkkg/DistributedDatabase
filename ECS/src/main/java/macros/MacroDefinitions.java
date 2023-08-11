package macros;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.logging.Level;

@NoArgsConstructor
@Setter
@Getter
public class MacroDefinitions {
    public int serverPort = 8888;

    public String logDirectory = "/ecs";

    public Level loglevel = null;

    public String listenAddress;

    public String coordiantorServer;


}
