/* *********************************************************************** *
 * project: org.matsim.*
 * KtiLikeActivitiesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.scoring;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.thibautd.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;
import playground.thibautd.socnetsim.population.JointActingTypes;

/**
 * This factory creates "CharyparNagel" scoring functions, but with
 * a KTI activity scoring function.
 * <br>
 * This way, the Zürich "v2" population can be used for joint trips simulation:
 * with the default scoring function, the different interpretation of the desired
 * duration creates problems.
 * <br>
 * Usage of this scoring function requires the use of facilities.
 *
 * @author thibautd
 */
public class KtiLikeActivitiesScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
    private final Scenario scenario;
	private final TreeMap<Id, FacilityPenalty> facilityPenalties;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
    public KtiLikeActivitiesScoringFunctionFactory(
			final PlanCalcScoreConfigGroup config,
			final Scenario scenario) {
		this( config , new TreeMap<Id, FacilityPenalty>() , scenario );
	}

    public KtiLikeActivitiesScoringFunctionFactory(
			final PlanCalcScoreConfigGroup config,
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			final Scenario scenario) {
		this.params = new CharyparNagelScoringParameters(config);
		this.scenario = scenario;
		this.facilityPenalties = facilityPenalties;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(
				new CarPoolingOpeningTimesActivityScoringFunction(
					plan, 
					params,
					facilityPenalties,
					((ScenarioImpl) scenario).getActivityFacilities() ));

		// standard modes
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.car,
					LegScoringParameters.createForCar(
						params ),
					scenario.getNetwork()));
		// TODO: adapt costs to travel card.
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.pt,
					LegScoringParameters.createForPt(
						params ),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.walk,
					LegScoringParameters.createForWalk(
						params ),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.bike,
					LegScoringParameters.createForBike(
						params ),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.transit_walk,
					LegScoringParameters.createForWalk(
						params ),
					scenario.getNetwork()));

		// joint modes
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					JointActingTypes.DRIVER,
					LegScoringParameters.createForCar(
						params ),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					JointActingTypes.DRIVER,
					new LegScoringParameters(
						params.constantCar,
						params.marginalUtilityOfTraveling_s,
						// passenger doesn't pay gasoline
						0 ),
					scenario.getNetwork()));

		// other standard stuff
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelMoneyScoring( params ));
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelAgentStuckScoring( params ));

		return scoringFunctionAccumulator;
	}
}

