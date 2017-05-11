package playground.clruch.jmapviewer.tilesources;

/**
 * Seamark overlay
 */
public class SeamarkTileSource extends AbstractOsmTileSource {

    private static final String PATTERN = "http://tiles.openseamap.org/seamark";

    public SeamarkTileSource() {
        super("Seamark", PATTERN, "seamark");
    }

    @Override
    public String getBaseUrl() {
        return PATTERN;
    }

}