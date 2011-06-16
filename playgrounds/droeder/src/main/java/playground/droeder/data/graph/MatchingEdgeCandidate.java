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
import java.util.List;
import java.util.ListIterator;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class MatchingEdgeCandidate implements GraphElement, Cloneable {

	private Id id;
	private MatchingNode toNode;
	private MatchingNode fromNode;
	private ArrayList<MatchingSegment> segments;
	private Id refEdgeId;
	
	public MatchingEdgeCandidate(MatchingEdge e){
		this.id = e.getId();
		this.toNode = e.getToNode();
		this.fromNode = e.getFromNode();
		this.segments = e.getSegments();
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
	public ArrayList<MatchingSegment> getSegments(){
		return this.segments;
	}
	
	public void setRefEdgeId(Id id){
		this.refEdgeId = id;
	}
	
	private List<ResultsOfSegmentComparison> segmentComparing;
	public void compareSegments2RefSegments(List<MatchingSegment> refs){
		segmentComparing = new ArrayList<ResultsOfSegmentComparison>();
		ListIterator<MatchingSegment> refIt = refs.listIterator();
		ListIterator<MatchingSegment> candIt = this.segments.listIterator();
		MatchingSegment ref = null, cand = null;
		StraightComparer comp = null;
		ResultsOfSegmentComparison results;
		
		
		while(refIt.hasNext() && candIt.hasNext()){
			if(cand == null && ref == null){
				// take the first segment of each edge
				cand = candIt.next();
				ref = refIt.next();
			}else{
				// check which segment end's before the other ends and take it
				if(comp.straightOneIsUndershot()){
					ref = refIt.next();
				}else{
					cand = candIt.next();
				}
			}
			results = new ResultsOfSegmentComparison(ref.getId(), cand.getId());
			comp = new StraightComparer(new Straight(ref.getStart(), ref.getEnd()), new Straight(cand.getStart(), cand.getEnd()), results);
			this.segmentComparing.add(results);
		}
		
	}
}

class ResultsOfSegmentComparison{
	
	private Id ref;
	private Id cand;
	private Double avDist;
	private Double lenghtOne;
	private Double lengthTwo;
	private Double angle;

	public ResultsOfSegmentComparison(Id ref, Id cand){
		this.ref = ref;
		this.cand = cand;
	}
	
	public void setAvDist(Double avDist){
		this.avDist = avDist;
	}
	
	public void setMatchedLengthOne(Double length){
		this.lenghtOne = length;
	}
	
	public void setMatchedLengthTwo(Double length){
		this.lengthTwo = length;
	}
	
	public void setAngle(Double angle){
		this.angle = angle;
	}
}
