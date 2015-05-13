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
package playground.thibautd.socnetsimusages.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.ivt.kticompatibility.KtiLikeActivitiesScoringFunctionFactory;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.matsim2030.scoring.DestinationEspilonScoring;
import playground.ivt.scoring.ElementalCharyparNagelLegScoringFunction;
import playground.ivt.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;
import playground.thibautd.socnetsim.framework.scoring.GroupCompositionPenalizer;
import playground.thibautd.socnetsim.framework.scoring.GroupSizePreferencesConfigGroup;
import playground.thibautd.socnetsim.jointtrips.population.JointActingTypes;
import playground.thibautd.socnetsim.run.ScoringFunctionConfigGroup;

/**
 * @author thibautd
 */
public class KtiScoringFunctionFactoryWithJointModes implements ScoringFunctionFactory {
    private final ScoringFunctionFactory delegate;

	private final CharyparNagelScoringParameters params;
	private final Scenario scenario;

	private final ScoringFunctionConfigGroup group;

	private static final double UTIL_OF_NOT_PERF = -1000;

	public KtiScoringFunctionFactoryWithJointModes(
			final StageActivityTypes typesNotToScore,
			final KtiLikeScoringConfigGroup ktiConfig,
			final PlanCalcScoreConfigGroup config,
			final ScoringFunctionConfigGroup group,
			final Scenario scenario) {
		this.scenario = scenario;
		this.params = new CharyparNagelScoringParameters(config);
		this.group = group;
		this.delegate = new KtiLikeActivitiesScoringFunctionFactory(
			typesNotToScore,
			ktiConfig,
			config,
			scenario);
	}

	public KtiScoringFunctionFactoryWithJointModes(
			final ScoringFunctionFactory delegate,
			final Scenario scenario) {
		this( delegate,
				scenario.getConfig().planCalcScore(),
				( ScoringFunctionConfigGroup ) scenario.getConfig().getModule( ScoringFunctionConfigGroup.GROUP_NAME ),
				scenario );
	}

	public KtiScoringFunctionFactoryWithJointModes(
			final ScoringFunctionFactory delegate,
			final PlanCalcScoreConfigGroup config,
			final ScoringFunctionConfigGroup group,
			final Scenario scenario) {
		this.scenario = scenario;
		this.params = new CharyparNagelScoringParameters(config);
		this.group = group;
		this.delegate = delegate;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		final SumScoringFunction scoringFunctionAccumulator =
			(SumScoringFunction) delegate.createNewScoringFunction( person );

		// joint modes
		// XXX: do better for shared cost
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					JointActingTypes.DRIVER,
					new LegScoringParameters(
						group.getConstantDriver(),
						group.getMarginalUtilityOfBeingDriver_s(),
						params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m ),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					JointActingTypes.PASSENGER,
					new LegScoringParameters(
						group.getConstantPassenger(),
						group.getMarginalUtilityOfBeingPassenger_s(),
						// passenger doesn't pay gasoline
						0 ),
					scenario.getNetwork()));

		scoringFunctionAccumulator.addScoringFunction( 
				// technical penalty: penalize plans which do not result in performing
				// all activities.
				// This is necessary when using huge time mutation ranges.
				new ActivityScoring() {
					int actCount = 0;

					@Override
					public void finish() {}

					@Override
					public double getScore() {
						final int nNonPerfActs = TripStructureUtils.getActivities( person.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE ).size() - actCount;
						assert nNonPerfActs >= 0 : nNonPerfActs+" < 0 for plan "+person;
						return nNonPerfActs * UTIL_OF_NOT_PERF;
					}

					@Override
					public void handleActivity(Activity act) {
						actCount++;
					}

					@Override
					public void handleFirstActivity(Activity act) { actCount++; }
					
					@Override
					public void handleLastActivity(Activity act) { actCount++; }
				});

		// XXX this doesn't work, because it just gets the events from the agent
		// (not alters)
		//scoringFunctionAccumulator.addScoringFunction(
		//		new BeingTogetherScoring(
		//			marginalUtilityOfBeingTogether_s,
		//			plan.getPerson().getId(),
		//			socialNetwork.getAlters( plan.getPerson().getId() ) ) );

		if ( group.isUseLocationChoiceEpsilons() ) {
			scoringFunctionAccumulator.addScoringFunction(
					new DestinationEspilonScoring(
						person,
						getOrCreateDestinationChoiceContext(
							scenario ) ) );
		}

		final GroupSizePreferencesConfigGroup groupSizeGroup = (GroupSizePreferencesConfigGroup)
			scenario.getConfig().getModule( GroupSizePreferencesConfigGroup.GROUP_NAME );

		scoringFunctionAccumulator.addScoringFunction(
				new GroupCompositionPenalizer(
					groupSizeGroup.getActivityType(),
					new GroupCompositionPenalizer.MinGroupSizeLinearUtilityOfTime(
						groupSizeGroup.getPersonPreference( person ),
						groupSizeGroup.getUtilityOfMissingContact_util_s() ) ) );

		return scoringFunctionAccumulator;
	}

	private static synchronized DestinationChoiceBestResponseContext getOrCreateDestinationChoiceContext(
			final Scenario scenario) {
		if ( scenario.getScenarioElement( DestinationChoiceBestResponseContext.ELEMENT_NAME ) != null ) {
			return (DestinationChoiceBestResponseContext) scenario.getScenarioElement( DestinationChoiceBestResponseContext.ELEMENT_NAME );
		}

		final DestinationChoiceBestResponseContext context = new DestinationChoiceBestResponseContext( scenario );
		context.init();
		scenario.addScenarioElement( DestinationChoiceBestResponseContext.ELEMENT_NAME , context );
		return context;
	}
}

