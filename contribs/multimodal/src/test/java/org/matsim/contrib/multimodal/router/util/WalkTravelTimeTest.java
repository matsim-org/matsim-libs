/* *********************************************************************** *
 * project: org.matsim.*
 * WalkTravelTimeTest.java
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

public class WalkTravelTimeTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final Logger log = LogManager.getLogger(WalkTravelTimeTest.class);

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

		WalkTravelTime walkTravelTime;
		walkTravelTime = new WalkTravelTime(scenario.getConfig().routing(), linkSlopes);

		double speed;
		double expectedTravelTime;
		double calculatedTravelTime;

		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0, person, null);
		// reference speed * person factor * slope factor
		speed = defaultWalkSpeed * walkTravelTime.personFactors.get(person.getId()) * 1.0;
		expectedTravelTime = link.getLength() / speed;

		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 0.42018055124753945, 0.0, 0);

		// increase age
		PersonUtils.setAge(person, 80);
		walkTravelTime = new WalkTravelTime(scenario.getConfig().routing(), linkSlopes);
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultWalkSpeed * walkTravelTime.personFactors.get(person.getId()) * 1.0;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 0.9896153709417187, 0.0, 0);

		// change gender
		PersonUtils.setSex(person, "f");
		walkTravelTime = new WalkTravelTime(scenario.getConfig().routing(), linkSlopes);
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultWalkSpeed * walkTravelTime.personFactors.get(person.getId()) * 1.0;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 1.0987068291557665, 0.0, 0);

		// change slope from 0% to -10%
		h2 = -0.1;
		slope = 100 * (h2 - h1) / link.getLength();
		linkSlopes.put(link.getId(), slope);
		walkTravelTime = new WalkTravelTime(scenario.getConfig().routing(), linkSlopes);
		double slope2 = walkTravelTime.getSlope(link);
		double slopeFactor = walkTravelTime.getSlopeFactor(slope2);
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultWalkSpeed * walkTravelTime.personFactors.get(person.getId()) * slopeFactor;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 1.0489849428640121, 0.0, 0);

		// change slope from -10% to 10%
		h2 = 0.1;
		slope = 100 * (h2 - h1) / link.getLength();
		linkSlopes.put(link.getId(), slope);
		walkTravelTime = new WalkTravelTime(scenario.getConfig().routing(), linkSlopes);
        slopeFactor = walkTravelTime.getSlopeFactor(slope);
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultWalkSpeed * walkTravelTime.personFactors.get(person.getId()) * slopeFactor;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, expectedTravelTime, calculatedTravelTime, slope);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < MatsimTestUtils.EPSILON);
		Assertions.assertEquals(calculatedTravelTime - 1.2397955643824945, 0.0, 0);
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

		Person p1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		PersonUtils.setAge(p1, 90);
		PersonUtils.setSex(p1, "f");

		Person p2 = PopulationUtils.getFactory().createPerson(Id.create(2, Person.class));
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

		WalkTravelTime walkTravelTime = new WalkTravelTime(config.routing(), linkSlopes);

		double tt1 = walkTravelTime.getLinkTravelTime(link, 0.0, p1, null);
		double tt2 = walkTravelTime.getLinkTravelTime(link, 0.0, p2, null);

		log.info("run concurrent tests");
		Runnable r1 = new TestRunnable(p1, p2, walkTravelTime, link, tt1, tt2);
		Runnable r2 = new TestRunnable(p2, p1, walkTravelTime, link, tt2, tt1);
		Runnable r3 = new TestRunnable(p1, p2, walkTravelTime, link, tt1, tt2);
		Runnable r4 = new TestRunnable(p2, p1, walkTravelTime, link, tt2, tt1);

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
		private final WalkTravelTime walkTravelTime;
		private final Link link;
		private final double expectedTravelTime1;
		private final double expectedTravelTime2;

		public TestRunnable(Person p1, Person p2, WalkTravelTime walkTravelTime, Link link,
				double expectedTravelTime1, double expectedTravelTime2) {
			this.p1 = p1;
			this.p2 = p2;
			this.walkTravelTime = walkTravelTime;
			this.link = link;
			this.expectedTravelTime1 = expectedTravelTime1;
			this.expectedTravelTime2 = expectedTravelTime2;
		}

		@Override
		public void run() {
			log.info("Thread " + Thread.currentThread().getName() + " started");
			for (int i = 0; i < 10000; i++) {
				// check with alternating persons
				double tt1 = walkTravelTime.getLinkTravelTime(link, 0.0, p1, null);
				assertTrue(tt1 == expectedTravelTime1);

				double tt2 = walkTravelTime.getLinkTravelTime(link, 0.0, p2, null);
				assertTrue(tt2 == expectedTravelTime2);

				// check with constant person
				for (int j = 0; j < 5; j++) {
					double tt = walkTravelTime.getLinkTravelTime(link, 0.0, p1, null);
					assertTrue(tt == expectedTravelTime1);
				}
			}
			log.info("Thread " + Thread.currentThread().getName() + " finished");
		}
	}
}
