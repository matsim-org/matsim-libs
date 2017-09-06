// code by jph
package playground.clib.jmapviewer.tilesources;

public class GrayMapnikTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c" };

    public GrayMapnikTileSource() {
        super("GrayMapnik", "http://%s.tiles.wmflabs.org/bw-mapnik", "GrayMapnik", SERVER);
    }

}