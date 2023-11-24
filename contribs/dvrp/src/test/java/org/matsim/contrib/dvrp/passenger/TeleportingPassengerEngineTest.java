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

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule.PassengerEngineType;
import org.matsim.contrib.dvrp.passenger.TeleportingPassengerEngine.TeleportedRouteCalculator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.GenericRouteImpl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.matsim.contrib.dvrp.passenger.PassengerEngineTestFixture.*;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TeleportingPassengerEngineTest {
	private final PassengerEngineTestFixture fixture = new PassengerEngineTestFixture();

	@Test
	public void test_valid_teleported() {
		double departureTime = 0;
		fixture.addPersonWithLeg(fixture.linkAB, fixture.linkBA, departureTime, fixture.PERSON_ID);

		double travelTime = 999;
		double travelDistance = 555;
		TeleportedRouteCalculator teleportedRouteCalculator = request -> {
			Route route = new GenericRouteImpl(request.getFromLink().getId(), request.getToLink().getId());
			route.setTravelTime(travelTime);
			route.setDistance(travelDistance);
			return route;
		};
		PassengerRequestValidator requestValidator = request -> Set.of();//valid
		createQSim(teleportedRouteCalculator, requestValidator).run();

		double arrivalTime = departureTime + travelTime;
		var requestId = Id.create("taxi_0", Request.class);
		fixture.assertPassengerEvents(
				Collections.singleton(fixture.PERSON_ID),
				new ActivityEndEvent(departureTime, fixture.PERSON_ID, fixture.linkAB.getId(), null, START_ACTIVITY),
				new PersonDepartureEvent(departureTime, fixture.PERSON_ID, fixture.linkAB.getId(), MODE, MODE),
				new PassengerWaitingEvent(departureTime, MODE, requestId, List.of(fixture.PERSON_ID)),
				new PassengerRequestScheduledEvent(departureTime, MODE, requestId, List.of(fixture.PERSON_ID), null, departureTime,
						arrivalTime), new PassengerPickedUpEvent(departureTime, MODE, requestId, fixture.PERSON_ID, null),
				new PassengerDroppedOffEvent(arrivalTime, MODE, requestId, fixture.PERSON_ID, null),
				new TeleportationArrivalEvent(arrivalTime, fixture.PERSON_ID, travelDistance, MODE),
				new PersonArrivalEvent(arrivalTime, fixture.PERSON_ID, fixture.linkBA.getId(), MODE),
				new ActivityStartEvent(arrivalTime, fixture.PERSON_ID, fixture.linkBA.getId(), null, END_ACTIVITY));
	}

	@Test
	public void test_invalid_rejected() {
		double departureTime = 0;
		fixture.addPersonWithLeg(fixture.linkAB, fixture.linkBA, departureTime, fixture.PERSON_ID);

		TeleportedRouteCalculator teleportedRouteCalculator = request -> null; // unused
		PassengerRequestValidator requestValidator = request -> Set.of("invalid");
		createQSim(teleportedRouteCalculator, requestValidator).run();

		var requestId = Id.create("taxi_0", Request.class);
		fixture.assertPassengerEvents(
				Collections.singleton(fixture.PERSON_ID),
				new ActivityEndEvent(departureTime, fixture.PERSON_ID, fixture.linkAB.getId(), null, START_ACTIVITY),
				new PersonDepartureEvent(departureTime, fixture.PERSON_ID, fixture.linkAB.getId(), MODE, MODE),
				new PassengerWaitingEvent(departureTime, MODE, requestId, List.of(fixture.PERSON_ID)),
				new PassengerRequestRejectedEvent(departureTime, MODE, requestId, List.of(fixture.PERSON_ID), "invalid"),
				new PersonStuckEvent(departureTime, fixture.PERSON_ID, fixture.linkAB.getId(), MODE));
	}

	private QSim createQSim(TeleportedRouteCalculator teleportedRouteCalculator,
			PassengerRequestValidator requestValidator) {
		return new QSimBuilder(fixture.config).useDefaults()
				.addQSimModule(new PassengerEngineQSimModule(MODE, PassengerEngineType.TELEPORTING))
				.addQSimModule(new AbstractDvrpModeQSimModule(MODE) {
					@Override
					protected void configureQSim() {
						//general
						bindModal(Network.class).toInstance(fixture.network);
						bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();

						//requests
						bindModal(PassengerRequestCreator.class).toInstance(fixture.requestCreator);
						bindModal(PassengerRequestValidator.class).toInstance(requestValidator);

						//supply
						bindModal(TeleportedRouteCalculator.class).toInstance(teleportedRouteCalculator);
					}
				})
				.configureQSimComponents(components -> components.addComponent(DvrpModes.mode(MODE)))
				.build(fixture.scenario, fixture.eventsManager);
	}
}
