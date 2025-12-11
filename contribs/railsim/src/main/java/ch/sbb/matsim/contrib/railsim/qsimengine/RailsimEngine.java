/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.DispositionResponse;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleType;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.events.RailsimDetourEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainLeavesLinkEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.TrainDisposition;

/**
 * Engine to simulate train movement.
 */
final class RailsimEngine implements Steppable {

	/**
	 * Additional safety distance in meter that is added to the reservation distance.
	 * Ensure that trains always have enough distance to progress.
	 */
	private static final double SAFETY_DIST = 10;
	private static final Logger log = LogManager.getLogger(RailsimEngine.class);
	private final EventsManager eventsManager;
	private final RailsimConfigGroup config;
	private final List<TrainState> activeTrains = new ArrayList<>();
	private final Queue<UpdateEvent> updateQueue = new PriorityQueue<>();
	private final RailResourceManager resources;
	private final TrainDisposition disposition;

	RailsimEngine(EventsManager eventsManager, RailsimConfigGroup config, RailResourceManager resources, TrainDisposition disposition) {
		this.eventsManager = eventsManager;
		this.config = config;
		this.resources = resources;
		this.disposition = disposition;
	}

	@Override
	public void doSimStep(double time) {

		UpdateEvent update = updateQueue.peek();

		// Update loop over all required state updates
		while (update != null && update.plannedTime <= time) {
			updateQueue.poll();

			// Use planned time here, otherwise there will be inaccuracies
			updateState(update.plannedTime, update);

			// Add the update event again
			if (update.type != UpdateEvent.Type.IDLE) {
				updateQueue.add(update);
			}

			update = updateQueue.peek();
		}

		if (time % config.updateInterval == 0.) {
			updateAllPositions(time);
		}
	}

	/**
	 * Update the current state of all trains, even if no update would be needed.
	 */
	public void updateAllStates(double time) {

		// Process all waiting events first
		doSimStep(time);

		// Potential duplication of position update events here, if time matches updateInterval
		updateAllPositions(time);
	}

	/**
	 * Handle the departure of a train.
	 */
	public boolean handleDeparture(double now, MobsimDriverAgent agent, Id<Link> linkId, NetworkRoute route) {

		log.debug("Train {} is departing at {}", agent.getVehicle(), now);

		// Queue the update event
		// NO events can be generated here, or temporal ordering is not guaranteed
		// (departures are handled before event queue is processed)

		List<RailLink> list = route.getLinkIds().stream().map(resources::getLink).collect(Collectors.toList());
		list.add(0, resources.getLink(linkId));
		list.add(resources.getLink(route.getEndLinkId()));

		VehicleType type = agent.getVehicle().getVehicle().getType();
		TrainState state = new TrainState(agent, new TrainInfo(type, config), now, linkId, list);

		state.train.checkConsistency();

		activeTrains.add(state);

		disposition.onDeparture(now, state.driver, state.route);

		updateQueue.add(new UpdateEvent(state, UpdateEvent.Type.DEPARTURE));

		return true;
	}

	private void updateAllPositions(double time) {
		for (TrainState train : activeTrains) {
			if (train.timestamp < time)
				updateState(time, new UpdateEvent(train, UpdateEvent.Type.POSITION));
		}
	}

	private void createEvent(Event event) {
		// Because of the 1s update interval, events need to be rounded to the current simulation step
		event.setTime(Math.ceil(event.getTime()));
		this.eventsManager.processEvent(event);
	}

	private void updateState(double time, UpdateEvent event) {

		// Do different updates depending on the type
		switch (event.type) {
			case DEPARTURE -> updateDeparture(time, event);
			case POSITION -> {
				updatePosition(time, event);
				decideNextUpdate(event);
			}
			case SPEED_CHANGE -> updateSpeed(time, event);
			case ENTER_LINK -> enterLink(time, event);
			case LEAVE_LINK -> leaveLink(time, event);
			case BLOCK_TRACK -> blockTrack(time, event);
			case WAIT_FOR_RESERVATION -> checkTrackReservation(time, event);
			case UNBLOCK_LINK -> {
				unblockTrack(time, event.state, event.unblockLink);
				// event will be removed
				event.type = UpdateEvent.Type.IDLE;
			}
			default -> throw new IllegalStateException("Unhandled update type " + event.type);
		}
	}

