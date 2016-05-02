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

package org.matsim.roadpricing;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests that {@link CalcPaidToll} calculates the correct tolls
 * and adds them to the scores of the executed plans.
 *
 * @author mrieser
 */
public class CalcPaidTollTest extends MatsimTestCase {

	static private final Logger log = Logger.getLogger(CalcPaidTollTest.class);

	public void testDistanceToll() {
		Config config = loadConfig(this.getClassInputDirectory() + "config.xml");
		final String tollFile = this.getClassInputDirectory() + "/roadpricing1.xml";

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);

		Map<Id<Person>, ? extends Person> referencePopulation = Fixture.createReferencePopulation1( config ).getPersons();
		Map<Id<Person>, ? extends Person> population = runTollSimulation(tollFile, "distance", config ).getPersons();

		compareScores(
				referencePopulation.get(id1).getPlans().get(0).getScore().doubleValue(),
				population.get(id1).getPlans().get(0).getScore().doubleValue(),
				200 * 0.00020 + 100 * 0.00030);
		compareScores(
				referencePopulation.get(id2).getPlans().get(0).getScore().doubleValue(),
				population.get(id2).getPlans().get(0).getScore().doubleValue(),
				200 * 0.00010 + 100 * 0.00020);
		compareScores(
				referencePopulation.get(id3).getPlans().get(0).getScore().doubleValue(),
				population.get(id3).getPlans().get(0).getScore().doubleValue(),
				200 * 0.00020 + 100 * 0.00030);
		compareScores(
				referencePopulation.get(id4).getPlans().get(0).getScore().doubleValue(),
				population.get(id4).getPlans().get(0).getScore().doubleValue(),
				100 * 0.00020 + 100 * 0.00010 + 100 * 0.00020);
		compareScores(
				referencePopulation.get(id5).getPlans().get(0).getScore().doubleValue(),
				population.get(id5).getPlans().get(0).getScore().doubleValue(),
				100 * 0.00020 + 100 * 0.00030); // agent departs on a tolled link which must NOT be paid.
	}

	public void testAreaToll() {
		Config config = loadConfig(this.getClassInputDirectory() + "config.xml");
		final String tollFile = this.getClassInputDirectory() + "/roadpricing2.xml";

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id7 = Id.create("7", Person.class);
		Id<Person> id8 = Id.create("8", Person.class);
		Id<Person> id10 = Id.create("10", Person.class);

		Map<Id<Person>, ? extends Person> referencePopulation = Fixture.createReferencePopulation1( config ).getPersons();
		Map<Id<Person>, ? extends Person> population = runTollSimulation(tollFile, "area", config ).getPersons();

		compareScores(
				referencePopulation.get(id1).getPlans().get(0).getScore().doubleValue(),
				population.get(id1).getPlans().get(0).getScore().doubleValue(),
				2.00);
		compareScores(
				referencePopulation.get(id2).getPlans().get(0).getScore().doubleValue(),
				population.get(id2).getPlans().get(0).getScore().doubleValue(),
				0.00);
		compareScores(
				referencePopulation.get(id3).getPlans().get(0).getScore().doubleValue(),
				population.get(id3).getPlans().get(0).getScore().doubleValue(),
				2.00);
		compareScores(
				referencePopulation.get(id4).getPlans().get(0).getScore().doubleValue(),
				population.get(id4).getPlans().get(0).getScore().doubleValue(),
				2.00);
		compareScores(
				referencePopulation.get(id5).getPlans().get(0).getScore().doubleValue(),
				population.get(id5).getPlans().get(0).getScore().doubleValue(),
				2.00);
		compareScores(
				referencePopulation.get(id7).getPlans().get(0).getScore().doubleValue(),
				population.get(id7).getPlans().get(0).getScore().doubleValue(),
				2.00);
		compareScores(
				referencePopulation.get(id8).getPlans().get(0).getScore().doubleValue(),
				population.get(id8).getPlans().get(0).getScore().doubleValue(),
				2.00);
		compareScores(
				referencePopulation.get(id10).getPlans().get(0).getScore().doubleValue(),
				population.get(id10).getPlans().get(0).getScore().doubleValue(),
				2.00); // drives out of the area, must still pay the toll
	}

	public void testCordonToll() {
		Config config = loadConfig(this.getClassInputDirectory() + "config.xml");
		final String tollFile = this.getClassInputDirectory() + "/roadpricing3.xml";

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id7 = Id.create("7", Person.class);
		Id<Person> id8 = Id.create("8", Person.class);

		Map<Id<Person>, ? extends Person> referencePopulation = Fixture.createReferencePopulation1( config ).getPersons();
		Map<Id<Person>, ? extends Person> population = runTollSimulation(tollFile, "cordon", config ).getPersons();

		compareScores(
				referencePopulation.get(id1).getPlans().get(0).getScore().doubleValue(),
				population.get(id1).getPlans().get(0).getScore().doubleValue(),
				2.00);
		compareScores(
				referencePopulation.get(id2).getPlans().get(0).getScore().doubleValue(),
				population.get(id2).getPlans().get(0).getScore().doubleValue(),
				1.00);
		compareScores(
				referencePopulation.get(id3).getPlans().get(0).getScore().doubleValue(),
				population.get(id3).getPlans().get(0).getScore().doubleValue(),
				2.00);
		compareScores(
				referencePopulation.get(id4).getPlans().get(0).getScore().doubleValue(),
				population.get(id4).getPlans().get(0).getScore().doubleValue(),
				1.50);
		compareScores(
				referencePopulation.get(id5).getPlans().get(0).getScore().doubleValue(),
				population.get(id5).getPlans().get(0).getScore().doubleValue(),
				1.00); // this agent only pays when entering in the second area, as it starts in the first where it should not be tolled.
		compareScores(
				referencePopulation.get(id7).getPlans().get(0).getScore().doubleValue(),
				population.get(id7).getPlans().get(0).getScore().doubleValue(),
				0.00); // this agent only leaves the area and should thus never pay a toll
		compareScores(
				referencePopulation.get(id8).getPlans().get(0).getScore().doubleValue(),
				population.get(id8).getPlans().get(0).getScore().doubleValue(),
				0.00); // this agent only travels within the area and should thus never pay a toll
	}

	private void compareScores(final double scoreWithoutToll, final double scoreWithToll, final double expectedToll) {
		log.info("score without toll: " + scoreWithoutToll);
		log.info("score with toll:    " + scoreWithToll);
		log.info("expected toll:      " + expectedToll);
		assertEquals(expectedToll, scoreWithoutToll - scoreWithToll, 1e-8);
	}

	private Population runTollSimulation(final String tollFile, final String tollType, final Config config) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario( config );
		Fixture.createNetwork1(scenario);
//        ConfigUtils.addOrGetModule(scenario.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setUseRoadpricing(true);
        RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		scenario.addScenarioElement( RoadPricingScheme.ELEMENT_NAME , scheme);
		RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(scheme);
		reader.parse(tollFile);
		assertEquals(tollType, scheme.getType());

		Fixture.createPopulation1(scenario);
		runTollSimulation(scenario, scheme);
		return scenario.getPopulation();
	}

	private void runTollSimulation(final Scenario scenario, final RoadPricingScheme toll) {
		EventsManager events = EventsUtils.createEventsManager();
		CalcPaidToll paidToll = new CalcPaidToll(scenario.getNetwork(), toll);
		events.addHandler(paidToll);
		EventsToScore scoring = EventsToScore.createWithScoreUpdating(scenario, new CharyparNagelScoringFunctionFactory(scenario), events);
		scoring.beginIteration(0);

		Mobsim sim = QSimUtils.createDefaultQSim(scenario, events);
		sim.run();

		paidToll.sendMoneyEvents(Time.MIDNIGHT, events);

		scoring.finish();
	}

}
