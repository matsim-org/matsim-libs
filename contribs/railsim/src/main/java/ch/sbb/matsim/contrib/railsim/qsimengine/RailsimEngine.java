package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainLeavesLinkEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.diposition.SimpleDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.diposition.TrainDisposition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.VehicleType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Engine to simulate train movement.
 */
final class RailsimEngine implements Steppable {

	private static final Logger log = LogManager.getLogger(RailsimEngine.class);

	/**
	 * If trains need to wait, they will check every x seconds if they can proceeed.
	 */
	private static final double POLL_INTERVAL = 10;

	private final EventsManager eventsManager;
	private final RailsimConfigGroup config;

	private final List<TrainState> activeTrains = new ArrayList<>();

	private final Queue<UpdateEvent> updateQueue = new PriorityQueue<>();

	private final RailResourceManager resources;

	private final TrainDisposition disposition;

	public RailsimEngine(EventsManager eventsManager, RailsimConfigGroup config, RailResourceManager resources) {
		this.eventsManager = eventsManager;
		this.config = config;
		this.resources = resources;
		this.disposition = new SimpleDisposition(resources);
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
	}

	/**
	 * Update the current state of all trains, even if no update would be needed.
	 */
	public void updateAllStates(double time) {

		// Process all waiting events first
		doSimStep(time);

		for (TrainState train : activeTrains) {
			if (train.timestamp < time)
				updateState(time, new UpdateEvent(train, UpdateEvent.Type.POSITION));
		}
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

	private void createEvent(Event event) {
		// Because of the 1s update interval, events need to be rounded to the current simulation step
		event.setTime(Math.ceil(event.getTime()));
//	 	System.out.println(event.getTime());
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
			default -> throw new IllegalStateException("Unhandled update type " + event.type);
		}
	}

	private void updateSpeed(double time, UpdateEvent event) {

		TrainState state = event.state;

		updatePosition(time, event);

		state.targetSpeed = event.newSpeed;
		if (state.targetSpeed < state.speed)
			state.acceleration = -state.train.deceleration();
		else if (state.targetSpeed > state.speed)
			state.acceleration = state.train.acceleration();

		// Remove update information
		event.newSpeed = -1;

		createEvent(state.asEvent(time));

		decideNextUpdate(event);
	}

	private void blockTrack(double time, UpdateEvent event) {

		TrainState state = event.state;

		updatePosition(time, event);

		if (!blockLinkTracks(time, state.routeIdx, state)) {

			// TODO: calculate speed to arrival at the last blocked link

			// Break when reservation is not possible
			state.targetSpeed = 0;
			state.acceleration = -state.train.deceleration();

			event.checkReservation = time + POLL_INTERVAL;
			decideNextUpdate(event);

		} else {
			event.checkReservation = -1;
			decideNextUpdate(event);
		}
	}

	private void checkTrackReservation(double time, UpdateEvent event) {

		TrainState state = event.state;

		if (blockLinkTracks(time, state.routeIdx, state)) {

			updatePosition(time, event);
			// TODO: maximum speed could be lower
			// see enterLink as well

			state.allowedMaxSpeed = retrieveAllowedMaxSpeed(state);
			state.targetSpeed = state.allowedMaxSpeed;
			state.acceleration = state.train.acceleration();

			event.checkReservation = -1;
			decideNextUpdate(event);

		} else {

			event.checkReservation = time + POLL_INTERVAL;
			decideNextUpdate(event);

		}

	}

	private void updateDeparture(double time, UpdateEvent event) {

		TrainState state = event.state;
		state.timestamp = time;

		state.headPosition = resources.getLink(state.headLink).length;
		state.tailPosition = resources.getLink(state.headLink).length - state.train.length();

		if (blockLinkTracks(time, 0, state)) {

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

			if (stopTime == 0) {
				// Call enter link logic immediately
				enterLink(time, event);
			} else {
				event.plannedTime = time + stopTime;
				event.type = UpdateEvent.Type.ENTER_LINK;
			}

		} else {
			// vehicle will wait and call departure again
			event.plannedTime += POLL_INTERVAL;
		}
	}

