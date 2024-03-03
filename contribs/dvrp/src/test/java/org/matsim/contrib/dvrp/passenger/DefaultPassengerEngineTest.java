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

import static org.matsim.contrib.dvrp.passenger.PassengerEngineTestFixture.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiActionCreator;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiOptimizer;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule.PassengerEngineType;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MobsimScopeEventHandlingModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DefaultPassengerEngineTest {
	private final PassengerEngineTestFixture fixture = new PassengerEngineTestFixture();

	private final Id<DvrpVehicle> VEHICLE_ID = Id.create("taxi1", DvrpVehicle.class);
	private final DvrpVehicle oneTaxi = new DvrpVehicleImpl(ImmutableDvrpVehicleSpecification.newBuilder()
			.id(VEHICLE_ID)
			.serviceBeginTime(0)
			.serviceEndTime(3600)
			.startLinkId(fixture.linkAB.getId())
			.capacity(1)
			.build(), fixture.linkAB);
	private final Fleet fleet = () -> ImmutableMap.of(oneTaxi.getId(), oneTaxi);

	@Test
	void test_valid_served() {
		double departureTime = 0;
		fixture.addPersonWithLeg(fixture.linkAB, fixture.linkBA, departureTime, fixture.PERSON_ID);

		PassengerRequestValidator requestValidator = request -> Set.of();//valid
		createQSim(requestValidator, OneTaxiOptimizer.class).run();

		double pickupStartTime = 1;
		double pickupEndTime = pickupStartTime + OneTaxiOptimizer.PICKUP_DURATION;
		double taxiDepartureTime = pickupEndTime + 1;
		double taxiEntersLinkBATime = taxiDepartureTime + 1;
		double taxiArrivalTime = taxiEntersLinkBATime + (fixture.linkBA.getLength() / fixture.linkBA.getFreespeed());
		double dropoffEndTime = taxiArrivalTime + OneTaxiOptimizer.DROPOFF_DURATION;

		//1 second delay between pickupEndTime and taxiDepartureTime is not considered in schedules
		double scheduledDropoffTime = dropoffEndTime - pickupStartTime - 1;

		var requestId = Id.create("taxi_0", Request.class);
		fixture.assertPassengerEvents(
				Collections.singleton(fixture.PERSON_ID),
				new ActivityEndEvent(departureTime, fixture.PERSON_ID, fixture.linkAB.getId(), null, START_ACTIVITY),
				new PersonDepartureEvent(departureTime, fixture.PERSON_ID, fixture.linkAB.getId(), MODE, MODE),
				new PassengerWaitingEvent(departureTime, MODE, requestId, List.of(fixture.PERSON_ID)),
				new PassengerRequestScheduledEvent(departureTime, MODE, requestId, List.of(fixture.PERSON_ID), VEHICLE_ID, 0,
						scheduledDropoffTime),
				new PersonEntersVehicleEvent(pickupStartTime, fixture.PERSON_ID, Id.createVehicleId(VEHICLE_ID)),
				new PassengerPickedUpEvent(pickupStartTime, MODE, requestId, fixture.PERSON_ID, VEHICLE_ID),
				new PassengerDroppedOffEvent(dropoffEndTime, MODE, requestId, fixture.PERSON_ID, VEHICLE_ID),
				new PersonLeavesVehicleEvent(dropoffEndTime, fixture.PERSON_ID, Id.createVehicleId(VEHICLE_ID)),
				new PersonArrivalEvent(dropoffEndTime, fixture.PERSON_ID, fixture.linkBA.getId(), MODE),
				new ActivityStartEvent(dropoffEndTime, fixture.PERSON_ID, fixture.linkBA.getId(), null, END_ACTIVITY));
	}

	@Test
	void test_invalid_rejected() {
		double departureTime = 0;
		fixture.addPersonWithLeg(fixture.linkAB, fixture.linkBA, departureTime, fixture.PERSON_ID);

		PassengerRequestValidator requestValidator = request -> Set.of("invalid");
		createQSim(requestValidator, OneTaxiOptimizer.class).run();

		var requestId = Id.create("taxi_0", Request.class);
		fixture.assertPassengerEvents(
				Collections.singleton(fixture.PERSON_ID),
				new ActivityEndEvent(0, fixture.PERSON_ID, fixture.linkAB.getId(), null, START_ACTIVITY),
				new PersonDepartureEvent(0, fixture.PERSON_ID, fixture.linkAB.getId(), MODE, MODE),
				new PassengerWaitingEvent(departureTime, MODE, requestId, List.of(fixture.PERSON_ID)),
				new PassengerRequestRejectedEvent(0, MODE, requestId, List.of(fixture.PERSON_ID), "invalid"),
				new PersonStuckEvent(1, fixture.PERSON_ID, fixture.linkAB.getId(), MODE));
	}

	@Test
	void test_valid_rejected() {
		double departureTime = 0;
		fixture.addPersonWithLeg(fixture.linkAB, fixture.linkBA, departureTime, fixture.PERSON_ID);

		PassengerRequestValidator requestValidator = request -> Set.of();
		createQSim(requestValidator, RejectingOneTaxiOptimizer.class).run();

		var requestId = Id.create("taxi_0", Request.class);
		fixture.assertPassengerEvents(
				Collections.singleton(fixture.PERSON_ID),
				new ActivityEndEvent(0, fixture.PERSON_ID, fixture.linkAB.getId(), null, START_ACTIVITY),
				new PersonDepartureEvent(0, fixture.PERSON_ID, fixture.linkAB.getId(), MODE, MODE),
				new PassengerWaitingEvent(departureTime, MODE, requestId, List.of(fixture.PERSON_ID)),
				new PassengerRequestRejectedEvent(0, MODE, requestId, List.of(fixture.PERSON_ID), "rejecting_all_requests"),
				new PersonStuckEvent(1, fixture.PERSON_ID, fixture.linkAB.getId(), MODE));
	}

	private static class RejectingOneTaxiOptimizer implements VrpOptimizer {
		@Inject
		private EventsManager eventsManager;

		@Inject
		private MobsimTimer timer;

		@Override
		public void requestSubmitted(Request request) {
			PassengerRequest passengerRequest = (PassengerRequest)request;
			eventsManager.processEvent(new PassengerRequestRejectedEvent(timer.getTimeOfDay(), MODE, request.getId(),
					passengerRequest.getPassengerIds(), "rejecting_all_requests"));
		}

		@Override
		public void nextTask(DvrpVehicle vehicle) {
			throw new UnsupportedOperationException();//no task is planned
		}
	}

	private QSim createQSim(PassengerRequestValidator requestValidator, Class<? extends VrpOptimizer> optimizerClass) {
		return new QSimBuilder(fixture.config).useDefaults()
				.addOverridingModule(new MobsimScopeEventHandlingModule())
				.addQSimModule(new PassengerEngineQSimModule(MODE, PassengerEngineType.DEFAULT))
				.addQSimModule(new VrpAgentSourceQSimModule(MODE))
				.addQSimModule(new AbstractDvrpModeQSimModule(MODE) {
					@Override
					protected void configureQSim() {
						bindModal(Network.class).toInstance(fixture.network);
						bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();

						//requests
						bindModal(PassengerRequestCreator.class).to(OneTaxiRequest.OneTaxiRequestCreator.class)
								.asEagerSingleton();
						bindModal(PassengerRequestValidator.class).toInstance(requestValidator);
						bindModal(AdvanceRequestProvider.class).toInstance(AdvanceRequestProvider.NONE);

						//supply
						addQSimComponentBinding(DynActivityEngine.COMPONENT_NAME).to(DynActivityEngine.class);
						bindModal(Fleet.class).toInstance(fleet);
						bindModal(VehicleType.class).toInstance(VehicleUtils.getDefaultVehicleType());
						bindModal(VrpOptimizer.class).to(optimizerClass).asEagerSingleton();
						bindModal(VrpAgentLogic.DynActionCreator.class).to(OneTaxiActionCreator.class)
								.asEagerSingleton();
					}
				})
				.configureQSimComponents(components -> {
					components.addComponent(DvrpModes.mode(MODE));
					components.addNamedComponent(DynActivityEngine.COMPONENT_NAME);
				})
				.build(fixture.scenario, fixture.eventsManager);
	}
}
