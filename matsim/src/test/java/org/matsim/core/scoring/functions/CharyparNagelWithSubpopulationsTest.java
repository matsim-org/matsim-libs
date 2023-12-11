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
package org.matsim.core.scoring.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ScoringParameterSet;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;

/**
 * @author thibautd
 */
public class CharyparNagelWithSubpopulationsTest {
	@Test
	void testLegsScoredDifferently() {
		final Scenario sc = createTestScenario();

		final CharyparNagelScoringFunctionFactory functionFactory = new CharyparNagelScoringFunctionFactory( sc );

		final ScoringFunction function1 =
				functionFactory.createNewScoringFunction(
						sc.getPopulation().getPersons().get(
								Id.createPersonId( 1 ) ) );

		final ScoringFunction function2 =
				functionFactory.createNewScoringFunction(
						sc.getPopulation().getPersons().get(
								Id.createPersonId( 2 ) ) );

		final Leg leg = PopulationUtils.createLeg("skateboard");
		leg.setDepartureTime( 10 );
		leg.setTravelTime(10);

		final Route route = RouteUtils.createGenericRouteImpl(null, null);
		route.setDistance( 10 );
		route.setTravelTime( 10 );
		leg.setRoute( route );

		function1.handleLeg(leg);
		function1.finish();

		function2.handleLeg(leg);
		function2.finish();

		Assertions.assertFalse(
				Math.abs( function1.getScore() - function2.getScore() ) < 1E-9,
				"same score for legs of agents in different subpopulations" );
	}

	@Test
	void testActivitiesScoredDifferently() {
		final Scenario sc = createTestScenario();

		final CharyparNagelScoringFunctionFactory functionFactory = new CharyparNagelScoringFunctionFactory( sc );

		final ScoringFunction function1 =
				functionFactory.createNewScoringFunction(
						sc.getPopulation().getPersons().get(
								Id.createPersonId( 1 ) ) );

		final ScoringFunction function2 =
				functionFactory.createNewScoringFunction(
						sc.getPopulation().getPersons().get(
								Id.createPersonId( 2 ) ) );

		final Activity act = PopulationUtils.createActivityFromCoordAndLinkId("chill", null, null);
		act.setStartTime( 8 * 3600d );
		act.setEndTime( 18 * 3600d );

		function1.handleActivity( act );
		function1.finish();

		function2.handleActivity( act );
		function2.finish();

		Assertions.assertFalse(
				Math.abs( function1.getScore() - function2.getScore() ) < 1E-9,
				"same score for legs of agents in different subpopulations" );
	}

	private Scenario createTestScenario() {
		final Config config = ConfigUtils.createConfig();
		final Scenario sc = ScenarioUtils.createScenario(config);

		for ( int i=1; i <= 2; i++ ) {
			final String subpop = ""+i;
			final Person person = sc.getPopulation().getFactory().createPerson(Id.createPersonId(i));
			sc.getPopulation().addPerson(person);
//			sc.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", subpop);
			PopulationUtils.putPersonAttribute( person, "subpopulation", subpop );

			final double util = (double) i;
			final ScoringParameterSet params = config.scoring().getOrCreateScoringParameters(subpop);

			params.setMarginalUtlOfWaitingPt_utils_hr(-util);
			params.setEarlyDeparture_utils_hr(-util);
			params.setLateArrival_utils_hr(-util);
			params.setMarginalUtilityOfMoney(util);
			params.setMarginalUtlOfWaiting_utils_hr(-util);
			params.setPerforming_utils_hr(util);
			params.setUtilityOfLineSwitch(-util);

			final ModeParams modeParams = params.getOrCreateModeParams( "skateboard" );
			modeParams.setConstant(-util);
			modeParams.setMarginalUtilityOfDistance(-util);
			modeParams.setMarginalUtilityOfTraveling(-util);
			modeParams.setMonetaryDistanceRate(util);

			final ActivityParams activityParams = params.getOrCreateActivityParams( "chill" );
			activityParams.setTypicalDuration( util * 3600d );
		}

		return sc;
	}
}
