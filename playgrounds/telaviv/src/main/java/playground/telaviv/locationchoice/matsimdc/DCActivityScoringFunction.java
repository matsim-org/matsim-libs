/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import playground.telaviv.locationchoice.CalculateDestinationChoice;

import java.util.Map;
import java.util.TreeMap;

public class DCActivityScoringFunction extends org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityScoringFunction {
	static final Logger log = Logger.getLogger(DCActivityScoringFunction.class);
	private TelAvivDestinationScoring destinationChoiceScoring;	
	private Plan plan;
	private DestinationChoiceBestResponseContext dcContext;
		
	public DCActivityScoringFunction(Plan plan, final TreeMap<Id, FacilityPenalty> facilityPenalties, 
			DestinationChoiceBestResponseContext dcContext, Map<Id, Integer> facilityToZoneIndexMap, CalculateDestinationChoice dcCalculator) {
		super(plan, dcContext);
		this.destinationChoiceScoring = new TelAvivDestinationScoring(dcContext, facilityToZoneIndexMap, dcCalculator);
		this.plan = plan;
		this.dcContext = dcContext;
	}
	
	@Override
	public void finish() {				
		super.finish();	
		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof Activity && dcContext.getFlexibleTypes().contains(((Activity) pe).getType())) {				
				this.score += destinationChoiceScoring.getZonalScore((PlanImpl)plan, (ActivityImpl)pe);
			}
		}
	}
}
