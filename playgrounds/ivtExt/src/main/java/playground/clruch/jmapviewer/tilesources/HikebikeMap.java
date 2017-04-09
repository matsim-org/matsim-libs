package playground.clruch.jmapviewer.tilesources;

public class HikebikeMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tiles.wmflabs.org/hikebike";

    private static final String[] SERVER = { "a", "b", "c" };

    private int serverNum;

    /**
     * Constructs a new {@code CycleMap} tile source.
     */
    public HikebikeMap() {
        super("Hikebike", PATTERN, "hikebike");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }
}