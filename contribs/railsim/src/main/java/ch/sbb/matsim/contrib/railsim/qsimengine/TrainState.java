package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

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
	 * Transit agent, if this is a pt transit.
	 */
	@Nullable
	final TransitDriverAgent pt;

	/**
	 * Next transit stop.
	 */
	@Nullable
	TransitStopFacility nextStop;

	/**
	 * Train specific parameters.
	 */
	final TrainInfo train;

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
	 * Trains target speed.
	 */
	double targetSpeed;

	/**
	 * Current allowed speed, which depends on train type, links, but not on other trains or speed needed to stop.
	 */
	double allowedMaxSpeed;

	/**
	 * Distance in meters away from the {@code headLink}s {@code fromNode}.
	 */
	double headPosition;

	/**
	 * * Distance in meters away from the {@code tailLink}s {@code fromNode}.
	 */
	double tailPosition;

	/**
	 * Speed in m/s.
	 */
	double speed;

	/**
	 * Current Acceleration, (or deceleration if negative)
	 */
	double acceleration;

	TrainState(MobsimDriverAgent driver, TrainInfo train, double timestamp, @Nullable Id<Link> linkId, List<RailLink> route) {
		this.driver = driver;
		this.pt = driver instanceof TransitDriverAgent ptDriver ? ptDriver : null;
		this.nextStop = pt != null ? pt.getNextTransitStop() : null;
		this.train = train;
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
			", targetSpeed=" + targetSpeed +
			", allowedMaxSpeed=" + allowedMaxSpeed +
			", headPosition=" + headPosition +
			", tailPosition=" + tailPosition +
			", speed=" + speed +
			", acceleration=" + acceleration +
			'}';
	}


	boolean isRouteAtEnd() {
		return routeIdx >= route.size();
	}

	/**
	 * Check whether to stop at certain link.
	 */
	boolean isStop(Id<Link> link) {
		return nextStop != null && nextStop.getLinkId().equals(link);
	}

	RailsimTrainStateEvent asEvent(double time) {
		return new RailsimTrainStateEvent(time, driver.getVehicle().getId(),
			headLink, headPosition,
			tailLink, tailPosition,
			speed, acceleration, targetSpeed);
	}

}
