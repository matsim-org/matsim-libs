package playground.clruch.gfx;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;

import playground.clruch.gfx.helper.SiouxFallstoWGS84;
import playground.clruch.gfx.helper.WGS84toSiouxFalls;

public enum ReferenceFrame {
    IDENTITY( //
            new IdentityTransformation(), //
            new IdentityTransformation()), //
    SIOUXFALLS( //
            new SiouxFallstoWGS84(), //
            new WGS84toSiouxFalls()), //
    SWITZERLAND( //
            new CH1903LV03PlustoWGS84(), //
            new WGS84toCH1903LV03Plus()), //
    ;
    // ---
    CoordinateTransformation coords_toWGS84;
    CoordinateTransformation coords_fromWGS84;

    private ReferenceFrame(CoordinateTransformation c1, CoordinateTransformation c2) {
        this.coords_toWGS84 = c1;
        this.coords_fromWGS84 = c2;

    }
}
