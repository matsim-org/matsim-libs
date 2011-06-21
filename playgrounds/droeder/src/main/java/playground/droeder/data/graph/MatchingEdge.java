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
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class MatchingEdge implements GraphElement{
	private static final Logger log = Logger.getLogger(MatchingEdge.class);

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
	 * clears the segmentlist and creates new segments from the given shape-coord. Start and end of the edge should be equal to 
	 * start and end of the shape-coords. also the shape-coords should be in the correct order
	 * @return
	 */
	public void addShapePointsAndCreateSegments(ArrayList<Coord> shapeCoords){
		
		//TODO doesn't work
		if(shapeCoords.size()< 2){
			log.error("can not create segments for edge " + this.id + ", because at least two points are needed!");
		}else if(shapeCoords.get(0).equals(this.fromNode.getCoord()) && shapeCoords.get(shapeCoords.size() - 1).equals(this.toNode.getCoord())){
			this.clearSegments();
			ListIterator<Coord> it = shapeCoords.listIterator();
			Coord start = null, end = null;
			while(it.hasNext()){
				if(start ==  null){
					start = it.next();
				}else{
					end = it.next();
					this.segments.add(new MatchingSegment(start, end, this.id, this.segmentCounter));
					start = end;
				}
			}
		}else{
			log.error("can not create segments for edge " + this.id + ", because start or end-point not correct!");
		}
	}
	
	private void addSegment(MatchingSegment s){
		// probably the segment is used anywhere else, so use a clone
		this.segments.add(s);
		this.segmentCounter++;
	}
	
	private void clearSegments(){
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
