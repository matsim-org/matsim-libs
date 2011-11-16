package playground.gregor;

import java.util.ArrayList;
import java.util.List;

import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class IntersectTest {

	public static void main(String [] args) {

		List<LineString> ls = getLS();

		long before = System.currentTimeMillis();
		int intrs = testIntersects(ls);
		long after = System.currentTimeMillis();
		System.out.println(after-before + "  " + intrs);

		long before2 = System.currentTimeMillis();
		int intrs2 = testLeftOf(ls);
		long after2 = System.currentTimeMillis();
		System.out.println(after2-before2 + "  " + intrs2);
	}


	private static int testLeftOf(List<LineString> ls) {
		GeometryFactory geofac = new GeometryFactory();
		int ret = 0;
		for (double x1 = -10; x1 <= 10; x1 += 10) {
			for (double x2 = -10; x2 <= 10; x2 += 10){
				for (double y1 = -10; y1 <= 10; y1 += 10) {
					for (double y2 = -10; y2 <= 10; y2 += 10) {
						Coordinate c1 = new Coordinate(x1,y1);
						Coordinate c2 = new Coordinate(x2,y2);
						LineString l = geofac.createLineString(new Coordinate[]{c1,c2});
						for (LineString tmp : ls) {
							if (Algorithms.isLeftOfLine(l.getCoordinateN(1), tmp.getCoordinateN(0), tmp.getCoordinateN(1)) > 0){
								ret++;
							}
						}
					}
				}
			}
		}
		return ret;
	}


	private static int testIntersects(List<LineString> ls) {
		GeometryFactory geofac = new GeometryFactory();
		int ret = 0;
		for (double x1 = -10; x1 <= 10; x1 += 10) {
			for (double x2 = -10; x2 <= 10; x2 += 10){
				for (double y1 = -10; y1 <= 10; y1 += 10) {
					for (double y2 = -10; y2 <= 10; y2 += 10) {
						Coordinate c1 = new Coordinate(x1,y1);
						Coordinate c2 = new Coordinate(x2,y2);
						LineString l = geofac.createLineString(new Coordinate[]{c1,c2});
						for (LineString tmp : ls) {
							if (l.crosses(tmp)){
								ret++;
							}
						}
					}
				}
			}
		}
		return ret;
	}

	private static List<LineString> getLS() {
		List<LineString> ret = new ArrayList<LineString>();
		GeometryFactory geofac = new GeometryFactory();
		for (double x1 = -10; x1 <= 12; x1 += 2) {
			for (double x2 = -10; x2 <= 12; x2 += 2){
				for (double y1 = -10; y1 <= 12; y1 += 2) {
					for (double y2 = -10; y2 <= 12; y2 += 2) {
						Coordinate c1 = new Coordinate(x1,y1);
						Coordinate c2 = new Coordinate(x2,y2);
						LineString ls = geofac.createLineString(new Coordinate[]{c1,c2});
						ret.add(ls);
					}
				}
			}
		}

		return ret;
	}

}
