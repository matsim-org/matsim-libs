package playground.clruch.jmapviewer.tilesources;

abstract class CyclicTileSource extends AbstractOsmTileSource {

    private final String[] SERVER;

    private int serverNum;

    /**
     * Constructs a new {@code "Mapnik"} tile source.
     */
    protected CyclicTileSource(String name, String baseUrl, String id, String[] SERVER) {
        super(name, baseUrl, id);
        this.SERVER = SERVER;
    }

    @Override
    public final String getBaseUrl() {
        String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
        serverNum = (serverNum + 1) % SERVER.length;
        return url;
    }
}