package playground.clruch.jmapviewer.tilesources;

/**
 * The default "Mapnik" OSM tile source.
 */
public class CartoDarkMap extends AbstractOsmTileSource {

    private static final String PATTERN = "http://%s.basemaps.cartocdn.com/dark_all";

    private static final String[] SERVER = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "o", "p" };

    private int serverNum;

    /**
     * Constructs a new {@code "Mapnik"} tile source.
     */
    public CartoDarkMap() {
        super("CartoDark", PATTERN, "cartodark");
    }

    @Override
    public String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }
}