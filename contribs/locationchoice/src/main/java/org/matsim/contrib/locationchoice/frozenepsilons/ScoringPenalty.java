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

package org.matsim.contrib.locationchoice.frozenepsilons;


//import org.apache.log4j.Logger;


public class ScoringPenalty {

	private double startTime = 0;
	private double endTime = 0;
	private FacilityPenalty facilityPenalty = null;
	private double score = 0.0;

	//private static final Logger log = Logger.getLogger(Penalty.class);

	public ScoringPenalty(double startTime, double endTime, FacilityPenalty facilityPenalty, double score) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.facilityPenalty = facilityPenalty;
		this.score = score;
	}


	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}

	public double getPenalty() {
		
		double penaltyFactor = 0.0;
		if (this.facilityPenalty != null) {
			this.facilityPenalty.finish(); // is this still needed? we have a call in EventsToFacilityLoad
			penaltyFactor = this.facilityPenalty.getCapacityPenaltyFactor(startTime, endTime);
		}
		return this.score * penaltyFactor;
	}
}