	private void updateSpeed(double time, UpdateEvent event) {

		TrainState state = event.state;

		updatePosition(time, event);

		decideTargetSpeed(event, state);

		createEvent(state.asEvent(time));

		decideNextUpdate(event);
	}

	private void blockTrack(double time, UpdateEvent event) {

		TrainState state = event.state;

		updatePosition(time, event);

		if (!blockLinkTracks(time, state)) {
			// train needs to stop
			decideTargetSpeed(event, state);

			event.checkReservation = time + config.pollInterval;
			decideNextUpdate(event);

		} else {
			event.checkReservation = -1;
			decideNextUpdate(event);
		}
	}

	private void checkTrackReservation(double time, UpdateEvent event) {

		TrainState state = event.state;

		// train might be at the end of route already
		RailLink nextLink = state.isRouteAtEnd() ? null : state.route.get(state.routeIdx);

		boolean allBlocked = blockLinkTracks(time, state);

		// Driver can advance if there is approved distance
		if (allBlocked || FuzzyUtils.greaterThan(state.approvedDist, 0)) {

			if (allBlocked)
				event.checkReservation = -1;
			else {
				event.checkReservation = time + config.pollInterval;
			}

			// Train already waits at the end of previous link, next link is already blocked
			if (event.waitingForLink &&
				nextLink != null &&
				resources.isBlockedBy(nextLink, state) &&
				FuzzyUtils.equals(resources.getLink(state.headLink).length, state.headPosition)) {

				enterLink(time, event);
				event.waitingForLink = false;

			} else {

				updatePosition(time, event);
				decideTargetSpeed(event, state);
				decideNextUpdate(event);

			}

		} else {

			event.checkReservation = time + config.pollInterval;

			// If train is already standing still and waiting, there is no update needed.
			if (event.waitingForLink && FuzzyUtils.equals(state.speed, 0)) {
				event.plannedTime = time + config.pollInterval;
			} else {
				decideNextUpdate(event);
			}
		}
	}

	private void updateDeparture(double time, UpdateEvent event) {

		TrainState state = event.state;
		state.timestamp = time;

		state.allowedMaxSpeed = retrieveAllowedMaxSpeed(state);

		RailLink firstLink = resources.getLink(state.headLink);

		state.headPosition = firstLink.length;
		state.tailPosition = firstLink.length - state.train.length();

		// reserve links and start if first one is free
		if (blockLinkTracks(time, state) || resources.isBlockedBy(firstLink, state)) {

			createEvent(new PersonEntersVehicleEvent(time, state.driver.getId(), state.driver.getVehicle().getId()));
			createEvent(new VehicleEntersTrafficEvent(time, state.driver.getId(),
				state.headLink, state.driver.getVehicle().getId(), state.driver.getMode(), 1.0));

			state.timestamp = time;

			double stopTime = 0;
			if (state.isStop(state.headLink)) {
				stopTime = handleTransitStop(time, state);
			}

			// Train departs at the very end of the first link
			state.routeIdx = 1;

			createEvent(state.asEvent(time));

			if (stopTime <= 0) {
				// Call enter link logic immediately
				enterLink(time, event);
			} else {
				event.plannedTime = time + stopTime;
				event.type = UpdateEvent.Type.ENTER_LINK;
			}

		} else {
			// vehicle will wait and call departure again
			event.plannedTime += config.pollInterval;
		}
	}

	/**
	 * Reserve links in advance as necessary.
	 */
	private boolean blockLinkTracks(double time, TrainState state) {

		double reserveDist = RailsimCalc.calcReservationDistance(state, resources.getLink(state.headLink));

		DispositionResponse response = disposition.requestNextSegment(time, state, reserveDist);

		// Disposition assigned a detour
		if (state.pt != null && response.detour() != null) {

			List<RailLink> subRoute = state.route.subList(response.detour().startIdx(), response.detour().endIdx());

			TransitStopFacility newStop = state.pt.addDetour(subRoute, response.detour().newRoute());

			subRoute.clear();
			subRoute.addAll(response.detour().newRoute());

			if (newStop != null) {
				state.nextStop = newStop;
			}

			createEvent(new RailsimDetourEvent(
				time, state.driver.getVehicle().getId(),
				response.detour().startLink(), response.detour().endLink(),
				response.detour().newRoute().stream().map(RailLink::getLinkId).toList(),
				newStop != null ? newStop.getId() : null
			));

			// reserve the assigned links from the detour
			response = disposition.requestNextSegment(time, state, reserveDist);
		}

		state.approvedDist = response.approvedDist();

		assert FuzzyUtils.greaterEqualThan(state.approvedDist, 0) : "Approved distance must be positive, but was " + response.approvedDist();

		// At the moment the approved speed from the disposition is not used
		// Stop when the approved distance is not enough
		if (FuzzyUtils.lessThan(state.approvedDist, reserveDist + SAFETY_DIST))
			state.approvedSpeed = 0;
		else
			state.approvedSpeed = Double.POSITIVE_INFINITY;

		// Return whether the train has to stop
		return state.approvedSpeed != 0;
	}

