package playground.clruch.jmapviewer.tilesources;

/**
 * Wikimedia experimental
 */
public class WikimediaTileSource extends AbstractOsmTileSource {

    private static final String PATTERN = "https://maps.wikimedia.org/osm-intl";

    public WikimediaTileSource() {
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