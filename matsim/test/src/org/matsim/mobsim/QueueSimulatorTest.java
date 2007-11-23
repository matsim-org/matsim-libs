/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulatorTest.java
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

package org.matsim.mobsim;


import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.Link;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.Route;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.World;

public class QueueSimulatorTest extends MatsimTestCase {

	public void testFlowCapacity() {
		Config config = Gbl.createConfig(null);
		config.simulation().setFlowCapFactor(1.0);
		config.simulation().setStorageCapFactor(1.0);
		World world = Gbl.getWorld();

		/* build network */
		QueueNetworkLayer network = new QueueNetworkLayer();
		world.setNetworkLayer(network);
		network.setCapacityPeriod("1:00:00");
		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		network.createNode("3", "1100", "0", null);
		network.createNode("4", "1200", "0", null);
		Link link1 = network.createLink("1", "1", "2", "100", "10", "12000", "4", null, null);
		Link link2 = network.createLink("2", "2", "3", "1000", "10", "6000", "2", null, null);
		Link link3 = network.createLink("3", "3", "4", "100", "10", "12000", "4", null, null);

		/* build plans */
		Plans plans = new Plans(Plans.NO_STREAMING);

		try {
			// add a first person with leg from link2 to link3
			Person person = new Person(new Id(0), "m", 35, "yes", "yes", "yes");
			Plan plan = person.createPlan(null, null, "yes");
			plan.createAct("h", 199.0, 0.0, link2, 0, 7*3600-10, 7*3600-10, false);
			Leg leg = plan.createLeg(1, "car", 7*3600-110, Gbl.UNDEFINED_TIME, Gbl.UNDEFINED_TIME);
			Route route = new Route();
			route.setRoute("3");
			leg.setRoute(route);
			plan.createAct("w", 99.0, 0.0, link3, 7*3600+10, 24*36000, Gbl.UNDEFINED_TIME, true);
			plans.addPerson(person);

			// add a lot of other persons with legs from link1 to link3
			for (int i = 1; i < 7000; i++) {
				person = new Person(new Id(i), "m", 35, "yes", "yes", "yes");
				plan = person.createPlan(null, null, "yes");
				plan.createAct("h", 99.0, 0.0, link1, 0, 7*3600-110, 7*3600-110, false);
				leg = plan.createLeg(1, "car", 7*3600-110, Gbl.UNDEFINED_TIME, Gbl.UNDEFINED_TIME);
				route = new Route();
				route.setRoute("2 3");
				leg.setRoute(route);
				plan.createAct("w", 99.0, 0.0, link3, 7*3600+10, 24*36000, Gbl.UNDEFINED_TIME, true);
				plans.addPerson(person);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(network, plans, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink("2");
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		assertEquals(6000, volume[7]); // we should have maximum flow in this hour
		assertEquals(14, volume[6]); // well, just compare this, too, in that moment, it shouldn't hurt anyone...
		assertEquals(986, volume[8]); // ... and that, too
	}

}
