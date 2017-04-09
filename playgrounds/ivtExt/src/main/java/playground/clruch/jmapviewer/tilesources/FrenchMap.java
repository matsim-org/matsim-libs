package playground.clruch.jmapviewer.tilesources;

/**
 * French OSM base layer
 */
public class FrenchMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tile.openstreetmap.fr/osmfr";

    private static final String[] SERVER = { "a", "b", "c" };

    private int serverNum;

    /**
     * Constructs a new {@code CycleMap} tile source.
     */
    public FrenchMap() {
        super("FrenchMap", PATTERN, "french");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }

    // @Override
    // public int getMaxZoom() {
    // return 18;
    // }
}