	private void enterLink(double time, UpdateEvent event) {

		TrainState state = event.state;

		updatePosition(time, event);

		// current head link is the pt stop, which means the train is at the end of the link when this is called
		if (!event.waitingForLink && state.isStop(state.headLink)) {

			double stopTime = handleTransitStop(time, state);

			assert stopTime >= 0 : "Stop time must be positive";
//			assert FuzzyUtils.equals(state.speed, 0) : "Speed must be 0 at pt stop, but was " + state.speed;

			// Same event is re-scheduled after stopping,
			event.plannedTime = time + stopTime;
			state.speed = 0;

			return;
		}

		// Arrival at destination
		if (!event.waitingForLink && state.isRouteAtEnd()) {

//			assert FuzzyUtils.equals(state.speed, 0) : "Speed must be 0 at end, but was " + state.speed;

			// Free all reservations
			for (RailLink link : state.route) {
				if (resources.isBlockedBy(link, state)) {
					disposition.unblockRailLink(time, state.driver, link);
				}
			}

			state.speed = 0;
			state.driver.notifyArrivalOnLinkByNonNetworkMode(state.headLink);
			state.driver.endLegAndComputeNextState(Math.ceil(time));

			activeTrains.remove(state);

			event.type = UpdateEvent.Type.IDLE;
			return;
		}

		// Train stopped exactly at the end of the link and tries to enter the next link
		if (FuzzyUtils.equals(state.speed, 0) && !blockLinkTracks(time, state)) {

			RailLink currentLink = state.route.get(state.routeIdx);
			// If this linked is blocked the driver can continue
			if (!resources.isBlockedBy(currentLink, state)) {
				event.waitingForLink = true;
				event.type = UpdateEvent.Type.WAIT_FOR_RESERVATION;
				event.plannedTime = time + config.pollInterval;
				return;
			}
		}

		// On route departure the head link is null
		createEvent(new LinkLeaveEvent(time, state.driver.getVehicle().getId(), state.headLink));

		// Get link and increment
		state.headLink = state.route.get(state.routeIdx++).getLinkId();

		assert resources.isBlockedBy(resources.getLink(state.headLink), state) : "Link has to be blocked by driver when entered";

		state.headPosition = 0;
		// Reset waiting flag
		event.waitingForLink = false;

		state.driver.notifyMoveOverNode(state.headLink);
		createEvent(new LinkEnterEvent(time, state.driver.getVehicle().getId(), state.headLink));

		decideTargetSpeed(event, state);

		createEvent(state.asEvent(time));

		decideNextUpdate(event);
	}

	private void leaveLink(double time, UpdateEvent event) {

		TrainState state = event.state;

		RailLink nextTailLink = null;
		// Find the next link in the route
		for (int i = state.routeIdx; i >= 1; i--) {
			if (state.route.get(i - 1).getLinkId().equals(state.tailLink)) {
				nextTailLink = state.route.get(i);
			}
		}

		Objects.requireNonNull(nextTailLink, "Could not find next link in route");

		updatePosition(time, event);

		createEvent(new RailsimTrainLeavesLinkEvent(time, state.driver.getVehicle().getId(), state.tailLink));

		RailLink tailLink = resources.getLink(state.tailLink);

		state.tailLink = nextTailLink.getLinkId();
		state.tailPosition = 0;

		decideTargetSpeed(event, state);

		decideNextUpdate(event);

		if (tailLink.minimumHeadwayTime == 0)
			unblockTrack(time, state, tailLink);
		else
			updateQueue.add(new UpdateEvent(state, tailLink, time));
	}

	/**
	 * Unblocks a link.
	 */
	private void unblockTrack(double time, TrainState state, RailLink unblockLink) {
		disposition.unblockRailLink(time, state.driver, unblockLink);
	}

