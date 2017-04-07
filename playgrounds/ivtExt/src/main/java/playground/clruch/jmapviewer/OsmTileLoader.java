// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import playground.clruch.jmapviewer.interfaces.TileJob;
import playground.clruch.jmapviewer.interfaces.TileLoader;
import playground.clruch.jmapviewer.interfaces.TileLoaderListener;

/**
 * A {@link TileLoader} implementation that loads tiles from OSM.
 *
 * @author Jan Peter Stotz
 */
public class OsmTileLoader implements TileLoader {
    private static final ThreadPoolExecutor jobDispatcher = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

    private final class OsmTileJob implements TileJob {
        private final Tile tile;
        private InputStream input;
        private boolean force;

        private OsmTileJob(Tile tile) {
            this.tile = tile;
        }

        @Override
        public void run() {
            synchronized (tile) {
                if ((tile.isLoaded() && !tile.hasError()) || tile.isLoading())
                    return;
                tile.loaded = false;
                tile.error = false;
                tile.loading = true;
            }
            try {
                URLConnection conn = loadTileFromOsm(tile);
                if (force) {
                    conn.setUseCaches(false);
                }
                loadTileMetadata(tile, conn);
                if ("no-tile".equals(tile.getValue("tile-info"))) {
                    tile.setError("No tile at this zoom level");
                } else {
                    input = conn.getInputStream();
                    try {
                        tile.loadImage(input);
                    } finally {
                        input.close();
                        input = null;
                    }
                }
                tile.setLoaded(true);
                listener.tileLoadingFinished(tile, true);
            } catch (IOException e) {
                tile.setError(e.getMessage());
                listener.tileLoadingFinished(tile, false);
                if (input == null) {
                    try {
                        System.err.println("Failed loading " + tile.getUrl() +": "
                                +e.getClass() + ": " + e.getMessage());
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            } finally {
                tile.loading = false;
                tile.setLoaded(true);
            }
        }

        @Override
        public Tile getTile() {
            return tile;
        }

        @Override
        public void submit() {
            submit(false);
        }

        @Override
        public void submit(boolean force) {
            this.force = force;
            jobDispatcher.execute(this);
        }
    }

    /**
     * Holds the HTTP headers. Insert e.g. User-Agent here when default should not be used.
     */
    public Map<String, String> headers = new HashMap<>();

    public int timeoutConnect;
    public int timeoutRead;

    protected TileLoaderListener listener;

    public OsmTileLoader(TileLoaderListener listener) {
        this(listener, null);
    }

    public OsmTileLoader(TileLoaderListener listener, Map<String, String> headers) {
        this.headers.put("Accept", "text/html, image/png, image/jpeg, image/gif, */*");
        if (headers != null) {
            this.headers.putAll(headers);
        }
        this.listener = listener;
    }

    @Override
    public TileJob createTileLoaderJob(final Tile tile) {
        return new OsmTileJob(tile);
    }

    protected URLConnection loadTileFromOsm(Tile tile) throws IOException {
        URL url;
        url = new URL(tile.getUrl());
        URLConnection urlConn = url.openConnection();
        if (urlConn instanceof HttpURLConnection) {
            prepareHttpUrlConnection((HttpURLConnection) urlConn);
        }
        return urlConn;
    }

    protected void loadTileMetadata(Tile tile, URLConnection urlConn) {
        String str = urlConn.getHeaderField("X-VE-TILEMETA-CaptureDatesRange");
        if (str != null) {
            tile.putValue("capture-date", str);
        }
        str = urlConn.getHeaderField("X-VE-Tile-Info");
        if (str != null) {
            tile.putValue("tile-info", str);
        }

        Long lng = urlConn.getExpiration();
        if (lng.equals(0L)) {
            try {
                str = urlConn.getHeaderField("Cache-Control");
                if (str != null) {
                    for (String token: str.split(",")) {
                        if (token.startsWith("max-age=")) {
                            lng = Long.parseLong(token.substring(8)) * 1000 +
                                    System.currentTimeMillis();
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // ignore malformed Cache-Control headers
                if (JMapViewer.debug) {
                    System.err.println(e.getMessage());
                }
            }
        }
        if (!lng.equals(0L)) {
            tile.putValue("expires", lng.toString());
        }
    }

    protected void prepareHttpUrlConnection(HttpURLConnection urlConn) {
        for (Entry<String, String> e : headers.entrySet()) {
            urlConn.setRequestProperty(e.getKey(), e.getValue());
        }
        if (timeoutConnect != 0)
            urlConn.setConnectTimeout(timeoutConnect);
        if (timeoutRead != 0)
            urlConn.setReadTimeout(timeoutRead);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void cancelOutstandingTasks() {
        jobDispatcher.getQueue().clear();
    }

    /**
     * Sets the maximum number of concurrent connections the tile loader will do
     * @param num number of conncurent connections
     */
    public static void setConcurrentConnections(int num) {
        jobDispatcher.setMaximumPoolSize(num);
    }
}
