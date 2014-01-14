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


import java.util.TreeMap;
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
import playground.telaviv.zones.ZoneMapping;

public class DCActivityScoringFunction extends org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityScoringFunction {
	static final Logger log = Logger.getLogger(DCActivityScoringFunction.class);
	private TelAvivDestinationScoring destinationChoiceScoring;	
	private Plan plan;
		
	public DCActivityScoringFunction(Plan plan, final TreeMap<Id, FacilityPenalty> facilityPenalties, 
			DestinationChoiceBestResponseContext dcContext, ZoneMapping zoneMapping, CalculateDestinationChoice dcCalculator) {
		super(plan, facilityPenalties, dcContext);
		this.destinationChoiceScoring = new TelAvivDestinationScoring(dcContext, zoneMapping, dcCalculator);
		this.plan = plan;
	}
	
	@Override
	public void finish() {				
		super.finish();	
		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof Activity) {				
				this.score += destinationChoiceScoring.getZonalScore((PlanImpl)plan, (ActivityImpl)pe);
			}
		}
	}
}
