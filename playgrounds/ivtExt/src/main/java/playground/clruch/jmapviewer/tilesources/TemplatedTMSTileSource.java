// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.tilesources;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import playground.clruch.jmapviewer.interfaces.TemplatedTileSource;

/**
 * Handles templated TMS Tile Source. Templated means, that some patterns within
 * URL gets substituted.
 *
 * Supported parameters
 * {zoom} - substituted with zoom level
 * {z} - as above
 * {NUMBER-zoom} - substituted with result of equation "NUMBER - zoom",
 * eg. {20-zoom} for zoom level 15 will result in 5 in this place
 * {zoom+number} - substituted with result of equation "zoom + number",
 * eg. {zoom+5} for zoom level 15 will result in 20.
 * {x} - substituted with X tile number
 * {y} - substituted with Y tile number
 * {!y} - substituted with Yahoo Y tile number
 * {-y} - substituted with reversed Y tile number
 * {switch:VAL_A,VAL_B,VAL_C,...} - substituted with one of VAL_A, VAL_B, VAL_C. Usually
 * used to specify many tile servers
 * {header:(HEADER_NAME,HEADER_VALUE)} - sets the headers to be sent to tile server
 */
class TemplatedTMSTileSource extends TMSTileSource implements TemplatedTileSource {

    private Random rand;
    private String[] randomParts;
    private final Map<String, String> headers = new HashMap<>();

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private static final String COOKIE_HEADER = "Cookie";
    private static final String PATTERN_ZOOM = "\\{(?:(\\d+)-)?z(?:oom)?([+-]\\d+)?\\}";
    private static final String PATTERN_X = "\\{x\\}";
    private static final String PATTERN_Y = "\\{y\\}";
    private static final String PATTERN_Y_YAHOO = "\\{!y\\}";
    private static final String PATTERN_NEG_Y = "\\{-y\\}";
    private static final String PATTERN_SWITCH = "\\{switch:([^}]+)\\}";
    private static final String PATTERN_HEADER = "\\{header\\(([^,]+),([^}]+)\\)\\}";
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private static final String[] ALL_PATTERNS = { PATTERN_HEADER, PATTERN_ZOOM, PATTERN_X, PATTERN_Y, PATTERN_Y_YAHOO, PATTERN_NEG_Y, PATTERN_SWITCH };

    /**
     * Creates Templated TMS Tile Source based on ImageryInfo
     * 
     * @param info
     *            imagery info
     */
    public TemplatedTMSTileSource(TileSourceInfo info) {
        super(info);
        if (info.getCookies() != null) {
            headers.put(COOKIE_HEADER, info.getCookies());
        }
        handleTemplate();
    }

    private void handleTemplate() {
        // Capturing group pattern on switch values
        Matcher m = Pattern.compile(".*" + PATTERN_SWITCH + ".*").matcher(baseUrl);
        if (m.matches()) {
            rand = new Random();
            randomParts = m.group(1).split(",");
        }
        Pattern pattern = Pattern.compile(PATTERN_HEADER);
        StringBuffer output = new StringBuffer();
        Matcher matcher = pattern.matcher(baseUrl);
        while (matcher.find()) {
            headers.put(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        baseUrl = output.toString();
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        int finalZoom = zoom;
        Matcher m = Pattern.compile(".*" + PATTERN_ZOOM + ".*").matcher(this.baseUrl);
        if (m.matches()) {
            if (m.group(1) != null) {
                finalZoom = Integer.parseInt(m.group(1)) - zoom;
            }
            if (m.group(2) != null) {
                String ofs = m.group(2);
                if (ofs.startsWith("+"))
                    ofs = ofs.substring(1);
                finalZoom += Integer.parseInt(ofs);
            }
        }
        String r = this.baseUrl.replaceAll(PATTERN_ZOOM, Integer.toString(finalZoom)).replaceAll(PATTERN_X, Integer.toString(tilex)).replaceAll(PATTERN_Y, Integer.toString(tiley))
                .replaceAll(PATTERN_Y_YAHOO, Integer.toString((int) Math.pow(2, zoom - 1) - 1 - tiley))
                .replaceAll(PATTERN_NEG_Y, Integer.toString((int) Math.pow(2, zoom) - 1 - tiley));
        if (rand != null) {
            r = r.replaceAll(PATTERN_SWITCH, randomParts[rand.nextInt(randomParts.length)]);
        }
        return r;
    }

    /**
     * Checks if url is acceptable by this Tile Source
     * 
     * @param url
     *            URL to check
     */
    public static void checkUrl(String url) {
        assert url != null && !"".equals(url) : "URL cannot be null or empty";
        Matcher m = Pattern.compile("\\{[^}]*\\}").matcher(url);
        while (m.find()) {
            boolean isSupportedPattern = false;
            for (String pattern : ALL_PATTERNS) {
                if (m.group().matches(pattern)) {
                    isSupportedPattern = true;
                    break;
                }
            }
            if (!isSupportedPattern) {
                throw new IllegalArgumentException(m.group() + " is not a valid TMS argument. Please check this server URL:\n" + url);
            }
        }
    }
}
