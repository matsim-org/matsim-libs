package ch.sbb.matsim.contrib.railsim.prototype.supply;

import ch.sbb.matsim.contrib.railsim.prototype.supply.circuits.DefaultVehicleCircuitsPlanner;
import ch.sbb.matsim.contrib.railsim.prototype.supply.circuits.NoVehicleCircuitsPlanner;
import ch.sbb.matsim.contrib.railsim.prototype.supply.infrastructure.DefaultInfrastructureRepository;
import ch.sbb.matsim.contrib.railsim.prototype.supply.rollingstock.DefaultRollingStockRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Generate the supply for MATSim railsim extension
 * <p>
 * Create transit schedules and networks for running simulations with the railsim extension, by adding individual transit lines.
 *
 * @author Merlin Unterfinger
 * @author Ihab Kaddoura
 */
public class RailsimSupplyBuilder {

	private static final Logger log = LogManager.getLogger(RailsimSupplyBuilder.class);
	private final Scenario scenario;
	private final InfrastructureRepository infrastructureRepository;
	private final RollingStockRepository rollingStockRepository;
	private final VehicleCircuitsPlanner vehicleCircuitsPlanner;
	private final RailsimSupplyConfigGroup railsimSupplyConfigGroup;
	private final SupplyFactory supplyFactory;
	private final Map<String, StopInfo> stopInfos = new HashMap<>();
	private final Map<String, TransitLineInfo> transitLineInfos = new HashMap<>();
	private final Map<String, VehicleTypeInfo> vehicleTypeInfos = new HashMap<>();
	private final Map<String, DepotInfo> depotInfos = new HashMap<>();
	private final Map<String, SectionPartInfo> sectionPartInfos = new HashMap<>();
	private final Map<String, List<Id<Link>>> sectionParts = new HashMap<>();


