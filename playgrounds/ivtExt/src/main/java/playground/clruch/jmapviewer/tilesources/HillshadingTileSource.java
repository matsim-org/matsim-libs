package playground.clruch.jmapviewer.tilesources;

public class HillshadingTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c" };

    public HillshadingTileSource() {
        super("Hillshading", "http://%s.tiles.wmflabs.org/hillshading", "hillshading", SERVER);
    }

}