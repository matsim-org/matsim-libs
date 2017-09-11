// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer.interfaces;

import java.util.Map;

/**
 * Interface for template tile sources, @see TemplatedTMSTileSource
 *
 * @author Wiktor Niesiobędzki
 * @since 1.10
 */
public interface TemplatedTileSource extends TileSource {
    /**
     *
     * @return headers to be sent with http requests
     */
    Map<String, String> getHeaders();
}
