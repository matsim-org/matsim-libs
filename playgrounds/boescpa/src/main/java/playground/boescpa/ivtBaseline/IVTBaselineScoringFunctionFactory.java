/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.socnetsim.jointtrips.scoring.BlackListedActivityScoringFunction;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.matsim2030.scoring.DestinationEspilonScoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Based on playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory by thibautd
 *
 * @author boescpa
 */
public class IVTBaselineScoringFunctionFactory implements ScoringFunctionFactory {

	private final Scenario scenario;
	private final StageActivityTypes blackList;

	// very expensive to initialize:only do once!
	private final Map<Id, CharyparNagelScoringParameters> individualParameters = new HashMap<>();

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
	public IVTBaselineScoringFunctionFactory(
			final Scenario scenario,
			final StageActivityTypes typesNotToScore ) {
		this.scenario = scenario;
		this.blackList = typesNotToScore;
	}


	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		// get scenario elements at the lattest possible, to be sure all is initialized
		final PlanCalcScoreConfigGroup config = scenario.getConfig().planCalcScore();
		final ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();

		final SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		final CharyparNagelScoringParameters params =
				createParams( person , config , scenario.getConfig().scenario(), personAttributes );

		// activities
		scoringFunctionAccumulator.addScoringFunction(
				new BlackListedActivityScoringFunction(blackList,
						new CharyparNagelOpenTimesActivityScoring(params, scenario.getActivityFacilities())));
		//		CharyparNagelActivityScoring warns if first activity of the day and last activity of the day are not equal.
		//		As we have home and remote_home activities, this case occurs on intention very often in our scenarios.
		//		Ergo we have to suppress this output or we will get GBs of logs...
		Logger.getLogger( org.matsim.core.scoring.functions.CharyparNagelActivityScoring.class ).setLevel( Level.ERROR );

		// legs
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));

		// location choice
		final DestinationChoiceBestResponseContext locationChoiceContext = (DestinationChoiceBestResponseContext)
				scenario.getScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME);
		if ( locationChoiceContext != null) {
			scoringFunctionAccumulator.addScoringFunction(
					new DestinationEspilonScoring(person, locationChoiceContext));
		}

		// other standard stuff
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		return scoringFunctionAccumulator;
	}

	private CharyparNagelScoringParameters createParams(
			final Person person,
			final PlanCalcScoreConfigGroup config,
			final ScenarioConfigGroup scenarioConfig,
			final ObjectAttributes personAttributes) {
		if ( individualParameters.containsKey( person.getId() ) ) {
			return individualParameters.get( person.getId() );
		}

		final CharyparNagelScoringParameters.Builder builder =
				new CharyparNagelScoringParameters.Builder(config, config.getScoringParameters(null), scenarioConfig);
		final Set<String> handledTypes = new HashSet<>();
		for ( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan() , blackList ) ) {
			// XXX works only if no variation of type of activities between plans
			if ( !handledTypes.add( act.getType() ) ) continue; // parameters already gotten

			final String id = person.getId().toString();

			// I am not so pleased with this, as wrong parameters may silently be
			// used (for instance if individual preferences are ill-specified).
			// This should become nicer once we have a better format for specifying
			// utility parameters in the config.
			final ActivityUtilityParameters.Builder typeBuilder =
					new ActivityUtilityParameters.Builder(
							config.getActivityParams( act.getType() ) != null ?
									config.getActivityParams( act.getType() ) :
									new PlanCalcScoreConfigGroup.ActivityParams( act.getType() ) );

			final Double earliestEndTime =
					(Double) personAttributes.getAttribute(
							id,
							"earliestEndTime_"+act.getType() );
			if ( earliestEndTime != null ) {
				typeBuilder.setScoreAtAll(true);
				typeBuilder.setEarliestEndTime( earliestEndTime );
			}

			final Double latestStartTime =
					(Double) personAttributes.getAttribute(
							id,
							"latestStartTime_"+act.getType() );
			if ( latestStartTime != null ) {
				typeBuilder.setScoreAtAll(true);
				typeBuilder.setLatestStartTime(latestStartTime);
			}

			final Double minimalDuration =
					(Double) personAttributes.getAttribute(
							id,
							"minimalDuration_"+act.getType() );
			if ( minimalDuration != null ) {
				typeBuilder.setScoreAtAll( true );
				typeBuilder.setMinimalDuration(minimalDuration);
			}

			final Double typicalDuration =
					(Double) personAttributes.getAttribute(
							id,
							"typicalDuration_"+act.getType() );
			if ( typicalDuration != null ) {
				typeBuilder.setScoreAtAll( true );
				typeBuilder.setTypicalDuration_s(typicalDuration);
			}

			builder.setActivityParameters(
					act.getType(),
					typeBuilder );
		}

		final CharyparNagelScoringParameters params =
				builder.build();
		individualParameters.put( person.getId() , params );
		return params;
	}

}
