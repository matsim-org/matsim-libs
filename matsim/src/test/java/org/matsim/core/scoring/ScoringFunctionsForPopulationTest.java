
/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunctionsForPopulationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.scoring;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / Simunto GmbH
 */
public class ScoringFunctionsForPopulationTest {

	@Test
	public void testTripScoring() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Id<Person> personId = Id.create(1, Person.class);
		Person p = pf.createPerson(personId);
		population.addPerson(p);

		ControlerListenerManagerImpl controlerListenerManager = new ControlerListenerManagerImpl();
		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventsToActivities eventsToActivities = new EventsToActivities();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		ScoringFunctionFactory scoringFunctionFactory = agentId -> new RecordingScoringFunction();

		ScoringFunctionsForPopulation sf = new ScoringFunctionsForPopulation(controlerListenerManager, eventsManager, eventsToActivities, eventsToLegs, population, scoringFunctionFactory);
		controlerListenerManager.fireControlerIterationStartsEvent(0);
		ScoringFunction s = sf.getScoringFunctionForAgent(personId);
		Assert.assertEquals(RecordingScoringFunction.class, s.getClass());
		RecordingScoringFunction rs = (RecordingScoringFunction) s;

		sf.handleActivity(new PersonExperiencedActivity(personId, pf.createActivityFromCoord("home", new Coord(100, 100))));
		Assert.assertEquals(0, rs.tripCounter);
		sf.handleLeg(new PersonExperiencedLeg(personId, pf.createLeg("walk")));
		Assert.assertEquals(0, rs.tripCounter);
		sf.handleActivity(new PersonExperiencedActivity(personId, pf.createActivityFromCoord("work", new Coord(1000, 100))));
		Assert.assertEquals(1, rs.tripCounter);
		Assert.assertEquals(1, rs.lastTrip.getTripElements().size());
		Assert.assertEquals("walk", ((Leg) rs.lastTrip.getTripElements().get(0)).getMode());

		sf.handleLeg(new PersonExperiencedLeg(personId, pf.createLeg("transit_walk")));
		sf.handleActivity(new PersonExperiencedActivity(personId, pf.createActivityFromCoord("pt_interaction", new Coord(1000, 200))));
		Assert.assertEquals(1, rs.tripCounter);
		sf.handleLeg(new PersonExperiencedLeg(personId, pf.createLeg("pt")));
		sf.handleActivity(new PersonExperiencedActivity(personId, pf.createActivityFromCoord("pt_interaction", new Coord(1000, 200))));
		Assert.assertEquals(1, rs.tripCounter);
		sf.handleLeg(new PersonExperiencedLeg(personId, pf.createLeg("transit_walk")));
		sf.handleActivity(new PersonExperiencedActivity(personId, pf.createActivityFromCoord("leisure", new Coord(1000, 200))));
		Assert.assertEquals(2, rs.tripCounter);
		Assert.assertEquals(5, rs.lastTrip.getTripElements().size());
		Assert.assertEquals("transit_walk", ((Leg) rs.lastTrip.getTripElements().get(0)).getMode());
		Assert.assertEquals("pt", ((Leg) rs.lastTrip.getTripElements().get(2)).getMode());
		Assert.assertEquals("transit_walk", ((Leg) rs.lastTrip.getTripElements().get(4)).getMode());
	}

	private static class RecordingScoringFunction implements ScoringFunction {

		int tripCounter = 0;
		TripStructureUtils.Trip lastTrip = null;

		@Override
		public void handleActivity(Activity activity) {
		}

		@Override
		public void handleLeg(Leg leg) {
		}

		@Override
		public void handleTrip(TripStructureUtils.Trip trip) {
			this.tripCounter++;
			this.lastTrip = trip;
		}

		@Override
		public void agentStuck(double time) {
		}

		@Override
		public void addMoney(double amount) {
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return 0;
		}

		@Override
		public void handleEvent(Event event) {
		}
	}

}