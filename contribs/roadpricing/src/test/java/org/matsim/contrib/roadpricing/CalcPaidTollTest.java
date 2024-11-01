/* *********************************************************************** *
 * project: org.matsim.*
 * CalcPaidTollTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.roadpricing;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests that {@link RoadPricingTollCalculator} calculates the correct tolls
 * and adds them to the scores of the executed plans.
 *
 * @author mrieser
 */
public class CalcPaidTollTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	static private final Logger log = LogManager.getLogger(CalcPaidTollTest.class);

	@Test
	void testDistanceToll() {
		Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		final String tollFile = utils.getClassInputDirectory() + "/roadpricing1.xml";

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);

		Map<Id<Person>, ? extends Person> referencePopulation = RoadPricingTestUtils.createReferencePopulation1( config ).getPersons();
		Map<Id<Person>, ? extends Person> population = runTollSimulation(tollFile, "distance", config ).getPersons();

		compareScores(
				referencePopulation.get(id1).getPlans().get(0).getScore(),
				population.get(id1).getPlans().get(0).getScore(),
				200 * 0.00020 + 100 * 0.00030);
		compareScores(
				referencePopulation.get(id2).getPlans().get(0).getScore(),
				population.get(id2).getPlans().get(0).getScore(),
				200 * 0.00010 + 100 * 0.00020);
		compareScores(
				referencePopulation.get(id3).getPlans().get(0).getScore(),
				population.get(id3).getPlans().get(0).getScore(),
				200 * 0.00020 + 100 * 0.00030);
		compareScores(
				referencePopulation.get(id4).getPlans().get(0).getScore(),
				population.get(id4).getPlans().get(0).getScore(),
				100 * 0.00020 + 100 * 0.00010 + 100 * 0.00020);
		compareScores(
				referencePopulation.get(id5).getPlans().get(0).getScore(),
				population.get(id5).getPlans().get(0).getScore(),
				100 * 0.00020 + 100 * 0.00030); // agent departs on a tolled link which must NOT be paid.
	}

	@Test
	void testAreaToll() {
		Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		final String tollFile = utils.getClassInputDirectory() + "/roadpricing2.xml";

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id7 = Id.create("7", Person.class);
		Id<Person> id8 = Id.create("8", Person.class);
		Id<Person> id10 = Id.create("10", Person.class);

		Map<Id<Person>, ? extends Person> referencePopulation = RoadPricingTestUtils.createReferencePopulation1( config ).getPersons();
		Map<Id<Person>, ? extends Person> population = runTollSimulation(tollFile, "area", config ).getPersons();

		compareScores(
				referencePopulation.get(id1).getPlans().get(0).getScore(),
				population.get(id1).getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.get(id2).getPlans().get(0).getScore(),
				population.get(id2).getPlans().get(0).getScore(),
				0.00);
		compareScores(
				referencePopulation.get(id3).getPlans().get(0).getScore(),
				population.get(id3).getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.get(id4).getPlans().get(0).getScore(),
				population.get(id4).getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.get(id5).getPlans().get(0).getScore(),
				population.get(id5).getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.get(id7).getPlans().get(0).getScore(),
				population.get(id7).getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.get(id8).getPlans().get(0).getScore(),
				population.get(id8).getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.get(id10).getPlans().get(0).getScore(),
				population.get(id10).getPlans().get(0).getScore(),
				2.00); // drives out of the area, must still pay the toll
	}

	@Test
	void testCordonToll() {
		Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		final String tollFile = utils.getClassInputDirectory() + "/roadpricing3.xml";

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id7 = Id.create("7", Person.class);
		Id<Person> id8 = Id.create("8", Person.class);

		Map<Id<Person>, ? extends Person> referencePopulation = RoadPricingTestUtils.createReferencePopulation1( config ).getPersons();
		Map<Id<Person>, ? extends Person> population = runTollSimulation(tollFile, "link", config ).getPersons();

		compareScores(
				referencePopulation.get(id1).getPlans().get(0).getScore(),
				population.get(id1).getPlans().get(0).getScore(),
				3.00);
		compareScores(
				referencePopulation.get(id2).getPlans().get(0).getScore(),
				population.get(id2).getPlans().get(0).getScore(),
				1.50);
		compareScores(
				referencePopulation.get(id3).getPlans().get(0).getScore(),
				population.get(id3).getPlans().get(0).getScore(),
				3.00);
		compareScores(
				referencePopulation.get(id4).getPlans().get(0).getScore(),
				population.get(id4).getPlans().get(0).getScore(),
				2.0);
//		compareScores(
//				referencePopulation.get(id5).getPlans().get(0).getScore(),
//				population.get(id5).getPlans().get(0).getScore(),
//				1.00); // this agent only pays when entering in the second area, as it starts in the first where it should not be tolled.
//		compareScores(
//				referencePopulation.get(id7).getPlans().get(0).getScore(),
//				population.get(id7).getPlans().get(0).getScore(),
//				0.00); // this agent only leaves the area and should thus never pay a toll
//		compareScores(
//				referencePopulation.get(id8).getPlans().get(0).getScore(),
//				population.get(id8).getPlans().get(0).getScore(),
//				0.00); // this agent only travels within the area and should thus never pay a toll

		// The above test cases have to do with that somewhat weird cording pricing implementation where the toll was only paid on the first tolled link that was encountered.
		// (The assumption being that one has an area toll file and wants to use it for cording pricing.)  That execution path is now deleted.  kai/kai, feb'21

	}

	private void compareScores(final double scoreWithoutToll, final double scoreWithToll, final double expectedToll) {
		log.info("score without toll: " + scoreWithoutToll);
		log.info("score with toll:    " + scoreWithToll);
		log.info("expected toll:      " + expectedToll);
		Assertions.assertEquals(expectedToll, scoreWithoutToll - scoreWithToll, 1e-8);
	}

	/**
	 * FIXME This needs re-implementing with RoadPricingModule.
	 * @param tollFile path to roadpricing.xml file
	 * @param tollType one of four road pricing types
	 * @param config the config object/class.
	 * @return the post-mobsim population.
	 */
	private Population runTollSimulation(final String tollFile, final String tollType, final Config config) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario( config );
		RoadPricingTestUtils.createNetwork1(scenario);
        RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );
		RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(scheme);
		reader.readFile(tollFile);
		Assertions.assertEquals(tollType, scheme.getType());

		RoadPricingTestUtils.createPopulation1(scenario);
		runTollSimulation(scenario, scheme);
		return scenario.getPopulation();
	}

	private void runTollSimulation(final Scenario scenario, final RoadPricingScheme toll) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		@SuppressWarnings("unused")
		RoadPricingTollCalculator paidToll = new RoadPricingTollCalculator(scenario.getNetwork(), toll, eventsManager);
		EventsToScore scoring = EventsToScore.createWithScoreUpdating(scenario, new CharyparNagelScoringFunctionFactory(scenario), eventsManager);
		scoring.beginIteration(0, false);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, eventsManager)
			.run();


		scoring.finish();
	}

}
