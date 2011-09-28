package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;

import java.util.ArrayList;
import java.util.List;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

public class DenseMultiPointFromLineString {

	private final GeometryFactory geofac = new GeometryFactory();

	private static final double STEP_SIZE = .1;

	public MultiPoint getDenseMultiPointFromLineString(List<LineString> ls) {
		MultiPoint ret = null;

		List<Point> points = new ArrayList<Point>();
		for (LineString l  : ls) {
			for (int i = 0; i < l.getNumPoints(); i++) {
				Point p = l.getPointN(i);
				if (i > 0) {
					Point old = l.getPointN(i-1);
					double dist = old.distance(p);
					double dx = (p.getX() - old.getX())/dist;
					double dy = (p.getY() - old.getY())/dist;
					double steps = dist/STEP_SIZE;
					for (int j = 1; j < steps; j++) {
						double x = old.getX() + j*STEP_SIZE*dx;
						double y = old.getY() + j*STEP_SIZE*dy;
						Point tmp = this.geofac.createPoint(new Coordinate(x,y));
						points.add(tmp);
					}
				}
				points.add(p);
			}
		}

		Point [] pointsA = new Point[points.size()];
		for (int i = 0; i < points.size(); i++) {
			pointsA[i] = points.get(i);
		}

		ret = this.geofac.createMultiPoint(pointsA);
		return ret;
	}

}
