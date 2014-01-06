/* *********************************************************************** *
 * project: org.matsim.*
 * Section.java
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

package playground.gregor.sim2d_v4.scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import playground.gregor.sim2d_v4.cgal.LineSegment;

import com.vividsolutions.jts.geom.Polygon;

public class Section implements Identifiable {

	private final Id id;
	private final Polygon p;
	private int[] openingsIDs = null;
	private Id[] neighborsIDs = null;
	private final int level;
	private final List<Id> relatedLinks = new ArrayList<Id>();
	
	private List<LineSegment> obstacles;
	private List<LineSegment> openings;
	
	private final Map<LineSegment,Section> neighbors = new HashMap<LineSegment,Section>();
	private final Map<Section,LineSegment> neighborsInvMapping = new HashMap<Section, LineSegment>();

	/*package*/ Section(Id id, Polygon p, int[] openings, Id[] neighbors, int level) {
		this.id = id;
		this.p = p; //maybe we can sparse the polygon here and take the coordinate array of its exterior ring instead [gl Jan' 2013]
		this.openingsIDs = openings;
		this.neighborsIDs = neighbors;
		this.level = level;
	}

	public Polygon getPolygon() {
		return this.p;
	}

	public int getLevel() {
		return this.level;
	}

	public int[] getOpenings() {
		return this.openingsIDs;
	}
	
	public Id[] getNeighbors() {
		return this.neighborsIDs;
	}
	
	@Override
	public Id getId() {
		return this.id;
	}

	public void addRelatedLinkId(Id id2) {
//		throw new RuntimeException("not longer supported!");
		this.relatedLinks.add(id2);
	}
	
	public List<Id> getRelatedLinkIds() {
//		throw new RuntimeException("not longer supported!");
		return this.relatedLinks;
	}
	
	
	public Section getNeighbor(LineSegment opening) {
		return this.neighbors.get(opening);
	}
	
	public LineSegment getOpening(Section neighbor) {
		return this.neighborsInvMapping.get(neighbor);
	}
	
	public void addOpeningNeighborMapping(LineSegment opening, Section neighbor) {
		this.neighborsInvMapping.put(neighbor, opening);
		this.neighbors.put(opening, neighbor);
	}
	
	/*package*/ void setObstacles(List<LineSegment> obst) {
		this.obstacles = obst;
	}
	
	/*package*/ void setOpenings(List<LineSegment> open) {
		this.openings = open;
	}
	
	public List<LineSegment> getOpeningSegments() {
		return this.openings;
	}
	
	public List<LineSegment> getObstacleSegments() {
		return this.obstacles;
	}
}
