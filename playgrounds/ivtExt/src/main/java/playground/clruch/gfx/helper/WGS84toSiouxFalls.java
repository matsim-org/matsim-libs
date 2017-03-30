package playground.clruch.gfx.helper;

import java.awt.geom.AffineTransform;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

public class WGS84toSiouxFalls implements CoordinateTransformation {

    /**
     * can NOT be done with {@link AffineTransform} !
     */

    private static final double m00 = -2963.03282082134;
    private static final double m01 = 80888.80878297;
    private static final double m02 = 8.63678714951313e6;
    private static final double m10 = 111027.7773978451;
    private static final double m11 = 2140.952652700341;
    private static final double m12 = 196231.6329033908;

    @Override
    public Coord transform(Coord coord) {
        double y = coord.getX();
        double x = coord.getY();
        // TODO still need to test
        return new Coord(m00 * x + m01 * y + m02, m10 * x + m11 * y + m12);
    }

}
