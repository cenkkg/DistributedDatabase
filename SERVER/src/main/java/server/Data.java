package server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Data {
    String key;
    String value;
    String timestamp;
    int frequency;
}
