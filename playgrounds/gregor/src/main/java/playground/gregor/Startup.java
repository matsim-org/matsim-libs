package playground.gregor;

import java.util.List;

import playground.gregor.scenariogen.ScenarioGeneratorV;
import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class Startup {


	public static void main(String [] args) {

//		String [] argsII = {"/Users/laemmel/devel/dfg/input/network.xml","/Users/laemmel/devel/dfg/raw_input/networkL.shp","/Users/laemmel/devel/dfg/raw_input/networkP.shp","EPSG:3395"};
//		Links2ESRIShape.main(argsII);
		Coordinate c0 = new Coordinate(5, 5);
		double radius = 2;
		
		Coordinate c1 = new Coordinate(1,1);
		Coordinate[] ret = Algorithms.computeTangentsThroughPoint(c0, radius, c1);
		GeometryFactory geofac = new GeometryFactory();
		LinearRing shell = geofac.createLinearRing(new Coordinate[]{c1,ret[0],ret[1],c1});
		Polygon p = geofac.createPolygon(shell, null);
		
		
		List<Coordinate> oval = ScenarioGeneratorV.getOval(radius, c0.x, c0.y, 0, 0.1);
		Coordinate[] coords = new Coordinate[oval.size()+1];
		for (int i = 0; i < oval.size(); i++) {
			coords[i] = oval.get(i);
		}
		coords[oval.size()] = coords[0];
		LinearRing shell2 = geofac.createLinearRing(coords);
		Polygon p2 = geofac.createPolygon(shell2, null);
		GisDebugger.addGeometry(p2,"circle");
		GisDebugger.addGeometry(p, "tangents");
		GisDebugger.dump("/Users/laemmel/tmp/tangents.shp");

	}

}
