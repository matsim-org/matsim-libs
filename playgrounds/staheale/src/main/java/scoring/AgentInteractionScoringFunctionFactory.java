/* *********************************************************************** *
 * project: org.matsim.*
 * AgentInteractionScoringFunctionFactory.java
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

package scoring;

import java.util.TreeMap;
import occupancy.FacilityOccupancy;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.utils.objectattributes.ObjectAttributes;

public class AgentInteractionScoringFunctionFactory implements ScoringFunctionFactory {

	private TreeMap<Id, FacilityOccupancy> facilityOccupancies;
	private ObjectAttributes attributes;
	private final CharyparNagelScoringParameters params;
	private final ActivityFacilities facilities;
    private Network network;
    private double scaleNumberOfPersons;

    public AgentInteractionScoringFunctionFactory(final PlanCalcScoreConfigGroup config, final ActivityFacilities facilities, Network network, double scaleNumberOfPersons) {
		this.params = new CharyparNagelScoringParameters(config);
		this.facilities = facilities;
        this.network = network;
        this.scaleNumberOfPersons = scaleNumberOfPersons;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(final Plan plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new AgentInteractionScoringFunction(plan, params, facilityOccupancies, this.facilities, attributes, this.scaleNumberOfPersons));
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(params, network));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(params));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}

}

