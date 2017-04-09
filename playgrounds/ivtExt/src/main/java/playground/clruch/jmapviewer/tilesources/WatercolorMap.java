package playground.clruch.jmapviewer.tilesources;

public class WatercolorMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tile.stamen.com/watercolor";

    private static final String[] SERVER = { "a", "b", "c", "d" };

    private int serverNum;

    /**
     * Constructs a new {@code CycleMap} tile source.
     */
    public WatercolorMap() {
        super("Watercolor", PATTERN, "watercolor");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }

    @Override
    public int getMaxZoom() {
        return 18;
    }
}