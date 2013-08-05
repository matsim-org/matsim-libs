/* *********************************************************************** *
 * project: org.matsim.*
 * ModeAvailabilityChecker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class WalkSpeedComparatorTest {

	private static final Logger log = Logger.getLogger(WalkSpeedComparatorTest.class);
	
	@Test
	public void testComparatorOrder() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		scenario.getPopulation().addPerson(createPerson(factory, scenario.createId("p1"), 18, "m"));
		scenario.getPopulation().addPerson(createPerson(factory, scenario.createId("p2"), 4, "f"));
		scenario.getPopulation().addPerson(createPerson(factory, scenario.createId("p3"), 45, "m"));
		scenario.getPopulation().addPerson(createPerson(factory, scenario.createId("p4"), 70, "f"));
		scenario.getPopulation().addPerson(createPerson(factory, scenario.createId("p5"), 95, "f"));
		
		WalkSpeedComparator comparator = new WalkSpeedComparator();
		comparator.calcTravelTimes(scenario.getPopulation());
		
		Queue<Id> queue = new PriorityQueue<Id>(5, comparator);
		queue.add(scenario.createId("p1"));
		queue.add(scenario.createId("p2"));
		queue.add(scenario.createId("p3"));
		queue.add(scenario.createId("p4"));
		queue.add(scenario.createId("p5"));
				
		log.info(comparator.getTravelTimesMap().get(scenario.createId("p5")));
		log.info(comparator.getTravelTimesMap().get(scenario.createId("p2")));
		log.info(comparator.getTravelTimesMap().get(scenario.createId("p4")));
		log.info(comparator.getTravelTimesMap().get(scenario.createId("p3")));
		log.info(comparator.getTravelTimesMap().get(scenario.createId("p1")));
		
		Assert.assertEquals(scenario.createId("p5"), queue.poll());
		Assert.assertEquals(scenario.createId("p2"), queue.poll());
		Assert.assertEquals(scenario.createId("p4"), queue.poll());
		Assert.assertEquals(scenario.createId("p3"), queue.poll());		
		Assert.assertEquals(scenario.createId("p1"), queue.poll());
	}
	
	private Person createPerson(PopulationFactory factory, Id id, int age, String sex) {
		
		PersonImpl person = (PersonImpl) factory.createPerson(id);
		person.setAge(age);
		person.setSex(sex);
		
		return person;
	}
}
