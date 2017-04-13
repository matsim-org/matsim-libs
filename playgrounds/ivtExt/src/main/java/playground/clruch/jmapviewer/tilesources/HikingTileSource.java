package playground.clruch.jmapviewer.tilesources;

/**
 * Hiking overlay
 */
public class HikingTileSource extends AbstractOsmTileSource {

    private static final String PATTERN = "http://tile.waymarkedtrails.org/hiking";

    public HikingTileSource() {
        super("Hiking", PATTERN, "hiking");
    }

    @Override
    public String getBaseUrl() {
        return PATTERN;
    }

}