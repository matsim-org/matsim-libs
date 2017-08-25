// code by jph
package playground.clruch.data;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;

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
    public final CoordinateTransformation coords_toWGS84;
    public final CoordinateTransformation coords_fromWGS84;

    private ReferenceFrame(CoordinateTransformation c1, CoordinateTransformation c2) {
        this.coords_toWGS84 = c1;
        this.coords_fromWGS84 = c2;

    }    
    
    public static ReferenceFrame fromString(String stringRef){
        for(ReferenceFrame rframe : ReferenceFrame.values()){
            if(rframe.toString().equals(stringRef)) return rframe;
        }
        return null;
    }
}
