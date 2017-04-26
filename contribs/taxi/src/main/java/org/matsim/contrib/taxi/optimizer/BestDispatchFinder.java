package org.matsim.contrib.taxi.optimizer;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.util.LinkProvider;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

/**
 * @author michalm
 */
public class BestDispatchFinder {
	public static class Dispatch<D> {
		public final Vehicle vehicle;
		public final D destination;
		public final VrpPathWithTravelData path;

		public Dispatch(Vehicle vehicle, D destination, VrpPathWithTravelData path) {
			this.vehicle = vehicle;
			this.destination = destination;
			this.path = path;
		}
	}

	// typically we search through the 20-40 nearest requests/vehicles
	public static final int DEFAULT_EXPECTED_NEIGHBOURHOOD_SIZE = 40;

	private final TaxiOptimizerContext optimContext;
	private final MultiNodeDijkstra router;
	private final TaxiScheduleInquiry scheduleInquiry;
	private final int expectedNeighbourhoodSize;

	public BestDispatchFinder(TaxiOptimizerContext optimContext) {
		this(optimContext, DEFAULT_EXPECTED_NEIGHBOURHOOD_SIZE);
	}

	public BestDispatchFinder(TaxiOptimizerContext optimContext, int expectedNeighbourhoodSize) {
		this.optimContext = optimContext;
		this.scheduleInquiry = optimContext.scheduler;
		this.expectedNeighbourhoodSize = expectedNeighbourhoodSize;

		// TODO bug: cannot cast ImaginaryNode to RoutingNetworkNode
		// PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		// preProcessDijkstra.run(optimContext.network);
		PreProcessDijkstra preProcessDijkstra = null;
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessDijkstra)
				.createRoutingNetwork(optimContext.network);
		router = new FastMultiNodeDijkstra(routingNetwork, optimContext.travelDisutility, optimContext.travelTime,
				preProcessDijkstra, fastRouterFactory, false);
	}

	// for immediate requests only
	// minimize TW
	public Dispatch<TaxiRequest> findBestVehicleForRequest(TaxiRequest req, Iterable<? extends Vehicle> vehicles) {
		return findBestVehicle(req, vehicles, LinkProviders.REQUEST_TO_FROM_LINK);
	}

	// We use many-to-one forward search. Therefore, we cannot assess all vehicles.
	// However, that would be possible if one-to-many backward search were used instead.
	// TODO intuitively, many-to-one is slower, some performance tests needed before switching to
	// one-to-many
	public <D> Dispatch<D> findBestVehicle(D destination, Iterable<? extends Vehicle> vehicles,
			LinkProvider<D> destinationToLink) {
		double currTime = optimContext.timer.getTimeOfDay();
		Link toLink = destinationToLink.apply(destination);
		Node toNode = toLink.getFromNode();

		Map<Id<Node>, Vehicle> nodeToVehicle = new HashMap<>(expectedNeighbourhoodSize);
		Map<Id<Node>, InitialNode> initialNodes = new HashMap<>(expectedNeighbourhoodSize);
		for (Vehicle veh : vehicles) {
			LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(veh);
			if (departure != null) {

				Node vehNode;
				double delay = departure.time - currTime;
				if (departure.link == toLink) {
					// hack: we are basically there (on the same link), so let's pretend vehNode == toNode
					vehNode = toNode;
				} else {
					vehNode = departure.link.getToNode();

					// simplified, but works for taxis, since pickup trips are short (about 5 mins)
					delay += 1 + toLink.getFreespeed(departure.time);
				}

				InitialNode existingInitialNode = initialNodes.get(vehNode.getId());
				if (existingInitialNode == null || existingInitialNode.initialCost > delay) {
					InitialNode newInitialNode = new InitialNode(vehNode, delay, delay);
					initialNodes.put(vehNode.getId(), newInitialNode);
					nodeToVehicle.put(vehNode.getId(), veh);
				}
			}
		}

		if (initialNodes.isEmpty()) {
			return null;
		}

		ImaginaryNode fromNodes = MultiNodeDijkstra.createImaginaryNode(initialNodes.values());

		Path path = router.calcLeastCostPath(fromNodes, toNode, currTime, null, null);
		// the calculated path contains real nodes (no imaginary/initial nodes),
		// the time and cost are of real travel (between the first and last real node)
		// (no initial times/costs for imaginary<->initial are included)
		Node fromNode = path.nodes.get(0);
		Vehicle bestVehicle = nodeToVehicle.get(fromNode.getId());
		LinkTimePair bestDeparture = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(bestVehicle);

		VrpPathWithTravelData vrpPath = VrpPaths.createPath(bestDeparture.link, toLink, bestDeparture.time, path,
				optimContext.travelTime);
		return new Dispatch<>(bestVehicle, destination, vrpPath);
	}

	// for immediate requests only
	// minimize TP
	public Dispatch<TaxiRequest> findBestRequestForVehicle(Vehicle veh, Iterable<TaxiRequest> unplannedRequests) {
		return findBestDestination(veh, unplannedRequests, LinkProviders.REQUEST_TO_FROM_LINK);
	}

	public <D> Dispatch<D> findBestDestination(Vehicle veh, Iterable<D> destinations,
			LinkProvider<D> destinationToLink) {
		LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(veh);
		Node fromNode = departure.link.getToNode();

		Map<Id<Node>, D> nodeToDestination = new HashMap<>(expectedNeighbourhoodSize);
		Map<Id<Node>, InitialNode> initialNodes = new HashMap<>(expectedNeighbourhoodSize);
		for (D loc : destinations) {
			Link link = destinationToLink.apply(loc);

			if (departure.link == link) {
				return new Dispatch<>(veh, loc, VrpPaths.createZeroLengthPath(departure.link, departure.time));
			}

			Id<Node> locNodeId = link.getFromNode().getId();

			if (!initialNodes.containsKey(locNodeId)) {
				// simplified, but works for taxis, since pickup trips are short (about 5 mins)
				double delayAtLastLink = link.getFreespeed(departure.time);

				// works most fair (FIFO) if unplannedRequests (=destinations) are sorted by T0 (ascending)
				InitialNode newInitialNode = new InitialNode(link.getFromNode(), delayAtLastLink, delayAtLastLink);
				initialNodes.put(locNodeId, newInitialNode);
				nodeToDestination.put(locNodeId, loc);
			}
		}

		ImaginaryNode toNodes = MultiNodeDijkstra.createImaginaryNode(initialNodes.values());

		// calc path for departure.time+1 (we need 1 second to move over the node)
		Path path = router.calcLeastCostPath(fromNode, toNodes, departure.time + 1, null, null);

		// the calculated path contains real nodes (no imaginary/initial nodes),
		// the time and cost are of real travel (between the first and last real node)
		// (no initial times/costs for imaginary<->initial are included)
		Node toNode = path.nodes.get(path.nodes.size() - 1);
		D bestDestination = nodeToDestination.get(toNode.getId());
		VrpPathWithTravelData vrpPath = VrpPaths.createPath(departure.link, destinationToLink.apply(bestDestination),
				departure.time, path, optimContext.travelTime);
		return new Dispatch<>(veh, bestDestination, vrpPath);
	}
}
