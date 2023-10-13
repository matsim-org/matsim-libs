/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.passenger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiRequest.OneTaxiRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class PassengerEngineTestFixture {
	static final String MODE = TransportMode.taxi;

	final Id<Person> PERSON_ID = Id.createPersonId("person1");
	static final String START_ACTIVITY = "start";
	static final String END_ACTIVITY = "end";

	final Config config = ConfigUtils.createConfig(new DvrpConfigGroup());

	final Network network = NetworkUtils.createNetwork(config);
	final Node nodeA = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), new Coord(0, 0));
	final Node nodeB = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), new Coord(100, 100));
	final Link linkAB = NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), nodeA, nodeB, 150, 15, 20, 1);
	final Link linkBA = NetworkUtils.createAndAddLink(network, Id.createLinkId("BA"), nodeB, nodeA, 150, 15, 20, 1);

	final Scenario scenario = new ScenarioUtils.ScenarioBuilder(config).setNetwork(network).build();
	final EventsManager eventsManager = new EventsManagerImpl();
	final List<Event> recordedEvents = new ArrayList<>();

	final PassengerRequestCreator requestCreator = new OneTaxiRequestCreator();

	public PassengerEngineTestFixture() {
		eventsManager.addHandler((BasicEventHandler)recordedEvents::add);
		eventsManager.initProcessing();
	}

	void addPersonWithLeg(Link fromLink, Link toLink, double departureTime, Id<Person> person_id) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Plan plan = factory.createPlan();

		Activity startActivity = factory.createActivityFromLinkId(START_ACTIVITY, fromLink.getId());
		startActivity.setEndTime(departureTime);
		plan.addActivity(startActivity);

		Route route = new GenericRouteImpl(fromLink.getId(), toLink.getId());
		route.setDistance(toLink.getLength());
		route.setTravelTime(toLink.getLength() / toLink.getFreespeed());
		Leg leg = factory.createLeg(MODE);
		leg.setRoute(route);
		TripStructureUtils.setRoutingMode(leg, MODE);
		plan.addLeg(leg);

		plan.addActivity(factory.createActivityFromLinkId(END_ACTIVITY, toLink.getId()));

		Person person = factory.createPerson(person_id);
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	void assertPassengerEvents(Collection<Id<Person>> personIds, Event... events) {
		assertThat(recordedEvents.size()).isGreaterThanOrEqualTo(events.length);
		var recordedPassengerEvents = recordedEvents.stream()
				.filter(e ->
						e instanceof HasPersonId && personIds.contains(((HasPersonId)e).getPersonId()) ||
								e instanceof AbstractPassengerRequestEvent
				);
		assertThat(recordedPassengerEvents).usingRecursiveFieldByFieldElementComparator().containsExactly(events);
	}
}
