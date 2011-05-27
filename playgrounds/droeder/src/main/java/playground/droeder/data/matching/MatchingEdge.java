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
package playground.droeder.data.matching;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class MatchingEdge {

	private Id id;
	private MatchingNode toNode;
	private MatchingNode fromNode;
	private ArrayList<MatchingSegment> segments;
	
	public MatchingEdge(Id id, MatchingNode to, MatchingNode from){
		this.id = id;
		this.toNode = to;
		this.fromNode = from;
	}

	/**
	 * @return the toNode
	 */
	public MatchingNode getToNode() {
		return toNode;
	}

	/**
	 * @return the fromNode
	 */
	public MatchingNode getFromNode() {
		return fromNode;
	}

	/**
	 * @return
	 */
	public Id getId() {
		return this.id;
	}
	
	/**
	 * @return
	 */
	public boolean addSegments(ArrayList<MatchingSegment> segments){
		this.segments = segments;
		return true;
	}
	
	/**
	 * @return
	 */
	public ArrayList<MatchingSegment> getSegments(){
		return this.segments;
	}

}
