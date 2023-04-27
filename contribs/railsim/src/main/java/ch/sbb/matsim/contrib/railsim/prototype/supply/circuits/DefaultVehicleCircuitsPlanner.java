package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import ch.sbb.matsim.contrib.railsim.prototype.supply.RailsimSupplyConfigGroup;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteDirection;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteStopInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteType;
import ch.sbb.matsim.contrib.railsim.prototype.supply.TransitLineInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleAllocationInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleCircuitsPlanner;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleTypeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Default implementation of the vehicle circuits planner
 * <p>
 * Uses the RouteType and RouteDirection enums to determine the type of route and the direction of travel. The RouteType enum defines the possible route types for a transit line in two directions. The
 * RouteDirection enum defines the direction of the route, either starting from the origin or the destination (F: forward, R: Reverse).
 *
 * @author Merlin Unterfinger
 */
public class DefaultVehicleCircuitsPlanner implements VehicleCircuitsPlanner {

	private static final Logger log = LogManager.getLogger(DefaultVehicleCircuitsPlanner.class);
	private final double depotTravelTime;
	private final double circuitMaxWaitingTime;
	private final HashMap<TransitLineInfo, TransitLineVehicleAllocation> allocations = new HashMap<>();
	private final LinkedList<RouteDepartureEvent> routeDepartureEventQueue = new LinkedList<>();
	private final VehicleFleet vehicleFleet = new VehicleFleet();
	private int fromDepotCount = 0;
	private int toDepotCount = 0;

