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

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.modal.ModalProviders;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisData;

import java.util.*;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TeleportingEstimationPassengerEngine implements PassengerEngine, VisData {
	public static final String ORIGINAL_ROUTE_ATTRIBUTE = "originalRoute";

	private final String mode;
	private final EventsManager eventsManager;
	private final MobsimTimer mobsimTimer;

	private final PassengerRequestCreator requestCreator;
	private final Network network;
	private final PassengerRequestValidator requestValidator;

	private final InternalPassengerHandling internalPassengerHandling;
	private final TeleportationEngine teleportationEngine;

	private final Random random = new Random(1);

	public static final String EST_RIDE_TIME = "est_ride_time";
	public static final String EST_RIDE_DISTANCE = "est_ride_distance";
	public static final String EST_WAIT_TIME = "est_wait_time";
	public static final String EST_REJECTION_RATE = "est_rejection_rate";

	/**
	 * Request currently waiting for pickup.
	 */
	private final Queue<DoubleObjectPair<PassengerRequest>> waitingRequests = new PriorityQueue<>(
		Comparator.comparingDouble(DoubleObjectPair::keyDouble));

	/**
	 * Request currently onboard a vehicle and waiting for drop-off.
	 */
	private final Queue<DoubleObjectPair<PassengerRequest>> ridingRequests = new PriorityQueue<>(
		Comparator.comparingDouble(DoubleObjectPair::keyDouble));

	private InternalInterface internalInterface;

	TeleportingEstimationPassengerEngine(String mode, EventsManager eventsManager, MobsimTimer mobsimTimer,
										 PassengerRequestCreator requestCreator,
										 Network network, PassengerRequestValidator requestValidator, Scenario scenario) {
		this(mode, eventsManager, mobsimTimer, requestCreator, network, requestValidator,
			new DefaultTeleportationEngine(scenario, eventsManager, false));
	}

	TeleportingEstimationPassengerEngine(String mode, EventsManager eventsManager, MobsimTimer mobsimTimer,
										 PassengerRequestCreator requestCreator,
										 Network network, PassengerRequestValidator requestValidator, TeleportationEngine teleportationEngine) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
		this.requestCreator = requestCreator;
		this.network = network;
		this.requestValidator = requestValidator;
		this.teleportationEngine = teleportationEngine;

		internalPassengerHandling = new InternalPassengerHandling(mode, eventsManager);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		internalPassengerHandling.setInternalInterface(internalInterface);
		teleportationEngine.setInternalInterface(internalInterface);
	}

	@Override
	public void onPrepareSim() {
		teleportationEngine.onPrepareSim();
	}

	@Override
	public void doSimStep(double time) {

		// process waiting passengers
		while (!waitingRequests.isEmpty() && waitingRequests.peek().keyDouble() <= time) {
			PassengerRequest request = waitingRequests.poll().value();
			for (Id<Person> passenger : request.getPassengerIds()) {
				eventsManager.processEvent(new PassengerPickedUpEvent(time, mode, request.getId(), passenger, null));
			}
		}

		//first process passenger dropoff events
		while (!ridingRequests.isEmpty() && ridingRequests.peek().keyDouble() <= time) {
			PassengerRequest request = ridingRequests.poll().value();
			for (Id<Person> passenger : request.getPassengerIds()) {
				eventsManager.processEvent(new PassengerDroppedOffEvent(time, mode, request.getId(), passenger, null));
			}
		}

		//then end teleported rides
		teleportationEngine.doSimStep(time);
	}

	@Override
	public void afterSim() {
		teleportationEngine.afterSim();
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId) {
		if (!agent.getMode().equals(mode)) {
			return false;
		}

		MobsimPassengerAgent passenger = (MobsimPassengerAgent) agent;
		Id<Link> toLinkId = passenger.getDestinationLinkId();
		Leg leg = (Leg) ((PlanAgent) passenger).getCurrentPlanElement();
		Route route = leg.getRoute();
		PassengerRequest request = requestCreator.createRequest(internalPassengerHandling.createRequestId(),
			List.of(passenger.getId()), List.of(route), getLink(fromLinkId), getLink(toLinkId), now, now);

		eventsManager.processEvent(new PassengerWaitingEvent(now, mode, request.getId(), request.getPassengerIds()));

		double probabilityOfRejection = getEstimatedRejectionRate(leg);

		if (internalPassengerHandling.validateRequest(request, requestValidator, now) && isRequestAccepted(probabilityOfRejection, request, now)) {
			double waitTime = getEstimatedWaitTime(leg);
			double travelTime = waitTime + getEstimatedRideTime(leg);

			// Set information in the route for the teleportation engine
			leg.setTravelTime(travelTime);
			route.setTravelTime(travelTime);
			route.setDistance(getEstimatedRideDistance(leg));

			eventsManager.processEvent(new PassengerRequestScheduledEvent(mobsimTimer.getTimeOfDay(), mode, request.getId(),
				request.getPassengerIds(), null, now, now + travelTime));

			teleportationEngine.handleDeparture(now, passenger, fromLinkId);

			waitingRequests.add(DoubleObjectPair.of(now + waitTime, request));
			ridingRequests.add(DoubleObjectPair.of(now + travelTime, request));
		} else {
			//not much else can be done for immediate requests
			//set the passenger agent to abort - the event will be thrown by the QSim
			passenger.setStateToAbort(mobsimTimer.getTimeOfDay());
			internalInterface.arrangeNextAgentState(passenger);
		}

		return true;
	}

	private Link getLink(Id<Link> linkId) {
		return Preconditions.checkNotNull(network.getLinks().get(linkId),
			"Link id=%s does not exist in network for mode %s. Agent departs from a link that does not belong to that network?",
			linkId, mode);
	}

	@Override
	public boolean notifyWaitForPassengers(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
										   Id<Request> requestId) {
		throw new UnsupportedOperationException("No notifying when teleporting");
	}

	@Override
	public boolean tryPickUpPassengers(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
									   Id<Request> requestId, double now) {
		throw new UnsupportedOperationException("No picking-up when teleporting");
	}

	@Override
	public void dropOffPassengers(MobsimDriverAgent driver, Id<Request> requestId, double now) {
		throw new UnsupportedOperationException("No dropping-off when teleporting");
	}

	@Override
	public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions) {
		return teleportationEngine.addAgentSnapshotInfo(positions);
	}

	public static Provider<PassengerEngine> createProvider(String mode) {
		return new ModalProviders.AbstractProvider<>(mode, DvrpModes::mode) {
			@Inject
			private EventsManager eventsManager;

			@Inject
			private MobsimTimer mobsimTimer;

			@Inject
			private Scenario scenario;

			@Override
			public TeleportingEstimationPassengerEngine get() {
				return new TeleportingEstimationPassengerEngine(getMode(), eventsManager, mobsimTimer,
					getModalInstance(PassengerRequestCreator.class),
					getModalInstance(Network.class),
					getModalInstance(PassengerRequestValidator.class), scenario);
			}
		};
	}

	static double getEstimatedRideTime(Attributable element) {
		return (double) element.getAttributes().getAttribute(EST_RIDE_TIME);
		// Chengqi: there is a public static final String in the DRT contrib for est_ride_time. But it cannot be accessed here...
	}

	static double getEstimatedRideDistance(Attributable element) {
		return (double) element.getAttributes().getAttribute(EST_RIDE_DISTANCE);
	}

	static double getEstimatedWaitTime(Attributable element) {
		return (double) element.getAttributes().getAttribute(EST_WAIT_TIME);
	}

	static double getEstimatedRejectionRate(Attributable element) {
		return (double) element.getAttributes().getAttribute(EST_REJECTION_RATE);
	}

	/**
	 * Simulate if a valid request will be accepted or not.
	 *
	 * @param probabilityOfRejection this information is read from the attribute est_rejection_rate in the leg, which is written by the
	 *                               DRT estimator.
	 * @param request                Passenger request
	 * @param now                    time of the day
	 * @return true if request is to be accepted, false otherwise.
	 */
	private boolean isRequestAccepted(double probabilityOfRejection, PassengerRequest request, double now) {
		if (random.nextDouble() < probabilityOfRejection) {
			eventsManager.processEvent(
				new PassengerRequestRejectedEvent(now, mode, request.getId(), request.getPassengerIds(), "This passenger is rejected by " +
					"chance, because the probability of rejection is greater than 0"));
			return false;
		}
		return true;
	}

}