	/**
	 * Reserve links in advance as necessary.
	 */
	private boolean blockLinkTracks(double time, int idx, TrainState state) {

		List<RailLink> links = RailsimCalc.calcLinksToBlock(idx, state);

		if (links.isEmpty())
			return true;

		List<RailLink> blocked = disposition.blockRailSegment(time, state.driver, links);

		// Block the approved links
		for (RailLink link : blocked) {
			if (link.isBlockedBy(state.driver))
				continue;

			int track = link.blockTrack(state.driver);
			createEvent(new RailsimLinkStateChangeEvent(time, link.getLinkId(),
				state.driver.getVehicle().getId(), TrackState.BLOCKED, track));
		}

		// Only continue successfully if all requested link have been blocked
		return links.size() == blocked.size();
	}

	private void enterLink(double time, UpdateEvent event) {

		TrainState state = event.state;

		updatePosition(time, event);

		// current head link is the pt stop, which means the train is at the end of the link when this is called
		if (state.isStop(state.headLink)) {

			double stopTime = handleTransitStop(time, state);

			assert FuzzyUtils.equals(state.speed, 0) : "Speed must be 0 at pt stop, but was " + state.speed;

			// Same event is re-scheduled after stopping,
			event.plannedTime = time + stopTime;

			return;
		}

		// Arrival at destination
		if (state.isRouteAtEnd()) {

			assert FuzzyUtils.equals(state.speed, 0) : "Speed must be 0 at end, but was " + state.speed;

			// Free all reservations
			for (RailLink link : state.route) {
				if (link.isBlockedBy(state.driver)) {

					int track = link.releaseTrack(state.driver);
					createEvent(new RailsimLinkStateChangeEvent(time, link.getLinkId(), state.driver.getVehicle().getId(),
						TrackState.FREE, track));

					disposition.unblockRailLink(time, state.driver, link);
				}
			}

			state.driver.notifyArrivalOnLinkByNonNetworkMode(state.headLink);
			state.driver.endLegAndComputeNextState(Math.ceil(time));

			activeTrains.remove(state);

			event.type = UpdateEvent.Type.IDLE;
			return;
		}

		// On route departure the head link is null
		createEvent(new LinkLeaveEvent(time, state.driver.getVehicle().getId(), state.headLink));

		// Get link and increment
		state.headPosition = 0;
		state.headLink = state.route.get(state.routeIdx++).getLinkId();

		state.driver.notifyMoveOverNode(state.headLink);
		createEvent(new LinkEnterEvent(time, state.driver.getVehicle().getId(), state.headLink));

		RailLink link = resources.getLink(state.headLink);

		assert link.isBlockedBy(state.driver) : "Link has to be blocked by driver when entered";

		// TODO: this probably needs to be a separate function to calculate possible target speed more accurately
		if (RailsimCalc.calcDecelDistanceAndSpeed(link, event) == Double.POSITIVE_INFINITY &&
			!event.isAwaitingReservation()) {

			state.allowedMaxSpeed = retrieveAllowedMaxSpeed(state);

			if (state.allowedMaxSpeed > state.targetSpeed) {
				state.targetSpeed = state.allowedMaxSpeed;
				state.acceleration = state.train.acceleration();
			}
		}

		createEvent(state.asEvent(time));

		decideNextUpdate(event);
	}

	private void leaveLink(double time, UpdateEvent event) {

		TrainState state = event.state;

		RailLink tailLink = resources.getLink(state.tailLink);
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
		// TODO: link should be released after headway time
		int track = tailLink.releaseTrack(state.driver);
		createEvent(new RailsimLinkStateChangeEvent(time, state.tailLink, state.driver.getVehicle().getId(),
			TrackState.FREE, track));

		disposition.unblockRailLink(time, state.driver, resources.getLink(state.tailLink));

		state.tailLink = nextTailLink.getLinkId();
		state.tailPosition = 0;

		decideNextUpdate(event);
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

		state.headPosition += dist;
		state.tailPosition += dist;
		state.timestamp = time;

		assert state.routeIdx <= 1 || FuzzyUtils.greaterEqualThan(state.tailPosition, 0) : "Illegal state update. Tail position should not be negative";
		assert FuzzyUtils.lessEqualThan(state.headPosition, resources.getLink(state.headLink).length) : "Illegal state update. Head position must be smaller than link length";
		assert FuzzyUtils.greaterEqualThan(state.headPosition, 0) : "Head position must be positive";
		assert FuzzyUtils.lessEqualThan(state.speed, state.allowedMaxSpeed) : "Speed must be less equal than the allowed speed";

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
		state.nextStop = state.pt.getNextTransitStop();

		return stopTime;
	}

