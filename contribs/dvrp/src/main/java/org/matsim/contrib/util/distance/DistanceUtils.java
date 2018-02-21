package org.matsim.contrib.util.distance;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;

public class DistanceUtils {
	public static double calculateDistance(BasicLocation<?> fromLocation, BasicLocation<?> toLocation) {
		return calculateDistance(fromLocation.getCoord(), toLocation.getCoord());
	}

	public static double calculateSquaredDistance(BasicLocation<?> fromLocation, BasicLocation<?> toLocation) {
		return calculateSquaredDistance(fromLocation.getCoord(), toLocation.getCoord());
	}

	/**
	 * @return distance (for distance-based comparison/sorting, consider using the squared distance)
	 */
	public static double calculateDistance(Coord fromCoord, Coord toCoord) {
		return Math.sqrt(calculateSquaredDistance(fromCoord, toCoord));
	}

	/**
	 * @return SQUARED distance (to avoid unnecessary Math.sqrt() calls when comparing distances)
	 */
	public static double calculateSquaredDistance(Coord fromCoord, Coord toCoord) {
		double deltaX = toCoord.getX() - fromCoord.getX();
		double deltaY = toCoord.getY() - fromCoord.getY();
		return deltaX * deltaX + deltaY * deltaY;
	}
}
