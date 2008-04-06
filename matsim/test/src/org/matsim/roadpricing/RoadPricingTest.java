/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.Id;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Node;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.Route;
import org.matsim.roadpricing.RoadPricingScheme.Cost;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCostI;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.xml.sax.SAXException;

/**
 * Tests the RoadPricing features (scoring, router) as isolated as possible.
 *
 * @author mrieser
 */
public class RoadPricingTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
	}

	/** @return a simple network consisting of 5 equal links in a row. */
	private QueueNetworkLayer createNetwork1() {
		/* This creates the following network:
		 *
		 * (1)-------(2)-------(3)-------(4)-------(5)-------(6)
		 *       0         1         2         3         4
		 */
		/* The vehicles can travel with 18km/h = 5m/s, so it should take them 20 seconds
		 * to travel along one link.		 */
		QueueNetworkLayer network = new QueueNetworkLayer();
		network.setCapacityPeriod("01:00:00");
		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		network.createNode("3", "200", "0", null);
		network.createNode("4", "300", "0", null);
		network.createNode("5", "400", "0", null);
		network.createNode("6", "500", "0", null);
		// freespeed 18km/h = 5m/s --> 20s for 100m
		network.createLink("0", "1", "2", "100", "5", "100", "1", null, null);
		network.createLink("1", "2", "3", "100", "5", "100", "1", null, null);
		network.createLink("2", "3", "4", "100", "5", "100", "1", null, null);
		network.createLink("3", "4", "5", "100", "5", "100", "1", null, null);
		network.createLink("4", "5", "6", "100", "5", "100", "1", null, null);
		return network;
	}

	/** @return a simple network with route alternatives in 2 places. */
	private QueueNetworkLayer createNetwork2() {
		/* This creates the following network:
		 *
		 *            3 /----(3)----\ 4
		 *             /             \
		 * (1)-------(2)--------------(4)-------(5)
		 *  |    2            5             6    |
		 *  |1                                   |
		 *  |                                    |
		 * (0)                                   |
     *                                     7 |
     * (11)                                  |
		 *  |                                    |
		 *  |13                                  |
		 *  |    12          11             8    |
		 * (10)------(9)--------------(7)-------(6)
		 *             \             /
		 *           10 \----(8)----/ 9
		 *
		 * each link is 100m long and can be traveled along with 18km/h = 5m/s = 20s for 100m
		 */
		QueueNetworkLayer network = new QueueNetworkLayer();
		network.setCapacityPeriod("01:00:00");
		network.createNode( "0",   "0",   "10", null);
		network.createNode( "1",   "0",  "100", null);
		network.createNode( "2", "100",  "100", null);
		network.createNode( "3", "150",  "150", null);
		network.createNode( "4", "200",  "100", null);
		network.createNode( "5", "300",  "100", null);
		network.createNode( "6", "300", "-100", null);
		network.createNode( "7", "200", "-100", null);
		network.createNode( "8", "150", "-150", null);
		network.createNode( "9", "100", "-100", null);
		network.createNode("10",   "0", "-100", null);
		network.createNode("11",   "0",  "-10", null);
		network.createLink( "1",  "0",  "1", "100", "5", "100", "1", null, null);
		network.createLink( "2",  "1",  "2", "100", "5", "100", "1", null, null);
		network.createLink( "3",  "2",  "3", "100", "5", "100", "1", null, null);
		network.createLink( "4",  "3",  "4", "100", "5", "100", "1", null, null);
		network.createLink( "5",  "2",  "4", "100", "5", "100", "1", null, null);
		network.createLink( "6",  "4",  "5", "100", "5", "100", "1", null, null);
		network.createLink( "7",  "5",  "6", "100", "5", "100", "1", null, null);
		network.createLink( "8",  "6",  "7", "100", "5", "100", "1", null, null);
		network.createLink( "9",  "7",  "8", "100", "5", "100", "1", null, null);
		network.createLink("10",  "8",  "9", "100", "5", "100", "1", null, null);
		network.createLink("11",  "7",  "9", "100", "5", "100", "1", null, null);
		network.createLink("12",  "9", "10", "100", "5", "100", "1", null, null);
		network.createLink("13", "10", "11", "100", "5", "100", "1", null, null);
		return network;
	}

	/** @return a population for network1 */
	private Plans createPopulation1() throws Exception {
		Plans population = new Plans(Plans.NO_STREAMING);

		population.addPerson(createPerson1( 1, "07:00"   , "0", "2 3 4 5", "4")); // toll in 1st time slot
		population.addPerson(createPerson1( 2, "11:00"   , "0", "2 3 4 5", "4")); // toll in 2nd time slot
		population.addPerson(createPerson1( 3, "16:00"   , "0", "2 3 4 5", "4")); // toll in 3rd time slot
		population.addPerson(createPerson1( 4, "09:59:50", "0", "2 3 4 5", "4")); // toll in 1st and 2nd time slot
		population.addPerson(createPerson1( 5, "08:00:00", "1", "3 4 5", "4")); // starts on the 2nd link
		population.addPerson(createPerson1( 6, "09:00:00", "0", "2 3 4", "3")); // ends not on the last link
		population.addPerson(createPerson1( 7, "08:30:00", "1", "3 4", "3")); // starts and ends not on the first/last link
		population.addPerson(createPerson1( 8, "08:35:00", "1", "3", "2")); // starts and ends not on the first/last link
		population.addPerson(createPerson1( 9, "08:40:00", "1", "", "1")); // two acts on the same link
		population.addPerson(createPerson1(10, "08:45:00", "2", "4", "3"));

		return population;
	}

	/** @return a population for network2 */
	private Plans createPopulation2() throws Exception {
		Plans population = new Plans(Plans.NO_STREAMING);

		population.addPerson(createPerson2( 1, "07:00", "1", "7", "13"));

		return population;
	}

	private Person createPerson1(final int personId, final String startTime, final String homeLink, final String routeNodes, final String workLeg) throws Exception {
		Person person = new Person(new Id(personId), "m", 30, "yes", "yes", "yes");
		Plan plan = new Plan(person);
		person.addPlan(plan);
		plan.createAct("h", (String)null, null, homeLink, "00:00", startTime, startTime, "no");
		Leg leg = plan.createLeg("1", "car", startTime, "00:01", null);
		Route route = new Route();
		route.setRoute(routeNodes);
		leg.setRoute(route);
		plan.createAct("w", (String)null, null, workLeg, null, "24:00", null, "yes");
		return person;
	}

	private Person createPerson2(final int personId, final String startTime, final String homeLink, final String workLink, final String finishLink) throws Exception {
		Person person = new Person(new Id(personId), "m", 30, "yes", "yes", "yes");
		Plan plan = new Plan(person);
		person.addPlan(plan);
		plan.createAct("h", (String)null, null, homeLink, "00:00", startTime, startTime, "no");
		plan.createLeg("1", "car", startTime, "00:01", null);
		plan.createAct("w", (String)null, null, workLink, null, "16:00", "08:00", "no");
		plan.createLeg("2", "car", "16:00", null, null);
		plan.createAct("h", (String)null, null, finishLink, null, "24:00", "00:00", "no");
		return person;
	}

	private Plans createReferencePopulation1() {
		// run mobsim once without toll and get score for network1/population1
		try {
			QueueNetworkLayer network = createNetwork1();
			Gbl.getWorld().setNetworkLayer(network);
			Plans referencePopulation = createPopulation1();
			Events events = new Events();
			EventsToScore scoring = new EventsToScore(referencePopulation, new CharyparNagelScoringFunctionFactory());
			events.addHandler(scoring);
			QueueSimulation sim = new QueueSimulation(network, referencePopulation, events);
			sim.run();
			scoring.finish();

			return referencePopulation;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests reader and writer to ensure that reading and writing does not modify the schemes.
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void testWriteReadWrite() throws SAXException, ParserConfigurationException, IOException {

		final String origFile = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/roadpricing1.xml";
		final String tmpFile1 = getOutputDirectory() + "roadpricing1.xml";
		final String tmpFile2 = getOutputDirectory() + "roadpricing2.xml";

		QueueNetworkLayer network = createNetwork1();
		// first, read the scheme from file
		RoadPricingReaderXMLv1 reader1 = new RoadPricingReaderXMLv1(network);
		reader1.parse(origFile);
		RoadPricingScheme scheme1 = reader1.getScheme();

		// write the scheme to a file
		RoadPricingWriterXMLv1 writer1 = new RoadPricingWriterXMLv1(scheme1);
		writer1.writeFile(tmpFile1);

		/* we cannot yet compare the written file with the original file, as the
		 * original file may have be edited manually and may have other indentation
		 * than the written one. Thus, read this file again and write it again and
		 * compare them.
		 */

		RoadPricingReaderXMLv1 reader2 = new RoadPricingReaderXMLv1(network);
		reader2.parse(tmpFile1);
		RoadPricingScheme scheme2 = reader2.getScheme();

		// write the scheme to a file
		RoadPricingWriterXMLv1 writer2 = new RoadPricingWriterXMLv1(scheme2);
		writer2.writeFile(tmpFile2);

		// now compare the two files
		long cksum1 = CRCChecksum.getCRCFromFile(tmpFile1);
		long cksum2 = CRCChecksum.getCRCFromFile(tmpFile2);

		assertEquals(cksum1, cksum2);
	}

	public void testDistanceToll() {
		final String tollFile = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/roadpricing1.xml";

		Plans referencePopulation = createReferencePopulation1();
		Plans population = runTollSimulation(tollFile, "distance");

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
		final String tollFile = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/roadpricing2.xml";

		Plans referencePopulation = createReferencePopulation1();
		Plans population = runTollSimulation(tollFile, "area");

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
		final String tollFile = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/roadpricing3.xml";

		Plans referencePopulation = createReferencePopulation1();
		Plans population = runTollSimulation(tollFile, "cordon");

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

	private Plans runTollSimulation(final String tollFile, final String tollType) {
		QueueNetworkLayer network = createNetwork1();
		Gbl.getWorld().setNetworkLayer(network);

		try {
			RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(network);
			reader.parse(tollFile);
			RoadPricingScheme scheme = reader.getScheme();
			assertEquals(tollType, scheme.getType());

			Plans population = createPopulation1();
			runTollSimulation(network, population, scheme);
			return population;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void runTollSimulation(final QueueNetworkLayer network, final Plans population, final RoadPricingScheme toll) {
		try {
			// run mobsim with toll and get score
			SimulationTimer.reset();

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

	public void testDistanceTollRouter() {
		try {
			QueueNetworkLayer network = createNetwork2();
			Gbl.getWorld().setNetworkLayer(network);
			// a basic toll where only the morning hours are tolled
			RoadPricingScheme toll = new RoadPricingScheme(network);
			toll.setType("distance");
			toll.addLink("5");
			toll.addLink("11");
			Plans population = createPopulation2();
			FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
			TravelCostI costCalc = new TollTravelCostCalculator(timeCostCalc, toll); // we use freespeedTravelCosts as base costs

			// 1st case: without toll, agent chooses shortest path
			new PlansCalcRoute(network, costCalc, timeCostCalc).run(population);
			compareRoutes("1 2 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());

			Cost morningCost = toll.addCost(6*3600, 10*3600, 0.0003); // 0.0003 * link_length(100m) = 0.03, which is slightly below the threshold of 0.0333
			// 2nd case: with a low toll, agent still chooses shortest path
			new PlansCalcRoute(network, costCalc, timeCostCalc).run(population);
			compareRoutes("1 2 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());

			// 3rd case: with a higher toll, agent decides to drive around tolled link
			toll.removeCost(morningCost);
			toll.addCost(6*3600, 10*3600, 0.00035); // new morning toll, this should be slightly over the threshold
			new PlansCalcRoute(network, costCalc, timeCostCalc).run(population);
			compareRoutes("1 2 3 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void testAreaTollRouter() {
		try {
			QueueNetworkLayer network = createNetwork2();
			Gbl.getWorld().setNetworkLayer(network);

			// a basic toll where only the morning hours are tolled
			RoadPricingScheme toll = new RoadPricingScheme(network);
			toll.setType("area");
			toll.addLink("5");
			toll.addLink("11");
			Cost morningCost = toll.addCost(6*3600, 10*3600, 0.06);
			/* Start with a rather low toll. The toll is also so low, because we only
			 * have small network with short links: the cost to travel across one link
			 * is: 20s * (-6 EUR / h) = 20 * (-6) / 3600 = 0.03333
			 */

			Plans population = createPopulation2();
			FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();

			PreProcessLandmarks commonRouterData = new PreProcessLandmarks(timeCostCalc);
			commonRouterData.run(network);

			new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
			compareRoutes("1 2 3 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());
			compareRoutes("6 7 9 10", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(3))).getRoute());

			// now add a toll in the afternoon too, so it is cheaper to pay the toll
			Cost afternoonCost = toll.addCost(14*3600, 18*3600, 0.06);
			new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
			compareRoutes("1 2 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());
			compareRoutes("6 7 9 10", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(3))).getRoute());

			// now remove the costs and add them again, but with a higher amount
			toll.removeCost(morningCost);
			toll.removeCost(afternoonCost);
			toll.addCost(6*3600, 10*3600, 0.7);
			toll.addCost(14*3600, 18*3600, 0.7);
			// the agent should now decide to drive around
			new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
			compareRoutes("1 2 3 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());
			compareRoutes("6 7 8 9 10", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(3))).getRoute());

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public void testCordonTollRouter() {
		try {
			QueueNetworkLayer network = createNetwork2();
			Gbl.getWorld().setNetworkLayer(network);
			// a basic toll where only the morning hours are tolled
			RoadPricingScheme toll = new RoadPricingScheme(network);
			toll.setType("cordon");
			toll.addLink("5");
			toll.addLink("11");
			Plans population = createPopulation2();
			FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
			TravelCostI costCalc = new TollTravelCostCalculator(timeCostCalc, toll); // we use freespeedTravelCosts as base costs

			// 1st case: without toll, agent chooses shortest path
			new PlansCalcRoute(network, costCalc, timeCostCalc).run(population);
			compareRoutes("1 2 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());

			// 2nd case: with a low toll, agent still chooses shortest path and pay the toll
			Cost morningCost = toll.addCost(6*3600, 10*3600, 0.03);
			new PlansCalcRoute(network, costCalc, timeCostCalc).run(population);
			compareRoutes("1 2 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());
			toll.removeCost(morningCost);

			// 3rd case: with a higher toll, agent decides to drive around tolled link
			toll.addCost(6*3600, 10*3600, 0.035);
			new PlansCalcRoute(network, costCalc, timeCostCalc).run(population);
			compareRoutes("1 2 3 4 5", ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void compareRoutes(final String expectedRoute, final Route realRoute) {
		StringBuilder strBuilder = new StringBuilder();
		for (Node node : realRoute.getRoute()) {
			strBuilder.append(node.getId().toString());
			strBuilder.append(' ');
		}
		String route = strBuilder.toString();
		assertEquals(expectedRoute + " ", route);
	}
}
