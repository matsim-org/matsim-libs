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
package playground.ivt.kticompatibility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.jointtrips.scoring.BlackListedActivityScoringFunction;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import playground.ivt.scoring.LineChangeScoringFunction;

import java.util.Collection;

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

	private final StageActivityTypes blackList;
	private final KtiLikeScoringConfigGroup ktiConfig;
	private final CharyparNagelScoringParametersForPerson parameters;
    private final Scenario scenario;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
    public KtiLikeActivitiesScoringFunctionFactory(
			final StageActivityTypes typesNotToScore,
			final KtiLikeScoringConfigGroup ktiConfig,
			final Scenario scenario) {
		this.ktiConfig = ktiConfig;
		this.parameters = new SubpopulationCharyparNagelScoringParameters( scenario );
		this.scenario = scenario;
		this.blackList = typesNotToScore;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

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
							params),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.transit_walk,
					LegScoringParameters.createForWalk(
							params),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new LineChangeScoringFunction(
						params ) );

		// other standard stuff
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelMoneyScoring( params ));
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelAgentStuckScoring( params ));

		return scoringFunctionAccumulator;
	}
}

