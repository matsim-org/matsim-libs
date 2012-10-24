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
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class MixedActivityScoringFunction extends CharyparNagelOpenTimesActivityScoring {
	static final Logger log = Logger.getLogger(MixedActivityScoringFunction.class);
	private final ActivityFacilities facilities;
	
	// for destination scoring: -----------
	private DestinationChoiceScoring destinationChoiceScoring;	
	private Config config;
	// ------------------------------------
	
	public MixedActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, 
			final ActivityFacilities facilities, final TreeMap<Id, FacilityPenalty> facilityPenalties,
			Config config, ObjectAttributes facilitiesKValues, ObjectAttributes personsKValues,ScaleEpsilon scaleEpsilon) {
		//super(plan, params, facilityPenalties, facilities);
		super(plan, params, facilities);
		this.facilities = facilities;
		this.config = config;
		this.destinationChoiceScoring = new DestinationChoiceScoring(
				this.facilities, this.config, facilitiesKValues, personsKValues, scaleEpsilon);
	}
	
	@Override
	public void finish() {		
		
		// do not use distance scoring anymore
//		boolean distance = false;				
//		if (Double.parseDouble(config.findParam(LCEXP, "scoreElementDistance")) > 0.000001) distance = true;
		
		// ----------------------------------------------------------
		// The initial score is set when scoring during or just after the mobsim. 
		// Then the score is still NULL but this.score (ScoringFunction) is NOT.
		// Replanning (setting plan score to -999.0) is done afterwards.
		
		// Setting distance = true (plan.score=-999) for travel time estimation only
		// score is reset to 0.0 after estimation.
		//if (!(this.plan.getScore() == null)) {
			//if (this.plan.getScore() < -998) {
			//	distance = true;
			//}
		//}
		// ----------------------------------------------------------
		
		super.finish();

/* always use tt, thus 
* this.config.locationchoice().getTravelTimes() is always true)  
* i.e., score is never set to zero
*		if (!(Boolean.parseBoolean(this.config.locationchoice().getTravelTimes())) || distance) {
*			this.score = 0.0;
		}
*/		
		for (PlanElement pe : super.plan.getPlanElements()) {
			if (pe instanceof Activity) {
				this.score += destinationChoiceScoring.getDestinationScore((PlanImpl)plan, (ActivityImpl)pe);
			}
		}
	}
}
