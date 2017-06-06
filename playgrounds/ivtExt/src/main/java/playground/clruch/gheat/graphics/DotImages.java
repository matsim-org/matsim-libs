// code by jph
package playground.clruch.gheat.graphics;

import java.util.ArrayList;
import java.util.List;

public enum DotImages {
    ;
    // ---
    private static final List<DotImage> dotsList = new ArrayList<>();

    static {
        for (int zoom = 0; zoom <= 30; ++zoom) 
            dotsList.add(new DotImage(zoom));
    }

    public static DotImage get(int zoom) {
        return dotsList.get(zoom);
    }

}
