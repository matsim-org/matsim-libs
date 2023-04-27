package ch.sbb.matsim.contrib.railsim.prototype.supply;

import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Route stop information
 *
 * @author Merlin Unterfinger
 */
public class RouteStopInfo {

	private final StopInfo stopInfo;
	private final Link link;
	private final TransitStopFacility transitStop;
	private final double waitingTime;

	/**
	 * Create a RouteStopInfo
	 * <p>
	 * A route stop info is a stop on a previously defined StopInfo on a transit line.
	 *
	 * @param stopInfo    the stop.
	 * @param waitingTime the time to waiting at stop, is 0. for pass.
	 */
	RouteStopInfo(StopInfo stopInfo, double waitingTime) {
		this.stopInfo = stopInfo;
		this.link = stopInfo.getStopLink();
		this.transitStop = stopInfo.getStop();
		this.waitingTime = waitingTime;
	}

	/**
	 * @param link                the link where the depot sits on.
	 * @param transitStopFacility the depot transit stop facility.
	 * @param waitingTime         time to wait in depot.
	 */
	RouteStopInfo(Link link, TransitStopFacility transitStopFacility, double waitingTime) {
		this.stopInfo = null;
		this.link = link;
		this.transitStop = transitStopFacility;
		this.waitingTime = waitingTime;
	}

	/**
	 * Checks if the route stop info is a stopping pass.
	 *
	 * @return true if stopping, else false (non-stopping pass).
	 */
	public boolean isStoppingPass() {
		return waitingTime > 0.;
	}

	public Link getLink() {
		return link;
	}

	public TransitStopFacility getTransitStop() {
		return transitStop;
	}

	public double getWaitingTime() {
		return waitingTime;
	}

	public StopInfo getStopInfo() {
		return stopInfo;
	}
}
