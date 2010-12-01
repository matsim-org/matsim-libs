/* *********************************************************************** *
 * project: org.matsim.*
 * Arrow.java
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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * creates an Arrow from a link with direction
 * 
 * @author yu
 * 
 */
public class Arrow implements GeometricFigure {

	public CoordinateArraySequence getCoordinateArraySequence(double width,
			Coordinate from, Coordinate to) {
		double xdiff = to.x - from.x;
		double ydiff = to.y - from.y;
		double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
		double xwidth = width * ydiff / denominator;
		double ywidth = -width * xdiff / denominator;

		Coordinate fromB = new Coordinate(from.x + xwidth, from.y + ywidth, 0);

		Coordinate toC = new Coordinate(0.2 * fromB.x + 0.8 * (to.x + xwidth),
				0.2 * fromB.y + 0.8 * (to.y + ywidth), 0);
		Coordinate toD = new Coordinate(toC.x + xwidth, toC.y + ywidth, 0);

		return new CoordinateArraySequence(new Coordinate[] { from, to, toD,
				toC, fromB, from });
	}

}
