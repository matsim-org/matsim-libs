// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.tilesources;

import java.awt.Image;

import playground.clruch.jmapviewer.interfaces.ICoordinate;

/**
 * Abstract class for OSM Tile sources
 */
abstract class AbstractOsmTileSource extends TMSTileSource {

    /**
     * The OSM attribution. Must be always in line with
     * <a href="https://www.openstreetmap.org/copyright/en">https://www.openstreetmap.org/copyright/en</a>
     */
    public static final String DEFAULT_OSM_ATTRIBUTION = "\u00a9 OpenStreetMap contributors";

    /**
     * Constructs a new OSM tile source
     * 
     * @param name
     *            Source name as displayed in GUI
     * @param baseUrl
     *            Source URL
     * @param id
     *            unique id for the tile source; contains only characters that
     *            are safe for file names; can be null
     */
    public AbstractOsmTileSource(String name, String baseUrl, String id) {
        super(new TileSourceInfo(name, baseUrl, id));
    }

    @Override
    public int getMaxZoom() {
        return 19;
    }

    @Override
    public boolean requiresAttribution() {
        return true;
    }

    @Override
    public String getAttributionText(int zoom, ICoordinate topLeft, ICoordinate botRight) {
        return DEFAULT_OSM_ATTRIBUTION;
    }

    @Override
    public String getAttributionLinkURL() {
        return "https://openstreetmap.org/";
    }

    @Override
    public Image getAttributionImage() {
        return null;
    }

    @Override
    public String getAttributionImageURL() {
        return null;
    }

    @Override
    public String getTermsOfUseText() {
        return null;
    }

    @Override
    public String getTermsOfUseURL() {
        return "https://www.openstreetmap.org/copyright";
    }
}
