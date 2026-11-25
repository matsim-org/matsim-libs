/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.prebooking;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;
import org.matsim.contrib.drt.passenger.DrtStopCandidatesFactory;
import org.matsim.contrib.drt.passenger.StopCandidatesTripMaterializer;
import org.matsim.contrib.drt.prebooking.PrebookingManager.PersonLeg;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.common.base.Verify;
import com.google.common.collect.Iterables;

/**
 * Requests with stop candidates whose stops are not chosen yet, i.e. that are submitted but not scheduled. Used by
 * {@link PrebookingManager}:
 * <ul>
 * <li>{@link #stopCandidates}: the candidates of a new request, if its passengers can be moved to other stops;</li>
 * <li>{@link #add}: from submission on, passengers of a request with alternative pickup stops are held in the
 * activity before the trip, so that their plans can still be rewritten to the chosen pickup stop;</li>
 * <li>{@link #applyChosenStops}: once scheduled, the trips are rewritten to the chosen stops and the passengers are
 * released;</li>
 * <li>{@link #afterSimStep}: keeps holding, and releases the passengers of requests rejected or canceled meanwhile.</li>
 * </ul>
 * Holding works by moving the activity end two time steps ahead after each step, so it is only needed (and only
 * happens) for requests submitted shortly before the passengers would leave. Nothing here depends on prebooking as
 * such; with an earlier submission, holding is simply never needed.
 *
 * @author nkuehnel / MOIA
 */
final class PendingStopChoices {

	/**
	 * A passenger of a request with stop candidates: the activity before the trip (looked up once) and, while held in
	 * it, the originally planned end, its maximum duration (cleared while held) and the end set for holding.
	 */
	private static final class Passenger {
		private final PersonLeg personLeg;
		private final Activity origin;
		private double originalEndTime = Double.NaN;
		private OptionalTime originalMaximumDuration = OptionalTime.undefined();
		private double heldUntil = Double.NaN;

		private Passenger(PersonLeg personLeg) {
			this.personLeg = personLeg;
			this.origin = StopCandidatesTripMaterializer.originActivity(plan(personLeg), personLeg.leg());
		}

		private boolean isInOrigin() {
			return WithinDayAgentUtils.getCurrentPlanElement(personLeg.agent()) == origin;
		}

		private boolean hasLeftOrigin() {
			int originIndex = Iterables.indexOf(plan(personLeg).getPlanElements(), e -> e == origin);
			return WithinDayAgentUtils.getCurrentPlanElementIndex(personLeg.agent()) > originIndex;
		}

		/**
		 * @return true if the activity still ends at the time set for holding; exact comparison on purpose, any other
		 * end means that someone else (e.g. an abort after a rejection) has changed it meanwhile
		 */
		private boolean isHeld() {
			return !Double.isNaN(heldUntil) && origin.getEndTime().isDefined()
					&& origin.getEndTime().seconds() == heldUntil;
		}
	}

	/**
	 * @param alternativePickups if true, the passengers are held until the request is scheduled
	 */
	private record Pending(List<Passenger> passengers, boolean alternativePickups) {
	}

	private final Network network;
	private final MobsimTimer mobsimTimer;
	private final IdMap<Request, Pending> pending = new IdMap<>(Request.class);
	private InternalInterface internalInterface;

	PendingStopChoices(Network network, MobsimTimer mobsimTimer) {
		this.network = network;
		this.mobsimTimer = mobsimTimer;
	}

	void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	/**
	 * Only passengers with own access/egress walks can be moved to another stop; a group gets candidates only if all its
	 * passengers have them (passengers without own walks, e.g. DRT companions, are not supported yet). Alternative pickup
	 * stops are only offered if no passenger has left for the routed pickup stop yet.
	 *
	 * @return the stop candidates of a new request for the given passengers and their routes, or empty
	 */
	Optional<DrtStopCandidates> stopCandidates(List<PersonLeg> personsLegs, List<Route> routes) {
		if (!DrtStopCandidatesFactory.haveAlternatives(routes) || !personsLegs.stream()
				.allMatch(p -> StopCandidatesTripMaterializer.hasSingleAccessAndEgressLeg(plan(p), p.leg()))) {
			return Optional.empty();
		}
		boolean pickupMayChange = personsLegs.stream().map(Passenger::new).noneMatch(Passenger::hasLeftOrigin);
		return DrtStopCandidatesFactory.fromRoutes(routes, network, pickupMayChange);
	}

