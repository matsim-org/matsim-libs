/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

public class ScoredAlternative implements Comparable<ScoredAlternative> {

	// numerics
	private final static double epsilon = 0.000001;
	
	private double score;
	private Id<ActivityFacility> alternativeId;
	
	public ScoredAlternative(double score, Id<ActivityFacility> alternativeId) {
		this.score = score;
		this.alternativeId = alternativeId;
	}
	
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public Id<ActivityFacility> getAlternativeId() {
		return alternativeId;
	}
	public void setAlternativeId(Id<ActivityFacility> alternativeId) {
		this.alternativeId = alternativeId;
	}

	/* 
	 * Compare keys (double scores). 
	 * If the scores are identical, additionally use the 'alternatives' id's to sort such that deterministic order is ensured
	 */
	@Override
	public int compareTo(ScoredAlternative o) {
		
		// usually it is:
		// this >  o  -> + 1
		// o  == this ->   0
		// this <  o  -> - 1
		
//		// here: reverse order:
//		if (Math.abs(this.score - o.getScore()) > epsilon) {
//			if (this.score > o.getScore()) return -1;
//			else return +1;
//		}		
//		else {
//			return this.alternativeId.compareTo(o.getAlternativeId());
//		}
		
		// Not sure why this epsilon was used :? However, I ran into an Exception: "java.lang.IllegalArgumentException: Comparison method violates its general contract!"
		// Therefore, I adapted it and compared it to 0.0 - hope that's fine. cdobler, oct'15
		if (Math.abs(this.score - o.getScore()) > 0.0) {
			if (this.score > o.getScore()) return -1;
			else return +1;
		} else return this.alternativeId.compareTo(o.getAlternativeId());
	}
}
