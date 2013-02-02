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
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class MixedActivityWOFacilitiesScoringFunction extends CharyparNagelActivityScoring {
	static final Logger log = Logger.getLogger(MixedActivityWOFacilitiesScoringFunction.class);
	private final ActivityFacilities facilities;
	
	private DestinationChoiceScoring destinationChoiceScoring;	
	private Config config;
	private Plan plan ;
	
	public MixedActivityWOFacilitiesScoringFunction(Plan plan, CharyparNagelScoringParameters params, 
			final ActivityFacilities facilities, final TreeMap<Id, FacilityPenalty> facilityPenalties,
			Config config, ObjectAttributes facilitiesKValues, ObjectAttributes personsKValues,ScaleEpsilon scaleEpsilon) {
		//super(plan, params, facilityPenalties, facilities);
		super(params);
		this.facilities = facilities;
		this.config = config;
		this.destinationChoiceScoring = new DestinationChoiceScoring(
				this.facilities, this.config, facilitiesKValues, personsKValues, scaleEpsilon);
		this.plan = plan ;
	}
	
	@Override
	public void finish() {		
		
		super.finish();

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				this.score += destinationChoiceScoring.getDestinationScore((PlanImpl)plan, (ActivityImpl)pe);
			}
		}
	}
}
