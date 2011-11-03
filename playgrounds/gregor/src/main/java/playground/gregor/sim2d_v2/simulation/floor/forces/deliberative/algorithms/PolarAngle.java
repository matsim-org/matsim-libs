package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.algorithms;

import com.vividsolutions.jts.geom.Coordinate;

public class PolarAngle {

	//Implementation of atan2 since Math.atan2 seems to be buggy
	public static double getPolarAngle(Coordinate c1, Coordinate c2) {
		double x = c2.x - c1.x;
		double y = c2.y - c1.y;

		if (x > 0) {
			return Math.atan(y/x);
		} else if (y >= 0 && x < 0) {
			return Math.atan(y/x) + Math.PI;
		} else if (y < 0 && x <0) {
			return Math.atan(y/x) - Math.PI;
		} else if (y > 0 && x == 0) {
			return Math.PI/2;
		} else if (y < 0 && x == 0) {
			return -Math.PI/2;
		}

		return Double.NaN;
	}

}