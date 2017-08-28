// code adapted by jph
package playground.clruch.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public enum LruCache {
    ;
    public static <K, V> Map<K, V> create(final int maxSize) {
        return new LinkedHashMap<K, V>(maxSize * 4 / 3, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        };
    }
}
