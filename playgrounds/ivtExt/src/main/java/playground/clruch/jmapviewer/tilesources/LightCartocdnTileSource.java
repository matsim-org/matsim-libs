package playground.clruch.jmapviewer.tilesources;

public class LightCartocdnTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "o", "p" };

    public LightCartocdnTileSource() {
        super("CartocdnLight", "http://%s.basemaps.cartocdn.com/light_all", "CartocdnLight", SERVER);
    }

 // FIXME attribution
}