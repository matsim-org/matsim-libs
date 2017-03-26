package playground.clruch.gheat;

import java.util.ArrayList;
import java.util.List;

public class Opacity {
    /* Alpha value that indicates an image is not transparent */
    public static final int OPAQUE = 255;
    /* Alpha value that indicates that an image is transparent */
    public static final int TRANSPARENT = 0;
    /* Max zoom that google supports */
    public final int MAX_ZOOM = 31;
    public final int ZOOM_OPAQUE = -15;
    public final int ZOOM_TRANSPARENT = 15;
    public final int DEFAULT_OPACITY = 50;
    private int _zoomOpaque;
    private int _zoomTransparent;

    public Opacity(int zoomOpaque, int zoomTransparent) {
        _zoomOpaque = zoomOpaque;
        _zoomTransparent = zoomTransparent;
    }

    /* Uses default values if not specified */
    public Opacity() {
        _zoomOpaque = ZOOM_OPAQUE;
        _zoomTransparent = ZOOM_TRANSPARENT;
    }

    /*
     * Build and return the zoom_to_opacity mapping
     * returns index=zoom and value of the element is the opacity
     */
    public int[] BuildZoomMapping() {
        List<Integer> zoomMapping = new ArrayList<Integer>();
        int numberOfOpacitySteps;
        float opacityStep;
        numberOfOpacitySteps = _zoomTransparent - _zoomOpaque;
        if (numberOfOpacitySteps < 1) // don't want general fade
        {
            for (int i = 0; i <= MAX_ZOOM; i++)
                zoomMapping.add(0);
        } else // want general fade
        {
            opacityStep = ((float) OPAQUE / (float) numberOfOpacitySteps); // chunk of opacity
            for (int zoom = 0; zoom <= MAX_ZOOM; zoom++) {
                if (zoom <= _zoomOpaque)
                    zoomMapping.add(OPAQUE);
                else if (zoom >= _zoomTransparent)
                    zoomMapping.add(TRANSPARENT);
                else
                    zoomMapping.add((int) ((float) OPAQUE - (((float) zoom - (float) _zoomOpaque) * opacityStep)));
            }
        }
        return toIntArray(zoomMapping);
    }

    int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)
            ret[i++] = e.intValue();
        return ret;
    }
}
