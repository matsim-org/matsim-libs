package playground.wrashid.lib;

import playground.wrashid.lib.obj.Coord;

/**
 * Function is only defined on the predefined part.
 * 
 * @author wrashid
 * 
 */
public class LinearFunctionPart implements Comparable<LinearFunctionPart> {

	Coord startCoord;
	Coord endCoord;

	/**
	 * precondition: startCoord.x < endCoord.x
	 * 
	 * @param startCoord
	 * @param endCoord
	 */
	public LinearFunctionPart(Coord startCoord, Coord endCoord) {
		super();

		/**
		 * check precondition
		 */
		if (!(startCoord.getX() < endCoord.getX())) {
			throw new RuntimeException("startCoord.getX() < endCoord.getX()");
		}

		this.startCoord = startCoord;
		this.endCoord = endCoord;
	}

	/**
	 * precondition: assure, x is within the defined range
	 * 
	 * @param x
	 * @return
	 */
	public double getYValue(double x) {
		/**
		 * check precondition
		 */
		if (x < startCoord.getX() || x > endCoord.getX()) {
			throw new RuntimeException("x<startCoord.getX() || x>endCoord.getX()");
		}

		// if constant value function
		if (endCoord.getY() == startCoord.getY()) {
			return startCoord.getY();
		} else {
			return x * (endCoord.getY() - startCoord.getY()) / (endCoord.getX() - startCoord.getX());
		}
	}

	public int compareTo(LinearFunctionPart o) {
		if (startCoord.getX() > o.startCoord.getX()) {
			return 1;
		} else if (startCoord.getX() < o.startCoord.getX()) {
			return -1;
		}
		return 0;
	}

}
