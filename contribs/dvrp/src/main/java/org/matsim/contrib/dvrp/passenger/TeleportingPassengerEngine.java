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

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TeleportingPassengerEngine implements PassengerEngine {
	public static final String ORIGINAL_ROUTE_ATTR = "originalRoute";

	public interface TeleportedRouteCalculator {
		Route calculateRoute(PassengerRequest request);
	}

	private final String mode;
	private final EventsManager eventsManager;
	private final MobsimTimer mobsimTimer;

	private final PassengerRequestCreator requestCreator;
	private final TeleportedRouteCalculator teleportedRouteCalculator;
	private final Network network;
	private final PassengerRequestValidator requestValidator;

	private final PassengerHandler passengerHandler;

	private InternalInterface internalInterface;

	public TeleportingPassengerEngine(String mode, EventsManager eventsManager, MobsimTimer mobsimTimer,
			PassengerRequestCreator requestCreator, TeleportedRouteCalculator teleportedRouteCalculator,
			Network network, PassengerRequestValidator requestValidator,
			PassengerRequestEventForwarder passengerRequestEventForwarder) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
		this.requestCreator = requestCreator;
		this.teleportedRouteCalculator = teleportedRouteCalculator;
		this.network = network;
		this.requestValidator = requestValidator;

		passengerHandler = new PassengerHandler(mode, eventsManager);
		passengerRequestEventForwarder.registerListenerForMode(mode, this);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		passengerHandler.setInternalInterface(internalInterface);
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void doSimStep(double time) {
	}

	@Override
	public void afterSim() {
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId) {
		if (!agent.getMode().equals(mode)) {
			return false;
		}

		MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;
		Id<Link> toLinkId = passenger.getDestinationLinkId();
		Route route = ((Leg)((PlanAgent)passenger).getCurrentPlanElement()).getRoute();

		PassengerRequest request = requestCreator.createRequest(passengerHandler.createRequestId(), passenger.getId(),
				route, getLink(fromLinkId), getLink(toLinkId), now, now);
		if (passengerHandler.validateRequest(request, requestValidator, now)) {
			adaptRouteForTeleportation(passenger, request, now);
			return false;//teleport the passenger (will be handled by the teleportation engine)
		} else {
			//not much else can be done for immediate requests
			//set the passenger agent to abort - the event will be thrown by the QSim
			passenger.setStateToAbort(mobsimTimer.getTimeOfDay());
			internalInterface.arrangeNextAgentState(passenger);
			return true;//stop processing this departure
		}
	}

	private void adaptRouteForTeleportation(MobsimPassengerAgent passenger, PassengerRequest request, double now) {
		Route teleportedRoute = teleportedRouteCalculator.calculateRoute(request);

		Leg leg = (Leg)((PlanAgent)passenger).getCurrentPlanElement();
		Route originalRoute = leg.getRoute();
		Verify.verify(originalRoute.getStartLinkId().equals(teleportedRoute.getStartLinkId()));
		Verify.verify(originalRoute.getEndLinkId().equals(teleportedRoute.getEndLinkId()));
		Verify.verify(teleportedRoute.getTravelTime().isDefined());

		leg.getAttributes().putAttribute(ORIGINAL_ROUTE_ATTR, originalRoute);
		leg.setRoute(teleportedRoute);

		eventsManager.processEvent(new PassengerRequestScheduledEvent(mobsimTimer.getTimeOfDay(), mode, request.getId(),
				request.getPassengerId(), null, now, now + teleportedRoute.getTravelTime().seconds()));
	}

	private Link getLink(Id<Link> linkId) {
		return Preconditions.checkNotNull(network.getLinks().get(linkId),
				"Link id=%s does not exist in network for mode %s. Agent departs from a link that does not belong to that network?",
				linkId, mode);
	}

	@Override
	public boolean tryPickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
			PassengerRequest request, double now) {
		throw new UnsupportedOperationException("No picking-up when teleporting");
	}

	@Override
	public void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now) {
		throw new UnsupportedOperationException("No dropping-off when teleporting");
	}

	@Override
	public void notifyPassengerRequestRejected(PassengerRequestRejectedEvent event) {
	}

	@Override
	public void notifyPassengerRequestScheduled(PassengerRequestScheduledEvent event) {
	}

	public static Provider<PassengerEngine> createProvider(String mode) {
		return new ModalProviders.AbstractProvider<>(mode) {
			@Inject
			private EventsManager eventsManager;

			@Inject
			private MobsimTimer mobsimTimer;

			@Inject
			private PassengerRequestEventForwarder passengerRequestEventForwarder;

			@Override
			public TeleportingPassengerEngine get() {
				return new TeleportingPassengerEngine(getMode(), eventsManager, mobsimTimer,
						getModalInstance(PassengerRequestCreator.class),
						getModalInstance(TeleportedRouteCalculator.class), getModalInstance(Network.class),
						getModalInstance(PassengerRequestValidator.class), passengerRequestEventForwarder);
			}
		};
	}
}
