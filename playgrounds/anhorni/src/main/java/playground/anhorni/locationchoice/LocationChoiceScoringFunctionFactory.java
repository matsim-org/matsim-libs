/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoiceScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;
import org.matsim.locationchoice.facilityload.FacilityPenalty;


/**
 * A factory to create {@link LocationChoiceScoringFunction}s.
 *
 * @author anhorni
 */
public class LocationChoiceScoringFunctionFactory implements ScoringFunctionFactory {

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;

	private final CharyparNagelScoringParameters params;
	private final ActivityFacilities facilities;
	
	public LocationChoiceScoringFunctionFactory(final PlanCalcScoreConfigGroup config, 
			final TreeMap<Id, FacilityPenalty> facilityPenalties, final ActivityFacilities facilities) {
		this.params = new CharyparNagelScoringParameters(config);
		this.facilityPenalties = facilityPenalties;
		this.facilities = facilities;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(final Plan plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new LocationChoiceScoringFunction(plan, params, facilityPenalties, this.facilities));
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(params));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(params));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}

}
