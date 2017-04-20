// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.tilesources;

import java.util.Map;
import java.util.Set;

/**
 * Data class that keeps basic information about a tile source.
 *
 * @since 31122
 */
class TileSourceInfo {
    /** id for this imagery entry, optional at the moment */
    protected String id;

    /** URL of the imagery service */
    protected String url;

    /** name of the imagery layer */
    protected String name;

    /** headers meaning, that there is no tile at this zoom level */
    protected Map<String, Set<String>> noTileHeaders;

    /** checksum of empty tiles */
    protected Map<String, Set<String>> noTileChecksums;

    /** minimum zoom level supported by the tile source */
    protected int minZoom;

    /** maximum zoom level supported by the tile source */
    protected int maxZoom;

    /** cookies that needs to be sent to tile source */
    protected String cookies = "";

    /** tile size of the displayed tiles */
    private int tileSize = -1;

    /** mapping &lt;header key, metadata key&gt; */
    protected Map<String, String> metadataHeaders;

    /**
     * Create a TileSourceInfo class
     *
     * @param name
     *            name
     * @param baseUrl
     *            base URL
     * @param id
     *            unique id
     */
    public TileSourceInfo(String name, String baseUrl, String id) {
        this.name = name;
        this.url = baseUrl;
        this.id = id;
    }

    /**
     * Create a TileSourceInfo class
     *
     * @param name
     *            name
     */
    public TileSourceInfo(String name) {
        this(name, null, null);
    }

    /**
     * Creates empty TileSourceInfo class
     */
    public TileSourceInfo() {
        this(null, null, null);
    }

    /**
     * Request name of the tile source
     * 
     * @return name of the tile source
     */
    public final String getName() {
        return name;
    }

    /**
     * Request URL of the tile source
     * 
     * @return url of the tile source
     */
    public final String getUrl() {
        return url;
    }

    /**
     * Request ID of the tile source. Id can be null. This gets the configured id as is.
     * Due to a user error, this may not be unique.
     * 
     * @return id of the tile source
     */
    public final String getId() {
        return id;
    }

    /**
     * Request header information for empty tiles for servers delivering such tile types
     * 
     * @return map of headers, that when set, means that this is "no tile at this zoom level" situation
     * @since 32022
     */
    public Map<String, Set<String>> getNoTileHeaders() {
        return noTileHeaders;
    }

    /**
     * Checkusm for empty tiles for servers delivering such tile types
     * 
     * @return map of checksums, that when detected, means that this is "no tile at this zoom level" situation
     * @since 32022
     */
    public Map<String, Set<String>> getNoTileChecksums() {
        return noTileChecksums;
    }

    /**
     * Request supported minimum zoom level
     * 
     * @return minimum zoom level supported by tile source
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * Request supported maximum zoom level
     * 
     * @return maximum zoom level supported by tile source
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * Request cookies to be sent together with request
     * 
     * @return cookies to be sent along with request to tile source
     */
    public String getCookies() {
        return cookies;
    }

    /**
     * Request tile size of this tile source
     * 
     * @return tile size provided by this tile source, or -1 when default value should be used
     */
    public int getTileSize() {
        return tileSize;
    }

    /**
     * Request metadata headers
     * 
     * @return mapping &lt;HTTP header name, Metadata key name&gt; for copying HTTP headers to Tile metadata
     * @since 31125
     */
    public Map<String, String> getMetadataHeaders() {
        return metadataHeaders;
    }

    /**
     * Sets the tile size provided by this tile source
     * 
     * @param tileSize
     *            tile size in pixels
     */
    public final void setTileSize(int tileSize) {
        if (tileSize == 0 || tileSize < -1) {
            throw new AssertionError("Invalid tile size: " + tileSize);
        }
        this.tileSize = tileSize;
    }

    /**
     * Sets the tile URL.
     * 
     * @param url
     *            tile URL
     */
    public final void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the tile name.
     * 
     * @param name
     *            tile name
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the tile id.
     * 
     * @param id
     *            tile id
     */
    public final void setId(String id) {
        this.id = id;
    }
}
