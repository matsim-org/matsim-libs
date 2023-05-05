package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
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

	public RailsimEngine(EventsManager eventsManager, RailsimConfigGroup config,  Map<Id<Link>, ? extends Link> network) {
		this.eventsManager = eventsManager;
		this.config = config;
		this.links = new IdMap<>(Link.class, network.size());
		for (Map.Entry<Id<Link>, ? extends Link> e : network.entrySet()) {
			this.links.put(e.getKey(), new RailLink(e.getValue()));
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

		for (TrainState train : activeTrains) {
			updateState(time, new UpdateEvent(train, UpdateEvent.Type.POSITION));
		}
	}

	private void updateState(double time, UpdateEvent event) {

		// Do different updates depending on the type
		switch (event.type) {
			case DEPARTURE -> updateDeparture(time, event);
			case POSITION -> updatePosition(time, event);
			case ENTER_LINK -> enterLink(time, event);
		}

		if (event.type != UpdateEvent.Type.IDLE) {
			updateQueue.add(event);
		}
	}

	private void enterLink(double time, UpdateEvent event) {

		TrainState state = event.state;

		// Get link and increment
		state.headLink = state.route.get(state.routeIdx++).getId();

		state.driver.notifyMoveOverNode(state.headLink);
		eventsManager.processEvent(new LinkEnterEvent(time, state.driver.getVehicle().getId(), state.headLink));

		RailLink link = links.get(state.headLink);

		int track = link.blockTrack(state.driver);
		eventsManager.processEvent(new RailsimLinkStateChangeEvent(time, state.headLink, TrackState.BLOCKED, track));

		// TODO: now block links in advance, check when tracks in the back can be released


		// TODO: smarter position updates
		event.type = UpdateEvent.Type.POSITION;
		event.plannedTime += 1;
	}

	private void updateDeparture(double time, UpdateEvent event) {

		TrainState state = event.state;
		state.timestamp = time;

		RailLink firstLink = state.route.get(0);

		// for departure only the track has to be free and no tracks in advance
		if (firstLink.hasFreeTrack()) {
			int track = firstLink.reserveTrack(state.driver);
			eventsManager.processEvent(new RailsimLinkStateChangeEvent(time, firstLink.getId(), TrackState.RESERVED, track));

			// Call enter link logic immediately
			enterLink(time, event);
		}

		// vehicle will wait implicitly
		// TODO: should be done via callback later
		event.plannedTime += 1;
	}

	/**
	 * Update position within a link.
	 */
	private void updatePosition(double time, UpdateEvent event) {

		// TODO: calculate the new position depending on the last and time gone by

		// TODO: fixed update every one second
		event.plannedTime += 1;
	}
}
