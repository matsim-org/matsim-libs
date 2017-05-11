package playground.clruch.jmapviewer.tilesources;

/**
 * The default "Mapnik" OSM tile source.
 */
public class OpenCycleTileSource extends CyclicTileSource {
    private static final String[] SERVER = { "a", "b", "c" };

    /**
     * Constructs a new {@code "Mapnik"} tile source.
     */
    public OpenCycleTileSource() {
        super("OpenCycle", "http://%s.tile2.opencyclemap.org/transport", "OpenCycle", SERVER);
    }

}