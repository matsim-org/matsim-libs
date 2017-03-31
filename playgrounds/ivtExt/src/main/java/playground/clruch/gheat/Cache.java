package playground.clruch.gheat;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import playground.clruch.gheat.graphics.ColorScheme;

public class Cache {
    private static Map<ColorScheme, BufferedImage> _emptyTile = new HashMap<>();
    private static Object syncroot = new Object();

    private Cache() {
    }

    public static boolean hasEmptyTile(ColorScheme key) {
        return _emptyTile.containsKey(key);
    }

    public static BufferedImage getEmptyTile(ColorScheme key) {
        return _emptyTile.get(key);
    }

    public static void putEmptyTile(ColorScheme key, BufferedImage tile) {
        synchronized (syncroot) {
            _emptyTile.put(key, tile);
        }
    }
}
