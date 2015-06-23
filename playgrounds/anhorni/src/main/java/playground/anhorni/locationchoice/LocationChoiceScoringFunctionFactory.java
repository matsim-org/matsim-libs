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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.facilities.ActivityFacilities;


/**
 * A factory to create {@link LocationChoiceScoringFunction}s.
 *
 * @author anhorni
 */
public class LocationChoiceScoringFunctionFactory implements ScoringFunctionFactory {

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;

	private final CharyparNagelScoringParameters params;
	private final ActivityFacilities facilities;
    private Network network;

    public LocationChoiceScoringFunctionFactory(final PlanCalcScoreConfigGroup config,
                                                final TreeMap<Id, FacilityPenalty> facilityPenalties, final ActivityFacilities facilities, Network network) {
		this.params = CharyparNagelScoringParameters.getBuilder(config).create();
		this.facilityPenalties = facilityPenalties;
		this.facilities = facilities;
        this.network = network;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new LocationChoiceScoringFunction(person.getSelectedPlan(), params, facilityPenalties, this.facilities));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		return scoringFunctionAccumulator;
	}

}