	/**
	 * Registers a submitted request with the given stop candidates. A booking at the activity end is submitted before the
	 * activity end of this step is processed, so the passengers are held right away; holding after the step would come
	 * too late.
	 */
	void add(double now, Id<Request> requestId, List<PersonLeg> personsLegs, DrtStopCandidates stopCandidates) {
		Pending request = new Pending(personsLegs.stream().map(Passenger::new).toList(),
				!stopCandidates.alternativeAccess().isEmpty());
		pending.put(requestId, request);
		holdPassengersAboutToLeave(now, request);
	}

	/**
	 * Rewrites the trips of the passengers of a scheduled request to the chosen stops and releases them.
	 */
	void applyChosenStops(double now, PassengerRequestScheduledEvent event) {
		Pending request = pending.remove(event.getRequestId());
		if (request == null) {
			return;
		}
		Route referenceRoute = request.passengers().getFirst().personLeg.leg().getRoute();
		Id<Link> pickupLinkId = event.getPickupLinkId().orElse(referenceRoute.getStartLinkId());
		Id<Link> dropoffLinkId = event.getDropoffLinkId().orElse(referenceRoute.getEndLinkId());

		for (Passenger passenger : request.passengers()) {
			Leg leg = passenger.personLeg.leg();
			// passengers are held until scheduled, so the pickup can only differ if they are still at the origin
			Verify.verify(pickupLinkId.equals(leg.getRoute().getStartLinkId()) || !passenger.hasLeftOrigin(),
					"Passenger %s has left for the routed pickup stop, but %s was scheduled",
					passenger.personLeg.agent().getId(), pickupLinkId);
			if (StopCandidatesTripMaterializer.materialize(plan(passenger.personLeg), leg, pickupLinkId,
					dropoffLinkId)) {
				WithinDayAgentUtils.resetCaches(passenger.personLeg.agent());
			}
		}
		release(now, request);
	}

	/**
	 * Holds passengers of unscheduled requests with alternative pickups in their activity and releases those of
	 * requests that are no longer active (rejected, canceled). Runs after each sim step, i.e. after the activity ends of
	 * the step have been processed.
	 *
	 * @param isActive whether a request is still submitted and not rejected or canceled
	 */
	void afterSimStep(double now, Predicate<Id<Request>> isActive) {
		Iterator<Map.Entry<Id<Request>, Pending>> iterator = pending.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			if (!isActive.test(entry.getKey())) {
				// rejected or canceled before being scheduled: the plan still leads to the routed stop
				iterator.remove();
				release(now, entry.getValue());
				continue;
			}
			holdPassengersAboutToLeave(now, entry.getValue());
		}
	}

	/**
	 * Holds the passengers of a pending request with alternative pickups who would leave their activity in this or the
	 * next time step. The end is set two steps ahead, so that it is extended again before the passenger would leave.
	 */
	private void holdPassengersAboutToLeave(double now, Pending request) {
		if (!request.alternativePickups()) {
			return;
		}
		double timeStep = mobsimTimer.getSimTimestepSize();
		for (Passenger passenger : request.passengers()) {
			MobsimAgent agent = passenger.personLeg.agent();
			if (!passenger.isInOrigin() || agent.getActivityEndTime() > now + timeStep) {
				continue; // not yet in the activity, or not about to leave it
			}
			if (Double.isNaN(passenger.originalEndTime)) {
				passenger.originalEndTime = agent.getActivityEndTime();
				// under minOfDurationAndEndTime, a maximum duration would end the activity regardless of the end time
				passenger.originalMaximumDuration = passenger.origin.getMaximumDuration();
				passenger.origin.setMaximumDurationUndefined();
			}
			passenger.heldUntil = now + 2 * timeStep;
			passenger.origin.setEndTime(passenger.heldUntil);
			WithinDayAgentUtils.resetCaches(agent);
			internalInterface.getMobsim().rescheduleActivityEnd(agent);
		}
	}

	/**
	 * Lets held passengers leave, at the earliest at their originally planned activity end. Passengers whose activity
	 * end was changed meanwhile (e.g. aborted after a rejection) are left alone.
	 */
	private void release(double now, Pending request) {
		double timeStep = mobsimTimer.getSimTimestepSize();
		for (Passenger passenger : request.passengers()) {
			if (!passenger.isInOrigin() || !passenger.isHeld()) {
				continue;
			}
			passenger.origin.setEndTime(Math.max(passenger.originalEndTime, now + timeStep));
			passenger.originalMaximumDuration.ifDefined(passenger.origin::setMaximumDuration);
			MobsimAgent agent = passenger.personLeg.agent();
			WithinDayAgentUtils.resetCaches(agent);
			internalInterface.getMobsim().rescheduleActivityEnd(agent);
		}
	}

	private static Plan plan(PersonLeg personLeg) {
		return WithinDayAgentUtils.getModifiablePlan(personLeg.agent());
	}
}
