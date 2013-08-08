package playground.wdoering.oldstufffromgregor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class DenseMultiPointFromGeometries {
	private final GeometryFactory geofac = new GeometryFactory();

	private static final double MAX_STEP_SIZE = .05;

	private final List<Point> points = new ArrayList<Point>();

	public MultiPoint getDenseMultiPointFromGeometryCollection(Collection<Geometry> geos) {
		MultiPoint ret = null;
		for (Geometry geo : geos){
			if (geo instanceof LineString) {
				handleLineString((LineString)geo);
			} else if (geo instanceof MultiLineString) {
				handleMultiLineString((MultiLineString)geo);
			} else if (geo instanceof MultiPolygon){
				handleMultiPolygon((MultiPolygon)geo);
			} else if (geo instanceof Polygon){
				handlePolygon((Polygon) geo);
			}else{
				throw new RuntimeException("Unsupported geometry type:" + geo.getGeometryType());
			}
		}


		Point [] pointsA = new Point[this.points.size()];
		for (int i = 0; i < this.points.size(); i++) {
			pointsA[i] = this.points.get(i);
		}

		ret = this.geofac.createMultiPoint(pointsA);
		return ret;
	}


	private void handleMultiPolygon(MultiPolygon geo) {
		for (int i = 0; i < geo.getNumGeometries(); i++) {
			Geometry g = geo.getGeometryN(i);
			handlePolygon((Polygon)g);
		}

	}


	private void handlePolygon(Polygon g) {
		LineString shell = g.getExteriorRing();
		handleLineString(shell);
		for (int i = 0; i < g.getNumInteriorRing(); i++)  {
			LineString hole = g.getInteriorRingN(i);
			handleLineString(hole);
		}

	}


	private void handleMultiLineString(MultiLineString ml) {
		for (int i = 0; i < ml.getNumGeometries(); i++) {
			Geometry geo = ml.getGeometryN(i);
			handleLineString((LineString) geo);
		}

	}


	private void handleLineString(LineString l) {
		for (int i = 0; i < l.getNumPoints(); i++) {
			Point p = l.getPointN(i);
			if (i > 0) {
				Point old = l.getPointN(i-1);
				double dist = old.distance(p);
				double dx = (p.getX() - old.getX())/dist;
				double dy = (p.getY() - old.getY())/dist;
				double steps = dist/MAX_STEP_SIZE;
				for (int j = 1; j < steps; j++) {
					double x = old.getX() + j*MAX_STEP_SIZE*dx;
					double y = old.getY() + j*MAX_STEP_SIZE*dy;
					Point tmp = this.geofac.createPoint(new Coordinate(x,y));
					this.points.add(tmp);
				}
			}
			this.points.add(p);
		}
	}

}