	/**
	 * Decide which update is the earliest and needs to be the next.
	 */
	private void decideNextUpdate(UpdateEvent event) {

		TrainState state = event.state;
		RailLink currentLink = resources.getLink(state.headLink);

//		if (state.routeIdx >= 877 && state.routeIdx <= 880 && state.timestamp >= 7300)
//			log.info("debug");

		// (1) max speed reached
		double accelDist = Double.POSITIVE_INFINITY;
		if (state.acceleration > 0 && FuzzyUtils.greaterThan(state.targetSpeed, state.speed)) {
			accelDist = RailsimCalc.calcTraveledDist(state.speed, (state.targetSpeed - state.speed) / state.acceleration, state.acceleration);
		}

		// (2) start deceleration
		double decelDist = (event.newSpeed == state.targetSpeed) ?
			Double.POSITIVE_INFINITY :
			RailsimCalc.calcDecelDistanceAndSpeed(currentLink, event);

		assert FuzzyUtils.greaterEqualThan(decelDist, 0) : "Deceleration distance must be larger than 0, but was" + decelDist;

		// (3) next link needs reservation
		double reserveDist = Double.POSITIVE_INFINITY;
		if (!state.isRouteAtEnd() && !event.isAwaitingReservation()) {
			reserveDist = RailsimCalc.nextLinkReservation(state, currentLink);

			if (reserveDist < 0)
				reserveDist = 0;
		}

		// (4) tail link changes
		double tailDist = resources.getLink(state.tailLink).length - state.tailPosition;

		// (5) head link changes
		double headDist = currentLink.length - state.headPosition;

		assert FuzzyUtils.greaterEqualThan(tailDist, 0) : "Tail distance must be positive";
		assert FuzzyUtils.greaterEqualThan(headDist, 0) : "Head distance must be positive";

		// Find the earliest required update

		double dist;
		if (accelDist <= decelDist && accelDist <= reserveDist && accelDist <= tailDist && accelDist <= headDist) {
			dist = accelDist;
			event.type = UpdateEvent.Type.POSITION;
		} else if (decelDist <= accelDist && decelDist <= reserveDist && decelDist <= tailDist && decelDist <= headDist) {
			dist = decelDist;
			event.type = UpdateEvent.Type.SPEED_CHANGE;
		} else if (reserveDist <= accelDist && reserveDist <= decelDist && reserveDist <= tailDist && reserveDist <= headDist) {
			dist = reserveDist;
			event.type = UpdateEvent.Type.BLOCK_TRACK;
		} else if (tailDist <= decelDist && tailDist <= reserveDist && tailDist <= headDist) {
			dist = tailDist;
			event.type = UpdateEvent.Type.LEAVE_LINK;
		} else {
			dist = headDist;
			event.type = UpdateEvent.Type.ENTER_LINK;
		}

		assert FuzzyUtils.greaterEqualThan(dist, 0) : "Distance for next update must be positive";

		// dist is the minimum of all supplied distances
		event.plannedTime = state.timestamp + RailsimCalc.calcRequiredTime(state, dist);

		// insert reservation event if necessary
		if (event.checkReservation >= 0 && event.plannedTime > event.checkReservation) {
			event.type = UpdateEvent.Type.WAIT_FOR_RESERVATION;
			event.plannedTime = event.checkReservation;
		}

		assert Double.isFinite(event.plannedTime) : "Planned update time must be finite, but was " + event.plannedTime;
	}


	/**
	 * Allowed speed for the train.
	 */
	private double retrieveAllowedMaxSpeed(TrainState state) {

		double maxSpeed = resources.getLink(state.headLink).getAllowedFreespeed(state.driver);

		for (int i = state.routeIdx - 1; i >= 0; i--) {
			RailLink link = state.route.get(i);
			maxSpeed = Math.min(maxSpeed, link.getAllowedFreespeed(state.driver));
			if (link.getLinkId().equals(state.tailLink)) {
				break;
			}

		}

		return maxSpeed;
	}

}
