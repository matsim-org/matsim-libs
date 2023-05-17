package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainLeavesLinkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.population.routes.NetworkRoute;

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

	/**
	 * Rail links
	 */
	private final Map<Id<Link>, RailLink> links;

	private final List<TrainState> activeTrains = new ArrayList<>();

	private final Queue<UpdateEvent> updateQueue = new PriorityQueue<>();

	public RailsimEngine(EventsManager eventsManager, RailsimConfigGroup config, Map<Id<Link>, ? extends Link> network) {
		this.eventsManager = eventsManager;
		this.config = config;
		this.links = new IdMap<>(Link.class, network.size());
		for (Map.Entry<Id<Link>, ? extends Link> e : network.entrySet()) {

			// This link and the opposite need to have the same attributes

			Id<Link> opposite = RailsimUtils.getOppositeDirectionLink(e.getValue());
			// Use existing instead of creating a new one
			if (links.containsKey(opposite)) {
				this.links.put(e.getKey(), links.get(opposite));
			} else {
				this.links.put(e.getKey(), new RailLink(e.getValue(), opposite));
			}
		}
	}

	@Override
	public void doSimStep(double time) {

		UpdateEvent update = updateQueue.peek();

		// Update loop over all required state updates
		while (update != null && update.plannedTime <= time) {
			updateQueue.poll();

			// Use planned time here, otherwise there will be inaccuracies
			updateState(update.plannedTime, update);
			update = updateQueue.peek();
		}
	}

	/**
	 * Handle the departure of a train.
	 */
	public boolean handleDeparture(double now, MobsimDriverAgent agent, Id<Link> linkId, NetworkRoute route) {

		log.info("Train {} is departing", agent.getVehicle());

		List<RailLink> list = route.getLinkIds().stream().map(links::get).collect(Collectors.toList());
		list.add(0, links.get(linkId));
		list.add(links.get(route.getEndLinkId()));

		TrainState state = new TrainState(agent, new TrainInfo(agent.getVehicle().getVehicle().getType(), config),
			now, null, list);

		activeTrains.add(state);

		updateQueue.add(new UpdateEvent(state, UpdateEvent.Type.DEPARTURE));

		return true;
	}

	/**
	 * Update the current state of all trains, even if no update would be needed.
	 */
	public void updateAllStates(double time) {

		// Process all waiting events first
		doSimStep(time);
		updateQueue.clear();

		for (TrainState train : activeTrains) {
			if (train.timestamp < time)
				updateState(time, new UpdateEvent(train, UpdateEvent.Type.POSITION));
		}
	}

	private void updateState(double time, UpdateEvent event) {

		// Do different updates depending on the type
		switch (event.type) {
			case DEPARTURE -> updateDeparture(time, event);
			case POSITION -> {
				updatePosition(time, event);
				decideNextUpdate(time, event);
			}
			case SPEED_CHANGE -> updateSpeed(time, event);
			case ENTER_LINK -> enterLink(time, event);
			case LEAVE_LINK -> leaveLink(time, event);
			case TRACK_RESERVATION -> reserveTrack(time, event);
			default -> throw new IllegalStateException("Unhandled update type " + event.type);
		}

		if (event.type != UpdateEvent.Type.IDLE) {
			updateQueue.add(event);
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

		eventsManager.processEvent(state.asEvent(time));

		decideNextUpdate(time, event);
	}

	private void reserveTrack(double time, UpdateEvent event) {

		TrainState state = event.state;

		updatePosition(time, event);

		if (!reserveLinkTracks(time, state.routeIdx, state)) {
			// Break when reservation is not possible
			state.targetSpeed = 0;
			state.acceleration = -state.train.deceleration();

			event.type = UpdateEvent.Type.WAIT_FOR_RESERVATION;
			event.plannedTime += POLL_INTERVAL;
		} else
			decideNextUpdate(time, event);
	}

	private void updateDeparture(double time, UpdateEvent event) {

		TrainState state = event.state;
		state.timestamp = time;

		RailLink firstLink = state.route.get(0);

		// for departure only the track has to be free and no tracks in advance
		if (firstLink.hasFreeTrack()) {

			reserveLinkTracks(time, 0, state);

			state.timestamp = time;

			// Call enter link logic immediately
			enterLink(time, event);
		}

		// vehicle will wait
		event.plannedTime += POLL_INTERVAL;
	}

	/**
	 * Reserve links in advance as necessary.
	 */
	private boolean reserveLinkTracks(double time, int idx, TrainState state) {

		double stopTime = state.targetSpeed / state.train.deceleration();
		// safety distance
		double safety = calcTraveledDist(state.targetSpeed, stopTime, -state.train.deceleration());

		double reserved = 0;

		do {
			RailLink nextLink = state.route.get(idx++);
			int track = nextLink.reserveTrack(state.driver);

			if (track > -1) {
				reserved += nextLink.length;
				eventsManager.processEvent(new RailsimLinkStateChangeEvent(time, nextLink.getLinkId(),
					state.driver.getVehicle().getId(), TrackState.RESERVED, track));

			} else {
				return false;
			}

		} while (reserved < safety && idx < state.route.size());

		return true;
	}

	private void enterLink(double time, UpdateEvent event) {

		TrainState state = event.state;

		updatePosition(time, event);

		// On route departure the head link is null
		if (state.headLink != null) {
			eventsManager.processEvent(new LinkLeaveEvent(time, state.driver.getVehicle().getId(), state.headLink));
		}

		// Get link and increment
		state.headPosition = 0;
		state.headLink = state.route.get(state.routeIdx++).getLinkId();

		// Arrival at destination
		if (state.isRouteAtEnd()) {

			assert FuzzyUtils.equals(state.speed, 0) : "Speed must be 0 at end but was " + state.speed;

			// TODO: release all blocked or occupied tracks

			state.driver.notifyArrivalOnLinkByNonNetworkMode(state.headLink);
			state.driver.endLegAndComputeNextState(time);

			activeTrains.remove(state);

			event.type = UpdateEvent.Type.IDLE;
			return;
		}

		// On departure tail link is the same head link
		if (state.tailLink == null) {
			state.tailLink = state.headLink;
			state.tailPosition = links.get(state.tailLink).length + state.train.length();
		}

		state.driver.notifyMoveOverNode(state.headLink);
		eventsManager.processEvent(new LinkEnterEvent(time, state.driver.getVehicle().getId(), state.headLink));

		RailLink link = links.get(state.headLink);

		int track = link.blockTrack(state.driver);
		eventsManager.processEvent(new RailsimLinkStateChangeEvent(time, state.headLink, state.driver.getVehicle().getId(),
			TrackState.BLOCKED, track));

		// TODO: this probably needs to be a separate function to calculate possible target speed more accurately
		if (calcDeccelDistance(event) == Double.POSITIVE_INFINITY) {
			state.allowedMaxSpeed = retrieveAllowedMaxSpeed(state);
			state.targetSpeed = state.allowedMaxSpeed;
			state.acceleration = state.train.acceleration();
		}

		eventsManager.processEvent(state.asEvent(time));

		decideNextUpdate(time, event);
	}

	private void leaveLink(double time, UpdateEvent event) {

		TrainState state = event.state;
		RailLink link = null;
		// Find the next link in the route
		for (int i = state.routeIdx; i >= 1; i--) {
			if (state.route.get(i - 1).getLinkId().equals(state.tailLink)) {
				link = state.route.get(i);
			}
		}

		Objects.requireNonNull(link, "Could not find next link in route");

		updatePosition(time, event);

		state.tailLink = link.getLinkId();
		state.tailPosition = link.length + state.train.length();

		eventsManager.processEvent(new RailsimTrainLeavesLinkEvent(time, state.driver.getVehicle().getId(), state.tailLink));

		// TODO: link is released after headway time
		int track = link.releaseTrack(state.driver);
		eventsManager.processEvent(new RailsimLinkStateChangeEvent(time, state.tailLink, state.driver.getVehicle().getId(),
			TrackState.FREE, track));

		decideNextUpdate(time, event);
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
			dist = calcTraveledDist(state.speed, accelTime, state.acceleration);

			// Remaining time at constant speed
			if (state.acceleration > 0)
				dist += calcTraveledDist(state.targetSpeed, elapsed - accelTime, 0);

			// Target speed was reached
			state.speed = state.targetSpeed;
			state.acceleration = 0;

		} else {

			// Acceleration was constant the whole time
			dist = calcTraveledDist(state.speed, elapsed, state.acceleration);
			state.speed = state.speed + elapsed * state.acceleration;

		}

		assert FuzzyUtils.greaterEqualThan(dist, 0) : "Travel distance must be positive, but was" + dist;

		state.headPosition += dist;
		state.tailPosition -= dist;
		state.timestamp = time;

		assert FuzzyUtils.greaterEqualThan(state.tailPosition, 0) : "Illegal state update. Tail position should not be negative";
		assert FuzzyUtils.lessEqualThan(state.headPosition, links.get(state.headLink).length) : "Illegal state update. Head position must be smaller than link length";
		assert FuzzyUtils.greaterEqualThan(state.headPosition, 0) : "Head position must be positive";

		eventsManager.processEvent(state.asEvent(time));
	}

	/**
	 * Decide which update is the earliest and needs to be the next.
	 */
	private void decideNextUpdate(double time, UpdateEvent event) {

		TrainState state = event.state;
		RailLink currentLink = links.get(state.headLink);

		// (1) max speed reached
		double accelDist = Double.POSITIVE_INFINITY;
		if (state.acceleration > 0 && state.targetSpeed > state.speed) {

			accelDist = calcTraveledDist(state.speed, (state.targetSpeed - state.speed) / state.acceleration, state.acceleration);
		}

		// (2) start deceleration
		double deccelDist = calcDeccelDistance(event);

		assert FuzzyUtils.greaterEqualThan(deccelDist, 0) : "Deceleration distance must be larger than 0";

		// (3) next link needs reservation
		double reserveDist = Double.POSITIVE_INFINITY;
		if (!state.isRouteAtEnd() && !state.route.get(state.routeIdx).isReserved(state.driver)) {
			// time needed for full stop
			double stopTime = state.allowedMaxSpeed / state.train.deceleration();

			assert stopTime > 0 : "Stop time can not be negative";

			// Distance for full stop
			double safetyDist = calcTraveledDist(state.allowedMaxSpeed, stopTime, -state.train.deceleration());

			reserveDist = currentLink.length - safetyDist - state.headPosition;
		}

		// (4) tail link changes
		double tailDist = state.tailPosition;
		// (5) head link changes
		double headDist = currentLink.length - state.headPosition;

		assert tailDist >= 0 : "Tail distance must be positive";
		assert headDist >= 0 : "Head distance must be positive";

		// Find the earliest required update

		double dist;
		if (accelDist <= deccelDist && accelDist <= reserveDist && accelDist <= tailDist && accelDist <= headDist) {
			dist = accelDist;
			event.type = UpdateEvent.Type.POSITION;
		} else if (deccelDist <= accelDist && deccelDist <= reserveDist && deccelDist <= tailDist && deccelDist <= headDist &&
					event.newSpeed != state.targetSpeed) {
			dist = deccelDist;
			event.type = UpdateEvent.Type.SPEED_CHANGE;
		} else if (reserveDist <= accelDist && reserveDist <= deccelDist && reserveDist <= tailDist && reserveDist <= headDist) {
			dist = reserveDist;
			event.type = UpdateEvent.Type.TRACK_RESERVATION;
		} else if (tailDist <= deccelDist && tailDist <= reserveDist && tailDist <= headDist) {
			dist = tailDist;
			event.type = UpdateEvent.Type.LEAVE_LINK;
		} else {
			dist = headDist;
			event.type = UpdateEvent.Type.ENTER_LINK;
		}

		assert dist >= 0 : "Distance for next update must be positive";

		// dist is the minimum of all supplied distances
		event.plannedTime = time + calcRequiredTime(state, dist);
	}

	/**
	 * Calculate time needed to advance distance {@code dist}. Depending on acceleration and max speed.
	 */
	private static double calcRequiredTime(TrainState state, double dist) {

		if (state.acceleration == 0)
			return state.speed == 0 ? Double.POSITIVE_INFINITY : dist / state.speed;

		if (state.acceleration > 0) {

			double accelTime = (state.targetSpeed - state.speed) / state.acceleration;

			double d = calcTraveledDist(state.speed, accelTime, state.acceleration);

			// The required distance is reached during acceleration
			if (d > dist) {
				return solveTraveledDist(state.speed, dist, state.acceleration);

			} else
				// Time for accel plus remaining dist at max speed
				return accelTime + (dist - d) / state.targetSpeed;

		} else {

			double deccelTime = -(state.speed - state.targetSpeed) / state.acceleration;

			// max distance that can be reached
			double max = calcTraveledDist(state.speed, deccelTime, state.acceleration);

			if (dist < max) {
				return solveTraveledDist(state.speed, dist, state.acceleration);
			} else
				return deccelTime;
		}
	}


	/**
	 * Calculate traveled distance given initial speed and constant acceleration.
	 */
	static double calcTraveledDist(double speed, double elapsedTime, double acceleration) {
		return speed * elapsedTime + (elapsedTime * elapsedTime * acceleration / 2);
	}

	/**
	 * Inverse of {@link #calcTraveledDist(double, double, double)}, solves for distance.
	 */
	static double solveTraveledDist(double speed, double dist, double acceleration) {
		if (acceleration == 0)
			return dist / speed;

		return (Math.sqrt(2 * acceleration * dist + speed * speed) - speed) / acceleration;
	}

	/**
	 * Calc when deceleration needs to start.
	 */
	private double calcDeccelDistance(UpdateEvent event) {

		// TODO: pure calculations without updates can probably be put into separate classes

		TrainState state = event.state;

		if (state.speed == 0)
			return Double.POSITIVE_INFINITY;

		double assumedSpeed = state.speed;

		// Lookahead window
		double window = calcTraveledDist(assumedSpeed, assumedSpeed / state.train.deceleration(),
			-state.train.deceleration()) + links.get(state.headLink).length;

		// Distance to the next speed change point (link)
		double dist = links.get(state.headLink).length - state.headPosition;

		double deccelDist = Double.POSITIVE_INFINITY;
		double speed = 0;

		for (int i = state.routeIdx; i < state.route.size(); i++) {

			RailLink link = state.route.get(i);
			double allowed;
			// Last track where train comes to halt
			if (i == state.route.size() - 1)
				allowed = 0;
			else {
				allowed = link.getAllowedFreespeed(state.driver);
			}

			if (allowed < assumedSpeed) {
				double timeDeccel = (assumedSpeed - allowed) / state.train.deceleration();
				double newDeccelDist = calcTraveledDist(assumedSpeed, timeDeccel, -state.train.deceleration());

				if ((dist - newDeccelDist) < deccelDist) {
					deccelDist = dist - newDeccelDist;
					speed = allowed;
				}
			}

			dist += link.length;

			// don't need to look further than distance needed for full stop
			if (dist >= window)
				break;
		}

		event.newSpeed = speed;
		return deccelDist;
	}

	/**
	 * Allowed speed for the train.
	 */
	private double retrieveAllowedMaxSpeed(TrainState state) {
		// TODO: needs to check the whole part of current route

		return Math.min(
			links.get(state.headLink).getAllowedFreespeed(state.driver),
			links.get(state.tailLink).getAllowedFreespeed(state.driver)
		);
	}

}
