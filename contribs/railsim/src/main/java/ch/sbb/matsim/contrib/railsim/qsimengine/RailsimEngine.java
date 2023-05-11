package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
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
			updateState(time, update);
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
			case POSITION -> updatePosition(time, event);
			case ENTER_LINK -> enterLink(time, event);
			case TRACK_RESERVATION -> reserveTrack(time, event);
			default -> throw new IllegalStateException("Unhandled update type " + event.type);
		}

		if (event.type != UpdateEvent.Type.IDLE) {
			updateQueue.add(event);
		}
	}

	private void reserveTrack(double time, UpdateEvent event) {

		TrainState state = event.state;

		RailLink link = state.route.get(state.routeIdx);

		int track = link.reserveTrack(state.driver);
		if (track >= 0) {

			eventsManager.processEvent(new RailsimLinkStateChangeEvent(time, link.getLinkId(),
				state.driver.getVehicle().getId(), TrackState.RESERVED, track));

		} else {
			// Brake
			state.targetSpeed = 0;

			// TODO: continue when track is released
		}

		updatePosition(time, event);
	}

	private void updateDeparture(double time, UpdateEvent event) {

		TrainState state = event.state;
		state.timestamp = time;

		RailLink firstLink = state.route.get(0);

		// for departure only the track has to be free and no tracks in advance
		if (firstLink.hasFreeTrack()) {
			int track = firstLink.reserveTrack(state.driver);
			eventsManager.processEvent(new RailsimLinkStateChangeEvent(time, firstLink.getLinkId(),
				state.driver.getVehicle().getId(), TrackState.RESERVED, track));

			// Call enter link logic immediately
			enterLink(time, event);
		}

		// vehicle will wait implicitly
		// TODO: should be done via callback later
		event.plannedTime += 1;
	}

	private void enterLink(double time, UpdateEvent event) {

		TrainState state = event.state;

		// Get link and increment
		state.headPosition = 0;
		state.headLink = state.route.get(state.routeIdx++).getLinkId();

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

		state.acceleration = state.train.acceleration();
		state.allowedMaxSpeed = retrieveAllowedMaxSpeed(state);
		state.targetSpeed = state.allowedMaxSpeed;
		state.timestamp = time;

		event.type = UpdateEvent.Type.POSITION;

		updatePosition(time, event);
	}

	/**
	 * Update position within a link and decides on next update.
	 */
	private void updatePosition(double time, UpdateEvent event) {

		TrainState state = event.state;

		double elapsed = time - state.timestamp;

		if (elapsed > 0) {

			advancePosition(state, elapsed);
			state.timestamp = time;

			assert state.tailPosition >= 0 : "Illegal state update. Tail position should not be negative";
			assert state.headPosition <= links.get(state.headLink).length : "Illegal state update. Head position must be smaller than link length";
		}

		// Decide when the next update is necessary

		// (1) start deceleration
		double deccelDist = Double.POSITIVE_INFINITY;
		double nextSpeed = retrieveNextSpeed(state);
		if (nextSpeed < state.targetSpeed) {
			double timeDeccel = (state.targetSpeed - nextSpeed) / state.train.deceleration();

			deccelDist = calcTraveledDist(state.targetSpeed, timeDeccel, -state.train.deceleration());
		}

		// (2) next link needs reservation
		double reserveDist = Double.POSITIVE_INFINITY;
		if (!state.route.get(state.routeIdx).isReserved(state.driver)) {
			// time needed for full stop
			double stopTime = state.allowedMaxSpeed / state.train.deceleration();
			// safety distance
			reserveDist = calcTraveledDist(state.speed, stopTime, -state.train.deceleration());
		}

		// (3) tail link changes
		double tailDist = state.tailPosition;
		// (4) head link changes
		double headDist = links.get(state.headLink).length - state.tailPosition;

		decideNextUpdate(event, time, deccelDist, reserveDist, tailDist, headDist);

		eventsManager.processEvent(new RailsimTrainStateEvent(time, state.driver.getVehicle().getId(),
			state.headPosition, state.tailPosition, state.speed, state.acceleration, state.targetSpeed));
	}

	/**
	 * Calculated updated state based on elapsed time.
	 */
	private static void advancePosition(TrainState state, double elapsed) {
		double accelTime = state.acceleration >= 0
			// Time to reach full speed
			? (state.targetSpeed - state.speed) / state.acceleration
			// Time to reach 0
			: (state.speed) / state.acceleration;

		double dist;
		if (accelTime < elapsed) {

			// Travelled distance under constant acceleration
			dist = calcTraveledDist(state.speed, accelTime, state.acceleration);

			// Remaining time at constant speed
			if (state.acceleration >= 0)
				dist += calcTraveledDist(state.allowedMaxSpeed, accelTime - elapsed, 0);

			// Reach either max speed or 0
			state.speed = state.acceleration >= 0 ? state.allowedMaxSpeed : 0;
			state.acceleration = 0;

		} else {

			// Acceleration was constant the whole time
			dist = calcTraveledDist(state.speed, elapsed, state.acceleration);
			state.speed = state.speed + elapsed * state.acceleration;

		}

		state.headPosition += dist;
		state.tailPosition -= dist;
	}

	/**
	 * Decide which update is the earliest and needs to be the next.
	 */
	private void decideNextUpdate(UpdateEvent event, double time,
								  double deccelDist, double reserveDist,
								  double tailDist, double headDist) {

		double dist;
		if (deccelDist <= reserveDist && deccelDist <= tailDist && deccelDist <= headDist) {
			dist = deccelDist;
			event.type = UpdateEvent.Type.SPEED_CHANGE;
		} else if (reserveDist <= deccelDist && reserveDist <= tailDist && reserveDist <= headDist) {
			dist = reserveDist;
			event.type = UpdateEvent.Type.TRACK_RESERVATION;
		} else if (tailDist <= deccelDist && tailDist <= reserveDist && tailDist <= headDist) {
			dist = tailDist;
			event.type = UpdateEvent.Type.LEAVE_LINK;
		} else {
			dist = headDist;
			event.type = UpdateEvent.Type.ENTER_LINK;
		}

		// dist is the minimum of all supplied distances
		event.plannedTime = time + calcRequiredTime(event.state, dist);
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
				return solveTraveledDist(state.speed, d, state.acceleration);

			} else
				// Time for accel plus remaining dist at max speed
				return accelTime + (dist - d) / state.targetSpeed;

		} else {
			throw new IllegalStateException("Not needed / implemented yet");
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
	 * Allowed speed for the train.
	 */
	private double retrieveAllowedMaxSpeed(TrainState state) {
		// TODO: needs to check the whole part of current route

		return Math.min(
			links.get(state.headLink).getAllowedFreespeed(state.driver),
			links.get(state.tailLink).getAllowedFreespeed(state.driver)
		);
	}

	private double retrieveNextSpeed(TrainState state) {

		// TODO: next link could be a transit stop, then the speed would be 0

		return state.route.get(state.routeIdx).getAllowedFreespeed(state.driver);
	}

}
