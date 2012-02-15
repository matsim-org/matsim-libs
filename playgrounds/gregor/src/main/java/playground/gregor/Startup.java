package playground.gregor;

import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

import com.vividsolutions.jts.geom.Coordinate;

public class Startup {

	public static int SIZE = 10000000;

	public static void main(String [] args) {

//		String [] argsII = {"/Users/laemmel/devel/dfg/input/network.xml","/Users/laemmel/devel/dfg/raw_input/networkL.shp","/Users/laemmel/devel/dfg/raw_input/networkP.shp","EPSG:3395"};
//		Links2ESRIShape.main(argsII);
		Coordinate c0 = new Coordinate(5, 5);
		
		Coordinate c1 = new Coordinate(1,1);
		
		
		
		double radius = .3;
		Coordinate[][] coords = new Coordinate[SIZE][2];
		fillArray(coords,radius);
		
		long timeSum =0;
		long start = System.nanoTime();
		for (int i = 0; i < SIZE; i++) {
			c0 = coords[i][0];
			c1 = coords[i][1];
			
			Coordinate[] ret = Algorithms.computeTangentsThroughPoint(c0, radius, c1);
			
			ret[0] = null;
			ret[1] = null;
		}
		long stop = System.nanoTime();
		timeSum += stop-start;
		System.out.println("sum:" + timeSum + "  avg:" + (double)timeSum/(double)SIZE);
		
		
		
	}

	private static void fillArray(Coordinate[][] coords, double radius) {
		int idx = 0;
		while (idx < SIZE) {
			double x0 = (MatsimRandom.getRandom().nextDouble()-0.5)*10;
			double y0 = (MatsimRandom.getRandom().nextDouble()-0.5)*10;
			Coordinate c0 = new Coordinate(x0,y0);
			
			double x1 = (MatsimRandom.getRandom().nextDouble()-0.5)*10;
			double y1 = (MatsimRandom.getRandom().nextDouble()-0.5)*10;
			Coordinate c1 = new Coordinate(x1,y1);
			
			if (c0.distance(c1) <= radius) {
				continue;
			}
			
			coords[idx][0] = c0;
			coords[idx][1] = c1;
			idx++;
		}
		
	}

}
