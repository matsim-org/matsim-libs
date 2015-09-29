/* *********************************************************************** *
 * project: org.matsim.*
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

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.socnetsim.framework.scoring.GroupCompositionPenalizer;
import org.matsim.contrib.socnetsim.framework.scoring.GroupSizePreferencesConfigGroup;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;
import org.matsim.contrib.socnetsim.run.ScoringFunctionConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.pt.PtConstants;
import playground.ivt.matsim2030.scoring.DestinationEspilonScoring;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;
import playground.thibautd.analysis.scoretracking.ScoreTrackingListenner;
import playground.thibautd.socnetsimusages.traveltimeequity.EquityConfigGroup;
import playground.thibautd.socnetsimusages.traveltimeequity.StandardDeviationScorer;
import playground.thibautd.socnetsimusages.traveltimeequity.TravelTimesRecord;

import java.util.Set;

/**
 * @author thibautd
 */
public class KtiScoringFunctionFactoryWithJointModesAndEquity implements ScoringFunctionFactory {
	private final TravelTimesRecord travelTimesRecords;
	private final double betaStdDev;

	private final ScoringFunctionFactory delegate;

	private final CharyparNagelScoringParametersForPerson parameters;
	private final Scenario scenario;

	private final ScoringFunctionConfigGroup group;

	private static final double UTIL_OF_NOT_PERF = -1000;

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

	@Inject
	public KtiScoringFunctionFactoryWithJointModesAndEquity(
				final ScoreTrackingListenner tracker,
				final Scenario scenario,
				final TravelTimesRecord travelTimesRecords,
				final Config config) {
		this.scenario = scenario;
		this.parameters = new SubpopulationCharyparNagelScoringParameters( scenario );
		this.delegate =
					new MATSim2010ScoringFunctionFactory(
							scenario,
							new StageActivityTypesImpl(
									PtConstants.TRANSIT_ACTIVITY_TYPE,
									JointActingTypes.INTERACTION) );
		this.travelTimesRecords = travelTimesRecords;

		this.group = (ScoringFunctionConfigGroup) config.getModule( ScoringFunctionConfigGroup.GROUP_NAME );
		this.betaStdDev = ((EquityConfigGroup) config.getModule( EquityConfigGroup.GROUP_NAME) ).getBetaStandardDev();
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		final SumScoringFunction function =
			(SumScoringFunction) delegate.createNewScoringFunction( person );

		final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

		// joint modes
		// XXX: do better for shared cost
		function.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
						JointActingTypes.DRIVER,
						new LegScoringParameters(
								group.getConstantDriver(),
								group.getMarginalUtilityOfBeingDriver_s(),
								params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m),
						scenario.getNetwork()));
		function.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
						JointActingTypes.PASSENGER,
						new LegScoringParameters(
								group.getConstantPassenger(),
								group.getMarginalUtilityOfBeingPassenger_s(),
								// passenger doesn't pay gasoline
								0),
						scenario.getNetwork()));

		function.addScoringFunction(
				// technical penalty: penalize plans which do not result in performing
				// all activities.
				// This is necessary when using huge time mutation ranges.
				new ActivityScoring() {
					int actCount = 0;

					@Override
					public void finish() {
					}

					@Override
					public double getScore() {
						final int nNonPerfActs = TripStructureUtils.getActivities(person.getSelectedPlan(), EmptyStageActivityTypes.INSTANCE).size() - actCount;
						assert nNonPerfActs >= 0 : nNonPerfActs + " < 0 for plan " + person;
						return nNonPerfActs * UTIL_OF_NOT_PERF;
					}

					@Override
					public void handleActivity(Activity act) {
						actCount++;
					}

					@Override
					public void handleFirstActivity(Activity act) {
						actCount++;
					}

					@Override
					public void handleLastActivity(Activity act) {
						actCount++;
					}
				});

		if ( group.isUseLocationChoiceEpsilons() ) {
			function.addScoringFunction(
					new DestinationEspilonScoring(
							person,
							getOrCreateDestinationChoiceContext(
									scenario)));
		}

		final GroupSizePreferencesConfigGroup groupSizeGroup = (GroupSizePreferencesConfigGroup)
			scenario.getConfig().getModule( GroupSizePreferencesConfigGroup.GROUP_NAME );

		function.addScoringFunction(
				new GroupCompositionPenalizer(
						groupSizeGroup.getActivityType(),
						new GroupCompositionPenalizer.MinGroupSizeLinearUtilityOfTime(
								groupSizeGroup.getPersonPreference(person),
								groupSizeGroup.getUtilityOfMissingContact_util_s())));


		function.addScoringFunction(
				new StandardDeviationScorer(
						travelTimesRecords,
						group.getJoinableActivityTypes(),
						betaStdDev ) );
		return function;
	}
}
