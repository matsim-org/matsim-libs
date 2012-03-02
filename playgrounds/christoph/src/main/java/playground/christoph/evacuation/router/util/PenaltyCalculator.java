/* *********************************************************************** *
 * project: org.matsim.*
 * PenaltyCalculator.java
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

package playground.christoph.evacuation.router.util;

import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.christoph.evacuation.config.EvacuationConfig;

public class PenaltyCalculator {

	private final Map<Id, Double> distanceFactors;	// 0..1 (0 .. link at outer boundary, 1 .. link at center point)
	private final double timePenaltyFactor;
	
	/*package*/ PenaltyCalculator(Map<Id, Double> distanceFactors, double timePenaltyFactor) {
		this.distanceFactors = distanceFactors;
		this.timePenaltyFactor = timePenaltyFactor;
	}
	
	public double getPenaltyFactor(Id linkId, double time) {
		if (time <= EvacuationConfig.evacuationTime) return 1.0;
		else {
			Double distanceFactor = distanceFactors.get(linkId);
			if (distanceFactor == null) return 1.0;
					
			// calculate the factor like timePenaltyFactor^(time since evacuation started in hours)
			double timeFactor = Math.pow(timePenaltyFactor, (time - EvacuationConfig.evacuationTime) / 3600);
			
			// scale the time factor linear with the distance
			return 1 + ((timeFactor - 1) * distanceFactor);
		}
	}
}
