package playground.clruch.jmapviewer.tilesources;

public class HillshadingMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tiles.wmflabs.org/hillshading";

    private static final String[] SERVER = { "a", "b", "c" };

    private int serverNum;

    /**
     * Constructs a new {@code CycleMap} tile source.
     */
    public HillshadingMap() {
        super("Hillshading", PATTERN, "hillshading");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }
}