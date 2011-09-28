package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MultiPolygonFromLineStrings {

	private static final double FETCH_RADIUS = 0.2;

	GeometryFactory geofac = new GeometryFactory();

	private enum Connector {Start, End};

	public MultiPolygon getMultiPolygon(Collection<LineString> ls, Envelope e) {
		MultiPolygon ret = null;



		QuadTree<LineString> lsTree = new QuadTree<LineString>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());


		List<Point> points = new ArrayList<Point>();
		for (LineString  l : ls) {
			Point st = l.getStartPoint();
			Point en = l.getEndPoint();
			lsTree.put(st.getX(), st.getY(), l);//TODO make this robust (i.e. handle situations where another line string already exists at the location of st)
			lsTree.put(en.getX(), en.getY(), l);//TODO same here
			points.add(st);
			points.add(en);
		}


		//		List<LinearRing> lrs = new ArrayList<LinearRing>();
		List<Polygon> ps = new ArrayList<Polygon>();
		for (Point p : points) {
			Collection<LineString> neighbors = lsTree.get(p.getX(),p.getY(), FETCH_RADIUS);

			if (neighbors.size() == 2) {
				Iterator<LineString> it = neighbors.iterator();
				LineString ls1 = it.next();
				LineString ls2 = it.next();

				lsTree.remove(ls1.getStartPoint().getX(), ls1.getStartPoint().getY(), ls1);
				lsTree.remove(ls1.getEndPoint().getX(), ls1.getEndPoint().getY(), ls1);
				lsTree.remove(ls2.getStartPoint().getX(), ls2.getStartPoint().getY(), ls2);
				lsTree.remove(ls2.getEndPoint().getX(), ls2.getEndPoint().getY(), ls2);

				if (ls1.equals(ls2)) {
					ls1.getEndPoint().getCoordinate().setCoordinate(new Coordinate(ls1.getStartPoint().getCoordinate()));
					LinearRing lr = this.geofac.createLinearRing(ls1.getCoordinates());
					ps.add(this.geofac.createPolygon(lr, null));
				} else {
					LineString tmp = combine(ls1,ls2);
					lsTree.put(tmp.getStartPoint().getX(), tmp.getStartPoint().getY(), tmp);
					lsTree.put(tmp.getEndPoint().getX(), tmp.getEndPoint().getY(), tmp);
				}
			} else if (neighbors.size() == 1){
				Iterator<LineString> it = neighbors.iterator();
				LineString ls1 = it.next();
				Coordinate [] coords = new Coordinate [ls1.getNumPoints() * 2];
				for (int i = 0; i < ls1.getNumPoints(); i++) {
					coords[i] = ls1.getCoordinateN(i);
				}
				int j = ls1.getNumPoints();
				for (int i = ls1.getNumPoints()-1; i >= 0; i--) {
					coords[j++] = ls1.getCoordinateN(i);
				}
				LinearRing lr = this.geofac.createLinearRing(coords);
				Polygon polygon = (Polygon) this.geofac.createPolygon(lr, null).buffer(0.1);
				ps.add(polygon);

			}else if (neighbors.size() >1) {
				throw new RuntimeException("size of the collections must either be 2 or 1");
			}

		}

		Polygon [] psA = new Polygon[ps.size()];
		for (int i = 0; i < ps.size(); i++) {
			psA[i] = ps.get(i);
		}

		ret = this.geofac.createMultiPolygon(psA);
		//		GisDebugger.addGeometry(ret);
		//		GisDebugger.dump("/Users/laemmel/tmp/dump.shp");

		return ret;
	}

	private LineString combine(LineString ls1, LineString ls2) {
		Connector c1 = null;
		Connector c2 = null;

		//TODO robust
		if (ls1.getStartPoint().distance(ls2.getStartPoint()) <= FETCH_RADIUS) {
			c1 = Connector.Start;
			c2 = Connector.Start;
		} else if (ls1.getStartPoint().distance(ls2.getEndPoint()) <= FETCH_RADIUS) {
			c1 = Connector.Start;
			c2 = Connector.End;
		} else if (ls1.getEndPoint().distance(ls2.getEndPoint()) <= FETCH_RADIUS) {
			c1 = Connector.End;
			c2 = Connector.End;
		} else if (ls1.getEndPoint().distance(ls2.getStartPoint()) <= FETCH_RADIUS) {
			c1 = Connector.End;
			c2 = Connector.Start;
		}

		List<Coordinate> coords = new ArrayList<Coordinate>();

		if (c1 == Connector.Start) {
			for (int i = ls1.getNumPoints() -1; i >= 0; i--) {
				coords.add(ls1.getCoordinateN(i));
			}
		} else {
			for (int i = 0; i < ls1.getNumPoints(); i++) {
				coords.add(ls1.getCoordinateN(i));
			}
		}

		if (c2 == Connector.Start) {
			for (int i = 1; i < ls2.getNumPoints(); i++) {
				coords.add(ls2.getCoordinateN(i));
			}
		} else {
			for (int i = ls2.getNumPoints() -2; i >= 0; i--) {
				coords.add(ls2.getCoordinateN(i));
			}
		}

		Coordinate [] coordsA = new Coordinate[coords.size()];
		for (int i = 0; i < coords.size(); i++) {
			coordsA[i] = coords.get(i);
		}

		return this.geofac.createLineString(coordsA);
	}

}
