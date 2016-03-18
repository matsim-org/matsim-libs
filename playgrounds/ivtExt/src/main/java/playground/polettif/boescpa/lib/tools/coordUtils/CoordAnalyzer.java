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

package playground.polettif.boescpa.lib.tools.coordUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.facilities.Facility;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

// copy from christoph-playground
public class CoordAnalyzer {

	private final Geometry affectedArea;
	private final GeometryFactory factory;
	private final Map<Id, Boolean> nodeCache;
	private final Map<Id, Boolean> linkCache;
	private final Map<Id, Boolean> facilityCache;
	
	public CoordAnalyzer(Geometry affectedArea) {
		this.affectedArea = affectedArea;
		
		this.factory = new GeometryFactory();
		this.nodeCache = new ConcurrentHashMap<Id, Boolean>();
		this.linkCache = new ConcurrentHashMap<Id, Boolean>();
		this.facilityCache = new ConcurrentHashMap<Id, Boolean>();
	}
	
	private CoordAnalyzer(Geometry affectedArea, Map<Id, Boolean> nodeCache, Map<Id, Boolean> linkCache, Map<Id, Boolean> facilityCache) {
		this.affectedArea = affectedArea;
		this.factory = new GeometryFactory();
		this.nodeCache = nodeCache;
		this.linkCache = linkCache;
		this.facilityCache = facilityCache;
	}
	
	/*
	 * Create a new instance which uses the same affected area and CacheMaps which are
	 * ConcurrentHashMaps and therefore thread-safe.
	 */
	public CoordAnalyzer createInstance() {
		return new CoordAnalyzer((Geometry) this.affectedArea.clone(), this.nodeCache, this.linkCache, this.facilityCache);
	}
	
	public void clearCache() {
		nodeCache.clear();
		linkCache.clear();
		facilityCache.clear();
	}

	public boolean isNodeAffected(Node node) {
		Boolean isAffected = nodeCache.get(node.getId());
		if (isAffected == null) {
			isAffected = isCoordAffected(node.getCoord());
			nodeCache.put(node.getId(), isAffected);
			return isAffected;
		} else return isAffected;
	}
	
	public boolean isLinkAffected(Link link) {
		Boolean isAffected = linkCache.get(link.getId());
		if (isAffected == null) {
			isAffected = isCoordAffected(link.getCoord());
			linkCache.put(link.getId(), isAffected);
			return isAffected;
		} else return isAffected;
	}
	
	public boolean isFacilityAffected(Facility facility) {
		Boolean isAffected = facilityCache.get(facility.getId());
		if (isAffected == null) {
			isAffected = isCoordAffected(facility.getCoord());
			facilityCache.put(facility.getId(), isAffected);
			return isAffected;
		} else return isAffected;
	}
	
	/*
	 * If possible, use one of the other methods. They cache
	 * nodes/links/facilities.
	 */
	public boolean isCoordAffected(Coord coord) {
		Point point = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		return affectedArea.contains(point);
	}
}