	/**
	 * @param scenario a scenario, set the CRS.
	 */
	public RailsimSupplyBuilder(Scenario scenario) {
		this(scenario, new DefaultInfrastructureRepository(scenario), new DefaultRollingStockRepository(scenario), switch (ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimSupplyConfigGroup.class).getCircuitPlanningApproach()) {
			case DEFAULT -> new DefaultVehicleCircuitsPlanner(scenario);
			case NONE -> new NoVehicleCircuitsPlanner();
		});
	}

	/**
	 * @param scenario                 a scenario, set the CRS.
	 * @param infrastructureRepository an infrastructure provider for rail capacities, speed limits and coordinates of depots and stops.
	 * @param rollingStockRepository   a rolling stock provider, to create the vehicle types with attributes (maximum velocity, passenger capacity, acceleration and deceleration).
	 * @param vehicleCircuitsPlanner   a vehicle circuits planner for planning the transit line vehicle allocations.
	 */
	public RailsimSupplyBuilder(Scenario scenario, InfrastructureRepository infrastructureRepository, RollingStockRepository rollingStockRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner) {
		this.scenario = scenario;
		this.infrastructureRepository = infrastructureRepository;
		this.rollingStockRepository = rollingStockRepository;
		this.vehicleCircuitsPlanner = vehicleCircuitsPlanner;
		railsimSupplyConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimSupplyConfigGroup.class);
		supplyFactory = new SupplyFactory(scenario);
	}

	/**
	 * Adds a stop info to the supply.
	 * <p>
	 * Note: StopInfos have only to be added once. Use the RouteStopInfo to define a route profile.
	 *
	 * @param id the unique id or name of the stop.
	 * @param x  the x coordinates of the stop.
	 * @param y  the y coordinates of the stop.
	 */
	public void addStop(String id, double x, double y) {
		// ensure unique stop ids
		if (stopInfos.containsKey(id)) {
			throw new RuntimeException("Stop already existing for id " + id);
		}
		// get stop infos from infrastructure repository
		var stopInfo = infrastructureRepository.getStop(id, x, y);
		x = stopInfo.getCoord().getX();
		y = stopInfo.getCoord().getY();
		final double stopLinkLength = stopInfo.getStopLinkLength();
		// create the stop link
		var tIn = supplyFactory.createNode(id + "_IN", new Coord(x - stopLinkLength, y));
		var tOut = supplyFactory.createNode(id + "_OUT", stopInfo.getCoord());
		var stopLink = supplyFactory.createLink(LinkType.STOP, tIn, tOut, stopLinkLength, stopInfo.getLinkAttributes());
		// put transit stop on the link
		var stop = supplyFactory.createTransitStopFacility(id, stopLink);
		// store stop information
		stopInfo.setStopLink(stopLink);
		stopInfo.setStop(stop);
		this.stopInfos.put(stopInfo.getId(), stopInfo);
	}

	/**
	 * Get a stop info from the supply builder.
	 *
	 * @param id the unique id or name of the stop.
	 * @return returns a StopInfo from the supply if it exists and null otherwise.
	 */
	StopInfo getStop(String id) {
		StopInfo stopInfo = stopInfos.get(id);
		if (stopInfo == null) {
			throw new RuntimeException(String.format("Stop %s not existing, it has to be added first.", id));
		}
		return stopInfo;
	}

	/**
	 * Creates and adds a transit line information to the supply.
	 * <p>
	 * The TransitLineInfo has then to be completed with further stops (addStop), passes (addPass) and departures (addDepartures).
	 *
	 * @param id            the unique name or id of the transit line.
	 * @param vehicleTypeid the unique name or id of the vehicle type used in the transit line.
	 * @param firstStopId   the unique name or id of the first stop.
	 * @param waitingTime   the waiting time in seconds at the first stop.
	 * @return A transit line information object.
	 */
	public TransitLineInfo addTransitLine(String id, String vehicleTypeid, String firstStopId, double waitingTime) {
		// ensure unique transit line ids
		if (transitLineInfos.containsKey(id)) {
			throw new RuntimeException("Transit line already existing for id " + id);
		}
		var firstStopInfo = getStop(firstStopId);
		// get vehicle type info from repository and ensure consistency
		var vehicleTypeInfo = rollingStockRepository.getVehicleType(vehicleTypeid);
		if (!isConsistent(vehicleTypeInfo)) {
			throw new RuntimeException("Vehicle type from repository is not consistent for id " + vehicleTypeInfo.getId());
		}
		// create transit line and matsim object
		var transitLineInfo = new TransitLineInfo(id, new RouteStopInfo(firstStopInfo, waitingTime), vehicleTypeInfo, supplyFactory.createTransitLine(id), this);
		// store transit line info
		transitLineInfos.put(id, transitLineInfo);
		return transitLineInfo;
	}

	private boolean isConsistent(VehicleTypeInfo received) {
		var existing = vehicleTypeInfos.get(received.getId());
		if (existing == null) {
			vehicleTypeInfos.put(received.getId(), received);
			return true;
		}
		return received.equals(existing);
	}

	/**
	 * Build the transit schedule
	 * <p>
	 * Plans the vehicle circuits and creates the needed MATSim objects (transit routes, vehicle types and vehicles).
	 */
	public void build() {
		// build transit lines and create route links
		transitLineInfos.values().forEach(TransitLineInfo::build);
		// plan vehicle circuits
		var vehicleAllocations = vehicleCircuitsPlanner.plan(new ArrayList<>(transitLineInfos.values()));
		// add transit lines with allocated vehicles to the schedule
		for (var entry : vehicleAllocations.entrySet()) {
			final var transitLineInfo = entry.getKey();
			final var vehicleTypeInfo = transitLineInfo.getVehicleTypeInfo();
			final var vehicleAllocationInfo = entry.getValue();
			// construct or get vehicle type
			var vehicleType = supplyFactory.getOrCreateVehicleType(vehicleTypeInfo.getId(), vehicleTypeInfo.getLength(), vehicleTypeInfo.getMaxVelocity(), vehicleTypeInfo.getCapacity(), vehicleTypeInfo.getAttributes());
			// add routes for each type and direction
			for (var routeType : RouteType.values()) {
				// from origin
				var departures = vehicleAllocationInfo.getDepartures(routeType, RouteDirection.FORWARD);
				var vehicleIds = vehicleAllocationInfo.getVehicleIds(routeType, RouteDirection.FORWARD);
				addTransitRouteToTransitLine(transitLineInfo, routeType, vehicleType, RouteDirection.FORWARD, departures, vehicleIds);
				// from destination
				departures = vehicleAllocationInfo.getDepartures(routeType, RouteDirection.REVERSE);
				vehicleIds = vehicleAllocationInfo.getVehicleIds(routeType, RouteDirection.REVERSE);
				addTransitRouteToTransitLine(transitLineInfo, routeType, vehicleType, RouteDirection.REVERSE, departures, vehicleIds);
			}
		}
	}

	/**
	 * Add a depot to the stop
	 * <p>
	 * Creates a TransitStop facility and inLink, depotLink and outLink for the depot. Then constructs a DepotInfo and sets it on the stopInfo.
	 *
	 * <pre>
	 * 		(t_IN)------------------>(t_OUT)
	 * 		^          stationLink         |
	 * 		| outLink               inLink |
	 * 		|           depotLink          v
	 * 		(t_DPT_OUT)<----------(t_DPT_IN)
	 * </pre>
	 *
	 * @param stopInfo the stopInfo to add the depot to.
	 */
	private void addDepotToStop(StopInfo stopInfo) {
		// request depot info from infrastructure repository and ensure consistency
		var depotInfo = infrastructureRepository.getDepot(stopInfo);
		if (!isConsistent(depotInfo)) {
			throw new RuntimeException("Depot is from repository not consistent for id " + depotInfo.getId());
		}
		// create nodes
		log.info("Adding depot to stop " + stopInfo.getId());
		var dIn = supplyFactory.createNode(depotInfo.getId() + "_DPT_IN", depotInfo.getCoord());
		var dOut = supplyFactory.createNode(depotInfo.getId() + "_DPT_OUT", new Coord(depotInfo.getCoord().getX() - depotInfo.getLength(), depotInfo.getCoord().getY()));
		// create depot links
		var depotInLink = supplyFactory.createLink(LinkType.DEPOT, stopInfo.getStopLink().getToNode(), dIn, depotInfo.getInLength(), depotInfo.getInLinkAttributes());
		var depotLink = supplyFactory.createLink(LinkType.DEPOT, dIn, dOut, depotInfo.getLength(), depotInfo.getDepotLinkAttributes());
		var depotOutLink = supplyFactory.createLink(LinkType.DEPOT, dOut, stopInfo.getStopLink().getFromNode(), depotInfo.getOutLength(), depotInfo.getOutLinkAttributes());
		// put transit depot on the link
		var depot = supplyFactory.createTransitStopFacility(stopInfo.getId() + "_DPT", depotLink);
		// add links and stop facility to depot
		depotInfo.setDepot(depot);
		depotInfo.setDepotIn(depotInLink);
		depotInfo.setDepotLink(depotLink);
		depotInfo.setDepotOut(depotOutLink);
		// add depot to transit stop
		stopInfo.setDepotInfo(depotInfo);
	}

	private boolean isConsistent(DepotInfo received) {
		var existing = depotInfos.get(received.getId());
		if (existing == null) {
			depotInfos.put(received.getId(), received);
			return true;
		}
		return received.equals(existing);
	}

	/**
	 * Creates and adds routes for a transit route type to the transit line
	 * <p>
	 * Note: The TransitLine object has to be created and added to the TransitLineInfo object beforehand. The HashMaps containing the departures and vehicles for each route type have to be created
	 * already.
	 *
	 * @param transitLineInfo transit line info container (which holds the transit line).
	 * @param routeType       the type of the route to create (e.g. STATION_TO_DEPOT).
	 * @param vehicleType     the type of the vehicle for the route.
	 * @param routeDirection  the direction of the route.
	 * @param departures      the HashMap which holds the departures for the route type.
	 * @param vehicleIds      the HashMap which holds the vehicle ids for the route type.
	 */
	private void addTransitRouteToTransitLine(TransitLineInfo transitLineInfo, RouteType routeType, VehicleType vehicleType, RouteDirection routeDirection, LinkedList<Double> departures, LinkedList<String> vehicleIds) {
		if (departures == null || vehicleIds == null) {
			log.info(String.format("Omitting route type %s for transit line %s (%s)...", routeType.toString(), transitLineInfo.getId(), routeDirection));
			return;
		}
		if (departures.size() != vehicleIds.size()) {
			throw new RuntimeException(String.format("Failed adding transit route for %s (%s); departures and vehicles must have same size.", transitLineInfo.getId(), routeDirection));
		}
		log.info(String.format("Adding routes (n=%d) for route type %s for transit line %s (%s)...", departures.size(), routeType.toString(), transitLineInfo.getId(), routeDirection));
		// get a copy of the transit line attribute lists (shallow, since only the order matters)
		final LinkedList<RouteStopInfo> routeStopInfos = new LinkedList<>(transitLineInfo.getRouteStopInfos(routeDirection));
		final LinkedList<Double> travelTimes = new LinkedList<>(transitLineInfo.getTravelTimes(routeDirection));
		final LinkedList<Id<Link>> routeLinks = new LinkedList<>(transitLineInfo.getRouteLinks(routeDirection));
		// build route type
		final double depotTravelTime = railsimSupplyConfigGroup.getDepotTravelTime();
		if (routeType == RouteType.DEPOT_TO_STATION || routeType == RouteType.DEPOT_TO_DEPOT) {
			addDepotAtOriginOfRoute(departures, routeStopInfos, travelTimes, routeLinks, depotTravelTime);
		}
		if (routeType == RouteType.STATION_TO_DEPOT || routeType == RouteType.DEPOT_TO_DEPOT) {
			addDepotAtDestinationOfRoute(routeStopInfos, travelTimes, routeLinks, depotTravelTime);
		}
		// construct route
		final List<TransitRouteStop> stops = new ArrayList<>();
		double cumulativeTravelTime = 0.;
		// first stop
		stops.add(supplyFactory.createTransitTerminalStop(routeStopInfos.get(0).getTransitStop(), cumulativeTravelTime, true));
		// intermediate stops
		for (int stopCounter = 1; stopCounter <= routeStopInfos.size() - 2; stopCounter++) {
			// increase cumulative travel time until arrival
			cumulativeTravelTime += travelTimes.get(stopCounter - 1);
			// increase cumulative travel time by waiting time until departure
			double departureTime = cumulativeTravelTime + routeStopInfos.get(stopCounter).getWaitingTime();
			TransitRouteStop transitStop = supplyFactory.createTransitRouteStop(routeStopInfos.get(stopCounter).getTransitStop(), cumulativeTravelTime, departureTime, true);
			stops.add(transitStop);
			// set cumulative travel time to departure time
			cumulativeTravelTime = departureTime;
		}
		// final stop
		cumulativeTravelTime += travelTimes.getLast();
		stops.add(supplyFactory.createTransitTerminalStop(routeStopInfos.get(routeStopInfos.size() - 1).getTransitStop(), cumulativeTravelTime, false));
		// define route and add to transit line
		final String routeId = String.format("%s_%s_%s", transitLineInfo.getId(), routeDirection.getAbbreviation(), routeType);
		TransitRoute route = supplyFactory.createTransitRoute(transitLineInfo.getTransitLine(), routeId, routeLinks, stops);
		// add departures and vehicles
		Iterator<String> iter = vehicleIds.iterator();
		int depCounter = 0;
		for (Double departureTime : departures) {
			Departure departure = supplyFactory.createDeparture(String.valueOf(depCounter), departureTime);
			departure.setVehicleId(supplyFactory.getOrCreateVehicle(vehicleType, iter.next()).getId());
			route.addDeparture(departure);
			depCounter++;
		}
	}

	/**
	 * Adds a depot at the origin of the route
	 * <p>
	 * The departure time is decreased by the depot travel time.
	 *
	 * @param departures      the departures when the route is started.
	 * @param routeStopInfos  the ordered stops of the route.
	 * @param travelTimes     the ordered travel times between the stops.
	 * @param routeLinks      the ordered links between the stops of the complete route.
	 * @param depotTravelTime the travel time to reach the depot.
	 */
	private void addDepotAtOriginOfRoute(LinkedList<Double> departures, LinkedList<RouteStopInfo> routeStopInfos, LinkedList<Double> travelTimes, LinkedList<Id<Link>> routeLinks, double depotTravelTime) {
		final var origStop = routeStopInfos.getFirst().getStopInfo();
		final double waitingTime = routeStopInfos.getFirst().getWaitingTime();
		if (origStop.hasNoDepot()) {
			addDepotToStop(origStop);
		}
		// decrease departures, since the first stop is set to the depot
		for (int i = 0; i < departures.size(); i++) {
			double departure = departures.removeFirst();
			departures.addLast(departure - (depotTravelTime + waitingTime));
		}
		// add origin depot stop and links
		var origDepo = origStop.getDepotInfo();
		routeStopInfos.addFirst(new RouteStopInfo(origDepo.getDepotLink(), origDepo.getDepot(), 0.));
		routeLinks.addAll(0, List.of(origDepo.getDepotLink().getId(), origDepo.getDepotOut().getId()));
		travelTimes.addFirst(depotTravelTime);
	}

	/**
	 * Adds a depot at the destination of the route
	 *
	 * @param routeStopInfos  the ordered stops of the route.
	 * @param travelTimes     the ordered travel times between the stops.
	 * @param routeLinks      the ordered links between the stops of the complete route.
	 * @param depotTravelTime the travel time to reach the depot.
	 */
	private void addDepotAtDestinationOfRoute(LinkedList<RouteStopInfo> routeStopInfos, LinkedList<Double> travelTimes, LinkedList<Id<Link>> routeLinks, double depotTravelTime) {
		var destStop = routeStopInfos.getLast().getStopInfo();
		if (destStop.hasNoDepot()) {
			addDepotToStop(destStop);
		}
		// add destination depot stop and links
		var destDepo = destStop.getDepotInfo();
		routeStopInfos.add(new RouteStopInfo(destDepo.getDepotLink(), destDepo.getDepot(), 0.));
		routeLinks.addAll(List.of(destDepo.getDepotIn().getId(), destDepo.getDepotLink().getId()));
		travelTimes.add(depotTravelTime);
	}

	/**
	 * Connect two stops
	 * <p>
	 * Connects two stops (or passes) of a transit line in the transit network .
	 *
	 * @param fromStop starting stop.
	 * @param toStop   destination stop.
	 * @return A list of connecting links.
	 */
	List<Id<Link>> connectStops(StopInfo fromStop, StopInfo toStop) {
		String id = String.format("%s_%s", fromStop.getId(), toStop.getId());
		var sectionPart = sectionParts.get(id);
		if (sectionPart != null) {
			log.debug("Section part already existing, skipping {}", id);
			return sectionPart;
		}
		// request section part information from infrastructure repository and ensure consistency
		var sectionPartInfo = infrastructureRepository.getSectionPart(fromStop, toStop);
		if (!isConsistent(sectionPartInfo)) {
			throw new RuntimeException("Section part from repository is not consistent for id " + createSectionPartInfoId(sectionPartInfo));
		}
		// create link for each section until last
		var nodePrefix = String.format("%s_", id);
		var links = new ArrayList<Id<Link>>();
		var sectionSegmentInfos = sectionPartInfo.getSectionSegmentInfos();
		var currentNode = fromStop.getStopLink().getToNode();
		var currentSegmentInfo = sectionSegmentInfos.get(0);
		int count = 0;
		for (int i = 0; i < sectionSegmentInfos.size() - 1; i++) {
			var nextNode = supplyFactory.createNode(nodePrefix + count, sectionSegmentInfos.get(i + 1).getToCoord());
			var nextSegmentInfo = sectionSegmentInfos.get(i + 1);
			links.add(supplyFactory.createLink(LinkType.ROUTE, currentNode, nextNode, currentSegmentInfo.getLength(), currentSegmentInfo.getLinkAttributes()).getId());
			currentNode = nextNode;
			currentSegmentInfo = nextSegmentInfo;
			count++;
		}
		// create and add last link
		var lastStop = toStop.getStopLink().getFromNode();
		var lastSegmentInfo = sectionSegmentInfos.get(sectionSegmentInfos.size() - 1);
		links.add(supplyFactory.createLink(LinkType.ROUTE, currentNode, lastStop, lastSegmentInfo.getLength(), lastSegmentInfo.getLinkAttributes()).getId());
		// store links of section part
		sectionParts.put(id, links);
		return links;
	}

	private boolean isConsistent(SectionPartInfo received) {
		var id = createSectionPartInfoId(received);
		var existing = sectionPartInfos.get(id);
		if (existing == null) {
			sectionPartInfos.put(id, received);
			return true;
		}
		return received.equals(existing);
	}

	private static String createSectionPartInfoId(SectionPartInfo sectionPartInfo) {
		return String.format("%s_%s", sectionPartInfo.getFromStopId(), sectionPartInfo.getToStopId());
	}

	public Scenario getScenario() {
		return scenario;
	}
}
