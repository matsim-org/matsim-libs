
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

	/**
 * @author mrieser / Simunto GmbH
 */
public class ScoringFunctionsForPopulationTest {

	 @Test
	 void testTripScoring() {
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

		ScoringFunctionsForPopulation sf = new ScoringFunctionsForPopulation(controlerListenerManager, eventsManager, eventsToActivities, eventsToLegs, population, scoringFunctionFactory, scenario.getConfig());
		controlerListenerManager.fireControlerIterationStartsEvent(0, false);
		ScoringFunction s = sf.getScoringFunctionForAgent(personId);
		Assertions.assertEquals(RecordingScoringFunction.class, s.getClass());
		RecordingScoringFunction rs = (RecordingScoringFunction) s;

		sf.handleActivity(new PersonExperiencedActivity(personId, pf.createActivityFromCoord("home", new Coord(100, 100))));
		Assertions.assertEquals(0, rs.tripCounter);
		sf.handleLeg(new PersonExperiencedLeg(personId, pf.createLeg("walk")));
		Assertions.assertEquals(0, rs.tripCounter);
		sf.handleEvent(new ActivityStartEvent(8*3600, personId, null, null, "work", new Coord(1000, 100)));
		Assertions.assertEquals(1, rs.tripCounter);
		sf.handleActivity(new PersonExperiencedActivity(personId, pf.createActivityFromCoord("work", new Coord(1000, 100))));
		Assertions.assertEquals(1, rs.tripCounter);
		Assertions.assertEquals(1, rs.lastTrip.getTripElements().size());
		Assertions.assertEquals("walk", ((Leg) rs.lastTrip.getTripElements().get(0)).getMode());

		sf.handleLeg(new PersonExperiencedLeg(personId, pf.createLeg("transit_walk")));
		sf.handleEvent(new ActivityStartEvent(17*3600 - 10, personId, null, null, "pt interaction", new Coord(1000, 200)));
		Assertions.assertEquals(1, rs.tripCounter);
		sf.handleActivity(new PersonExperiencedActivity(personId, PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord(1000, 200), null, TransportMode.pt)));
		Assertions.assertEquals(1, rs.tripCounter);
		sf.handleLeg(new PersonExperiencedLeg(personId, pf.createLeg("pt")));
		sf.handleEvent(new ActivityStartEvent(17*3600, personId, null, null, "pt interaction", new Coord(1000, 200)));
		Assertions.assertEquals(1, rs.tripCounter);
		sf.handleActivity(new PersonExperiencedActivity(personId, PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord(1000, 200), null, TransportMode.pt)));
		Assertions.assertEquals(1, rs.tripCounter);
		sf.handleLeg(new PersonExperiencedLeg(personId, pf.createLeg("transit_walk")));
		sf.handleEvent(new ActivityStartEvent(17*3600 + 10, personId, null, null, "leisure", new Coord(1000, 200)));
		Assertions.assertEquals(2, rs.tripCounter);
		sf.handleActivity(new PersonExperiencedActivity(personId, pf.createActivityFromCoord("leisure", new Coord(1000, 200))));
		Assertions.assertEquals(2, rs.tripCounter);
		Assertions.assertEquals(5, rs.lastTrip.getTripElements().size());
		Assertions.assertEquals("transit_walk", ((Leg) rs.lastTrip.getTripElements().get(0)).getMode());
		Assertions.assertEquals("pt", ((Leg) rs.lastTrip.getTripElements().get(2)).getMode());
		Assertions.assertEquals("transit_walk", ((Leg) rs.lastTrip.getTripElements().get(4)).getMode());
	}

	 @Test
	 void testPersonScoreEventScoring() {
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

		ScoringFunctionsForPopulation sf = new ScoringFunctionsForPopulation(controlerListenerManager, eventsManager, eventsToActivities, eventsToLegs, population, scoringFunctionFactory, scenario.getConfig());
		controlerListenerManager.fireControlerIterationStartsEvent(0, false);
		ScoringFunction s = sf.getScoringFunctionForAgent(personId);

		eventsManager.initProcessing();
		eventsManager.processEvent(new PersonScoreEvent(7*3600, p.getId(), 1.234, "testing"));
		eventsManager.processEvent(new PersonScoreEvent(8*3600, p.getId(), 2.345, "testing"));
		eventsManager.processEvent(new PersonScoreEvent(9*3600, Id.create("xyz", Person.class), 2.345, "testing"));
		eventsManager.finishProcessing();

		Assertions.assertTrue(s instanceof RecordingScoringFunction);
		RecordingScoringFunction rsf = (RecordingScoringFunction) s;
		Assertions.assertEquals(2, rsf.separateScoreCounter);
		Assertions.assertEquals(1.234+2.345, rsf.separateScoreSum, 1e-7);
	}

	private static class RecordingScoringFunction implements ScoringFunction {

		int tripCounter = 0;
		TripStructureUtils.Trip lastTrip = null;
		int separateScoreCounter = 0;
		double separateScoreSum = 0;

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
		public void addScore(double amount) {
			this.separateScoreCounter++;
			this.separateScoreSum += amount;
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
