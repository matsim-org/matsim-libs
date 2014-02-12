/* *********************************************************************** *
 * project: org.matsim.*
 * KtiScoringFunctionFactoryWithJointModes.java
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
package playground.thibautd.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ActivityScoring;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.ivt.kticompatibility.KtiLikeActivitiesScoringFunctionFactory;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.scoring.ElementalCharyparNagelLegScoringFunction;
import playground.ivt.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;
import playground.thibautd.socnetsim.population.JointActingTypes;

/**
 * @author thibautd
 */
public class KtiScoringFunctionFactoryWithJointModes implements ScoringFunctionFactory {
    private final ScoringFunctionFactory delegate;
	private final CharyparNagelScoringParameters params;
	private final Scenario scenario;

	private final double marginalUtilOfBeingDriver_s;
	private final double marginalUtilOfBeingPassenger_s;

	private static final double UTIL_OF_NOT_PERF = -1000;

	public KtiScoringFunctionFactoryWithJointModes(
			final double marginalUtilityOfBeingDriver_s,
			final double marginalUtilityOfBeingPassenger_s,
			final StageActivityTypes typesNotToScore,
			final KtiLikeScoringConfigGroup ktiConfig,
			final PlanCalcScoreConfigGroup config,
			final Scenario scenario) {
		this.marginalUtilOfBeingDriver_s = marginalUtilityOfBeingDriver_s;
		this.marginalUtilOfBeingPassenger_s = marginalUtilityOfBeingPassenger_s;
		this.scenario = scenario;
		this.params = new CharyparNagelScoringParameters(config);
		this.delegate = new KtiLikeActivitiesScoringFunctionFactory(
			typesNotToScore,
			ktiConfig,
			config,
			scenario);
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Plan plan) {
		final ScoringFunctionAccumulator scoringFunctionAccumulator =
			(ScoringFunctionAccumulator) delegate.createNewScoringFunction( plan );

		// joint modes
		// XXX: do better for shared cost
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					JointActingTypes.DRIVER,
					new LegScoringParameters(
						params.modeParams.get(TransportMode.car).constant,
						marginalUtilOfBeingDriver_s,
						params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m ),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					JointActingTypes.PASSENGER,
					new LegScoringParameters(
						params.modeParams.get(TransportMode.car).constant,
						marginalUtilOfBeingPassenger_s,
						// passenger doesn't pay gasoline
						0 ),
					scenario.getNetwork()));

		scoringFunctionAccumulator.addScoringFunction( 
				// technical penalty: penalize plans which do not result in performing
				// all activities.
				// This is necessary when using huge time mutation ranges.
				new ActivityScoring() {
					// start at one, because first act doesnt generate a start event
					int actCount = 1;

					@Override
					public void finish() {}

					@Override
					public double getScore() {
						final int nNonPerfActs = TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE ).size() - actCount;
						assert nNonPerfActs >= 0 : nNonPerfActs+" < 0 for plan "+plan;
						return nNonPerfActs * UTIL_OF_NOT_PERF;
					}

					@Override
					public void reset() {}

					@Override
					public void startActivity(double time, Activity act) {
						actCount++;
					}

					@Override
					public void endActivity(double time, Activity act) {}
				});

		// XXX this doesn't work, because it just gets the events from the agent
		// (not alters)
		//scoringFunctionAccumulator.addScoringFunction(
		//		new BeingTogetherScoring(
		//			marginalUtilityOfBeingTogether_s,
		//			plan.getPerson().getId(),
		//			socialNetwork.getAlters( plan.getPerson().getId() ) ) );

		return scoringFunctionAccumulator;
	}
}

