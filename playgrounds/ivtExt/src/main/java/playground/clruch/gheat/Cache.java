package playground.clruch.gheat;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Cache {
    private static Map<String, BufferedImage> _emptyTile = new HashMap<String, BufferedImage>();
    private static Object syncroot = new Object();

    private Cache() {
    }

    public static boolean hasEmptyTile(int key, int zoomOpacity) {
        return _emptyTile.containsKey(key + "_" + zoomOpacity);
    }

    public static BufferedImage getEmptyTile(int key, int zoomOpacity) {
        return _emptyTile.get(key + "_" + zoomOpacity);
    }

    public static void putEmptyTile(BufferedImage tile, int key, int zoomOpacity) {
        synchronized (syncroot) {
            _emptyTile.put(Integer.toString(key) + "_" + zoomOpacity, tile);
        }
    }
}
