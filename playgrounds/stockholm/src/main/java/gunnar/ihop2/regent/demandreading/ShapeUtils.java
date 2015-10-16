package gunnar.ihop2.regent.demandreading;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class ShapeUtils {

	private ShapeUtils() {
	}

	public static Coord drawPointFromGeometry(final Geometry geom) {
		final Random rnd = MatsimRandom.getLocalInstance();
		final double deltaX = geom.getEnvelopeInternal().getMaxX()
				- geom.getEnvelopeInternal().getMinX();
		final double deltaY = geom.getEnvelopeInternal().getMaxY()
				- geom.getEnvelopeInternal().getMinY();
		Point p;
		do {
			final double x = geom.getEnvelopeInternal().getMinX()
					+ rnd.nextDouble() * deltaX;
			final double y = geom.getEnvelopeInternal().getMinY()
					+ rnd.nextDouble() * deltaY;
			p = MGC.xy2Point(x, y);
		} while (!geom.contains(p));
		return new Coord(p.getX(), p.getY());
	}
}
