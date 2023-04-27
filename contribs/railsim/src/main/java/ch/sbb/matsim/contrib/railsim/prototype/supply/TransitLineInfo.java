package ch.sbb.matsim.contrib.railsim.prototype.supply;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transit line information.
 * <p>
 * Create an instance of this class to add transit lines and routes in both directions to the transit schedule using addTransitLineWithVehicleCircuits.
 *
 * @author Merlin Unterfinger
 */
public class TransitLineInfo {

	private static final Logger log = LogManager.getLogger(TransitLineInfo.class);

	private static final double NO_STOP_TIME = 0.;
	private final RailsimSupplyBuilder supplyBuilder;
	// line
	private final String id;
	private final VehicleTypeInfo vehicleTypeInfo;
	private final TransitLine transitLine;
	// route profile
	private final EnumMap<RouteDirection, List<RouteStopInfo>> routeStopInfos = new EnumMap<>(RouteDirection.class);
	private final EnumMap<RouteDirection, List<Id<Link>>> routeLinks = new EnumMap<>(RouteDirection.class);
	private final EnumMap<RouteDirection, List<Double>> travelTimes = new EnumMap<>(RouteDirection.class);
	// route departure
	private final EnumMap<RouteDirection, List<Double>> departures = new EnumMap<>(RouteDirection.class);
	private boolean built;

	/**
	 * Constructs a new transit line object and makes shallow copies of the input lists to prevent from outside manipulations. Reverses the route stop infos and travel times.
	 *
	 * @param id              unique name or id of the transit line.
	 * @param firstRouteStop  unique name or id of the first stop in the route.
	 * @param vehicleTypeInfo the vehicle type information.
	 * @param supplyBuilder   the supply builder the transit line resides on.
	 */
	TransitLineInfo(String id, RouteStopInfo firstRouteStop, VehicleTypeInfo vehicleTypeInfo, TransitLine transitLine, RailsimSupplyBuilder supplyBuilder) {
		this.id = id;
		this.supplyBuilder = supplyBuilder;
		this.vehicleTypeInfo = vehicleTypeInfo;
		this.transitLine = transitLine;
		routeStopInfos.put(RouteDirection.FORWARD, new ArrayList<>());
		travelTimes.put(RouteDirection.FORWARD, new ArrayList<>());
		departures.put(RouteDirection.FORWARD, new ArrayList<>());
		departures.put(RouteDirection.REVERSE, new ArrayList<>());
		built = false;
		addFirstStop(firstRouteStop);
	}

	public void addStop(String stopId, double travelTime, double waitingTime) {
		if (built) {
			throw new RuntimeException(String.format("Cannot add stop %s to already built TransitLineInfo %s", stopId, id));
		}
		StopInfo stopInfo = supplyBuilder.getStop(stopId);
		this.routeStopInfos.get(RouteDirection.FORWARD).add(new RouteStopInfo(stopInfo, waitingTime));
		this.travelTimes.get(RouteDirection.FORWARD).add(travelTime);
	}

	public void addPass(String stopId) {
		if (built) {
			throw new RuntimeException(String.format("Cannot add pass %s to already built TransitLineInfo %s", stopId, id));
		}
		StopInfo stopInfo = supplyBuilder.getStop(stopId);
		this.routeStopInfos.get(RouteDirection.FORWARD).add(new RouteStopInfo(stopInfo, NO_STOP_TIME));
	}

	public void addDeparture(RouteDirection routeDirection, double departureTime) {
		if (built) {
			throw new RuntimeException(String.format("Cannot add departure %s to already built TransitLineInfo %s", Time.writeTime(departureTime, Time.TIMEFORMAT_HHMMSS), id));
		}
		this.departures.get(routeDirection).add(departureTime);
	}

	/**
	 * Build the transit line
	 * <p>
	 * After calling this method no further stops, passes or departures can be added to the transit line.
	 */
	void build() {
		if (departures.get(RouteDirection.FORWARD).isEmpty() && departures.get(RouteDirection.REVERSE).isEmpty())
			throw new RuntimeException("Transit line needs at least one departure");
		if (routeStopInfos.get(RouteDirection.FORWARD).size() < 2)
			throw new RuntimeException("Transit line needs at least one additional stop to origin stop");
		log.info("Building TransitLineInfo {}", id);
		sortDepartures();
		initializeReverseDirection();
		createLinksAndConnectStops();
		logEntry();
		built = true;
	}

	private void addFirstStop(RouteStopInfo firstRouteStop) {
		routeStopInfos.get(RouteDirection.FORWARD).add(firstRouteStop);
	}

	private void sortDepartures() {
		Collections.sort(departures.get(RouteDirection.FORWARD));
		Collections.sort(departures.get(RouteDirection.REVERSE));
	}

	private void initializeReverseDirection() {
		this.routeStopInfos.put(RouteDirection.REVERSE, new ArrayList<>(this.routeStopInfos.get(RouteDirection.FORWARD)));
		this.travelTimes.put(RouteDirection.REVERSE, new ArrayList<>(this.travelTimes.get(RouteDirection.FORWARD)));
		Collections.reverse(this.routeStopInfos.get(RouteDirection.REVERSE));
		Collections.reverse(this.travelTimes.get(RouteDirection.REVERSE));
	}

