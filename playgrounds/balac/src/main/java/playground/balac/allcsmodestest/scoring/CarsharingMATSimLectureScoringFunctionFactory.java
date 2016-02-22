/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim2010ScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.balac.allcsmodestest.scoring;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.socnetsim.jointtrips.scoring.BlackListedActivityScoringFunction;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.balac.allcsmodestest.scoring.CarsharingMTATSimLectureLegScoringFunction.CarsharingLegScoringParameters;
import playground.ivt.kticompatibility.KtiActivityScoring;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.matsim2030.scoring.DestinationEspilonScoring;

/**
 * @author thibautd
 */
public class CarsharingMATSimLectureScoringFunctionFactory implements ScoringFunctionFactory {

	private final Scenario scenario;
	private final StageActivityTypes blackList;

	// very expensive to initialize:only do once!
	private final Map<Id, CharyparNagelScoringParameters> individualParameters = new HashMap< >();

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
    public CarsharingMATSimLectureScoringFunctionFactory(
			final Scenario scenario,
			final StageActivityTypes typesNotToScore ) {
		this.scenario = scenario;
		this.blackList = typesNotToScore;
	}


	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		// get scenario elements at the lattest possible, to be sure all is initialized
		final KtiLikeScoringConfigGroup ktiConfig = (KtiLikeScoringConfigGroup)
			scenario.getConfig().getModule( KtiLikeScoringConfigGroup.GROUP_NAME );
		final PlanCalcScoreConfigGroup config = scenario.getConfig().planCalcScore();

		final DestinationChoiceBestResponseContext locationChoiceContext = (DestinationChoiceBestResponseContext)
			scenario.getScenarioElement( DestinationChoiceBestResponseContext.ELEMENT_NAME );
		final ObjectAttributes personAttributes =
				scenario.getPopulation().getPersonAttributes();

		final SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		//final ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		final CharyparNagelScoringParameters params = createParams( person , config , personAttributes );

		scoringFunctionAccumulator.addScoringFunction(
				new BlackListedActivityScoringFunction(
					blackList,
					new KtiActivityScoring(
						person.getSelectedPlan(),
						params,
						scenario.getActivityFacilities() )) );

		// standard modes
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.car,
					LegScoringParameters.createForCar(
						params ),
					scenario.getNetwork()));
		// KTI like consideration of influence of travel card
		// (except that is was not expressed as a ratio)
		final Collection<String> travelCards = PersonUtils.getTravelcards(person);
		final double utilityOfDistancePt =
			travelCards == null || travelCards.isEmpty() ?
				params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m :
				params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * ktiConfig.getTravelCardRatio();
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.pt,
					new LegScoringParameters(
						params.modeParams.get(TransportMode.pt).constant,
						params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s,
						utilityOfDistancePt),
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

		scoringFunctionAccumulator.addScoringFunction(new CarsharingMTATSimLectureLegScoringFunction("carsharing", scenario.getConfig() ,scenario.getNetwork(),
				CarsharingLegScoringParameters.createForWalk(
						params )));
		
		
		// other standard stuff
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelMoneyScoring( params ));
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelAgentStuckScoring( params ));

		if ( locationChoiceContext != null ) {
			scoringFunctionAccumulator.addScoringFunction(
					new DestinationEspilonScoring(
						person,
						locationChoiceContext ) );
		}

		return scoringFunctionAccumulator;
	}

	private CharyparNagelScoringParameters createParams(
			final Person person,
			final PlanCalcScoreConfigGroup config,
			final ObjectAttributes personAttributes) {
		if ( individualParameters.containsKey( person.getId() ) ) {
			return individualParameters.get( person.getId() );
		}

		// this is ugly, but otherwise there are warnings logged for each
		// scoring function creation about the (default) non-null PathSizeBeta...
		Logger.getLogger( PlanCalcScoreConfigGroup.class ).setLevel( Level.ERROR );

		final PlanCalcScoreConfigGroup dummyGroup = new PlanCalcScoreConfigGroup();
		for ( Map.Entry<String, String> e : config.getParams().entrySet() ) {
			dummyGroup.addParam( e.getKey() , e.getValue() );
		}

		for ( Collection<? extends ConfigGroup> sets : config.getParameterSets().values() ) {
			for ( ConfigGroup set : sets ) {
				final ConfigGroup dummySet = dummyGroup.createParameterSet( set.getName() );

				for ( Map.Entry<String, String> e : set.getParams().entrySet() ) {
					dummySet.addParam( e.getKey() , e.getValue() );
				}

				dummyGroup.addParameterSet( dummySet );
			}
		}

		final Set<String> handledTypes = new HashSet<String>();
		for ( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan() , blackList ) ) {
			// XXX works only if no variation of type of activities between plans
			if ( !handledTypes.add( act.getType() ) ) continue; // parameters already gotten

			final String id = person.getId().toString();

			// I am not so pleased with this, as wrong parameters may silently be
			// used (for instance if individual preferences are ill-specified).
			// This should become nicer once we have a better format for specifying
			// utility parameters in the config.
			ActivityParams actParams = dummyGroup.getActivityParams( act.getType() );
			if ( actParams == null ) {
				actParams = new ActivityParams( act.getType() );
				dummyGroup.addActivityParams( actParams );
			}

			final Double earliestEndTime =
					(Double) personAttributes.getAttribute(
						id,
						"earliestEndTime_"+act.getType() );
			if ( earliestEndTime != null ) {
				actParams.setScoringThisActivityAtAll( true );
				actParams.setEarliestEndTime( earliestEndTime );
			}

			final Double latestStartTime =
					(Double) personAttributes.getAttribute(
						id,
						"latestStartTime_"+act.getType() );
			if ( latestStartTime != null ) {
				actParams.setScoringThisActivityAtAll( true );
				actParams.setLatestStartTime( latestStartTime );
			}

			final Double minimalDuration =
					(Double) personAttributes.getAttribute(
						id,
						"minimalDuration_"+act.getType() );
			if ( minimalDuration != null ) {
				actParams.setScoringThisActivityAtAll( true );
				actParams.setMinimalDuration( minimalDuration );
			}

			final Double typicalDuration =
					(Double) personAttributes.getAttribute(
						id,
						"typicalDuration_"+act.getType() );
			if ( typicalDuration != null ) {
				actParams.setScoringThisActivityAtAll( true );
				actParams.setTypicalDuration( typicalDuration );
			}
		}

		// TODO: use builder from the beginning
		final CharyparNagelScoringParameters params =
				new CharyparNagelScoringParameters.Builder(dummyGroup, dummyGroup.getScoringParameters(null), scenario.getConfig().scenario()).build();
		individualParameters.put( person.getId() , params );
		return params;
	}
}

