package playground.clruch.jmapviewer.tilesources;

/**
 * Hiking overlay
 */
public class HikingMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://tile.waymarkedtrails.org/hiking";

    public HikingMap() {
        super("Hiking", PATTERN, "hiking");
    }

    @Override
    public String getBaseUrl() {
        return PATTERN;
    }

}