// code by jph
package playground.clruch.data;

import java.awt.geom.AffineTransform;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

/**
 * confirmed to work with Sioux Falls
 */
// https://epsg.io/transform#s_srs=4326&t_srs=2056&x=7.6155211&y=47.5555322
// https://www.geodata4edu.ch/
/* package */ class SiouxFallstoWGS84 implements CoordinateTransformation {

    /**
     * functionality can NOT be reproduced by java's own {@link AffineTransform} !
     * since the matrices are too singular
     */

    private static final double m00 = -2.382211512259682e-7;
    private static final double m01 = 9.00039761518002e-6;
    private static final double m02 = 0.2913026568441075;
    private static final double m10 = 0.0000123539233417494;
    private static final double m11 = 3.29692993820347e-7;
    private static final double m12 = -106.7629025586264;

    @Override
    public Coord transform(Coord coord) {
        double x = coord.getX();
        double y = coord.getY();
        // this is not a mistake! matsim uses lon lat, but osm the other way around
        return new Coord(m10 * x + m11 * y + m12, m00 * x + m01 * y + m02);
    }

}
