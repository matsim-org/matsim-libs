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

package playground.telaviv.locationchoice.matsimdc;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import playground.telaviv.locationchoice.CalculateDestinationChoice;
import playground.telaviv.locationchoice.Coefficients;
import playground.telaviv.zones.Emme2Zone;
import playground.telaviv.zones.ZoneMapping;

public class TelAvivDestinationScoring extends org.matsim.contrib.locationchoice.bestresponse.scoring.DestinationScoring { 
	private CalculateDestinationChoice dcCalculator;
	Map<Integer, Emme2Zone> zones;
		
	public TelAvivDestinationScoring(DestinationChoiceBestResponseContext dcContext, ZoneMapping zoneMapping, CalculateDestinationChoice dcCalculator) {
		super(dcContext);	
		this.zones = zoneMapping.getParsedZones();
		this.dcCalculator = dcCalculator;
	}
	
	public double getZonalScore(PlanImpl plan, ActivityImpl act) {
		Activity previousActivity = plan.getPreviousActivity(plan.getPreviousLeg(act));
		int fromZoneIndex = this.getZoneIndex(previousActivity.getLinkId());
		int toZoneIndex = this.getZoneIndex(act.getLinkId());
		
		int timeSlotIndex = 0;
		// not required as constant factors are not given per time slot
		// int timeSlotIndex = this.dcCalculator.getTimeSlotIndex(plan.getPreviousLeg(act).getDepartureTime())
		
		double utility = this.dcCalculator.getVtod()
				[Coefficients.types.indexOf(act.getType())] // type
						[fromZoneIndex] // origin
								[toZoneIndex] // destination
										[timeSlotIndex]; //time bin
		
		return utility;
	}
	
	// could be speed up: search for fromZone and toZone at the same time
	private int getZoneIndex(Id linkId) {
		int index = 0;
		for (Emme2Zone zone : this.zones.values()) {
			if (zone.linkIds.contains(linkId)) return index;
			index++;
		}
		return -1;
		
	}
}
