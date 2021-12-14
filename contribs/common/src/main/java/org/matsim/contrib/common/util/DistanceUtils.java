/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.common.util;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DistanceUtils {
	public static double calculateDistance(BasicLocation fromLocation, BasicLocation toLocation) {
		return calculateDistance(fromLocation.getCoord(), toLocation.getCoord());
	}

	public static double calculateSquaredDistance(BasicLocation fromLocation, BasicLocation toLocation) {
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
