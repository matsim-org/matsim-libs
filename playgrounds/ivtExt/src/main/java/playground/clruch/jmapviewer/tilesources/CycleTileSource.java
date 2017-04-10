package playground.clruch.jmapviewer.tilesources;

/**
 * The "Cycle Map" OSM tile source.
 */
public class CycleTileSource extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tile.opencyclemap.org/cycle";

    private static final String[] SERVER = { "a", "b", "c" };

    private int serverNum;

    /**
     * Constructs a new {@code CycleMap} tile source.
     */
    public CycleTileSource() {
        super("Cyclemap", PATTERN, "opencyclemap");
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