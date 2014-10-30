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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.gregor.sim2d_v4.cgal.LineSegment;

import com.vividsolutions.jts.geom.Polygon;

public class Section implements Identifiable<Section> {

	private final Id<Section> id;
	private final Polygon p;
	private int[] openingsIDs = null;
	private Id<Section>[] neighborsIDs = null;
	private final int level;
	private final List<Id<Link>> relatedLinks = new ArrayList<>();
	
	private List<LineSegment> obstacles;
	private List<LineSegment> openings;
	
	private final Map<LineSegment,Section> neighbors = new HashMap<LineSegment,Section>();
	private final Map<Section,LineSegment> neighborsInvMapping = new HashMap<Section, LineSegment>();
	private final Map<Id<Node>,LineSegment> openingsMappings = new HashMap<>(); 
	private Id<Node>[] openingMATSimIds;

	/*package*/ Section(Id<Section> id, Polygon p, int[] openings, Id<Section>[] neighbors, int level) {
		this(id,p,openings,neighbors,null,level);
	}
	
	/*package*/ Section(Id<Section> id, Polygon p, int[] openings, Id<Section>[] neighbors, Id<Node>[] openingsMATSimIds, int level) {
		this.id = id;
		this.p = p; //maybe we can sparse the polygon here and take the coordinate array of its exterior ring instead [gl Jan' 2013]
		this.openingsIDs = openings;
		this.neighborsIDs = neighbors;
		this.level = level;
		this.openingMATSimIds = openingsMATSimIds;

	}
	
	@Deprecated //for transition period only
	public void setOpeningMATSimIds(Id<Node> [] ids) {
		this.openingMATSimIds = ids;
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
	
	public Id<Node>[] getOpeningsIds() {
		return this.openingMATSimIds;
	}
	
	public Id<Section>[] getNeighbors() {
		return this.neighborsIDs;
	}
	
	@Override
	public Id<Section> getId() {
		return this.id;
	}

	public void addRelatedLinkId(Id<Link> id2) {
//		throw new RuntimeException("not longer supported!");
		this.relatedLinks.add(id2);
	}
	
	public List<Id<Link>> getRelatedLinkIds() {
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
		if (this.neighbors.put(opening, neighbor) != null){
//			throw new RuntimeException();
		};
	}
	
	/*package*/ void setObstacles(List<LineSegment> obst) {
		this.obstacles = obst;
	}
	
	/*package*/ void setOpenings(List<LineSegment> open) {
		this.openings = open;
		if (this.openingMATSimIds != null) {
			for (int i = 0; i < open.size(); i++) {
				this.openingsMappings.put(this.openingMATSimIds[i], open.get(i));
			}
		}
	}
	
	public LineSegment getOpening(Id id) {
		return this.openingsMappings.get(id);
	}
	public List<LineSegment> getOpeningSegments() {
		return this.openings;
	}
	
	public List<LineSegment> getObstacleSegments() {
		return this.obstacles;
	}
}
