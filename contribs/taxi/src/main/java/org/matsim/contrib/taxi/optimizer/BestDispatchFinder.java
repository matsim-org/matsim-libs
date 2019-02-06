package org.matsim.contrib.taxi.optimizer;

import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.util.LinkProvider;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.RoutingNetworkImaginaryNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.Maps;

/**
 * @author michalm
 */
public class BestDispatchFinder {
	public static class Dispatch<D> {
		public final DvrpVehicle vehicle;
		public final D destination;
		public final VrpPathWithTravelData path;

		public Dispatch(DvrpVehicle vehicle, D destination, VrpPathWithTravelData path) {
			this.vehicle = vehicle;
			this.destination = destination;
			this.path = path;
		}
	}

	// typically we search through the 20-40 nearest requests/vehicles
	private static final int EXPECTED_NEIGHBOURHOOD_SIZE = 40;

	private final MultiNodeDijkstra router;
	private final TaxiScheduleInquiry scheduleInquiry;
	private final MobsimTimer timer;
	private final TravelTime travelTime;

	public BestDispatchFinder(TaxiScheduleInquiry scheduleInquiry, Network network, MobsimTimer mobsimTimer,
			TravelTime travelTime, TravelDisutility travelDisutility) {
		this.scheduleInquiry = scheduleInquiry;
		this.timer = mobsimTimer;
		this.travelTime = travelTime;

		router = (MultiNodeDijkstra)new FastMultiNodeDijkstraFactory(false).createPathCalculator(network,
				travelDisutility, travelTime);
	}

	// for immediate requests only
	// minimize TW
	public Dispatch<TaxiRequest> findBestVehicleForRequest(TaxiRequest req, Stream<? extends DvrpVehicle> vehicles) {
		return findBestVehicle(req, vehicles, r -> r.getFromLink());
	}

	// We use many-to-one forward search. Therefore, we cannot assess all vehicles.
	// However, that would be possible if one-to-many backward search were used instead.
	// TODO intuitively, many-to-one is slower, some performance tests needed before switching to
	// one-to-many
	public <D> Dispatch<D> findBestVehicle(D destination, Stream<? extends DvrpVehicle> vehicles,
			LinkProvider<D> destinationToLink) {
		double currTime = timer.getTimeOfDay();
		Link toLink = destinationToLink.apply(destination);
		Node toNode = toLink.getFromNode();

		Map<Id<Node>, DvrpVehicle> nodeToVehicle = Maps.newHashMapWithExpectedSize(EXPECTED_NEIGHBOURHOOD_SIZE);
		Map<Id<Node>, InitialNode> initialNodes = Maps.newHashMapWithExpectedSize(EXPECTED_NEIGHBOURHOOD_SIZE);
		vehicles.forEach(veh -> {
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
		});

		if (initialNodes.isEmpty()) {
			return null;
		}

		RoutingNetworkImaginaryNode fromNodes = new RoutingNetworkImaginaryNode(initialNodes.values());

		Path path = router.calcLeastCostPath(fromNodes, toNode, currTime, null, null);
		// the calculated path contains real nodes (no imaginary/initial nodes),
		// the time and cost are of real travel (between the first and last real node)
		// (no initial times/costs for imaginary<->initial are included)
		Node fromNode = path.nodes.get(0);
		DvrpVehicle bestVehicle = nodeToVehicle.get(fromNode.getId());
		LinkTimePair bestDeparture = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(bestVehicle);

		VrpPathWithTravelData vrpPath = VrpPaths.createPath(bestDeparture.link, toLink, bestDeparture.time, path,
				travelTime);
		return new Dispatch<>(bestVehicle, destination, vrpPath);
	}

	// for immediate requests only
	// minimize TP
	public Dispatch<TaxiRequest> findBestRequestForVehicle(DvrpVehicle veh, Stream<TaxiRequest> unplannedRequests) {
		return findBestDestination(veh, unplannedRequests, r -> r.getFromLink());
	}

	public <D> Dispatch<D> findBestDestination(DvrpVehicle veh, Stream<D> destinations,
			LinkProvider<D> destinationToLink) {
		LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(veh);
		Node fromNode = departure.link.getToNode();

		Map<Id<Node>, D> nodeToDestination = Maps.newHashMapWithExpectedSize(EXPECTED_NEIGHBOURHOOD_SIZE);
		Map<Id<Node>, InitialNode> initialNodes = Maps.newHashMapWithExpectedSize(EXPECTED_NEIGHBOURHOOD_SIZE);
		for (D loc : (Iterable<D>)destinations::iterator) {
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

		RoutingNetworkImaginaryNode toNodes = new RoutingNetworkImaginaryNode(initialNodes.values());

		// calc path for departure.time+1 (we need 1 second to move over the node)
		Path path = router.calcLeastCostPath(fromNode, toNodes, departure.time + 1, null, null);

		// the calculated path contains real nodes (no imaginary/initial nodes),
		// the time and cost are of real travel (between the first and last real node)
		// (no initial times/costs for imaginary<->initial are included)
		Node toNode = path.nodes.get(path.nodes.size() - 1);
		D bestDestination = nodeToDestination.get(toNode.getId());
		VrpPathWithTravelData vrpPath = VrpPaths.createPath(departure.link, destinationToLink.apply(bestDestination),
				departure.time, path, travelTime);
		return new Dispatch<>(veh, bestDestination, vrpPath);
	}
}
