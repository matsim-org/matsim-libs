package playground.wrashid.lib.tools.network.obj;

import org.matsim.api.core.v01.Coord;

public class EnclosingRectangle {

	double minX = Double.MAX_VALUE;
	double minY = Double.MAX_VALUE;
	double maxX = Double.MIN_VALUE;
	double maxY = Double.MIN_VALUE;
	
	public void registerCoord(Coord coord){
		if (coord.getX() < minX) {
			minX = coord.getX();
		}

		if (coord.getY() < minY) {
			minY = coord.getY();
		}

		if (coord.getX() > maxX) {
			maxX = coord.getX();
		}

		if (coord.getY() > maxY) {
			maxY = coord.getY();
		}
	}

	public double getMinX() {
		return minX;
	}

	public double getMinY() {
		return minY;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxY() {
		return maxY;
	}
	
}
