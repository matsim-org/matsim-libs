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

package playground.christoph.evacuation.trafficmonitoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.christoph.evacuation.api.core.v01.Coord3d;
import playground.christoph.evacuation.core.utils.geometry.Coord3dImpl;

public class WalkTravelTimeTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(WalkTravelTimeTest.class);
	
	public void testLinkTravelTimeCalculation() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Node node1 = scenario.getNetwork().getFactory().createNode(scenario.createId("n1"), new Coord3dImpl(0.0, 0.0, 0.0));
		Node node2 = scenario.getNetwork().getFactory().createNode(scenario.createId("n2"), new Coord3dImpl(1.0, 0.0, 0.0));
		Link link = scenario.getNetwork().getFactory().createLink(scenario.createId("l1"), node1, node2);
		link.setLength(1.0);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link);
		
		PersonImpl person = (PersonImpl) scenario.getPopulation().getFactory().createPerson(scenario.createId("p1"));
		person.setAge(20);
		person.setSex("m");
		
		// set default walk speed; according to Weidmann 1.34 [m/s]
		double defaultWalkSpeed = 1.34;
		scenario.getConfig().plansCalcRoute().setWalkSpeed(defaultWalkSpeed);
		
		WalkTravelTime walkTravelTime = new WalkTravelTime(scenario.getConfig().plansCalcRoute());
		walkTravelTime.setPerson(person);
		
		double speed;
		double expectedTravelTime;
		double calculatedTravelTime;
		
		// reference speed * person factor * slope factor		
		speed = defaultWalkSpeed * walkTravelTime.personFactor * 1.0;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);

		// increase age
		person.setAge(80);
		walkTravelTime.setPerson(person);
		speed = defaultWalkSpeed * walkTravelTime.personFactor * 1.0;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
				
		// change gender
		person.setSex("f");
		walkTravelTime.setPerson(person);
		speed = defaultWalkSpeed * walkTravelTime.personFactor * 1.0;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);

		// change slope from 0% to -10%
		((Coord3d) node2.getCoord()).setZ(-0.1);
		double slope = walkTravelTime.calcSlope(link);
		double slopeFactor = walkTravelTime.getSlopeFactor(slope);
		speed = defaultWalkSpeed * walkTravelTime.personFactor * slopeFactor;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);				
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		
		// change slope from -10% to 10%
		((Coord3d) node2.getCoord()).setZ(0.1);
		slope = walkTravelTime.calcSlope(link);
		slopeFactor = walkTravelTime.getSlopeFactor(slope);
		speed = defaultWalkSpeed * walkTravelTime.personFactor * slopeFactor;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);				
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
	}
	
	private void printInfo(PersonImpl p, Link l, double expected, double calculated) {
		StringBuffer sb = new StringBuffer();
		sb.append("Age: ");
		sb.append(p.getAge());
		sb.append("; ");

		sb.append("Sex: ");
		sb.append(p.getSex());
		sb.append("; ");
		
		sb.append("Link Steepness: ");
		Coord3d fromNodeCoord = (Coord3d) l.getFromNode().getCoord();
		Coord3d toNodeCoord = (Coord3d) l.getToNode().getCoord();
		sb.append(100.0 * (toNodeCoord.getZ() - fromNodeCoord.getZ()) / l.getLength());
		sb.append("%; ");
		
		sb.append("Expected Travel Time: ");
		sb.append(expected);
		sb.append("; ");

		sb.append("Calculated Travel Time: ");
		sb.append(calculated);
		sb.append("; ");
		
		log.info(sb.toString());
	}
	
}