	/**
	 * Update position within a link and decides on next update.
	 */
	private void updatePosition(double time, UpdateEvent event) {

		TrainState state = event.state;

		double elapsed = time - state.timestamp;

		if (elapsed == 0)
			return;

		double accelTime = (state.targetSpeed - state.speed) / state.acceleration;

		double dist;
		if (state.acceleration == 0) {
			dist = state.speed * elapsed;

		} else if (accelTime < elapsed) {

			// Travelled distance under constant acceleration
			dist = RailsimCalc.calcTraveledDist(state.speed, accelTime, state.acceleration);

			// Remaining time at constant speed
			if (state.acceleration > 0)
				dist += RailsimCalc.calcTraveledDist(state.targetSpeed, elapsed - accelTime, 0);

			// Target speed was reached
			state.speed = state.targetSpeed;
			state.acceleration = 0;

		} else {

			// Acceleration was constant the whole time
			dist = RailsimCalc.calcTraveledDist(state.speed, elapsed, state.acceleration);
			state.speed = state.speed + elapsed * state.acceleration;

			if (FuzzyUtils.equals(state.speed, state.targetSpeed)) {
				state.speed = state.targetSpeed;
				state.acceleration = 0;
			}

		}

		assert FuzzyUtils.greaterEqualThan(dist, 0) : "Travel distance must be positive, but was" + dist;

		// This corrects inaccuracy happening when the train is at the end of the link
		// Should very rarely be necessary
		if (FuzzyUtils.greaterThan(state.headPosition + dist, resources.getLink(state.headLink).length)) {
			// Dist will be dist to end of link
			dist = resources.getLink(state.headLink).length - state.headPosition;
		}
		// In the same way also correct the speed
		if (FuzzyUtils.greaterThan(state.speed, state.allowedMaxSpeed)) {
			state.speed = state.allowedMaxSpeed;
			state.acceleration = 0;
		}

		state.headPosition += dist;
		state.tailPosition += dist;
		state.approvedDist -= dist;

		if (Double.isFinite(state.targetDecelDist)) {
			state.targetDecelDist -= dist;
		}

		// When trains are put into the network their tail may be longer than the current link
		// this assertion may not hold depending on the network
//		assert state.routeIdx <= 2 || FuzzyUtils.greaterEqualThan(state.tailPosition, 0) : "Illegal state update. Tail position should not be negative";

		assert FuzzyUtils.greaterEqualThan(state.headPosition, 0) : "Head position must be positive";
		assert FuzzyUtils.lessEqualThan(state.speed, state.allowedMaxSpeed) : "Speed must be less equal than the allowed speed";
		assert FuzzyUtils.greaterEqualThan(state.approvedDist, 0) : "Approved distance must be positive";

		state.timestamp = time;

		// Only emit events on certain occasions
		if (event.type == UpdateEvent.Type.ENTER_LINK || event.type == UpdateEvent.Type.LEAVE_LINK || event.type == UpdateEvent.Type.POSITION || event.type == UpdateEvent.Type.SPEED_CHANGE)
			createEvent(state.asEvent(time));
	}

	/**
	 * Handle transit stop and update the state.
	 *
	 * @return stop time
	 */
	private double handleTransitStop(double time, TrainState state) {

		assert state.pt != null : "Pt driver must be present";

		// Time needs to be rounded to current sim step
		double stopTime = state.pt.handleTransitStop(state.nextStop, Math.ceil(time));
		// If stop time > 0, train has not yet departed
		if (stopTime <= 0)
			state.nextStop = state.pt.getNextTransitStop();

		return stopTime;
	}

