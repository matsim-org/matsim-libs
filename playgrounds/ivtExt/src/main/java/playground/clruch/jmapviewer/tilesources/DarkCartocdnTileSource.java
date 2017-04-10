package playground.clruch.jmapviewer.tilesources;

public class DarkCartocdnTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "o", "p" };

    public DarkCartocdnTileSource() {
        super("CartoDark", "http://%s.basemaps.cartocdn.com/dark_all", "cartodark", SERVER);
    }
    
    // FIXME attribution

}