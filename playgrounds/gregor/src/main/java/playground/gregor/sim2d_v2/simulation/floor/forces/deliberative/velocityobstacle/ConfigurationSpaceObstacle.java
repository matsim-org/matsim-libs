package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle;

import java.util.ArrayList;
import java.util.List;

import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class ConfigurationSpaceObstacle {


	public static Coordinate[] getCObstacle(CCWPolygon p1, CCWPolygon p2) {

		Coordinate[] w = p1.getCCWRing();
		Coordinate[] v = p2.getReflectedCCWRing();


		List<Coordinate>minkowskiCoords = new ArrayList<Coordinate>();

		int i = 0;
		int j = 0;



		do {
			minkowskiCoords.add(new Coordinate(v[i].x + w[j].x, v[i].y + w[j].y));

			if ( i == v.length-1) {
				j++;
			} else if (j == w.length-1) {
				i++;
			} else {
				//				double angleVi = Algorithms.getPolarAngle(v[i], v[i+1]);
				//				double angleWi = Algorithms.getPolarAngle(w[j], w[j+1]);

				int angleViBigger = Algorithms.isAngleBigger(v[i], v[i+1], w[j], w[j+1]);

				if (angleViBigger < 0) {
					i++;
				} else if (angleViBigger > 0) {
					j++;
				} else {
					i++;
					j++;
				}
			}

		}while (i < v.length && j < w.length);

		Coordinate[] a = minkowskiCoords.toArray(new Coordinate[0]);//FIXME don't create an array here!! (it slows down the simulation (a bit))

		return a;
	}

	public static void main(String [] args) {

		GeometryFactory geofac = new GeometryFactory();


		int numOfParts = 18;
		double agentRadius = .25;

		Coordinate[] cs1 = new Coordinate[numOfParts + 1];
		double angle = Math.PI * 2 / numOfParts;
		for(int i = 0; i <numOfParts; i++){
			cs1[i] = new Coordinate(agentRadius * Math.cos(angle * i), agentRadius * Math.sin(angle * i));
		}
		cs1[numOfParts] = cs1[0];

		Coordinate[] cs2 = new Coordinate[numOfParts + 1];
		for(int i = 0; i <numOfParts; i++){
			cs2[i] = new Coordinate(agentRadius * Math.cos(angle * i), agentRadius * Math.sin(angle * i));
		}
		cs2[numOfParts] = cs1[0];

		Coordinate c = new Coordinate(0,0);
		CCWPolygon ccwp1 = new CCWPolygon(cs1,c);
		CCWPolygon ccwp2 = new CCWPolygon(cs2,new Coordinate(0,0));
		ccwp2.translate(1, 1);

		GisDebugger.addGeometry(geofac.createPolygon(geofac.createLinearRing(ccwp1.getCCWRing()), null));

		GisDebugger.addGeometry(geofac.createPolygon(geofac.createLinearRing(ccwp1.getReflectedCCWRing()), null));
		GisDebugger.addGeometry(geofac.createPolygon(geofac.createLinearRing(ccwp2.getCCWRing()), null));

		Polygon p =  geofac.createPolygon(geofac.createLinearRing(ConfigurationSpaceObstacle.getCObstacle(ccwp2, ccwp1)), null);
		GisDebugger.addGeometry(p);

		CCWPolygon obstacle = new CCWPolygon(p.getExteriorRing().getCoordinates(),new Coordinate(0,0));
		int[] indices = Algorithms.getTangentIndices(c, obstacle.getCCWRing());

		Coordinate c1 = obstacle.getCCWRing()[indices[0]];
		Coordinate c2 = obstacle.getCCWRing()[indices[1]];

		Coordinate [] VO = {c, c1, c2, c};
		Polygon VOP = geofac.createPolygon(geofac.createLinearRing(VO), null);
		GisDebugger.addGeometry(VOP);
		GisDebugger.dump("/Users/laemmel/tmp/vis/minkowski.shp");
	}

}
