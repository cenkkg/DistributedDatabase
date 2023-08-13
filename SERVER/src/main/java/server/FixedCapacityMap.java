package server;

import java.util.LinkedHashMap;
import java.util.Map;

public class FixedCapacityMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public FixedCapacityMap(int capacity, boolean accessOrder) {
        super(capacity, 0.75f, accessOrder);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    public void slideAndInsert(K key, V value) {
        if (size() >= capacity) {
            Map.Entry<K, V> eldestEntry = entrySet().iterator().next();
            remove(eldestEntry.getKey());
        }

        LinkedHashMap<K, V> newCache = new LinkedHashMap<>(this);
        clear();

        put(key, value);
        putAll(newCache);
    }
}