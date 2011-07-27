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
package playground.droeder.data.graph.comparison;

import playground.droeder.data.graph.MatchingSegment;

/**
 * @author droeder
 *
 */
public class SegmentCompare extends AbstractCompare{
	
	private Double deltaAngle, avDist, matchedLengthRef, matchedLengthCand, lengthDiffRef, lenghtDiffCand; 
	private boolean sOneIsUndershot;

	public SegmentCompare(MatchingSegment refElement, MatchingSegment compareElement) {
		super(refElement, compareElement);
		this.computeValues(refElement, compareElement);
	}

	private void computeValues(MatchingSegment ref, MatchingSegment cand) {
		StraightComparer c = new StraightComparer(new Straight(ref.getStart(), ref.getEnd()), 
				new Straight(cand.getStart(), cand.getEnd()));
		this.deltaAngle = c.getAngle();
		this.avDist = c.getAverageDistance();
		this.matchedLengthRef = c.getTotalMatchedLengthStraightOne();
		this.lengthDiffRef = 1- (matchedLengthRef / ref.getLength());
		this.matchedLengthCand = c.getTotalMatchedLengthStraightTwo();
		this.lenghtDiffCand = 1 - (matchedLengthCand / cand.getLength());
		this.sOneIsUndershot = c.straightOneIsUndershot();
	}
	
	public boolean isMatched(Double deltaDist, Double deltaPhi, Double relLengthDiff){
		if((this.deltaAngle < deltaPhi) && (this.avDist < deltaDist) 
				&& (this.lengthDiffRef < relLengthDiff) && (this.lenghtDiffCand < relLengthDiff)){
			return true;
		}
		return false;
	}

	@Override
	public int compareTo(AbstractCompare o) {
		return super.compareTo(o);
	}

	/**
	 * @return the sOneIsUndershot
	 */
	public boolean refIsUndershot() {
		return sOneIsUndershot;
	}

	/**
	 * @return the deltaAngle
	 */
	public Double getDeltaAngle() {
		return deltaAngle;
	}

	/**
	 * @return the avDist
	 */
	public Double getAvDist() {
		return avDist;
	}

	/**
	 * @return the matchedLengthRef
	 */
	public Double getMatchedLengthRef() {
		return matchedLengthRef;
	}

	/**
	 * @return the matchedLengthComp
	 */
	public Double getMatchedLengthComp() {
		return matchedLengthCand;
	}
}
