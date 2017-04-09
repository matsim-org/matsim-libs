package playground.clruch.jmapviewer.tilesources;

/**
 * Wikimedia experimental
 */
public class WikimediaMap extends AbstractOsmTileSource {

    private static final String PATTERN = "https://maps.wikimedia.org/osm-intl";

    /**
     * Constructs a new {@code CycleMap} tile source.
     */
    public WikimediaMap() {
        super("Wikimedia", PATTERN, "wikimedia");
    }

    @Override
    public String getBaseUrl() {
        return PATTERN;
    }

    @Override
    public int getMaxZoom() {
        return 18;
    }
}