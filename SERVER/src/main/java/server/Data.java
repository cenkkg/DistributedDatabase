package server;

public class Data {
    String key;
    String value;
    String timestamp;
    int frequency;

    /**
     * Get key of data
     *
     * @param
     * @return key
     */
    public String getKey() {
        return key;
    }


    /**
     * Get value of data
     *
     * @param
     * @return value
     */
    public String getValue() {
        return value;
    }


    /**
     * Get timestamp of data
     *
     * @param
     * @return timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }


    /**
     * Get frequency of data
     *
     * @param
     * @return frequency
     */
    public int getFrequency() {
        return frequency;
    }


    /**
     * Set key of data
     *
     * @param key
     * @return
     */
    public void setKey(String key) {
        this.key = key;
    }


    /**
     * Set value of data
     *
     * @param value
     * @return
     */
    public void setValue(String value) {
        this.value = value;
    }


    /**
     * Set timestamp of data
     *
     * @param timestamp
     * @return
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    /**
     * Set frequency of data
     *
     * @param frequency
     * @return
     */
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
