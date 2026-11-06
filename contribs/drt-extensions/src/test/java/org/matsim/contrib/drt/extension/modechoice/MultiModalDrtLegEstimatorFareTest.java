/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalDrtLegEstimatorFareTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                            *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.modechoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;

public class MultiModalDrtLegEstimatorFareTest {

	@Test
	void fareLowersEstimatedUtility() {
		assertThat(estimate(true)).isCloseTo(estimate(false) - 1.5 * 2.5, within(1e-9));
	}

	private static double estimate(boolean withFare) {
		Config config = ConfigUtils.createConfig();
		config.scoring().setMarginalUtilityOfMoney(1.5);
		config.scoring().getOrCreateDefaultModeParams("drt");

		DrtConfigGroup drtCfg = new DrtConfigGroup();
		drtCfg.setMode("drt");
		if (withFare) {
			DrtFareParams fareParams = new DrtFareParams();
			fareParams.setBaseFare(2.5);
			drtCfg.addParameterSet(fareParams);
		}
		MultiModeDrtConfigGroup multiModeDrt = new MultiModeDrtConfigGroup();
		multiModeDrt.addDrtConfigGroup(drtCfg);
		config.addModule(multiModeDrt);

		Map<DvrpMode, DrtEstimator> estimators = Map.of(DvrpModes.mode("drt"),
			(route, departureTime) -> new DrtEstimator.Estimate(1000, 300, 120, 0));

		Scenario scenario = ScenarioUtils.createScenario(config);
		Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("p"));
		ScoringParameters scoring = new SubpopulationScoringParameters(scenario).getScoringParameters(person);

		Leg leg = PopulationUtils.createLeg("drt");
		DrtRoute route = new DrtRoute(Id.createLinkId("a"), Id.createLinkId("b"));
		route.setDistance(1000);
		route.setDirectRideTime(300);
		leg.setRoute(route);

		return new MultiModalDrtLegEstimator(config, estimators).estimate(new EstimatorContext(person, scoring), "drt", leg, ModeAvailability.YES);
	}
}
