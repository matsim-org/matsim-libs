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
		
		scenario.getPopulation().addPerson(createPerson(factory, Id.create("p1", Person.class), 18, "m"));
		scenario.getPopulation().addPerson(createPerson(factory, Id.create("p2", Person.class), 4, "f"));
		scenario.getPopulation().addPerson(createPerson(factory, Id.create("p3", Person.class), 45, "m"));
		scenario.getPopulation().addPerson(createPerson(factory, Id.create("p4", Person.class), 70, "f"));
		scenario.getPopulation().addPerson(createPerson(factory, Id.create("p5", Person.class), 95, "f"));
		
		WalkSpeedComparator comparator = new WalkSpeedComparator();
		comparator.calcTravelTimes(scenario.getPopulation());
		
		Queue<Id<Person>> queue = new PriorityQueue<Id<Person>>(5, comparator);
		queue.add(Id.create("p1", Person.class));
		queue.add(Id.create("p2", Person.class));
		queue.add(Id.create("p3", Person.class));
		queue.add(Id.create("p4", Person.class));
		queue.add(Id.create("p5", Person.class));
				
		log.info(comparator.getTravelTimesMap().get(Id.create("p5", Person.class)));
		log.info(comparator.getTravelTimesMap().get(Id.create("p2", Person.class)));
		log.info(comparator.getTravelTimesMap().get(Id.create("p4", Person.class)));
		log.info(comparator.getTravelTimesMap().get(Id.create("p3", Person.class)));
		log.info(comparator.getTravelTimesMap().get(Id.create("p1", Person.class)));
		
		Assert.assertEquals(Id.create("p5", Person.class), queue.poll());
		Assert.assertEquals(Id.create("p2", Person.class), queue.poll());
		Assert.assertEquals(Id.create("p4", Person.class), queue.poll());
		Assert.assertEquals(Id.create("p3", Person.class), queue.poll());		
		Assert.assertEquals(Id.create("p1", Person.class), queue.poll());
	}
	
	private Person createPerson(PopulationFactory factory, Id<Person> id, int age, String sex) {
		
		Person person = factory.createPerson(id);
		PersonImpl.setAge(person, age);
		PersonImpl.setSex(person, sex);
		
		return person;
	}
}
