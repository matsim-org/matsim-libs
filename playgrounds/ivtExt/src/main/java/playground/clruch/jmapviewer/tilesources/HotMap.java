package playground.clruch.jmapviewer.tilesources;

/**
 * Humanitarian focused OSM base layer
 */
public class HotMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tile.openstreetmap.fr/hot";

    private static final String[] SERVER = { "a", "b" };

    private int serverNum;

    /**
     * Constructs a new {@code CycleMap} tile source.
     */
    public HotMap() {
        super("HotMap", PATTERN, "hot");
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