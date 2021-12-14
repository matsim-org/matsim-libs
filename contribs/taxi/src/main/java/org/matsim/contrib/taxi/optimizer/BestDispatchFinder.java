package org.matsim.contrib.taxi.optimizer;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.speedy.SpeedyMultiSourceALT;
import org.matsim.core.router.speedy.SpeedyMultiSourceALT.StartNode;
import org.matsim.core.router.speedy.SpeedyMultiSourceALTFactory;
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

	private final SpeedyMultiSourceALT multiSourceALT;
	private final TaxiScheduleInquiry scheduleInquiry;
	private final MobsimTimer timer;
	private final TravelTime travelTime;

	public BestDispatchFinder(TaxiScheduleInquiry scheduleInquiry, Network network, MobsimTimer mobsimTimer,
			TravelTime travelTime, TravelDisutility travelDisutility) {
		this.scheduleInquiry = scheduleInquiry;
		this.timer = mobsimTimer;
		this.travelTime = travelTime;

		multiSourceALT = new SpeedyMultiSourceALTFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	// for immediate requests only
	// minimize TW
	public Dispatch<TaxiRequest> findBestVehicleForRequest(TaxiRequest req, Stream<? extends DvrpVehicle> vehicles) {
		return findBestVehicle(req, vehicles, TaxiRequest::getFromLink);
	}

	// we are moving FORWARDS from vehicles (ALT start nodes) to the destination (ALT end node)
	public <D> Dispatch<D> findBestVehicle(D destination, Stream<? extends DvrpVehicle> vehicles,
			Function<D, Link> destinationToLink) {
		double currTime = timer.getTimeOfDay();
		Link destinationLink = destinationToLink.apply(destination);
		Node destinationNode = destinationLink.getFromNode();

		Map<Id<Node>, DvrpVehicle> nodeToVehicle = Maps.newHashMapWithExpectedSize(EXPECTED_NEIGHBOURHOOD_SIZE);
		Map<Id<Node>, StartNode> vehicleNodes = Maps.newHashMapWithExpectedSize(EXPECTED_NEIGHBOURHOOD_SIZE);
		vehicles.forEach(veh -> {
			LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(veh);
			if (departure != null) {
				Node vehNode;
				double delay = departure.time - currTime;
				if (departure.link == destinationLink) {
					// hack: we are basically there (on the same link), so let's pretend vehNode == destinationNode
					vehNode = destinationNode;
				} else {
					vehNode = departure.link.getToNode();

					// simplified, but works for taxis, since pickup trips are short (about 5 mins)
					delay += 1 + destinationLink.getLength() / destinationLink.getFreespeed(departure.time);
				}

				StartNode existingVehicleNode = vehicleNodes.get(vehNode.getId());
				if (existingVehicleNode == null || existingVehicleNode.cost > delay) {
					StartNode newVehicleNode = new StartNode(vehNode, delay, departure.time + 1);
					vehicleNodes.put(vehNode.getId(), newVehicleNode);
					nodeToVehicle.put(vehNode.getId(), veh);
				}
			}
		});

		if (vehicleNodes.isEmpty()) {
			return null;
		}

		Path path = multiSourceALT.calcLeastCostPath(vehicleNodes.values(), destinationNode, null, null, false);

		// the calculated path contains real nodes (no imaginary/initial nodes),
		// the time and cost are of real travel (between the first and last real node)
		// (no initial times/costs for imaginary<->initial are included)
		Node fromNode = path.getFromNode();
		DvrpVehicle bestVehicle = nodeToVehicle.get(fromNode.getId());
		LinkTimePair bestDeparture = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(bestVehicle);

		VrpPathWithTravelData vrpPath = VrpPaths.createPath(bestDeparture.link, destinationLink, bestDeparture.time,
				path, travelTime);
		return new Dispatch<>(bestVehicle, destination, vrpPath);
	}

	// for immediate requests only
	// minimize TP
	public Dispatch<TaxiRequest> findBestRequestForVehicle(DvrpVehicle veh, Stream<TaxiRequest> unplannedRequests) {
		return findBestDestination(veh, unplannedRequests, TaxiRequest::getFromLink);
	}

	// we are moving BACKWARDS from destinations (ALT start nodes) to the vehicle (ALT end node)
	public <D> Dispatch<D> findBestDestination(DvrpVehicle veh, Stream<D> destinations,
			Function<D, Link> destinationToLink) {
		LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(veh);
		Node vehicleNode = departure.link.getToNode();

		Map<Id<Node>, D> nodeToDestination = Maps.newHashMapWithExpectedSize(EXPECTED_NEIGHBOURHOOD_SIZE);
		Map<Id<Node>, StartNode> destinationNodes = Maps.newHashMapWithExpectedSize(EXPECTED_NEIGHBOURHOOD_SIZE);
		for (D loc : (Iterable<D>)destinations::iterator) {
			Link link = destinationToLink.apply(loc);

			if (departure.link == link) {
				return new Dispatch<>(veh, loc, VrpPaths.createZeroLengthPath(departure.link, departure.time, false));
			}

			Id<Node> locNodeId = link.getFromNode().getId();

			// works most fair (FIFO) if unplannedRequests (=destinations) are sorted by T0 (ascending)
			if (!destinationNodes.containsKey(locNodeId)) {
				// simplified, but works for taxis, since pickup trips are short (about 5 mins)
				double delayAtLastLink = link.getLength() / link.getFreespeed(departure.time);

				double expectedPathDuration = 0;//TODO could be taken from the planned taxi legs.
				double expectedArrivalTime = departure.time + 1 + expectedPathDuration;
				StartNode newDestinationNode = new StartNode(link.getFromNode(), delayAtLastLink, expectedArrivalTime);
				destinationNodes.put(locNodeId, newDestinationNode);
				nodeToDestination.put(locNodeId, loc);
			}
		}

		Path path = multiSourceALT.calcLeastCostPath(destinationNodes.values(), vehicleNode, null, null, true);

		// the calculated path contains real nodes (no imaginary/initial nodes),
		// the time and cost are of real travel (between the first and last real node)
		// (no initial times/costs for imaginary<->initial are included)
		Node toNode = path.getToNode();
		D bestDestination = nodeToDestination.get(toNode.getId());
		VrpPathWithTravelData vrpPath = VrpPaths.createPath(departure.link, destinationToLink.apply(bestDestination),
				departure.time, path, travelTime);
		return new Dispatch<>(veh, bestDestination, vrpPath);
	}
}
