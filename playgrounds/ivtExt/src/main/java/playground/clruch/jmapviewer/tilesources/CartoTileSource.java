package playground.clruch.jmapviewer.tilesources;

abstract class CartoTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "o", "p" };

    CartoTileSource(String name, String baseUrl, String id) {
        super(name, baseUrl, id, SERVER);
    }

    @Override
    public final String getTermsOfUseText() {
        return "CARTO";
    }

    @Override
    public final String getTermsOfUseURL() {
        return "http://www.carto.com";
    }

}