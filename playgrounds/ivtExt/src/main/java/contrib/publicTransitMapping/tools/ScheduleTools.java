/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package contrib.publicTransitMapping.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import contrib.publicTransitMapping.config.PublicTransitMappingStrings;
import contrib.publicTransitMapping.mapping.PTMapperUtils;
import contrib.publicTransitMapping.mapping.networkRouter.Router;

import java.util.*;

/**
 * Methods to load and modify transit schedules. Also provides
 * methods to get information from transit routes.
 *
 * @author polettif
 */
public class ScheduleTools {

	protected static Logger log = Logger.getLogger(ScheduleTools.class);

	private ScheduleTools() {}

	/**
	 * @return the transitSchedule from scheduleFile.
	 */
	public static TransitSchedule readTransitSchedule(String fileName) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(fileName);
		return scenario.getTransitSchedule();
	}

	/**
	 * @return an empty transit schedule.
	 */
	public static TransitSchedule createSchedule() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		return scenario.getTransitSchedule();
	}

	/**
	 * Writes the transit schedule to filePath.
	 */
	public static void writeTransitSchedule(TransitSchedule schedule, String fileName) {
		log.info("Writing transit schedule to file " + fileName);
		new TransitScheduleWriter(schedule).writeFile(fileName);
		log.info("done.");
	}

	/**
	 * Creates vehicles with default vehicle types depending on the schedule
	 * transport mode.
	 */
	public static Vehicles createVehicles(TransitSchedule schedule) {
		log.info("Creating vehicles from schedule...");
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehiclesFactory vf = vehicles.getFactory();
		Map<String, VehicleType> vehicleTypes = new HashMap<>();

		long vehId = 0;
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				// create vehicle type
				if(!vehicleTypes.containsKey(route.getTransportMode())) {
					Id<VehicleType> vehicleTypeId = Id.create(route.getTransportMode(), VehicleType.class);
					VehicleType vehicleType = vf.createVehicleType(vehicleTypeId);
					VehicleCapacity capacity = new VehicleCapacityImpl();
					capacity.setSeats(50);
					capacity.setStandingRoom(0);
					vehicleType.setCapacity(capacity);
					vehicles.addVehicleType(vehicleType);
					vehicleTypes.put(route.getTransportMode(), vehicleType);
				}
				VehicleType vehicleType = vehicleTypes.get(route.getTransportMode());

				// create a vehicle for each departure
				for(Departure departure : route.getDepartures().values()) {
					Vehicle veh = vf.createVehicle(Id.create("veh_" + Long.toString(vehId++) + "_" + route.getTransportMode(), Vehicle.class), vehicleType);
					vehicles.addVehicle(veh);
					departure.setVehicleId(veh.getId());
				}
			}
		}
		return vehicles;
	}

	/**
	 * @return the vehicles from a given vehicles file.
	 */
	public static Vehicles readVehicles(String vehiclesFile) {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		new VehicleReaderV1(vehicles).readFile(vehiclesFile);
		return vehicles;
	}


	/**
	 * Add mode "pt" to any link of the network that is
	 * passed by any transitRoute of the schedule.
	 */
	public static void addPTModeToNetwork(TransitSchedule schedule, Network network) {
		log.info("... Adding mode \"pt\" to all links with public transit");

		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinkIds = new HashSet<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				if(transitRoute.getRoute() != null) {
					transitLinkIds.addAll(getTransitRouteLinkIds(transitRoute));
				}
			}
		}

		for(Id<Link> transitLinkId : transitLinkIds) {
			Link transitLink = networkLinks.get(transitLinkId);
			if(!transitLink.getAllowedModes().contains(TransportMode.pt)) {
				Set<String> modes = new HashSet<>();
				modes.addAll(transitLink.getAllowedModes());
				modes.add(TransportMode.pt);
				transitLink.setAllowedModes(modes);
			}
		}
	}

	/**
	 * Generates link sequences (network route) for all transit routes in
	 * the schedule, modifies the schedule. All stopFacilities used by a
	 * route must have a link referenced.
	 *
	 * @param schedule where transitRoutes should be routed
	 * @param network  the network where the routes should be routed
	 * @param routers  A map defining the Router for each scheduleTransportMode (the mode
	 *                 defined in the transitRoute).
	 */
	public static void routeSchedule(TransitSchedule schedule, Network network, Map<String, Router> routers) {
		Counter counterRoute = new Counter("route # ");

		log.info("Routing all routes with referenced links...");

		if(routers == null) {
			log.error("No routers given, routing cannot be completed!");
			return;
		}

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(!routers.containsKey(transitRoute.getTransportMode())) {
					throw new RuntimeException("No router defined for " + transitRoute.getTransportMode());
				}
				if(transitRoute.getStops().size() > 0) {
					Router modeDependentRouter = routers.get(transitRoute.getTransportMode());

					counterRoute.incCounter();

					List<TransitRouteStop> routeStops = transitRoute.getStops();
					List<Id<Link>> linkIdSequence = new LinkedList<>();
					linkIdSequence.add(routeStops.get(0).getStopFacility().getLinkId());

					// route
					for(int i = 0; i < routeStops.size() - 1; i++) {
						if(routeStops.get(i).getStopFacility().getLinkId() == null) {
							log.warn("stop facility " + routeStops.get(i).getStopFacility().getName() + " (" + routeStops.get(i).getStopFacility().getId() + ") not referenced!");
							linkIdSequence = null;
							break;
						}
						if(routeStops.get(i + 1).getStopFacility().getLinkId() == null) {
							log.warn("stop facility " + routeStops.get(i - 1).getStopFacility().getName() + " (" + routeStops.get(i + 1).getStopFacility().getId() + " not referenced!");
							linkIdSequence = null;
							break;
						}

						Id<Link> currentLinkId = Id.createLinkId(routeStops.get(i).getStopFacility().getLinkId().toString());
						Link currentLink = network.getLinks().get(currentLinkId);
						Link nextLink = network.getLinks().get(routeStops.get(i + 1).getStopFacility().getLinkId());

						LeastCostPathCalculator.Path leastCostPath = modeDependentRouter.calcLeastCostPath(currentLink.getToNode(), nextLink.getFromNode());

						List<Id<Link>> path = null;
						if(leastCostPath != null) {
							path = PTMapperUtils.getLinkIdsFromPath(leastCostPath);
						}

						if(path != null)
							linkIdSequence.addAll(path);

						linkIdSequence.add(nextLink.getId());
					} // -for stops

					// add link sequence to schedule
					if(linkIdSequence != null) {
						transitRoute.setRoute(RouteUtils.createNetworkRoute(linkIdSequence, network));
					}
				} else {
					log.warn("Route " + transitRoute.getId() + " on line " + transitLine.getId() + " has no stop sequence");
				}
			} // -route
		} // -line
		log.info("Routing all routes with referenced links... done");
	}

	/**
	 * Adds mode the schedule transport mode to links. Removes all network
	 * modes elsewhere. Adds mode "artificial" to artificial
	 * links. Used for debugging and visualization since networkModes
	 * should be combined to pt anyway.
	 */
	public static void assignScheduleModesToLinks(TransitSchedule schedule, Network network) {
		log.info("... Assigning schedule transport mode to network");

		Map<Id<Link>, Set<String>> transitLinkNetworkModes = new HashMap<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				if(route.getRoute() != null) {
					for(Id<Link> linkId : getTransitRouteLinkIds(route)) {
						MapUtils.getSet(linkId, transitLinkNetworkModes).add(route.getTransportMode());
					}
				}
			}
		}

		for(Link link : network.getLinks().values()) {
			if(transitLinkNetworkModes.containsKey(link.getId())) {
				Set<String> modes = new HashSet<>();
				Set<String> linkModes = transitLinkNetworkModes.get(link.getId());
				linkModes.addAll(link.getAllowedModes());

				for(String m : linkModes) {
					modes.add(m);
				}

				link.setAllowedModes(modes);
			}
		}
	}

	/**
	 * Transforms a MATSim Transit Schedule file. Overwrites the file.
	 */
	public static void transformScheduleFile(String scheduleFile, String fromCoordinateSystem, String toCoordinateSystem) {
		log.info("... Transformig schedule from " + fromCoordinateSystem + " to " + toCoordinateSystem);

		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(fromCoordinateSystem, toCoordinateSystem);
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new TransitScheduleReader(coordinateTransformation, scenario).readFile(scheduleFile);
		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleWriter(schedule).writeFile(scheduleFile);
	}


	/**
	 * @return the list of link ids used by transit routes (first and last
	 * links are included). Returns an empty list if no links are assigned
	 * to the route.
	 */
	public static List<Id<Link>> getTransitRouteLinkIds(TransitRoute transitRoute) {
		List<Id<Link>> list = new ArrayList<>();
		if(transitRoute.getRoute() == null) { return list;	}
		NetworkRoute networkRoute = transitRoute.getRoute();
		list.add(networkRoute.getStartLinkId());
		list.addAll(networkRoute.getLinkIds());
		list.add(networkRoute.getEndLinkId());
		return list;
	}

	public static List<Id<Link>> getSubRouteLinkIds(TransitRoute transitRoute, Id<Link> fromLinkId, Id<Link> toLinkId) {
		NetworkRoute route = transitRoute.getRoute();
		if(fromLinkId == null) {
			fromLinkId = route.getStartLinkId();
		}
		if(toLinkId == null) {
			toLinkId = route.getEndLinkId();
		}
		List<Id<Link>> list = new ArrayList<>();
		NetworkRoute networkRoute = route.getSubRoute(fromLinkId, toLinkId);
		list.add(networkRoute.getStartLinkId());
		list.addAll(networkRoute.getLinkIds());
		list.add(networkRoute.getEndLinkId());
		return list;
	}


	/**
	 * Based on {@link org.matsim.core.population.routes.LinkNetworkRouteImpl#getSubRoute}
	 *
	 * @param transitRoute the transitRoute
	 * @param fromLinkId   first link of the subroute. If <tt>null</tt> the first link of the route is used.
	 * @param toLinkId     last link of the subroute. If <tt>null</tt> the first link of the route is used.
	 * @return the list of link ids used by transit routes (fromLink and toLink
	 * links are included)
	 */
	public static List<Id<Link>> getLoopSubRouteLinkIds(TransitRoute transitRoute, Id<Link> fromLinkId, Id<Link> toLinkId) {
		NetworkRoute route = transitRoute.getRoute();
		if(fromLinkId == null) {
			fromLinkId = route.getStartLinkId();
		}
		if(toLinkId == null) {
			toLinkId = route.getEndLinkId();
		}

		List<Id<Link>> linkIdList = getTransitRouteLinkIds(transitRoute);

		/**
		 * the index where the link after fromLinkId can be found in the route:
		 * fromIndex==0 --> fromLinkId == startLinkId,
		 * fromIndex==1 --> fromLinkId == first link in the route, etc.
		 */
		int fromIndex = -1;
		/**
		 * the index where toLinkId can be found in the route
		 */
		int toIndex = -1;

		if(fromLinkId.equals(route.getStartLinkId())) {
			fromIndex = 0;
		} else {
			for(int i = 0, n = linkIdList.size(); (i < n) && (fromIndex < 0); i++) {
				if(fromLinkId.equals(linkIdList.get(i))) {
					fromIndex = i;
				}
			}
			if(fromIndex < 0 && fromLinkId.equals(route.getEndLinkId())) {
				fromIndex = linkIdList.size();
			}
			if(fromIndex < 0) {
				throw new IllegalArgumentException("Cannot create subroute because fromLinkId is not part of the route.");
			}
		}

		if(fromLinkId.equals(toLinkId)) {
			toIndex = fromIndex - 1;
		} else {
			for(int i = fromIndex, n = linkIdList.size(); (i < n) && (toIndex < 0); i++) {
				if(toLinkId.equals(linkIdList.get(i))) {
					toIndex = i;
				}
			}
			if(toIndex < 0 && toLinkId.equals(route.getEndLinkId())) {
				toIndex = linkIdList.size();
			}
			if(toIndex < 0) {
				throw new IllegalArgumentException("Cannot create subroute because toLinkId is not part of the route.");
			}
		}

		return linkIdList.subList(fromIndex, toIndex);
	}

	/**
	 * Writes the vehicles to the output file.
	 */
	public static void writeVehicles(Vehicles vehicles, String filePath) {
		log.info("Writing vehicles to file " + filePath);
		new VehicleWriterV1(vehicles).writeFile(filePath);
	}

	/**
	 * checks if a stop is accessed twice in a stop sequence
	 */
	public static boolean routeHasStopSequenceLoop(TransitRoute transitRoute) {
		Set<String> parentFacilities = new HashSet<>();
		for(TransitRouteStop stop : transitRoute.getStops()) {
			if(!parentFacilities.add(getParentId(stop.getStopFacility().getId().toString()))) {
				return true;
			}
		}
		return false;
	}


	/**
	 * @return the parent id of a stop facility id. This is the part befor the
	 * child stop facility suffix ".link:"
	 */
	public static String getParentId(String stopFacilityId) {
		String[] childStopSplit = stopFacilityId.split(PublicTransitMappingStrings.SUFFIX_CHILD_STOP_FACILITIES_REGEX);
		return childStopSplit[0];
	}
}