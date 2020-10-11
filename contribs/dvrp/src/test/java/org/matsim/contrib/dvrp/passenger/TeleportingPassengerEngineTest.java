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
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule.PassengerEngineType;
import org.matsim.contrib.dvrp.passenger.TeleportingPassengerEngine.TeleportedRouteCalculator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.mockito.ArgumentCaptor;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TeleportingPassengerEngineTest {
	private static final String MODE = "taxi";
	private static final Id<Person> PERSON_ID = Id.createPersonId("person1");

	private final Config config = ConfigUtils.createConfig(new DvrpConfigGroup());

	private final Network network = NetworkUtils.createNetwork(config);
	private final Node nodeA = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), new Coord(0, 0));
	private final Node nodeB = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), new Coord(100, 100));
	private final Link linkAB = NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), nodeA, nodeB, 150, 15, 20,
			1);
	private final Link linkBA = NetworkUtils.createAndAddLink(network, Id.createLinkId("BA"), nodeB, nodeA, 150, 15, 20,
			1);

	private final Scenario scenario = new ScenarioBuilder(config).setNetwork(network).build();
	private final EventsManager eventsManager = mock(EventsManager.class);

	private final PassengerRequestCreator requestCreator = (id, passengerId, route, fromLink, toLink, departureTime, submissionTime) -> new OneTaxiRequest(
			id, passengerId, MODE, fromLink, toLink, departureTime, submissionTime);

	@Test
	public void test_teleported() {
		scenario.getPopulation().addPerson(personWithLeg(linkAB, linkBA, 0));

		TeleportedRouteCalculator teleportedRouteCalculator = request -> {
			Route route = new GenericRouteImpl(request.getFromLink().getId(), request.getToLink().getId());
			route.setTravelTime(999);
			route.setDistance(555);
			return route;
		};
		PassengerRequestValidator requestValidator = request -> Set.of();//valid
		createQSim(teleportedRouteCalculator, requestValidator).run();

		var requestId = Id.create("taxi_0", Request.class);
		assertEvents(new ActivityEndEvent(0, PERSON_ID, linkAB.getId(), null, "start"),
				new PersonDepartureEvent(0, PERSON_ID, linkAB.getId(), MODE),
				new PassengerRequestScheduledEvent(0, MODE, requestId, PERSON_ID, null, 0, 999),
				new PassengerPickedUpEvent(0, MODE, requestId, PERSON_ID, null),
				new PassengerDroppedOffEvent(999, MODE, requestId, PERSON_ID, null),
				new TeleportationArrivalEvent(999, PERSON_ID, 555, MODE),
				new PersonArrivalEvent(999, PERSON_ID, linkBA.getId(), MODE),
				new ActivityStartEvent(999, PERSON_ID, linkBA.getId(), null, "end"));
	}

	@Test
	public void test_rejected() {
		scenario.getPopulation().addPerson(personWithLeg(linkAB, linkBA, 0));

		TeleportedRouteCalculator teleportedRouteCalculator = request -> null; // unused
		PassengerRequestValidator requestValidator = request -> Set.of("invalid");
		createQSim(teleportedRouteCalculator, requestValidator).run();

		var requestId = Id.create("taxi_0", Request.class);
		assertEvents(new ActivityEndEvent(0, PERSON_ID, linkAB.getId(), null, "start"),
				new PersonDepartureEvent(0, PERSON_ID, linkAB.getId(), MODE),
				new PassengerRequestRejectedEvent(0, MODE, requestId, PERSON_ID, "invalid"),
				new PersonStuckEvent(0, PERSON_ID, linkAB.getId(), MODE));
	}

	private QSim createQSim(TeleportedRouteCalculator teleportedRouteCalculator,
			PassengerRequestValidator requestValidator) {
		return new QSimBuilder(config).useDefaults()
				.addQSimModule(new PassengerEngineQSimModule(MODE, PassengerEngineType.TELEPORTING))
				.addQSimModule(new AbstractDvrpModeQSimModule(MODE) {
					@Override
					protected void configureQSim() {
						bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
						bindModal(PassengerRequestCreator.class).toInstance(requestCreator);
						bindModal(TeleportedRouteCalculator.class).toInstance(teleportedRouteCalculator);
						bindModal(PassengerRequestValidator.class).toInstance(requestValidator);
						bindModal(Network.class).toInstance(network);
					}
				})
				.configureQSimComponents(components -> components.addComponent(DvrpModes.mode(MODE)))
				.build(scenario, eventsManager);
	}

	private Person personWithLeg(Link fromLink, Link toLink, double departureTime) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Plan plan = factory.createPlan();

		Activity startActivity = factory.createActivityFromLinkId("start", fromLink.getId());
		startActivity.setEndTime(departureTime);
		plan.addActivity(startActivity);

		Route route = new GenericRouteImpl(fromLink.getId(), toLink.getId());
		route.setDistance(toLink.getLength());
		route.setTravelTime(toLink.getLength() / toLink.getFreespeed());
		Leg leg = factory.createLeg(MODE);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(factory.createActivityFromLinkId("end", toLink.getId()));

		Person person = factory.createPerson(PERSON_ID);
		person.addPlan(plan);
		return person;
	}

	private void assertEvents(Event... events) {
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventsManager, times(events.length)).processEvent(captor.capture());
		assertThat(captor.getAllValues()).usingFieldByFieldElementComparator().containsExactly(events);
	}
}
