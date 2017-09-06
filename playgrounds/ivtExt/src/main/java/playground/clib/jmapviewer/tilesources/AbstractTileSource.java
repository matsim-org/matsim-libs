// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer.tilesources;

import java.awt.Image;
import java.util.List;
import java.util.Map;

import playground.clib.jmapviewer.interfaces.ICoordinate;
import playground.clib.jmapviewer.interfaces.TileSource;

abstract class AbstractTileSource implements TileSource {

    protected String attributionText;
    protected String attributionLinkURL;
    protected Image attributionImage;
    protected String attributionImageURL;
    protected String termsOfUseText;
    protected String termsOfUseURL;

    @Override
    public boolean requiresAttribution() {
        return attributionText != null || attributionImage != null || termsOfUseText != null || termsOfUseURL != null;
    }

    @Override
    public String getAttributionText(int zoom, ICoordinate topLeft, ICoordinate botRight) {
        return attributionText;
    }

    @Override
    public String getAttributionLinkURL() {
        return attributionLinkURL;
    }

    @Override
    public Image getAttributionImage() {
        return attributionImage;
    }

    @Override
    public String getAttributionImageURL() {
        return attributionImageURL;
    }

    @Override
    public String getTermsOfUseText() {
        return termsOfUseText;
    }

    @Override
    public String getTermsOfUseURL() {
        return termsOfUseURL;
    }

    public void setAttributionText(String attributionText) {
        this.attributionText = attributionText;
    }

    public void setAttributionLinkURL(String attributionLinkURL) {
        this.attributionLinkURL = attributionLinkURL;
    }

    public void setAttributionImage(Image attributionImage) {
        this.attributionImage = attributionImage;
    }

    public void setAttributionImageURL(String attributionImageURL) {
        this.attributionImageURL = attributionImageURL;
    }

    public void setTermsOfUseText(String termsOfUseText) {
        this.termsOfUseText = termsOfUseText;
    }

    public void setTermsOfUseURL(String termsOfUseURL) {
        this.termsOfUseURL = termsOfUseURL;
    }

    @Override
    public boolean isNoTileAtZoom(Map<String, List<String>> headers, int statusCode, byte[] content) {
        // default handler - when HTTP 404 is returned, then treat this situation as no tile at this zoom level
        return statusCode == 404;
    }
}
