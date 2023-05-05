package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Stores the mutable current state of a train.
 */
final class TrainState {

	/**
	 * Driver of the train.
	 */
	final MobsimDriverAgent driver;

	/**
	 * Train specific parameters.
	 */
	final TrainInfo info;

	/**
	 * Route of this train.
	 */
	final List<RailLink> route;

	/**
	 * Current index in the list of route links.
	 */
	int routeIdx;

	/**
	 * Time of this state.
	 */
	double timestamp;

	/**
	 * The link the where the head of the train is on. Can be null if not yet departed.
	 */
	@Nullable
	Id<Link> headLink;

	/**
	 * Current link the very end of the train is on. Can be null if not yet departed.
	 */
	@Nullable
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

	TrainState(MobsimDriverAgent driver, TrainInfo info, double timestamp, @Nullable Id<Link> linkId, List<RailLink> route) {
		this.driver = driver;
		this.info = info;
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
