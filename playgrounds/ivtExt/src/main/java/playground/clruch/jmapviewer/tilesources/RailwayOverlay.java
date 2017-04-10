package playground.clruch.jmapviewer.tilesources;

/**
 * tiles are of different size
 */
@Deprecated
public class RailwayOverlay extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c" };

    public RailwayOverlay() {
        super("RailwayOverlay", "http://%s.tiles.openrailwaymap.org/standard", "RailwayOverlay", SERVER);
    }

}