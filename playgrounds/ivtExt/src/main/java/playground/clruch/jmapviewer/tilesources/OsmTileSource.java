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
     * The default "Mapnik" OSM tile source.
     */
    public static class GrayMapnik extends AbstractOsmTileSource {

        private static final String PATTERN = "http://%s.tiles.wmflabs.org/bw-mapnik";

        private static final String[] SERVER = { "a", "b", "c" };

        private int serverNum;

        /**
         * Constructs a new {@code "Mapnik"} tile source.
         */
        public GrayMapnik() {
            super("GrayMapnik", PATTERN, "GrayMapnik");
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

        // @Override
        // public int getMaxZoom() {
        // return 18;
        // }
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

        // @Override
        // public int getMaxZoom() {
        // return 18;
        // }
    }

    /**
     * Wikimedia experimental
     */
    public static class WikimediaMap extends AbstractOsmTileSource {

        private static final String PATTERN = "https://maps.wikimedia.org/osm-intl";

        /**
         * Constructs a new {@code CycleMap} tile source.
         */
        public WikimediaMap() {
            super("Wikimedia", PATTERN, "wikimedia");
        }

        @Override
        public String getBaseUrl() {
            return PATTERN;
        }

        @Override
        public int getMaxZoom() {
            return 18;
        }
    }

    public static class HikebikeMap extends AbstractOsmTileSource {

        private static final String PATTERN = "http://%s.tiles.wmflabs.org/hikebike";

        private static final String[] SERVER = { "a", "b", "c" };

        private int serverNum;

        /**
         * Constructs a new {@code CycleMap} tile source.
         */
        public HikebikeMap() {
            super("Hikebike", PATTERN, "hikebike");
        }

        @Override
        public String getBaseUrl() {
            String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
            serverNum = (serverNum + 1) % SERVER.length;
            return url;
        }
    }

    /**
     * Hiking overlay
     */
    public static class HikingMap extends AbstractOsmTileSource {

        private static final String PATTERN = "http://tile.waymarkedtrails.org/hiking";

        public HikingMap() {
            super("Hiking", PATTERN, "hiking");
        }

        @Override
        public String getBaseUrl() {
            return PATTERN;
        }

    }

    /**
     * Seamark overlay
     */
    public static class SeamarkMap extends AbstractOsmTileSource {

        private static final String PATTERN = "http://tiles.openseamap.org/seamark";

        public SeamarkMap() {
            super("Seamark", PATTERN, "seamark");
        }

        @Override
        public String getBaseUrl() {
            return PATTERN;
        }

    }

    public static class HillshadingMap extends AbstractOsmTileSource {

        private static final String PATTERN = "http://%s.tiles.wmflabs.org/hillshading";

        private static final String[] SERVER = { "a", "b", "c" };

        private int serverNum;

        /**
         * Constructs a new {@code CycleMap} tile source.
         */
        public HillshadingMap() {
            super("Hillshading", PATTERN, "hillshading");
        }

        @Override
        public String getBaseUrl() {
            String url = String.format(this.baseUrl, new Object[] { SERVER[serverNum] });
            serverNum = (serverNum + 1) % SERVER.length;
            return url;
        }
    }

}
