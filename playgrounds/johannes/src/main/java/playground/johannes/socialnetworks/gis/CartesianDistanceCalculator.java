/* *********************************************************************** *
 * project: org.matsim.*
 * CartesianDistanceCalculator.java
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
package playground.johannes.socialnetworks.gis;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class CartesianDistanceCalculator implements DistanceCalculator {

	public double distance(Point p1, Point p2) {
		double dx = p1.getCoordinate().x - p2.getCoordinate().x;
		double dy = p1.getCoordinate().y - p2.getCoordinate().y;
		return Math.sqrt(dx*dx + dy*dy);
	}

}
