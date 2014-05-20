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
package playground.ivt.matsim2030.scoring;

import java.util.Collection;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.ivt.kticompatibility.KtiActivityScoring;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.scoring.BlackListedActivityScoringFunction;
import playground.ivt.scoring.ElementalCharyparNagelLegScoringFunction;
import playground.ivt.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;

/**
 * @author thibautd
 */
public class MATSim2010ScoringFunctionFactory implements ScoringFunctionFactory {

	private final StageActivityTypes blackList;
	private final KtiLikeScoringConfigGroup ktiConfig;
	private final CharyparNagelScoringParameters params;
    private final Scenario scenario;
	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final DestinationChoiceBestResponseContext locationChoiceContext;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
    public MATSim2010ScoringFunctionFactory(
			final DestinationChoiceBestResponseContext locationChoiceContext,
			final StageActivityTypes typesNotToScore,
			final KtiLikeScoringConfigGroup ktiConfig,
			final PlanCalcScoreConfigGroup config,
			final Scenario scenario) {
		this.locationChoiceContext = locationChoiceContext;
		this.ktiConfig = ktiConfig;
		this.params = new CharyparNagelScoringParameters(config);
		this.scenario = scenario;
		this.facilityPenalties = new TreeMap<Id, FacilityPenalty>();
		this.blackList = typesNotToScore;
	}


	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(
				new BlackListedActivityScoringFunction(
					blackList,
					new KtiActivityScoring(
						person.getSelectedPlan(),
						params,
						facilityPenalties,
						((ScenarioImpl) scenario).getActivityFacilities() )) );

		// standard modes
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.car,
					LegScoringParameters.createForCar(
						params ),
					scenario.getNetwork()));
		// KTI like consideration of influence of travel card
		// (except that is was not expressed as a ratio)
		final Collection<String> travelCards = ((PersonImpl) person).getTravelcards();
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
}

