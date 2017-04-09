package playground.clruch.jmapviewer.tilesources;

/**
 * The default "Mapnik" OSM tile source.
 */
public class RailwayOverlay extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.tiles.openrailwaymap.org/standard";

    private static final String[] SERVER = { "a", "b", "c" };

    private int serverNum;

    /**
     * Constructs a new {@code "Mapnik"} tile source.
     */
    public RailwayOverlay() {
        super("RailwayOverlay", PATTERN, "RailwayOverlay");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }
}