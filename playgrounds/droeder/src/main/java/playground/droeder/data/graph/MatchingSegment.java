/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.data.graph;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.droeder.GeoCalculator;

/**
 * @author droeder
 *
 */
public class MatchingSegment implements GraphElement {
	
	private Coord start, end;
	private Id id;
	private Id parentEdgeId;
	private Double length;

	public MatchingSegment (Coord start, Coord end, Id parentEdgeId, int segmentCounter){
		this.start = start;
		this.end = end;
		this.id = new IdImpl(parentEdgeId + "_" + segmentCounter);
		this.parentEdgeId = parentEdgeId;
		this.length = GeoCalculator.distanceBetween2Points(start, end);
	}
	
	
	/**
	 * @return the start
	 */
	public Coord getStart() {
		return start;
	}

	/**
	 * @return the end
	 */
	public Coord getEnd() {
		return end;
	}

	/**
	 * @return the id
	 */
	public Id getId() {
		return id;
	}
	
	public Id getParentEdgeId(){
		return parentEdgeId;
	}
	
	public Double getLength(){
		return this.length;
	}
}
