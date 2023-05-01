package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteDirection;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteStopInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.TransitLineInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.misc.Time;

import java.util.LinkedList;

/**
 * A route departure event is the first departure of a route at the origin station.
 *
 * @author Merlin Unterfinger
 */
class RouteDepartureEvent implements Comparable<RouteDepartureEvent> {

	private static final Logger log = LogManager.getLogger(RouteDepartureEvent.class);
	private final double departureTime;
	private final double totalRouteTime;
	private final double destinationWaitingTime;
	private final double circuitMaxWaitingTime;
	private final RouteDirection routeDirection;
	private final TransitLineInfo transitLineInfo;
	private final LinkedList<RouteDepartureEvent> oppositeDepartures;

	/**
	 * This time is important for sorting, we want to process the departures according to the time when they are ready again for another departure.
	 */
	private final double readyAgainTime;

	/**
	 * @param departureTime          the departure time.
	 * @param totalRouteTime         the total time until the destination is reached (without waiting times at origin and destination).
	 * @param destinationWaitingTime the waiting time at the destination station.
	 * @param circuitMaxWaitingTime  the maximum time to wait at the destination for a next circuit departure, otherwise the vehicle is sent to the depot.
	 * @param routeDirection         the route direction.
	 * @param transitLineInfo        the transit line information.
	 * @param oppositeDepartures     the departures from the same transit line in the opposite direction.
	 */
	RouteDepartureEvent(double departureTime, double totalRouteTime, double destinationWaitingTime, double circuitMaxWaitingTime, RouteDirection routeDirection, TransitLineInfo transitLineInfo, LinkedList<RouteDepartureEvent> oppositeDepartures) {
		this.departureTime = departureTime;
		this.totalRouteTime = totalRouteTime;
		this.destinationWaitingTime = destinationWaitingTime;
		this.circuitMaxWaitingTime = circuitMaxWaitingTime;
		this.routeDirection = routeDirection;
		this.transitLineInfo = transitLineInfo;
		this.oppositeDepartures = oppositeDepartures;
		this.readyAgainTime = departureTime + totalRouteTime + destinationWaitingTime;
	}

	/**
	 * Search and get the next possible departures from the opposite station.
	 *
	 * @return the next planned departure or null.
	 */
	public RouteDepartureEvent getNextPossibleOppositeDeparture() {
		// delete all opposite departures which are departing before this current departure would get ready again in the destination stop queue.
		// we can do this, since the denatures are sorted according to arrivalTime, therefore no sooner readyAgain vehicle in the stop queue is possible than the current one.
		// e.g. ready again at 11:05 at destination, opposite departure at 11:00 could be deleted.
		while (!oppositeDepartures.isEmpty() && oppositeDepartures.getFirst().getDepartureTime() < readyAgainTime) {
			RouteDepartureEvent oppositeDeparture = oppositeDepartures.removeFirst();
			log.info(String.format("DELETING - %s", renderDepartureInfo(oppositeDeparture)));
		}
		// get the next departure that is departing after the current vehicle is ready again and check if it is in the possible time window for departure
		if (!oppositeDepartures.isEmpty()) {
			RouteDepartureEvent oppositeDeparture = oppositeDepartures.getFirst();
			log.info(String.format("CHECKING - %s", renderDepartureInfo(oppositeDeparture)));
			if (oppositeDeparture.departureTime >= this.readyAgainTime && oppositeDeparture.departureTime <= this.readyAgainTime + circuitMaxWaitingTime) {
				// match! Remove route departure event from queue and return
				oppositeDeparture = oppositeDepartures.removeFirst();
				log.info(String.format("SELECTED - %s", renderDepartureInfo(oppositeDeparture)));
				return oppositeDeparture;
			}
		}
		return null;
	}

	/**
	 * Sort according to ready again time (=arrival time + destinationWaitingTime).
	 *
	 * @param other the object to be compared.
	 * @return comparison.
	 */
	@Override
	public int compareTo(RouteDepartureEvent other) {
		return Double.compare(this.readyAgainTime, other.readyAgainTime);
	}

	/**
	 * Render route departure event info to string.
	 *
	 * @param oppositeDeparture the next opposite departure.
	 * @return the departure event information as string.
	 */
	private String renderDepartureInfo(RouteDepartureEvent oppositeDeparture) {
		return String.format("departure: %s, ready: %s, maxWaitingTime: %s, oppositeDeparture: %s", Time.writeTime(this.departureTime, Time.TIMEFORMAT_HHMMSS), Time.writeTime(this.readyAgainTime, Time.TIMEFORMAT_HHMMSS), Time.writeTime(this.readyAgainTime + circuitMaxWaitingTime, Time.TIMEFORMAT_HHMMSS), Time.writeTime(oppositeDeparture.departureTime, Time.TIMEFORMAT_HHMMSS));
	}

	public RouteStopInfo getOrigin() {
		return transitLineInfo.getOrigin(routeDirection);
	}

	public RouteStopInfo getDestination() {
		return transitLineInfo.getDestination(routeDirection);
	}

	public double getArrivalTime() {
		return departureTime + totalRouteTime;
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public double getTotalRouteTime() {
		return totalRouteTime;
	}

	public double getDestinationWaitingTime() {
		return destinationWaitingTime;
	}

	public RouteDirection getRouteDirection() {
		return routeDirection;
	}

	public TransitLineInfo getTransitLineInfo() {
		return transitLineInfo;
	}

	public LinkedList<RouteDepartureEvent> getOppositeDepartures() {
		return oppositeDepartures;
	}

	public double getReadyAgainTime() {
		return readyAgainTime;
	}
}