	/**
	 * Decide which update is the earliest and needs to be the next.
	 */
	private void decideNextUpdate(UpdateEvent event) {

		TrainState state = event.state;
		RailLink currentLink = resources.getLink(state.headLink);

		// (1) max speed reached
		double accelDist = Double.POSITIVE_INFINITY;
		if (state.acceleration > 0 && FuzzyUtils.greaterThan(state.targetSpeed, state.speed)) {
			accelDist = RailsimCalc.calcTraveledDist(state.speed, (state.targetSpeed - state.speed) / state.acceleration, state.acceleration);
		}

		// (2) start deceleration
		double decelDist = state.targetDecelDist;

		assert FuzzyUtils.greaterEqualThan(decelDist, 0) : "Deceleration distance must be larger than 0, but was " + decelDist;

		// (3) next link needs reservation
		double reserveDist = Double.POSITIVE_INFINITY;
		if (!state.isRouteAtEnd() && !event.isAwaitingReservation()) {

			// Check first if more distance than for the next stop is needed
			double nextStop = RailsimCalc.calcDistToNextStop(state, currentLink);

			if (!FuzzyUtils.equals(state.approvedDist, nextStop)) {
				double requiredDist = RailsimCalc.calcReservationDistance(state, currentLink);
				// when the approved distance is equal to the required distance, new reservation needs to be made
				reserveDist = state.approvedDist - requiredDist;
			}

			// With moving blocks the reserve distance might not be set to 0, but rather the accel or decel distance
			if (reserveDist < 0)
				reserveDist = 0;

			// Outside of block track the reserve distance is always greater 0
			// infinite loops would occur otherwise
			if (!(event.type != UpdateEvent.Type.BLOCK_TRACK || FuzzyUtils.greaterThan(reserveDist, 0))) {
				throw new AssertionError("Reserve distance must be positive, but was " + reserveDist);
			}
		}

		// (4) tail link changes
		double tailDist = resources.getLink(state.tailLink).length - state.tailPosition;

		// (5) head link changes
		double headDist = currentLink.length - state.headPosition;

		assert FuzzyUtils.greaterEqualThan(tailDist, 0) : "Tail distance must be positive";
		assert FuzzyUtils.greaterEqualThan(headDist, 0) : "Head distance must be positive";

		// Find the earliest required update

		double dist;
		if (FuzzyUtils.lessEqualThan(tailDist, decelDist) &&
			FuzzyUtils.lessEqualThan(tailDist, reserveDist) &&
			FuzzyUtils.lessEqualThan(tailDist, headDist) &&
			FuzzyUtils.lessEqualThan(tailDist, accelDist)) {

			// leave link has priority, and is ensured to be executed before others at same time step
			// otherwise train might block links of same length as itself

			dist = tailDist;
			event.type = UpdateEvent.Type.LEAVE_LINK;

		} else if (reserveDist <= accelDist && reserveDist <= decelDist && reserveDist <= tailDist && reserveDist <= headDist) {
			dist = reserveDist;
			event.type = UpdateEvent.Type.BLOCK_TRACK;
		} else if (accelDist <= decelDist && accelDist <= reserveDist && accelDist <= tailDist && accelDist <= headDist) {
			dist = accelDist;
			event.type = UpdateEvent.Type.SPEED_CHANGE;
		} else if (decelDist <= accelDist && decelDist <= reserveDist && decelDist <= tailDist && decelDist <= headDist) {
			dist = decelDist;
			event.type = UpdateEvent.Type.SPEED_CHANGE;
		} else {
			dist = headDist;
			event.type = UpdateEvent.Type.ENTER_LINK;
		}

		assert FuzzyUtils.greaterEqualThan(dist, 0) : "Distance for next update must be positive";

		// dist is the minimum of all supplied distances
		event.plannedTime = state.timestamp + RailsimCalc.calcRequiredTime(state, dist);

		// There could be old reservations events that need to be checked first
		if (event.isAwaitingReservation() && event.checkReservation < state.timestamp) {
			event.checkReservation = state.timestamp;
		}

		// insert reservation event if necessary
		if (event.isAwaitingReservation() && event.plannedTime > event.checkReservation) {

			event.type = UpdateEvent.Type.WAIT_FOR_RESERVATION;
			event.plannedTime = event.checkReservation;
		}

		assert Double.isFinite(event.plannedTime) : "Planned update time must be finite, but was " + event.plannedTime;
		assert event.plannedTime >= state.timestamp : "Planned time must be after current time";

	}

