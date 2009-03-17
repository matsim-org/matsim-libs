/* *********************************************************************** *
 * project: org.matsim.*
 * CoordUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.utils.geometry;

import org.matsim.interfaces.basic.v01.Coord;

public abstract class CoordUtils {

	public static double calcDistance(Coord coord, Coord other) {
		//depending on the coordinate system that is used, determining the
		//distance based on the euclidean distance will lead to wrong results.
		//however, if the distance is not to large (<1km) this will be a usable distance estimation.
		//Another comfortable way to calculate correct distances would be, to use the distance functions
		//provided by geotools lib. May be we need to discuss what part of GIS functionality we should implement
		//by our own and for what part we could use an existing GIS like geotools. We need to discuss this in terms
		//of code robustness, performance and so on ... [gl]
		double xDiff = other.getX()-coord.getX();
		double yDiff = other.getY()-coord.getY();
		return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
	}

}
