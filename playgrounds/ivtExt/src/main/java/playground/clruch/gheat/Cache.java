// code by varunpant
package playground.clruch.gheat;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import playground.clib.gheat.gui.ColorScheme;

/* package */ enum Cache {
    ;
    // ---
    private static Map<ColorScheme, BufferedImage> _emptyTile = new HashMap<>();
    private static Object syncroot = new Object();

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
