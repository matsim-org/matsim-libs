package ch.sbb.matsim.routing.pt.raptor;

import it.unimi.dsi.fastutil.Pair;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

/**
 * Utility class to strip transit schedule of parts that don't use chained departures.
 * This class is used to create test data from larger schedules.
 */
class StripTransitSchedule {

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: StripTransitSchedule <network-file> <transit-schedule-file> <output-schedule-file> [output-network-file]");
			System.exit(1);
		}

		String networkFile = args[0];
		String transitScheduleFile = args[1];
		String outputScheduleFile = args[2];
		String outputNetworkFile = args.length > 3 ? args[3] : null;

		// Read scenario with network and transit schedule
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);

		// Read network and transit schedule
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new TransitScheduleReader(scenario).readFile(transitScheduleFile);

		TransitSchedule schedule = scenario.getTransitSchedule();

		// Process the schedule to keep only routes/lines with chained departures
		stripTransitSchedule(schedule);

		cleanNetwork(scenario.getNetwork(), schedule);

		// Write the stripped schedule to file
		new TransitScheduleWriter(ScrambleTransitSchedule.scramble(schedule)).writeFile(outputScheduleFile);

		// Write the cleaned network to file if specified
		if (outputNetworkFile != null) {
			new NetworkWriter(scenario.getNetwork()).write(outputNetworkFile);
		}

		// Print statistics
		printScheduleStats(schedule);
	}

	/**
	 * Removes transit routes and lines that do not use chained departures.
	 * A route is kept if:
	 * 1. It has at least one departure with a chained departure, or
	 * 2. It is referenced by a chained departure from another route
	 */
	private static void stripTransitSchedule(TransitSchedule schedule) {
		// Step 1: First identify all routes that have or are referenced by chained departures

		Set<Id<TransitRoute>> routesToKeep = new HashSet<>();

		// Keep this specific line, needed for the test
		Set<Id<TransitLine>> linesToKeep = Set.of(Id.create("005-D-15301", TransitLine.class));

		// Find routes with chained departures
		for (TransitLine line : schedule.getTransitLines().values()) {

			// Clear all custom attributes
			line.getAttributes().clear();

			for (TransitRoute route : line.getRoutes().values()) {

				route.getAttributes().clear();

				if (linesToKeep.contains(line.getId())) {
					routesToKeep.add(route.getId());
				}

				for (Departure departure : route.getDepartures().values()) {
					if (!departure.getChainedDepartures().isEmpty()) {

						routesToKeep.add(route.getId());

						// Also add the referenced routes
						for (ChainedDeparture chainedDep : departure.getChainedDepartures()) {
							routesToKeep.add(chainedDep.getChainedRouteId());
						}
					}
				}
			}
		}

		// Step 2: Remove routes that are not in the keep set
		List<TransitLine> linesToRemove = new ArrayList<>();

		for (TransitLine line : schedule.getTransitLines().values()) {
			List<TransitRoute> routesToRemove = new ArrayList<>();

			for (TransitRoute route : line.getRoutes().values()) {
				if (!routesToKeep.contains(route.getId())) {
					routesToRemove.add(route);
				}
			}

			// Remove identified routes
			for (TransitRoute route : routesToRemove) {
				line.removeRoute(route);
			}

			// If line has no routes left, mark it for removal
			if (line.getRoutes().isEmpty()) {
				linesToRemove.add(line);
			}
		}

		// Remove empty transit lines
		for (TransitLine line : linesToRemove) {
			schedule.removeTransitLine(line);
		}


		// Remove stop facilities that are not used by any route
		Set<Id<TransitStopFacility>> usedStopFacilityIds = new HashSet<>();

		// Collect all stop facilities that are still used in routes
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					usedStopFacilityIds.add(stop.getStopFacility().getId());
				}
			}
		}

		// Identify stop facilities to remove
		List<TransitStopFacility> stopFacilitiesToRemove = new ArrayList<>();
		for (TransitStopFacility facility : schedule.getFacilities().values()) {
			if (!usedStopFacilityIds.contains(facility.getId())) {
				stopFacilitiesToRemove.add(facility);
			} else {
				// Clear attributes of kept facilities
				facility.getAttributes().clear();
				facility.setCoord(CoordUtils.round(facility.getCoord(), 0));
			}
		}

		// Remove unused stop facilities
		for (TransitStopFacility facility : stopFacilitiesToRemove) {
			schedule.removeStopFacility(facility);
		}

		List<Pair<Id<TransitStopFacility>, Id<TransitStopFacility>>> transfersToRemove = new ArrayList<>();

		MinimalTransferTimes.MinimalTransferTimesIterator it = schedule.getMinimalTransferTimes().iterator();
		while (it.hasNext()) {
			it.next();

			Id<TransitStopFacility> fromStopId = it.getFromStopId();
			Id<TransitStopFacility> toStopId = it.getToStopId();

			if (!usedStopFacilityIds.contains(fromStopId) || !usedStopFacilityIds.contains(toStopId)) {
				transfersToRemove.add(Pair.of(fromStopId, toStopId));
			}
		}

		for (Pair<Id<TransitStopFacility>, Id<TransitStopFacility>> pair : transfersToRemove) {
			schedule.getMinimalTransferTimes().remove(pair.first(), pair.second());
		}
	}

	/**
	 * Prints statistics about the schedule to standard output
	 */
	private static void printScheduleStats(TransitSchedule schedule) {
		int nLines = 0;
		int nRoutes = 0;
		int nDepartures = 0;
		int nChainedDepartures = 0;

		for (TransitLine line : schedule.getTransitLines().values()) {
			nLines++;

			for (TransitRoute route : line.getRoutes().values()) {
				nRoutes++;

				for (Departure departure : route.getDepartures().values()) {
					nDepartures++;
					nChainedDepartures += departure.getChainedDepartures().size();
				}
			}
		}

		System.out.println("Transit schedule statistics:");
		System.out.println("- Transit lines: " + nLines);
		System.out.println("- Transit routes: " + nRoutes);
		System.out.println("- Departures: " + nDepartures);
		System.out.println("- Chained departures: " + nChainedDepartures);
	}

	private static void cleanNetwork(Network network, TransitSchedule schedule) {
		// Collect all links used by the transit schedule
		Set<Id<Link>> usedLinkIds = new HashSet<>();

		// Step 1: Gather links from transit stop facilities
		for (TransitStopFacility facility : schedule.getFacilities().values()) {
			if (facility.getLinkId() != null) {
				usedLinkIds.add(facility.getLinkId());
			}
		}

		// Step 2: Gather links from transit routes
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				if (route.getRoute() != null) {
					// Add start and end links
					usedLinkIds.add(route.getRoute().getStartLinkId());
					usedLinkIds.add(route.getRoute().getEndLinkId());

					// Add all links in between
					usedLinkIds.addAll(route.getRoute().getLinkIds());
				}
			}
		}

		// Step 3: Remove unused links
		List<Id<Link>> linksToRemove = new ArrayList<>();
		for (Id<Link> linkId : network.getLinks().keySet()) {
			if (!usedLinkIds.contains(linkId)) {
				linksToRemove.add(linkId);
			}
		}

		for (Id<Link> linkId : linksToRemove) {
			network.removeLink(linkId);
		}

		// Step 4: Identify and remove unused nodes
		Set<Id<Node>> usedNodeIds = new HashSet<>();

		// All nodes connected to remaining links are used
		for (Link link : network.getLinks().values()) {
			link.getAttributes().clear();
			usedNodeIds.add(link.getFromNode().getId());
			usedNodeIds.add(link.getToNode().getId());
		}

		// Clear attributes of all nodes that are kept
		for (Node node : network.getNodes().values()) {
			if (usedNodeIds.contains(node.getId())) {
				node.getAttributes().clear();
				node.setCoord(CoordUtils.round(node.getCoord(), 0));
			}
		}

		// Remove unused nodes
		List<Id<Node>> nodesToRemove = new ArrayList<>();
		for (Id<Node> nodeId : network.getNodes().keySet()) {
			if (!usedNodeIds.contains(nodeId)) {
				nodesToRemove.add(nodeId);
			}
		}

		for (Id<Node> nodeId : nodesToRemove) {
			network.removeNode(nodeId);
		}

		for (Link link : network.getLinks().values()) {
			link.setLength(Math.round(link.getLength()));
		}

		// Print statistics
		System.out.println("Network cleaning:");
		System.out.println("- Removed links: " + linksToRemove.size());
		System.out.println("- Removed nodes: " + nodesToRemove.size());
		System.out.println("- Remaining links: " + network.getLinks().size());
		System.out.println("- Remaining nodes: " + network.getNodes().size());
	}
}
