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

package org.matsim.contrib.locationchoice.bestresponse.scoring;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.LocationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesActivityScoring;

public class MixedActivityScoringFunction extends CharyparNagelOpenTimesActivityScoring {
	static final Logger log = Logger.getLogger(MixedActivityScoringFunction.class);
	private DestinationChoiceScoring destinationChoiceScoring;	
	
	public MixedActivityScoringFunction(Plan plan, final TreeMap<Id, FacilityPenalty> facilityPenalties, LocationChoiceBestResponseContext lcContext) {
		super(plan, lcContext.getParams(), ((ScenarioImpl)lcContext.getScenario()).getActivityFacilities());
		this.destinationChoiceScoring = new DestinationChoiceScoring(lcContext);
	}
	
	@Override
	public void finish() {				
		super.finish();
		
		for (PlanElement pe : super.plan.getPlanElements()) {
			if (pe instanceof Activity) {
				this.score += destinationChoiceScoring.getDestinationScore((PlanImpl)plan, (ActivityImpl)pe);
			}
		}
	}
}
