/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingScoringFunctionTest.java
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

import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.World;

/**
 * Tests {@link RoadPricingScoringFunction} that it correctly scores the executed plans, respecting eventual tolls.
 *
 * @author mrieser
 */
public class RoadPricingScoringFunctionTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		loadConfig(this.getClassInputDirectory() + "config.xml");
	}

	public void testDistanceToll() {
		final String tollFile = this.getClassInputDirectory() + "/roadpricing1.xml";
		final World world = Gbl.createWorld();

		Population referencePopulation = Fixture.createReferencePopulation1(world);
		Population population = runTollSimulation(tollFile, "distance", world);

		compareScores(
				referencePopulation.getPerson("1").getPlans().get(0).getScore(),
				population.getPerson("1").getPlans().get(0).getScore(),
				300 * 0.00020);
		compareScores(
				referencePopulation.getPerson("2").getPlans().get(0).getScore(),
				population.getPerson("2").getPlans().get(0).getScore(),
				300 * 0.00010);
		compareScores(
				referencePopulation.getPerson("3").getPlans().get(0).getScore(),
				population.getPerson("3").getPlans().get(0).getScore(),
				300 * 0.00020);
		compareScores(
				referencePopulation.getPerson("4").getPlans().get(0).getScore(),
				population.getPerson("4").getPlans().get(0).getScore(),
				100 * 0.00020 + 200 * 0.00010);
		compareScores(
				referencePopulation.getPerson("5").getPlans().get(0).getScore(),
				population.getPerson("5").getPlans().get(0).getScore(),
				200 * 0.00020); // agent departs on a tolled link which must NOT be paid.
	}

	public void testAreaToll() {
		final String tollFile = this.getClassInputDirectory() + "/roadpricing2.xml";
		final World world = Gbl.createWorld();

		Population referencePopulation = Fixture.createReferencePopulation1(world);
		Population population = runTollSimulation(tollFile, "area", world);

		compareScores(
				referencePopulation.getPerson("1").getPlans().get(0).getScore(),
				population.getPerson("1").getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.getPerson("2").getPlans().get(0).getScore(),
				population.getPerson("2").getPlans().get(0).getScore(),
				0.00);
		compareScores(
				referencePopulation.getPerson("3").getPlans().get(0).getScore(),
				population.getPerson("3").getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.getPerson("4").getPlans().get(0).getScore(),
				population.getPerson("4").getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.getPerson("5").getPlans().get(0).getScore(),
				population.getPerson("5").getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.getPerson("7").getPlans().get(0).getScore(),
				population.getPerson("7").getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.getPerson("8").getPlans().get(0).getScore(),
				population.getPerson("8").getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.getPerson("10").getPlans().get(0).getScore(),
				population.getPerson("10").getPlans().get(0).getScore(),
				2.00); // drives out of the area, must still pay the toll
	}

	public void testCordonToll() {
		final String tollFile = this.getClassInputDirectory() + "/roadpricing3.xml";
		final World world = Gbl.createWorld();

		Population referencePopulation = Fixture.createReferencePopulation1(world);
		Population population = runTollSimulation(tollFile, "cordon", world);

		compareScores(
				referencePopulation.getPerson("1").getPlans().get(0).getScore(),
				population.getPerson("1").getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.getPerson("2").getPlans().get(0).getScore(),
				population.getPerson("2").getPlans().get(0).getScore(),
				1.00);
		compareScores(
				referencePopulation.getPerson("3").getPlans().get(0).getScore(),
				population.getPerson("3").getPlans().get(0).getScore(),
				2.00);
		compareScores(
				referencePopulation.getPerson("4").getPlans().get(0).getScore(),
				population.getPerson("4").getPlans().get(0).getScore(),
				1.50);
		compareScores(
				referencePopulation.getPerson("5").getPlans().get(0).getScore(),
				population.getPerson("5").getPlans().get(0).getScore(),
				1.00); // this agent only pays when entering in the second area, as it starts in the first where it should not be tolled.
		compareScores(
				referencePopulation.getPerson("7").getPlans().get(0).getScore(),
				population.getPerson("7").getPlans().get(0).getScore(),
				0.00); // this agent only leaves the area and should thus never pay a toll
		compareScores(
				referencePopulation.getPerson("8").getPlans().get(0).getScore(),
				population.getPerson("8").getPlans().get(0).getScore(),
				0.00); // this agent only travels within the area and should thus never pay a toll
	}

	private void compareScores(final double scoreWithoutToll, final double scoreWithToll, final double expectedToll) {
		System.out.println("score without toll: " + scoreWithoutToll);
		System.out.println("score with toll:    " + scoreWithToll);
		System.out.println("expected toll:      " + expectedToll);
		assertEquals(expectedToll, scoreWithoutToll - scoreWithToll, 1e-8);
	}

	private Population runTollSimulation(final String tollFile, final String tollType, final World world) {
		NetworkLayer network = Fixture.createNetwork1();
		world.setNetworkLayer(network);

		try {
			RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(network);
			reader.parse(tollFile);
			RoadPricingScheme scheme = reader.getScheme();
			assertEquals(tollType, scheme.getType());

			Population population = Fixture.createPopulation1();
			runTollSimulation(network, population, scheme);
			return population;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void runTollSimulation(final NetworkLayer network, final Population population, final RoadPricingScheme toll) {
		try {
			Events events = new Events();
			CalcPaidToll paidToll = new CalcPaidToll(network, toll);
			events.addHandler(paidToll);
			EventsToScore scoring = new EventsToScore(population, new RoadPricingScoringFunctionFactory(paidToll, new CharyparNagelScoringFunctionFactory()));
			events.addHandler(scoring);

			QueueSimulation sim = new QueueSimulation(network, population, events);
			sim.run();

			scoring.finish();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
