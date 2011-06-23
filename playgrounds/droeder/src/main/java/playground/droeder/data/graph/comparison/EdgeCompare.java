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

import java.util.ListIterator;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;

import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingSegment;

/**
 * @author droeder
 *
 */
public class EdgeCompare extends AbstractCompare{
	private Double 	refTotalLength = 0.0, 
			compTotalLength = 0.0, 
			avDist = 0.0, 
			avAngle = 0.0, 
			matchedLengthRef = 0.0, 
			matchedLengthComp = 0.0;
	
	private Coord startRef, startComp, endRef, endComp;

	public EdgeCompare(MatchingEdge refElement, MatchingEdge compareElement) {
		super(refElement, compareElement);
		this.computeValues(refElement, compareElement);
		this.refTotalLength = refElement.getSegmentLength();
		this.compTotalLength = compareElement.getSegmentLength();
		this.startRef = refElement.getFromNode().getCoord();
		this.startComp = compareElement.getFromNode().getCoord();
		this.endRef = refElement.getToNode().getCoord();
		this.endComp = compareElement.getToNode().getCoord();
		
		if(super.getRefId().equals(new IdImpl("U-2.001.001.H")) && super.getCompId().equals(new IdImpl("U2   _00619"))){
			ListIterator<MatchingSegment> candIt = refElement.getSegments().listIterator();
			ListIterator<MatchingSegment> refIt = compareElement.getSegments().listIterator();
			MatchingSegment r = null, c= null;
			while(candIt.hasNext() && refIt.hasNext()){
				r = refIt.next();
				c = candIt.next();
				System.out.println(r.getStart().getX() + "\t" + r.getStart().getY() + "\t" + c.getStart().getX() + "\t" + c.getStart().getY());
			}
			System.out.println(r.getEnd().getX() + "\t" + r.getEnd().getY() + "\t" + c.getEnd().getX() + "\t" + c.getEnd().getY());
			
		}
	}

	/**
	 * @param refElement
	 * @param compareElement
	 */
	private void computeValues(MatchingEdge refElement,	MatchingEdge compareElement) {
		ListIterator<MatchingSegment> candIt = refElement.getSegments().listIterator();
		ListIterator<MatchingSegment> refIt = compareElement.getSegments().listIterator();
		
		MatchingSegment rs = null, cs = null;
		SegmentCompare sc = null;
		int cnt = 0;
		
		while(candIt.hasNext() && refIt.hasNext()){
			if((rs == null) && (cs == null)){
				rs = refIt.next();
				cs = candIt.next();
			}else if(sc.refIsUndershot()){
				rs = refIt.next();
			}else if(!sc.refIsUndershot()){
				cs = candIt.next();
			}
			sc = new SegmentCompare(rs, cs);
			cnt++;
			avDist += sc.getAvDist();
			avAngle += sc.getDeltaAngle();
			matchedLengthRef += sc.getMatchedLengthRef();
			matchedLengthComp += sc.getMatchedLengthComp();
		}
		avDist = avDist / cnt;
		avAngle = avAngle / cnt;
	}
	
	public boolean isMatched(Double dDistMax, Double dPhiMax, Double lengthTolerancePercentage){
		if(super.getRefId().equals(new IdImpl("U-2.001.001.H")) && super.getCompId().equals(new IdImpl("U2   _00619"))){
			Log.info("aha");
		}
		if((avDist < dDistMax) && 
				(avAngle < dPhiMax) && 
				(Math.abs(1 - (matchedLengthRef / refTotalLength)) < lengthTolerancePercentage ) && 
				(Math.abs(1 - (matchedLengthComp / compTotalLength)) < lengthTolerancePercentage )){
			super.setScore((avDist / dDistMax) + 
					(avAngle / dPhiMax) + 
					(Math.abs(1 - (matchedLengthRef / refTotalLength)) / lengthTolerancePercentage ) + 
					(Math.abs(1 - (matchedLengthComp / compTotalLength)) / lengthTolerancePercentage ));
			return true;
		}
		return false;
	}

	@Override
	public int compareTo(AbstractCompare o) {
		return super.compareTo(o);
	}
}
