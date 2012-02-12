/* *********************************************************************** *
 * project: org.matsim.*
 * CoordAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CoordAnalyzer {

	private final Geometry affectedArea;
	private final GeometryFactory factory;
	private final Map<Id, Boolean> linkCache;
	
	public CoordAnalyzer(Geometry affectedArea) {
		this.affectedArea = affectedArea;
		
		this.factory = new GeometryFactory();
		this.linkCache = new HashMap<Id, Boolean>();
	}
	
	public void clearCache() {
		linkCache.clear();
	}
	
	public boolean isLinkAffected(Link link) {
		Boolean isAffected = linkCache.get(link.getId());
		if (isAffected == null) {
			isAffected = isCoordAffected(link.getCoord());
			linkCache.put(link.getId(), isAffected);
			return isAffected;
		} else return isAffected;
	}
	
	public boolean isCoordAffected(Coord coord) {
		Point point = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		return affectedArea.contains(point);
	}
}
