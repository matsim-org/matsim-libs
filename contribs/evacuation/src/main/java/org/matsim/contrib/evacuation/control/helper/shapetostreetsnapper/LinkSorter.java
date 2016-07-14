/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSorter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.control.helper.shapetostreetsnapper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.control.helper.Algorithms;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;

public class LinkSorter implements Comparator<Link> {

	Map<Link,Double> distCache = new HashMap<Link,Double>();
	private final Coordinate c0;
	private final Coordinate c1;
	
	public LinkSorter(Coordinate c0, Coordinate c1) {
		this.c0 = c0;
		this.c1 = c1;
		
	}
	
	
	@Override
	public int compare(Link o1, Link o2) {
		double dist1 = getDistToC0(o1);
		double dist2 = getDistToC0(o2);
		
		if (dist1 < dist2) {
			return -1;
		}
		
		if (dist1 > dist2) {
			return 1;
		}
		return 0;
	}
	
	private double getDistToC0(Link o1) {
		Double dist1 = this.distCache.get(o1);
		if (dist1 == null) {
			Coordinate intersection = new Coordinate(Double.NaN,Double.NaN);
			Algorithms.computeLineIntersection(this.c0,this.c1,MGC.coord2Coordinate(o1.getFromNode().getCoord()),MGC.coord2Coordinate(o1.getToNode().getCoord()),intersection);
			dist1 = this.c0.distance(intersection);
			this.distCache.put(o1, dist1);
		}
		return dist1;
	}
}