	/**
	 * Calculates possible target speed, consequential acceleration depending on current state.
	 */
	private void decideTargetSpeed(UpdateEvent event, TrainState state) {

		// Distance to next link
		RailLink currentLink = resources.getLink(state.headLink);

		double dist = currentLink.length - state.headPosition;
		state.allowedMaxSpeed = retrieveAllowedMaxSpeed(state);

		// Lookahead window
		double window = RailsimCalc.calcTraveledDist(state.allowedMaxSpeed, state.allowedMaxSpeed / state.train.deceleration(),
			-state.train.deceleration()) + currentLink.length;

		double minAllowed = state.allowedMaxSpeed;

		state.targetSpeed = state.allowedMaxSpeed;
		state.targetDecelDist = Double.POSITIVE_INFINITY;

		boolean stop = false;

		// Set speed approved by disposition
		if (state.approvedSpeed < minAllowed) {

			RailsimCalc.SpeedTarget target = RailsimCalc.calcTargetSpeed(state.approvedDist,
				state.train.acceleration(), state.train.deceleration(),
				state.speed, state.allowedMaxSpeed, state.approvedSpeed);

//			assert FuzzyUtils.greaterEqualThan(target.decelDist(), 0) : "Decel dist is " + target.decelDist() + ", stopping is not possible";

			// Because of numerical inaccuracies, the decel dist might be slightly negative
			if (FuzzyUtils.lessEqualThan(target.decelDist(), 0)) {
				state.targetSpeed = state.approvedSpeed;
				state.targetDecelDist = Double.POSITIVE_INFINITY;
				stop = true;
			} else {
				state.targetSpeed = target.targetSpeed();
				state.targetDecelDist = target.decelDist();
			}
		}

		for (int i = state.routeIdx; i <= state.route.size() && !stop; i++) {

			RailLink link;
			double allowed;
			if (i == state.route.size()) {
				link = null;
				allowed = 0;
			} else {
				link = state.route.get(i);

				// If the previous link is a transit stop the speed needs to be 0 at the next link
				// train stops at the very end of a link
				if (i > 0 && state.isStop(state.route.get(i - 1).getLinkId()))
					allowed = 0;
				else
					allowed = link.getAllowedFreespeed(state.driver);
			}

			// Only need to consider if speed is lower than the allowed speed
			if (!FuzzyUtils.equals(dist, 0) && allowed <= minAllowed) {
				RailsimCalc.SpeedTarget target = RailsimCalc.calcTargetSpeed(dist, state.train.acceleration(), state.train.deceleration(),
					state.speed, state.allowedMaxSpeed, Math.min(state.allowedMaxSpeed, allowed));

				assert FuzzyUtils.greaterEqualThan(target.decelDist(), 0) : "Decel dist must be greater than 0, or stopping is not possible";

				if (FuzzyUtils.equals(target.decelDist(), 0)) {

					// Need to decelerate now
					state.targetSpeed = allowed;
					state.targetDecelDist = Double.POSITIVE_INFINITY;
					break;
				} else if (target.decelDist() < state.targetDecelDist && target.targetSpeed() <= state.targetSpeed) {

					// Decelerate later
					state.targetSpeed = target.targetSpeed();
					state.targetDecelDist = target.decelDist();

				} else if (target.targetSpeed() > state.targetSpeed && !Double.isFinite(state.targetDecelDist)) {
					// Acceleration is required
					state.targetSpeed = target.targetSpeed();
					state.targetDecelDist = target.decelDist();
				}
			}

			if (link != null)
				dist += link.length;

			if (FuzzyUtils.greaterEqualThan(dist, window))
				break;

			minAllowed = allowed;
		}

		// Calc accel depending on target
		if (FuzzyUtils.equals(state.speed, state.targetSpeed)) {
			state.acceleration = 0;
		} else if (FuzzyUtils.lessThan(state.speed, state.targetSpeed)) {
			state.acceleration = state.train.acceleration();
		} else {
			state.acceleration = -state.train.deceleration();
		}

		assert FuzzyUtils.equals(state.targetSpeed, state.speed) || state.acceleration != 0 : "Acceleration must be set if target speed is different than current";
		assert FuzzyUtils.greaterThan(state.targetDecelDist, 0) : "Target decel must be greater than 0 after updating";

	}

	/**
	 * Allowed speed for the train.
	 */
	private double retrieveAllowedMaxSpeed(TrainState state) {

		double maxSpeed = resources.getLink(state.headLink).getAllowedFreespeed(state.driver);

		// get minium allowed speed on all links occupied by the train
		for (int i = state.routeIdx - 1; i >= 0; i--) {
			RailLink link = state.route.get(i);
			maxSpeed = Math.min(maxSpeed, link.getAllowedFreespeed(state.driver));
			if (link.getLinkId().equals(state.tailLink)) {
				break;
			}
		}

		return maxSpeed;
	}

	/**
	 * Remove all trains from simulation and generate events at the end of the day.
	 *
	 * @param now end of daytime
	 */
	void clearTrains(double now) {

		for (TrainState train : activeTrains) {
			eventsManager.processEvent(new VehicleAbortsEvent(now, train.driver.getVehicle().getId(), train.headLink));
			eventsManager.processEvent(new PersonStuckEvent(now, train.driver.getId(), train.headLink, train.driver.getMode()));
		}
	}
}
