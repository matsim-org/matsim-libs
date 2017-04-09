package playground.clruch.jmapviewer.tilesources;

public class BlackWhiteMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tile.stamen.com/toner";

    private static final String[] SERVER = { "a", "b", "c", "d" };

    private int serverNum;

    /**
     * Constructs a new {@code CycleMap} tile source.
     */
    public BlackWhiteMap() {
        super("BlackWhite", PATTERN, "blackwhite");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }

    @Override
    public int getMaxZoom() {
        return 16;
    }
}