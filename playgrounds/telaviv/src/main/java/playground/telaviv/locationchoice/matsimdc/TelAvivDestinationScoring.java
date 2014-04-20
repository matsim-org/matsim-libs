/* *********************************************************************** *
 * project: org.matsim.*
 * TelAvivDestinationScoring.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice.matsimdc;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

import playground.telaviv.locationchoice.CalculateDestinationChoice;
import playground.telaviv.locationchoice.Coefficients;

public class TelAvivDestinationScoring extends org.matsim.contrib.locationchoice.bestresponse.scoring.DestinationScoring {
	
	private final CalculateDestinationChoice dcCalculator;
	private final Map<Id, Integer> facilityToZoneIndexMap;
	
	private static final Logger log = Logger.getLogger(TelAvivDestinationScoring.class);
		
	public TelAvivDestinationScoring(DestinationChoiceBestResponseContext dcContext, 
			Map<Id, Integer> facilityToZoneIndexMap, CalculateDestinationChoice dcCalculator) {
		super(dcContext);	
		this.dcCalculator = dcCalculator;
		this.facilityToZoneIndexMap = facilityToZoneIndexMap;
	}
	
	public double getZonalScore(PlanImpl plan, ActivityImpl act) {
		Activity previousActivity = plan.getPreviousActivity(plan.getPreviousLeg(act));
		int fromZoneIndex = this.getZoneIndex(previousActivity.getFacilityId());
		int toZoneIndex = this.getZoneIndex(act.getFacilityId());
		
		int timeSlotIndex = 0;
		// not required as constant factors are not given per time slot
		// int timeSlotIndex = this.dcCalculator.getTimeSlotIndex(plan.getPreviousLeg(act).getDepartureTime())
		
		double utility = 0.0;
		if (fromZoneIndex < 0) {
			//log.warn("There is no zone mapping for the from link: " + previousActivity.getLinkId() + ". Returning zone utility = 0.0");
			return utility;
		} else if (toZoneIndex < 0) {
			//log.warn("There is no zone mapping for the to link: " + act.getLinkId() + ". Returning zone utility = 0.0");
			return utility;
		} else  
		utility = this.dcCalculator.getVtod()
				[Coefficients.types.indexOf(act.getType())] // type
						[fromZoneIndex] // origin
								[toZoneIndex] // destination
										[timeSlotIndex]; //time bin
		
		return utility;
	}
	
	// could be speed up: search for fromZone and toZone at the same time
	private int getZoneIndex(Id facilityId) {
		Integer index = this.facilityToZoneIndexMap.get(facilityId);
		if (index != null) return index;
		else return -1;
//		int index = 0;
//		for (Emme2Zone zone : this.zones.values()) {
//			if (zone.linkIds.contains(linkId)) return index;
//			index++;
//		}
//		return -1;
	}
	
}
