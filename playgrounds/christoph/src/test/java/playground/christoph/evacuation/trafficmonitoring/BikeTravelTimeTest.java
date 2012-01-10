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

public class BikeTravelTimeTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(BikeTravelTimeTest.class);
	
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
		
		// set default bike speed; Prakin and Rotheram according to 6.01 [m/s]
		double defaultBikeSpeed = 6.01;
		scenario.getConfig().plansCalcRoute().setBikeSpeed(defaultBikeSpeed);
		
		BikeTravelTime bikeTravelTime = new BikeTravelTime(scenario.getConfig().plansCalcRoute());
		bikeTravelTime.setPerson(person);
		
		double speed;
		double expectedTravelTime;
		double calculatedTravelTime;
		
		// reference speed * person factor * slope factor
		speed = defaultBikeSpeed * bikeTravelTime.personFactor * 1.0;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		
		// increase age
		person.setAge(80);
		bikeTravelTime.setPerson(person);
		speed = defaultBikeSpeed * bikeTravelTime.personFactor * 1.0;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		
		// change gender
		person.setSex("f");
		bikeTravelTime.setPerson(person);
		speed = defaultBikeSpeed * bikeTravelTime.personFactor * 1.0;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		
		// change slope from 0% to 10%
		((Coord3d) node2.getCoord()).setZ(0.1);
		double slope = bikeTravelTime.calcSlope(link);
		double slopeShift = bikeTravelTime.getSlopeShift(slope);
		speed = defaultBikeSpeed * bikeTravelTime.personFactor + slopeShift;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);				
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		
		// change slope from 10% to -10%
		((Coord3d) node2.getCoord()).setZ(-0.1);
		slope = bikeTravelTime.calcSlope(link);
		slopeShift = bikeTravelTime.getSlopeShift(slope);
		speed = defaultBikeSpeed * bikeTravelTime.personFactor + slopeShift;
		expectedTravelTime = link.getLength() / speed;
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime);				
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		
		// on very steep links bike speed should equals walk speed - set slope to 25%
		((Coord3d) node2.getCoord()).setZ(0.25);
		WalkTravelTime walkTravelTime = new WalkTravelTime(scenario.getConfig().plansCalcRoute());
		walkTravelTime.setPerson(person);
		expectedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0);	
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
