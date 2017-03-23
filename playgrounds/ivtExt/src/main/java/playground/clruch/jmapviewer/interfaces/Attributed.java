// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.interfaces;

import java.awt.Image;

public interface Attributed {
    /**
     * @return True if the tile source requires attribution in text or image form.
     */
    boolean requiresAttribution();

    /**
     * @param zoom The optional zoom level for the view.
     * @param botRight The bottom right of the bounding box for attribution.
     * @param topLeft The top left of the bounding box for attribution.
     * @return Attribution text for the image source.
     */
    String getAttributionText(int zoom, ICoordinate topLeft, ICoordinate botRight);

    /**
     * @return The URL to open when the user clicks the attribution text.
     */
    String getAttributionLinkURL();

    /**
     * @return The URL for the attribution image. Null if no image should be displayed.
     */
    Image getAttributionImage();

    /**
     * @return The URL to open when the user clicks the attribution image.
     * When return value is null, the image is still displayed (provided getAttributionImage()
     * returns a value other than null), but the image does not link to a website.
     */
    String getAttributionImageURL();

    /**
     * @return The attribution "Terms of Use" text.
     * In case it returns null, but getTermsOfUseURL() is not null, a default
     * terms of use text is used.
     */
    String getTermsOfUseText();

    /**
     * @return The URL to open when the user clicks the attribution "Terms of Use" text.
     */
    String getTermsOfUseURL();
}
