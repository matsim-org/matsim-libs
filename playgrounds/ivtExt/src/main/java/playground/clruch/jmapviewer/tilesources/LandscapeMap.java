package playground.clruch.jmapviewer.tilesources;

/**
 * The default "Mapnik" OSM tile source.
 */
public class LandscapeMap extends AbstractOsmTileSource {

    private static final String PATTERN = "https://%s.tile.thunderforest.com/landscape";

    private static final String[] SERVER = { "a", "b", "c" };

    private int serverNum;

    /**
     * Constructs a new {@code "Mapnik"} tile source.
     */
    public LandscapeMap() {
        super("Landscape", PATTERN, "landscape");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }
}