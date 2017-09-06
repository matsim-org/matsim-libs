// code by jph
package playground.clib.jmapviewer.tilesources;

public class FrenchTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c" };

    public FrenchTileSource() {
        super("FrenchMap", "http://%s.tile.openstreetmap.fr/osmfr", "french", SERVER);
    }

}