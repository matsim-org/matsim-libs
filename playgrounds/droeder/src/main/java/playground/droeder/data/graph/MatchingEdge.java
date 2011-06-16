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

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class MatchingEdge implements GraphElement{


	private Id id;
	private MatchingNode toNode;
	private MatchingNode fromNode;
	private ArrayList<MatchingSegment> segments;
	private int segmentCounter = 0;
	
	public MatchingEdge(Id id, MatchingNode to, MatchingNode from){
		this.id = id;
		this.toNode = to;
		this.fromNode = from;
		this.segments = new ArrayList<MatchingSegment>();
		this.addSegment(new MatchingSegment(from.getCoord(), to.getCoord(), this.id, this.segmentCounter));
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
	public void addSegments(ArrayList<MatchingSegment> segments){
		for(MatchingSegment s: segments){
			this.addSegment(s);
		}
	}
	
	/**
	 * add's the at last position in the segments-list. 
	 * call clearSegments() before adding own segments, because a default segment is added at the constructor
	 * @param s
	 */
	public void addSegment(MatchingSegment s){
		// probably the segment is used anywhere else, so use a clone
		this.segments.add(s.clone());
		this.segmentCounter++;
	}
	
	/**
	 * clears the segment-list and sets the counter to 0
	 */
	public void clearSegments(){
		this.segmentCounter = 0;
		this.segments.clear();
	}
	
	public int getSegmentCounter(){
		return this.segmentCounter ;
	}
	
	/**
	 * @return
	 */
	public ArrayList<MatchingSegment> getSegments(){
		return this.segments;
	}
}
