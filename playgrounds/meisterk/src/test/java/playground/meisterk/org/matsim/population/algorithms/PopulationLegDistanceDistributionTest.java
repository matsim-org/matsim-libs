/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationLegDistanceDistributionTest.java
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

package playground.meisterk.org.matsim.population.algorithms;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.org.matsim.population.algorithms.AbstractClassifiedFrequencyAnalysis.CrosstabFormat;

public class PopulationLegDistanceDistributionTest extends MatsimTestCase {

	public static final double[] distanceClasses = new double[]{
		0.0, 
		100, 200, 500, 
		1000, 2000, 5000, 
		10000, 20000, 50000, 
		100000, 200000, 500000,
		1000000};

	public void testGenerationDistribution() {
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer testNetwork = scenario.getNetwork();
		Node node1 = testNetwork.createAndAddNode(new IdImpl("1"), new CoordImpl(0.0, 0.0));
		Node node2 = testNetwork.createAndAddNode(new IdImpl("2"), new CoordImpl(500.0, 500.0));
		Node node3 = testNetwork.createAndAddNode(new IdImpl("3"), new CoordImpl(1000.0, 1000.0));
		Link startLink = testNetwork.createAndAddLink(new IdImpl("101"), node1, node2, 500.0, 27.7778, 2000.0, 1.0);
		Link endLink = testNetwork.createAndAddLink(new IdImpl("102"), node2, node3, 1000.0, 27.7778, 2000.0, 1.0);
		
		PersonImpl testPerson = new PersonImpl(new IdImpl("1000"));
		PlanImpl testPlan = testPerson.createAndAddPlan(true);
		
		ActivityImpl act = testPlan.createAndAddActivity("startActivity", startLink);
		
		Leg leg = testPlan.createAndAddLeg(TransportMode.car);
		
		NodeNetworkRouteImpl route = new NodeNetworkRouteImpl(startLink, endLink);
		route.setDistance(1200.0);
		
		leg.setRoute(route);
		
		act = testPlan.createAndAddActivity("endActivity", endLink);
		
		PopulationImpl pop = scenario.getPopulation();
		pop.setIsStreaming(true);
		
		PrintStream out = null;
		try {
			out = new PrintStream(this.getOutputDirectory() + "actualOutput.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AbstractClassifiedFrequencyAnalysis testee = new PopulationLegDistanceDistribution(out);
		pop.addAlgorithm(testee);
		
		pop.addPerson(testPerson);
		
		assertEquals(1, testee.getNumberOfModes());
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[4], distanceClasses[5]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[5], distanceClasses[4]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[4], distanceClasses[5]));

		leg.setMode(TransportMode.pt);
		route.setDistance(13456.7);

		pop.addPerson(testPerson);
		
		assertEquals(2, testee.getNumberOfModes());
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[4], distanceClasses[5]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt, distanceClasses[7], distanceClasses[8]));
		
		leg.setMode(TransportMode.car);
		route.setDistance(0.0);
		
		pop.addPerson(testPerson);
		
		assertEquals(2, testee.getNumberOfModes());
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, -1000.0, distanceClasses[0]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[5], distanceClasses[4]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt, distanceClasses[7], distanceClasses[8]));

		assertEquals(3, testee.getNumberOfLegs());
		assertEquals(2, testee.getNumberOfLegs(TransportMode.car));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt));
		assertEquals(1, testee.getNumberOfLegs(-1000.0, distanceClasses[0]));
		assertEquals(1, testee.getNumberOfLegs(distanceClasses[5], distanceClasses[4]));
		assertEquals(1, testee.getNumberOfLegs(distanceClasses[7], distanceClasses[8]));
		
		for (boolean isCumulative : new boolean[]{false, true}) {
			for (CrosstabFormat crosstabFormat : CrosstabFormat.values()) {
				testee.printClasses(crosstabFormat, isCumulative, distanceClasses, out);
			}
		}

		testee.printDeciles(true, out);

		testee.printQuantiles(true, 12, out);

		out.close();

		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + "expectedOutput.txt");
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + "actualOutput.txt");
		assertEquals(expectedChecksum, actualChecksum);
		
	}
	
}
