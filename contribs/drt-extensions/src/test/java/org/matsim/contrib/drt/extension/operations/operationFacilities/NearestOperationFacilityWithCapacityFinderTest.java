package org.matsim.contrib.drt.extension.operations.operationFacilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.common.collect.ImmutableMap;

/**
 * The finder routes only to facilities whose lower travel time bound can still beat the best one found. It has to
 * return the same facility and path as routing to every facility.
 */
public class NearestOperationFacilityWithCapacityFinderTest {

	private static final int GRID = 12;

	/** Free speed travel time, increased over the day; never faster than free speed. */
	private static final TravelTime TRAVEL_TIME = (link, time, person, vehicle) -> link.getLength() / link.getFreespeed()
			* (1.0 + 0.5 * Math.abs(Math.sin(time / 3600.0 + link.getId().index())));

	private Network createNetwork(Random random) {
		Network network = NetworkUtils.createNetwork();
		Node[][] nodes = new Node[GRID][GRID];
		for (int x = 0; x < GRID; x++) {
			for (int y = 0; y < GRID; y++) {
				Coord coord = new Coord(x * 500.0 + random.nextDouble() * 200.0, y * 500.0 + random.nextDouble() * 200.0);
				nodes[x][y] = NetworkUtils.createAndAddNode(network, Id.createNodeId(x + "_" + y), coord);
			}
		}
		for (int x = 0; x < GRID; x++) {
			for (int y = 0; y < GRID; y++) {
				if (x + 1 < GRID) {
					connect(network, nodes[x][y], nodes[x + 1][y], random);
				}
				if (y + 1 < GRID) {
					connect(network, nodes[x][y], nodes[x][y + 1], random);
				}
			}
		}
		return network;
	}

	private void connect(Network network, Node a, Node b, Random random) {
		double beeline = CoordUtils.calcEuclideanDistance(a.getCoord(), b.getCoord());
		for (Node[] direction : new Node[][] { { a, b }, { b, a } }) {
			// some links are shorter than the beeline distance between their nodes, as happens in real networks
			double length = beeline * (0.7 + random.nextDouble() * 0.8);
			double freespeed = 5.0 + random.nextDouble() * 15.0;
			Id<Link> id = Id.createLinkId(direction[0].getId() + "-" + direction[1].getId());
			NetworkUtils.createAndAddLink(network, id, direction[0], direction[1], length, freespeed, 1000.0, 1.0);
		}
	}

