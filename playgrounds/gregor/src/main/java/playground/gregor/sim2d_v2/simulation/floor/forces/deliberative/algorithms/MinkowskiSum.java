package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.algorithms;

import java.util.ArrayList;
import java.util.List;

import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.geometries.CCWPolygon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class MinkowskiSum {

	private static final GeometryFactory geofac = new GeometryFactory();

	public static Polygon getMinkowskiSum(CCWPolygon p1, CCWPolygon p2) {

		Coordinate[] w = p1.getCCWRing();
		Coordinate[] v = p2.getReflectedCCWRing();



		List<Coordinate>minkowskiCoords = new ArrayList<Coordinate>();

		int i = 0;
		int j = 0;

		do {
			minkowskiCoords.add(new Coordinate(v[i].x + w[j].x, v[i].y + w[j].y));

			if ( i == v.length -1) {
				j++;
			} else if (j == w.length -1) {
				i++;
			} else {
				double angleVi = PolarAngle.getPolarAngle(v[i], v[i+1]);
				double angleWi = PolarAngle.getPolarAngle(w[j], w[j+1]);

				if (angleVi < angleWi) {
					i++;
				} else if (angleVi > angleWi) {
					j++;
				} else {
					i++;
					j++;
				}
			}

		}while (i < v.length && j < w.length);

		Coordinate[] a = minkowskiCoords.toArray(new Coordinate[0]);

		return geofac.createPolygon(geofac.createLinearRing(a), null);
	}

	public static void main(String [] args) {

		Coordinate [] cs1 = {new Coordinate(0,1),new Coordinate(0,2),new Coordinate(1,2),new Coordinate(2,1), new Coordinate(0,1)};
		Coordinate [] cs2 = {new Coordinate(5,1),new Coordinate(5,0), new Coordinate(4,0), new Coordinate(4,1), new Coordinate(5,1)};

		CCWPolygon ccwp1 = new CCWPolygon(cs1,new Coordinate(.5,1.5));
		CCWPolygon ccwp2 = new CCWPolygon(cs2,new Coordinate(0,0));

		GisDebugger.addGeometry(ccwp1.getCCWPolygon());

		GisDebugger.addGeometry(geofac.createPolygon(geofac.createLinearRing(ccwp1.getReflectedCCWRing()), null));
		GisDebugger.addGeometry(ccwp2.getCCWPolygon());

		Polygon p = MinkowskiSum.getMinkowskiSum(ccwp2, ccwp1);
		GisDebugger.addGeometry(p);

		GisDebugger.dump("/Users/laemmel/tmp/vis/minkowski.shp");
	}

}