	/**
	 * @param scenario the scenario with the railsim supply config group.
	 */
	public DefaultVehicleCircuitsPlanner(Scenario scenario) {
		var config = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimSupplyConfigGroup.class);
		this.depotTravelTime = config.getDepotTravelTime();
		this.circuitMaxWaitingTime = config.getCircuitMaxWaitingTime();
	}

	/**
	 * Plan the day with vehicle circuits
	 * <p>
	 * <ul>
	 *   <li>Creates a TransitLineVehicleAllocation object per TransitLineInfo.</li>
	 *   <li>Sorts the departures according to arrivals</li>
	 *   <li>Loop through the day and creates vehicle circuits</li>
	 * </ul>
	 */
	@Override
	public Map<TransitLineInfo, VehicleAllocationInfo> plan(List<TransitLineInfo> transitLineInfos) {
		// reset state
		fromDepotCount = 0;
		toDepotCount = 0;
		routeDepartureEventQueue.clear();
		vehicleFleet.clear();
		allocations.clear();
		// create departure events, sorted according to arrival time
		sortAllDepartures(transitLineInfos);
		// process departure events
		for (RouteDepartureEvent routeDepartureEvent : routeDepartureEventQueue) {
			handleDepartureEvent(routeDepartureEvent);
		}
		// run consistency checks
		log.info("Checking planned circuits for consistency...");
		if (Stream.of(checkDepotCounts(), checkEmptyStopQueues(), checkDepotVehicleCounts()).anyMatch(check -> !check)) {
			throw new RuntimeException("Not all consistency checks passed! Aborting...");
		}
		log.info("PASSED - vehicle circuits are consistent");
		// convert to interface type
		return new HashMap<>(allocations);
	}

	/**
	 * Handles a chronologically ordered route departure event.
	 *
	 * @param routeDepartureEvent the current departure event.
	 */
	private void handleDepartureEvent(RouteDepartureEvent routeDepartureEvent) {
		log.info(String.format("Handling departure event: %s %s -> %s (departure: %s, arrival: %s, ready: %s)", routeDepartureEvent.getTransitLineInfo().getId(), routeDepartureEvent.getOrigin().getStopInfo().getId(), routeDepartureEvent.getDestination().getStopInfo().getId(), Time.writeTime(routeDepartureEvent.getDepartureTime(), Time.TIMEFORMAT_HHMMSS), Time.writeTime(routeDepartureEvent.getArrivalTime(), Time.TIMEFORMAT_HHMMSS), Time.writeTime(routeDepartureEvent.getReadyAgainTime(), Time.TIMEFORMAT_HHMMSS)));
		// get next vehicle from stop or depot, needs: transitLineInfo, routeDirection, departureTime
		final VehicleFleet.AvailableVehicle availableVehicle = vehicleFleet.getNextVehicleForDeparture(routeDepartureEvent.getTransitLineInfo(), routeDepartureEvent.getRouteDirection(), routeDepartureEvent.getDepartureTime());
		final boolean fromDepot = availableVehicle.fromDepot();
		final Vehicle vehicle = availableVehicle.vehicle();
		// increase counter for validation
		if (fromDepot) {
			fromDepotCount++;
		}
		// check if destination is depot and send vehicle to depot
		RouteDepartureEvent oppositeDeparture = routeDepartureEvent.getNextPossibleOppositeDeparture();
		boolean toDepot = false;
		if (oppositeDeparture == null) {
			// calculate time it takes to drive to the depot and back:
			// arrival time at stop + waiting time at stop + travel time to and from depot (2x) + waiting time at stop.
			final double readyFromDepotTime = routeDepartureEvent.getArrivalTime() + 2 * routeDepartureEvent.getDestinationWaitingTime() + 2 * depotTravelTime;
			toDepot = true;
			// increase counter for validation
			toDepotCount++;
			vehicleFleet.sendToDepot(routeDepartureEvent.getTransitLineInfo(), routeDepartureEvent.getRouteDirection(), vehicle, readyFromDepotTime);
		} else {
			vehicleFleet.stayOnStopLink(routeDepartureEvent.getTransitLineInfo(), routeDepartureEvent.getRouteDirection(), vehicle, routeDepartureEvent.getReadyAgainTime());
		}
		// determine route type
		RouteType routeType = fromDepot ?
				// from depot to ...
				(toDepot ? RouteType.DEPOT_TO_DEPOT : RouteType.DEPOT_TO_STATION) :
				// from station to ...
				(toDepot ? RouteType.STATION_TO_DEPOT : RouteType.STATION_TO_STATION);
		// allocate vehicle to departure
		allocations.get(routeDepartureEvent.getTransitLineInfo()).addVehicleAllocation(routeDepartureEvent, routeType, vehicle);
	}

	/**
	 * Sort route departures
	 * <p>
	 * Sorts route departures of all transit lines according to the arrival time of the route. Further it initializes a TransitLineVehicleAllocation for each TransitLineInfo.
	 */
	private void sortAllDepartures(List<TransitLineInfo> transitLineInfos) {
		log.info("Sorting departures of individual transit lines...");
		for (TransitLineInfo transitLineInfo : transitLineInfos) {
			// create allocation object for each transit line to store the allocations
			allocations.put(transitLineInfo, new TransitLineVehicleAllocation());
			// retrieve total route time (symmetric in both directions)
			final double totalRouteTime = calculateTotalRouteTime(transitLineInfo);
			// create linked lists for reference to opposite departures
			LinkedList<RouteDepartureEvent> plannedDeparturesOrig = new LinkedList<>();
			LinkedList<RouteDepartureEvent> plannedDeparturesDest = new LinkedList<>();
			// forward: starting from origin
			double destinationWaitingTime = calculateDestinationWaitingTime(transitLineInfo, RouteDirection.FORWARD);
			for (double departure : transitLineInfo.getDepartures(RouteDirection.FORWARD)) {
				RouteDepartureEvent routeDepartureEvent = new RouteDepartureEvent(departure, totalRouteTime, destinationWaitingTime, circuitMaxWaitingTime, RouteDirection.FORWARD, transitLineInfo, plannedDeparturesDest);
				plannedDeparturesOrig.add(routeDepartureEvent);
				routeDepartureEventQueue.add(routeDepartureEvent);
			}
			// reverse: starting from destination
			destinationWaitingTime = calculateDestinationWaitingTime(transitLineInfo, RouteDirection.REVERSE);
			for (double departure : transitLineInfo.getDepartures(RouteDirection.REVERSE)) {
				RouteDepartureEvent routeDepartureEvent = new RouteDepartureEvent(departure, totalRouteTime, destinationWaitingTime, circuitMaxWaitingTime, RouteDirection.REVERSE, transitLineInfo, plannedDeparturesOrig);
				plannedDeparturesDest.add(routeDepartureEvent);
				routeDepartureEventQueue.add(routeDepartureEvent);
			}
		}
		log.info("Sorting planned departures for all transit lines...");
		Collections.sort(routeDepartureEventQueue);
	}

	/**
	 * Check if outgoing and ingoing depot counts are equal.
	 *
	 * @return true if check passes, false otherwise.
	 */
	private boolean checkDepotCounts() {
		final boolean passed = fromDepotCount == toDepotCount;
		final String message = String.format("Outgoing and ingoing depot counts are %s (%d OUT %s %d IN)", passed ? "equal" : "not equal", fromDepotCount, passed ? "==" : "!=", toDepotCount);
		if (!passed) {
			log.warn(message);
			return false;
		}
		log.info(message);
		return true;
	}

	/**
	 * Check if all vehicles queues on stop links are empty at the end of the day.
	 *
	 * @return true if check passes, false otherwise.
	 */
	private boolean checkEmptyStopQueues() {
		final int totalVehicleQueueSize = vehicleFleet.getTotalQueueSize();
		final boolean passed = totalVehicleQueueSize == 0;
		final String message = String.format("%s", passed ? "All vehicle queues on stop links are empty" : "Not all vehicle queues on stop links are empty");
		if (!passed) {
			log.warn(message + " (vehicles left = " + totalVehicleQueueSize + ")");
			return false;
		}
		log.info(message);
		return true;
	}

	/**
	 * Check if all created vehicles are collected in a depot at the end of the day.
	 *
	 * @return true if check passes, false otherwise.
	 */
	private boolean checkDepotVehicleCounts() {
		final Map<VehicleTypeInfo, Integer> vehiclesCreated = vehicleFleet.getTotalVehicleCounts();
		final Map<VehicleTypeInfo, Integer> vehiclesCollected = vehicleFleet.getTotalVehicleInDepotCounts();
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		boolean passed = true;
		for (Map.Entry<VehicleTypeInfo, Integer> entry : vehiclesCreated.entrySet()) {
			int created = entry.getValue();
			int collected = vehiclesCollected.get(entry.getKey());
			passed = passed && created == collected;
			sb.append(entry.getKey().getId());
			sb.append(": ");
			sb.append(created);
			sb.append("+, ");
			sb.append(collected);
			sb.append("-; ");
		}
		sb.delete(sb.length() - 2, sb.length());
		sb.append(")");
		final String message = String.format("%s created vehicles are collected at the end of the day %s", passed ? "All" : "Not all", sb);
		if (!passed) {
			log.warn(message);
			return false;
		}
		log.info(message);
		return true;
	}

	/**
	 * Total route time
	 * <p>
	 * Get total route travel and waiting time between route origin and destination without first and last waiting time at station. Note: The direction does not matter, since it is symmetric. But
	 * could be changed in the future.
	 *
	 * @param transitLineInfo the transit line information.
	 * @return the total route time.
	 */
	private static double calculateTotalRouteTime(TransitLineInfo transitLineInfo) {
		final List<RouteStopInfo> routeStopInfos = transitLineInfo.getRouteStopInfos(RouteDirection.FORWARD);
		final List<Double> travelTimes = transitLineInfo.getTravelTimes(RouteDirection.FORWARD);
		// sum waiting times at intermediate stops
		return routeStopInfos.stream().limit(routeStopInfos.size() - 1).skip(1).mapToDouble(RouteStopInfo::getWaitingTime).sum()
				// sum travel times
				+ travelTimes.stream().mapToDouble(Double::doubleValue).sum();
	}

	/**
	 * Destination waiting time
	 * <p>
	 * Total time to wait at destination before next departure is possible. The calculation depends on the direction, since the waiting time at the last stop is taken.
	 *
	 * @param transitLineInfo the transit line information.
	 * @param routeDirection  does the route start at the origin?
	 * @return the destination waiting time.
	 */
	private static double calculateDestinationWaitingTime(TransitLineInfo transitLineInfo, RouteDirection routeDirection) {
		final double turnaroundTime = transitLineInfo.getVehicleTypeInfo().getTurnaroundTime();
		return Math.max(transitLineInfo.getDestination(routeDirection).getWaitingTime(), turnaroundTime);
	}

}
