/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.LEGO.miniscenario.run.scoring;

import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.locationchoice.facilityload.FacilityPenalty;

public class MixedActivityScoringFunction extends org.matsim.core.scoring.charyparNagel.ActivityScoringFunction {
	static final Logger log = Logger.getLogger(MixedActivityScoringFunction.class);
	private final ActivityFacilities facilities;
	
	// for destination scoring: -----------
	private Random random;
	private DestinationChoiceScoring destinationChoiceScoring;	
	private Config config;
	// ------------------------------------
	
	private final String LCEXP = "locationchoiceExperimental";

	public MixedActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, 
			final ActivityFacilities facilities, Random random, 
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			Config config) {
		//super(plan, params, facilityPenalties, facilities);
		super(plan, params);
		this.random = random;
		this.facilities = facilities;
		this.config = config;
		this.destinationChoiceScoring = new DestinationChoiceScoring(this.random, this.facilities, this.config);
	}
	
	@Override
	public void finish() {		
		boolean distance = false;
				
		if (Double.parseDouble(config.findParam(LCEXP, "scoreElementDistance")) > 0.000001) distance = true;
		
		// ----------------------------------------------------------
		// The initial score is set when scoring during or just after the mobsim. 
		// Then the score is still NULL but this.score (ScoringFunction) is NOT.
		// Replanning (setting plan score to -999.0) is done afterwards.
		
		// Setting distance = true (plan.score=-999) for travel time estimation only
		// score is reset to 0.0 after estimation.
		if (!(this.plan.getScore() == null)) {
			if (this.plan.getScore() < -998) {
			//	distance = true;
			}
		}
		// ----------------------------------------------------------
		
		super.finish();
		
		double offset = Double.parseDouble(config.findParam(LCEXP, "actScoreOffset"));
		double scale = Double.parseDouble(config.findParam(LCEXP, "actScoreScale"));
		
		if (Boolean.parseBoolean(this.config.locationchoice().getTravelTimes()) && !distance) {
			this.score = (this.score - offset) * scale;
		}
		else {
			this.score = 0.0;	
		}		
		for (PlanElement pe : super.plan.getPlanElements()) {
			if (pe instanceof Activity) {
				this.score += destinationChoiceScoring.getDestinationScore((PlanImpl)plan, (ActivityImpl)pe, distance);
			}
		}
	}
}
