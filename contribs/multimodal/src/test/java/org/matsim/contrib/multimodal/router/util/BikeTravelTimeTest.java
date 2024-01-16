/* *********************************************************************** *
 * project: org.matsim.*
 * BikeTravelTimeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.router.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class BikeTravelTimeTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final Logger log = LogManager.getLogger(BikeTravelTimeTest.class);

	@Test
	void testLinkTravelTimeCalculation() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Node node1 = scenario.getNetwork().getFactory().createNode(Id.create("n1", Node.class), new Coord(0.0, 0.0));
		Node node2 = scenario.getNetwork().getFactory().createNode(Id.create("n2", Node.class), new Coord(1.0, 0.0));
		Link link = scenario.getNetwork().getFactory().createLink(Id.create("l1", Link.class), node1, node2);
		link.setLength(1.0);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link);

		double h1 = 0.0;
		double h2 = 0.0;
		Map<Id<Link>, Double> linkSlopes = new HashMap<>();
		double slope = 100 * (h2 - h1) / link.getLength();
		linkSlopes.put(link.getId(), slope);

		Person person = scenario.getPopulation().getFactory().createPerson(Id.create("p1", Person.class));
		PersonUtils.setAge(person, 20);
		PersonUtils.setSex(person, "m");

		// set default walk speed; according to Weidmann 1.34 [m/s]
		double defaultWalkSpeed = 1.34;
		scenario.getConfig().routing().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed);

		// set default bike speed; Prakin and Rotheram according to 6.01 [m/s]
		double defaultBikeSpeed = 6.01;
		scenario.getConfig().routing().setTeleportedModeSpeed(TransportMode.bike, defaultBikeSpeed);

		BikeTravelTime bikeTravelTime;

		double speed;
		double expectedTravelTime;
		double calculatedTravelTime;

		// reference speed * person factor * slope factor
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().routing(), linkSlopes);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() * 1.0;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 0.09368418280727171, 0.0, 0);

		// increase age
		PersonUtils.setAge(person, 80);
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().routing(), linkSlopes);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() * 1.0;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 0.2206463555843433, 0.0, 0);

		// change gender
		PersonUtils.setSex(person, "f");
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().routing(), linkSlopes);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() * 1.0;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 0.24496957588497956, 0.0, 0);

		// change slope from 0% to 10%
		h2 = 0.1;
		slope = 100 * (h2 - h1) / link.getLength();
		linkSlopes.put(link.getId(), slope);
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().routing(), linkSlopes);

		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		double slope2 = bikeTravelTime.getSlope(link);
		double slopeShift = bikeTravelTime.getSlopeShift(slope2);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() + slopeShift;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 0.7332007724445855, 0.0, 0);

		// change slope from 10% to -10%
		h2 = -0.1;
		slope = 100 * (h2 - h1) / link.getLength();
		linkSlopes.put(link.getId(), slope);
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().routing(), linkSlopes);

		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		slope2 = bikeTravelTime.getSlope(link);
		slopeShift = bikeTravelTime.getSlopeShift(slope2);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() + slopeShift;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 0.40547153706106515, 0.0, 0);

		// on very steep links bike speed should equals walk speed - set slope to 25%
		h2 = 0.25;
		slope = 100 * (h2 - h1) / link.getLength();
		linkSlopes.put(link.getId(), slope);
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().routing(), linkSlopes);

		WalkTravelTime walkTravelTime = new WalkTravelTime(scenario.getConfig().routing(), linkSlopes);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		expectedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0, person, null);
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 1.7305194978040106, 0.0, 0);
	}

	private void printInfo(Person p, double expected, double calculated, double slope) {
		StringBuffer sb = new StringBuffer();
		sb.append("Age: ");
		sb.append(PersonUtils.getAge(p));
		sb.append("; ");

		sb.append("Sex: ");
		sb.append(PersonUtils.getSex(p));
		sb.append("; ");

		sb.append("Link Steepness: ");
		sb.append(slope);
		sb.append("%; ");

		sb.append("Expected Travel Time: ");
		sb.append(expected);
		sb.append("; ");

		sb.append("Calculated Travel Time: ");
		sb.append(calculated);
		sb.append("; ");

		log.info(sb.toString());
	}

	@Test
	void testThreadLocals() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		Person p1 = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		PersonUtils.setAge(p1, 90);
		PersonUtils.setSex(p1, "f");

		Person p2 = PopulationUtils.getFactory().createPerson(Id.create("2", Person.class));
		PersonUtils.setAge(p2, 20);
		PersonUtils.setSex(p2, "m");

		Node node1 = scenario.getNetwork().getFactory().createNode(Id.create("n1", Node.class), new Coord(0.0, 0.0));
		Node node2 = scenario.getNetwork().getFactory().createNode(Id.create("n2", Node.class), new Coord(1.0, 0.0));
		Link link = scenario.getNetwork().getFactory().createLink(Id.create("l1", Link.class), node1, node2);
		link.setLength(1.0);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link);

		double h1 = 0.0;
		double h2 = 0.0;
		Map<Id<Link>, Double> linkSlopes = new HashMap<>();
		double slope = 100 * (h2 - h1) / link.getLength();
		linkSlopes.put(link.getId(), slope);

		BikeTravelTime bikeTravelTime = new BikeTravelTime(config.routing(), linkSlopes);

		double tt1 = bikeTravelTime.getLinkTravelTime(link, 0.0, p1, null);
		double tt2 = bikeTravelTime.getLinkTravelTime(link, 0.0, p2, null);

		log.info("run concurrent tests");
		Runnable r1 = new TestRunnable(p1, p2, bikeTravelTime, link, tt1, tt2);
		Runnable r2 = new TestRunnable(p2, p1, bikeTravelTime, link, tt2, tt1);
		Runnable r3 = new TestRunnable(p1, p2, bikeTravelTime, link, tt1, tt2);
		Runnable r4 = new TestRunnable(p2, p1, bikeTravelTime, link, tt2, tt1);

		Thread t1 = new Thread(r1);
		Thread t2 = new Thread(r2);
		Thread t3 = new Thread(r3);
		Thread t4 = new Thread(r4);

		t1.setName("Thread 1");
		t2.setName("Thread 2");
		t3.setName("Thread 3");
		t4.setName("Thread 4");

		t1.start();
		t2.start();
		t3.start();
		t4.start();

		try {
			t1.join();
			t2.join();
			t3.join();
			t4.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		log.info("done");

	}

	private static class TestRunnable implements Runnable {

		private final Person p1;
		private final Person p2;
		private final BikeTravelTime bikeTravelTime;
		private final Link link;
		private final double expectedTravelTime1;
		private final double expectedTravelTime2;

		public TestRunnable(Person p1, Person p2, BikeTravelTime bikeTravelTime, Link link,
				double expectedTravelTime1, double expectedTravelTime2) {
			this.p1 = p1;
			this.p2 = p2;
			this.bikeTravelTime = bikeTravelTime;
			this.link = link;
			this.expectedTravelTime1 = expectedTravelTime1;
			this.expectedTravelTime2 = expectedTravelTime2;
		}

		@Override
		public void run() {
			log.info("Thread " + Thread.currentThread().getName() + " started");
			for (int i = 0; i < 10000; i++) {
				// check with alternating persons
				double tt1 = bikeTravelTime.getLinkTravelTime(link, 0.0, p1, null);
				assertTrue(tt1 == expectedTravelTime1);

				double tt2 = bikeTravelTime.getLinkTravelTime(link, 0.0, p2, null);
				assertTrue(tt2 == expectedTravelTime2);

				// check with constant person
				for (int j = 0; j < 5; j++) {
					double tt = bikeTravelTime.getLinkTravelTime(link, 0.0, p1, null);
					assertTrue(tt == expectedTravelTime1);
				}
			}
			log.info("Thread " + Thread.currentThread().getName() + " finished");
		}
	}
}