	/** The previous implementation: route to every facility, then filter and take the minimum. */
	private Optional<OperationFacilityFinder.FacilityWithPath> reference(Network network,
			OperationFacilities facilities, OperationFacilityReservationManager reservationManager, Link fromLink,
			DvrpVehicle vehicle, double departureTime, double latestArrival, double reservationEndTime,
			Set<OperationFacilityType> types) {
		LeastCostPathCalculator router = new SpeedyALTFactory().createPathCalculator(network,
				new OnlyTimeDependentTravelDisutility(TRAVEL_TIME), TRAVEL_TIME);
		return facilities.getFacilities().values().stream() //
				.filter(f -> types.contains(f.getType())) //
				.map(f -> {
					Link toLink = network.getLinks().get(f.getLinkId());
					VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router,
							TRAVEL_TIME);
					return new OperationFacilityFinder.FacilityWithPath(f, path);
				}) //
				.filter(f -> f.path().getArrivalTime() <= latestArrival) //
				.filter(f -> reservationManager.isAvailable(f.operationFacility(), vehicle, f.path().getArrivalTime(),
						reservationEndTime)) //
				.min(Comparator.comparing(f -> f.path().getTravelTime()));
	}

	@Test
	public void testSameFacilityAsRoutingToAllFacilities() {
		Random random = new Random(0);
		int found = 0;
		int compared = 0;

		for (int c = 0; c < 20; c++) {
			Network network = createNetwork(random);
			List<Link> links = new ArrayList<>(network.getLinks().values());

			Map<Id<OperationFacility>, OperationFacility> facilityMap = new LinkedHashMap<>();
			for (int i = 0; i < 15; i++) {
				OperationFacility facility = mock(OperationFacility.class);
				Id<OperationFacility> id = Id.create("f" + c + "_" + i, OperationFacility.class);
				when(facility.getId()).thenReturn(id);
				when(facility.getLinkId()).thenReturn(links.get(random.nextInt(links.size())).getId());
				when(facility.getType()).thenReturn(random.nextBoolean() ? OperationFacilityType.hub : OperationFacilityType.inField);
				facilityMap.put(id, facility);
			}
			OperationFacilities facilities = mock(OperationFacilities.class);
			when(facilities.getFacilities()).thenReturn(ImmutableMap.copyOf(facilityMap)); // keeps the insertion order

			// deterministic, about a quarter of the (facility, arrival) combinations is not available
			OperationFacilityReservationManager reservationManager = mock(OperationFacilityReservationManager.class);
			when(reservationManager.isAvailable(any(), any(), anyDouble(), anyDouble())).thenAnswer(invocation -> {
				OperationFacility facility = invocation.getArgument(0);
				double arrival = invocation.getArgument(2);
				return Math.floorMod(facility.getId().toString().hashCode() + (int) (arrival / 600.0), 4) != 0;
			});

			DvrpVehicle vehicle = mock(DvrpVehicle.class);
			when(vehicle.getServiceEndTime()).thenReturn(30 * 3600.0);
			MobsimTimer timer = mock(MobsimTimer.class);

			NearestOperationFacilityWithCapacityFinder finder = new NearestOperationFacilityWithCapacityFinder(
					facilities, reservationManager, network, TRAVEL_TIME,
					new OnlyTimeDependentTravelDisutility(TRAVEL_TIME), timer);

			for (int q = 0; q < 50; q++) {
				Link fromLink = links.get(random.nextInt(links.size()));
				double departureTime = random.nextDouble() * 24 * 3600.0;
				double latestArrival = random.nextBoolean() ? Double.POSITIVE_INFINITY
						: departureTime + random.nextDouble() * 900.0;
				Set<OperationFacilityType> types = random.nextBoolean() ? Set.of(OperationFacilityType.hub)
						: Set.of(OperationFacilityType.hub, OperationFacilityType.inField);

				Optional<OperationFacilityFinder.FacilityWithPath> expected = reference(network, facilities,
						reservationManager, fromLink, vehicle, departureTime, latestArrival, 30 * 3600.0, types);
				Optional<OperationFacilityFinder.FacilityWithPath> actual = finder.findFacilityForTime(fromLink,
						vehicle, departureTime, latestArrival, 30 * 3600.0, types);

				String message = "case " + c + ", query " + q;
				assertThat(actual.isPresent()).as(message).isEqualTo(expected.isPresent());
				if (expected.isPresent()) {
					assertThat(actual.get().operationFacility()).as(message).isSameAs(expected.get().operationFacility());
					assertThat(actual.get().path().getTravelTime()).as(message)
							.isEqualTo(expected.get().path().getTravelTime());
					assertThat(actual.get().path().getArrivalTime()).as(message)
							.isEqualTo(expected.get().path().getArrivalTime());
					found++;
				}

				when(timer.getTimeOfDay()).thenReturn(departureTime);
				Optional<OperationFacilityFinder.FacilityWithPath> expectedAnyTime = reference(network, facilities,
						reservationManager, fromLink, vehicle, departureTime, Double.POSITIVE_INFINITY,
						vehicle.getServiceEndTime(), types);
				Optional<OperationFacilityFinder.FacilityWithPath> actualAnyTime = finder.findFacility(fromLink,
						vehicle, types);
				assertThat(actualAnyTime.map(OperationFacilityFinder.FacilityWithPath::operationFacility)).as(message)
						.isEqualTo(expectedAnyTime.map(OperationFacilityFinder.FacilityWithPath::operationFacility));
				compared++;
			}
		}

		// make sure the cases are not trivially empty
		assertThat(found).isGreaterThan(compared / 4);
	}

	/**
	 * A link may be much shorter than the beeline distance between its nodes. The lower bound must still hold, so the
	 * facility behind such a link is found although it is far away by beeline.
	 */
	@Test
	public void testLinkShorterThanBeeline() {
		Network network = NetworkUtils.createNetwork();
		Node origin = NetworkUtils.createAndAddNode(network, Id.createNodeId("o"), new Coord(0.0, 0.0));
		Node start = NetworkUtils.createAndAddNode(network, Id.createNodeId("s"), new Coord(10.0, 0.0));
		Node near = NetworkUtils.createAndAddNode(network, Id.createNodeId("n"), new Coord(310.0, 0.0));
		Node far = NetworkUtils.createAndAddNode(network, Id.createNodeId("f"), new Coord(2010.0, 0.0));

		Link fromLink = NetworkUtils.createAndAddLink(network, Id.createLinkId("os"), origin, start, 10.0, 10.0, 1000.0, 1.0);
		// 300 m at 10 m/s: 30 s
		Link nearLink = NetworkUtils.createAndAddLink(network, Id.createLinkId("sn"), start, near, 300.0, 10.0, 1000.0, 1.0);
		// 2000 m beeline, but only 100 m long at 10 m/s: 10 s
		Link farLink = NetworkUtils.createAndAddLink(network, Id.createLinkId("sf"), start, far, 100.0, 10.0, 1000.0, 1.0);

		Map<Id<OperationFacility>, OperationFacility> facilityMap = new LinkedHashMap<>();
		for (Link link : List.of(nearLink, farLink)) {
			OperationFacility facility = mock(OperationFacility.class);
			Id<OperationFacility> id = Id.create(link.getId().toString(), OperationFacility.class);
			when(facility.getId()).thenReturn(id);
			when(facility.getLinkId()).thenReturn(link.getId());
			when(facility.getType()).thenReturn(OperationFacilityType.hub);
			facilityMap.put(id, facility);
		}
		OperationFacilities facilities = mock(OperationFacilities.class);
		when(facilities.getFacilities()).thenReturn(ImmutableMap.copyOf(facilityMap));

		OperationFacilityReservationManager reservationManager = mock(OperationFacilityReservationManager.class);
		when(reservationManager.isAvailable(any(), any(), anyDouble(), anyDouble())).thenReturn(true);
		DvrpVehicle vehicle = mock(DvrpVehicle.class);
		when(vehicle.getServiceEndTime()).thenReturn(30 * 3600.0);

		TravelTime freeSpeed = (link, time, person, v) -> link.getLength() / link.getFreespeed();
		NearestOperationFacilityWithCapacityFinder finder = new NearestOperationFacilityWithCapacityFinder(facilities,
				reservationManager, network, freeSpeed, new OnlyTimeDependentTravelDisutility(freeSpeed),
				mock(MobsimTimer.class));

		Optional<OperationFacilityFinder.FacilityWithPath> result = finder.findFacilityForTime(fromLink, vehicle, 0.0,
				Double.POSITIVE_INFINITY, 30 * 3600.0, Set.of(OperationFacilityType.hub));

		assertThat(result).isPresent();
		assertThat(result.get().operationFacility().getLinkId()).isEqualTo(farLink.getId());
	}
}
