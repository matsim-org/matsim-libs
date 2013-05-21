package playground.dziemke.potsdam.population;

import java.util.Random;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


public class DrawRandomPointFromGeometry {

	public static Coord DrawRandomPoint(Geometry g) {
		   Random rnd = new Random();
		   Point p;
		   double x, y;
		   do {
		      x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
		      y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
		      p = MGC.xy2Point(x, y);
		   } while (!g.contains(p));
		   Coord coord = new CoordImpl(p.getX(), p.getY());
		   return coord;
		}
}