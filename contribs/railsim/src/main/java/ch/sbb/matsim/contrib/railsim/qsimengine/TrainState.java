package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * Stores the mutable current state of a train.
 */
final class TrainState {

	/**
	 * Driver of the train.
	 */
	final MobsimDriverAgent driver;

	/**
	 * Route of this train.
	 */
	final NetworkRoute route;

	/**
	 * Current index in the list of route links.
	 */
	int routeIdx;

	/**
	 * Time of this state.
	 */
	double timestamp;


	/**
	 * The link the where the head of the train is on.
	 */
	Id<Link> headLink;

	/**
	 * Current link the very end of the train is on.
	 */
	Id<Link> tailLink;

	/**
	 * Current allowed speed, which depends on train type, links, but not on other trains or speed needed to stop.
	 */
	double allowedMaxSpeed;

	/**
	 * Distance in meters away from the {@code headLink}s {@code fromNode}.
	 */
	double headPosition;

	/**
	 * Speed in m/s.
	 */
	double speed;

	TrainState(MobsimDriverAgent driver, double timestamp, Id<Link> linkId, NetworkRoute route) {
		this.driver = driver;
		this.route = route;
		this.timestamp = timestamp;
		this.headLink = linkId;
		this.tailLink = linkId;
	}

	@Override
	public String toString() {
		return "TrainState{" +
			"driver=" + driver.getId() +
			", timestamp=" + timestamp +
			", headLink=" + headLink +
			", tailLink=" + tailLink +
			", allowedMaxSpeed=" + allowedMaxSpeed +
			", headPosition=" + headPosition +
			", speed=" + speed +
			'}';
	}
}
