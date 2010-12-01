/* *********************************************************************** *
 * project: org.matsim.*
 * GridUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

/**
 * 
 */
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset;

import org.matsim.api.core.v01.Coord;

/**
 * tools for the grid operation
 * 
 * @author yu
 * 
 */
public class GridUtils {
	public static boolean inGrid(Coord center, Coord coord, double sideLength) {
		double x = coord.getX(), y = coord.getY(), ctX = center.getX(), ctY = center
				.getY(), halfSideLength = sideLength / 2d;
		return x >= ctX - halfSideLength && x < ctX + halfSideLength
				&& y >= ctY - halfSideLength && y < ctY + halfSideLength;
	}
}
