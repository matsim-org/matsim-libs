package playground.clruch.jmapviewer.tilesources;

/**
 * The default "Mapnik" OSM tile source.
 */
public class OpenCycleMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tile2.opencyclemap.org/transport";

    private static final String[] SERVER = { "a", "b", "c" };

    private int serverNum;

    /**
     * Constructs a new {@code "Mapnik"} tile source.
     */
    public OpenCycleMap() {
        super("OpenCycle", PATTERN, "OpenCycle");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }
}