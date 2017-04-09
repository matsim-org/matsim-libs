// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.tilesources;

/**
 * OSM Tile source.
 */
public class OsmTileSource {

    /**
     * The default "Mapnik" OSM tile source.
     */
    public static class Mapnik extends AbstractOsmTileSource {

        private static final String PATTERN = "https://%s.tile.openstreetmap.org";

        private static final String[] SERVER = { "a", "b", "c" };

        private int serverNum;

        /**
         * Constructs a new {@code "Mapnik"} tile source.
         */
        public Mapnik() {
            super("Mapnik", PATTERN, "MAPNIK");
        }

        @Override
        public String getBaseUrl() {
            String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
            serverNum = (serverNum + 1) % SERVER.length;
            return url;
        }
    }

    /**
     * The "Cycle Map" OSM tile source.
     */
    public static class CycleMap extends AbstractOsmTileSource {

        private static final String PATTERN = "http://%s.tile.opencyclemap.org/cycle";

        private static final String[] SERVER = { "a", "b", "c" };

        private int serverNum;

        /**
         * Constructs a new {@code CycleMap} tile source.
         */
        public CycleMap() {
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

    /**
     * Humanitarian focused OSM base layer
     */
    public static class HotMap extends AbstractOsmTileSource {

        private static final String PATTERN = "http://%s.tile.openstreetmap.fr/hot";

        private static final String[] SERVER = { "a", "b" };

        private int serverNum;

        /**
         * Constructs a new {@code CycleMap} tile source.
         */
        public HotMap() {
            super("HotMap", PATTERN, "hot");
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

    /**
     * French OSM base layer
     */
    public static class FrenchMap extends AbstractOsmTileSource {

        private static final String PATTERN = "http://%s.tile.openstreetmap.fr/osmfr";

        private static final String[] SERVER = { "a", "b", "c" };

        private int serverNum;

        /**
         * Constructs a new {@code CycleMap} tile source.
         */
        public FrenchMap() {
            super("FrenchMap", PATTERN, "french");
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

    /**
     * Wikimedia experimental
     */
    public static class WikimediaMap extends AbstractOsmTileSource {

        private static final String PATTERN = "https://%s.wikimedia.org/osm-intl";

        private static final String[] SERVER = { "maps" };

        private int serverNum;

        /**
         * Constructs a new {@code CycleMap} tile source.
         */
        public WikimediaMap() {
            super("Wikimedia", PATTERN, "wikimedia");
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
}
