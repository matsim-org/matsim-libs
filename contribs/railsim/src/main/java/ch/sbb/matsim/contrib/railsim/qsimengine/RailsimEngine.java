package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.*;

/**
 * Engine to simulate train movement.
 */
final class RailsimEngine implements Steppable {

	private static final Logger log = LogManager.getLogger(RailsimEngine.class);

	/**
	 * Rail links
	 */
	private final Map<Id<Link>, Link> links;

	private final List<TrainState> activeTrains = new ArrayList<>();

	private final Queue<UpdateEvent> updateQueue = new PriorityQueue<>();

	public RailsimEngine(Map<Id<Link>, Link> links) {
		this.links = links;
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


		TrainState state = new TrainState(agent, now, linkId, route);

		activeTrains.add(state);
		updateQueue.add(new UpdateEvent(state, UpdateType.NEXT_HEAD_LINK));

		return true;
	}

	/**
	 * Update the current state of all trains, even if no update would be needed.
	 */
	public void updateAllStates(double time) {
		updateQueue.clear();
		for (TrainState train : activeTrains) {
			// TODO: needs different arguments
//			updateState(time, train);
		}
	}

	private void updateState(double time, UpdateEvent event) {

		TrainState state = event.state;
		MobsimVehicle vehicle = state.driver.getVehicle();

		state.driver.chooseNextLinkId();

//		state.driver.getVehicle().


		// TODO: fixed update very one second
		event.plannedTime += 1;

		updateQueue.add(event);
	}

	/**
	 * Helper class to queue when a state should be updated.
	 */
	private final static class UpdateEvent implements Comparable<UpdateEvent> {

		final TrainState state;
		double plannedTime;
		UpdateType type;

		public UpdateEvent(TrainState state, UpdateType type) {
			this.state = state;
			this.plannedTime = state.timestamp;
			this.type = type;
		}

		@Override
		public int compareTo(UpdateEvent o) {
			int compare = Double.compare(plannedTime, o.plannedTime);

			if (compare == 0)
				return state.driver.getId().compareTo(o.state.driver.getId());

			return compare;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			UpdateEvent that = (UpdateEvent) o;
			return Objects.equals(state, that.state);
		}

		@Override
		public int hashCode() {
			return Objects.hash(state);
		}
	}

	/**
	 * TODO: maybe this enum won't be necessary later
	 */
	private enum UpdateType {
		NEXT_HEAD_LINK
	}

}