	/**
	 * Create transit line links in both directions
	 */
	private void createLinksAndConnectStops() {
		final var routeLinks = new ArrayList<Id<Link>>();
		final var routeLinksReverse = new ArrayList<Id<Link>>();
		final var routeStopInfos = getRouteStopInfos(RouteDirection.FORWARD);
		// F: iterate over route stop infos and create links
		StopInfo nextStop = null;
		for (int i = 0; i < routeStopInfos.size() - 1; i++) {
			var currentStop = routeStopInfos.get(i).getStopInfo();
			nextStop = routeStopInfos.get(i + 1).getStopInfo();
			// add terminal link
			routeLinks.add(currentStop.getStopLink().getId());
			// add links to next stop
			routeLinks.addAll(supplyBuilder.connectStops(currentStop, nextStop));
		}
		assert nextStop != null;
		routeLinks.add(nextStop.getStopLink().getId());
		// R: iterate over route stop infos and create links
		nextStop = null;
		for (int i = routeStopInfos.size() - 1; i > 0; i--) {
			var currentStop = routeStopInfos.get(i).getStopInfo();
			nextStop = routeStopInfos.get(i - 1).getStopInfo();
			// add terminal link
			routeLinksReverse.add(currentStop.getStopLink().getId());
			// add links to next stop
			routeLinksReverse.addAll(supplyBuilder.connectStops(currentStop, nextStop));
		}
		assert nextStop != null;
		routeLinksReverse.add(nextStop.getStopLink().getId());
		// set route links on transit line info
		setRouteLinks(RouteDirection.FORWARD, routeLinks);
		setRouteLinks(RouteDirection.REVERSE, routeLinksReverse);
		// remove route stop infos with a waiting time of 0 minutes, since they are no stops, but needed for route link creation
		setRouteStopInfos(RouteDirection.FORWARD, getRouteStopInfos(RouteDirection.FORWARD).stream().filter(RouteStopInfo::isStoppingPass).collect(Collectors.toList()));
		setRouteStopInfos(RouteDirection.REVERSE, getRouteStopInfos(RouteDirection.REVERSE).stream().filter(RouteStopInfo::isStoppingPass).collect(Collectors.toList()));
	}

	public RouteStopInfo getOrigin(RouteDirection routeDirection) {
		return routeStopInfos.get(routeDirection).get(0);
	}

	public RouteStopInfo getDestination(RouteDirection routeDirection) {
		var directedRouteStopInfos = routeStopInfos.get(routeDirection);
		return directedRouteStopInfos.get(directedRouteStopInfos.size() - 1);
	}

	public List<RouteStopInfo> getRouteStopInfos(RouteDirection routeDirection) {
		return routeStopInfos.get(routeDirection);
	}

	void setRouteStopInfos(RouteDirection routeDirection, List<RouteStopInfo> routeStopInfos) {
		this.routeStopInfos.put(routeDirection, routeStopInfos);
	}

	List<Id<Link>> getRouteLinks(RouteDirection routeDirection) {
		return routeLinks.get(routeDirection);
	}

	void setRouteLinks(RouteDirection routeDirection, List<Id<Link>> routeLinks) {
		this.routeLinks.put(routeDirection, routeLinks);
	}

	public List<Double> getTravelTimes(RouteDirection routeDirection) {
		return travelTimes.get(routeDirection);
	}

	public List<Double> getDepartures(RouteDirection routeDirection) {
		return departures.get(routeDirection);
	}

	private void logEntry() {
		var sb = new StringBuilder("Transit line log entry of ").append(id).append("\n");
		// departures
		sb.append("Departures:\n").append("- FORWARD: ").append(this.departures.get(RouteDirection.FORWARD).stream().map(d -> Time.writeTime(d, Time.TIMEFORMAT_HHMMSS)).collect(Collectors.joining(", "))).append("\n- REVERSE: ").append(this.departures.get(RouteDirection.REVERSE).stream().map(d -> Time.writeTime(d, Time.TIMEFORMAT_HHMMSS)).collect(Collectors.joining(", ")));
		// route stops
		sb.append("\nRoute stops and passes:\n").append(this.routeStopInfos.get(RouteDirection.FORWARD).stream().map(r -> String.format("%s (%s)", r.getStopInfo().getId(), Time.writeTime(r.getWaitingTime(), Time.TIMEFORMAT_HHMMSS))).collect(Collectors.joining(", ")));
		// route stops
		sb.append("\nTravel times:\n").append(this.travelTimes.get(RouteDirection.FORWARD).stream().map(d -> Time.writeTime(d, Time.TIMEFORMAT_HHMMSS)).collect(Collectors.joining(", ")));
		log.info(sb);
	}

	public String getId() {
		return id;
	}

	TransitLine getTransitLine() {
		return transitLine;
	}

	public VehicleTypeInfo getVehicleTypeInfo() {
		return vehicleTypeInfo;
	}
}
