package org.matsim.contrib.emissions.analysis;

import java.util.HashMap;
import java.util.Map;

public class TimeBin<K, V> {

    private double startTime;
    private Map<K, V> values = new HashMap<>();

    public TimeBin(double startTime) {
        this.startTime = startTime;
    }

    public double getStartTime() {
        return startTime;
    }

    public V get(K key) {
        return values.get(key);
    }

    public V put(K key, V value) {
        return values.put(key, value);
    }
}
