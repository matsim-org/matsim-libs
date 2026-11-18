package org.matsim.contrib.drt.optimizer.insertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.passenger.DrtStopCandidate;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;
import org.matsim.contrib.drt.stops.PrebookingStopTimeCalculator;
import org.matsim.contrib.drt.stops.StaticPassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;

/**
 * Tests that {@link InsertionGenerator} evaluates all request variants created from stop candidates.
 */
public class InsertionGeneratorStopCandidatesTest {
	private static final IntegerLoadType LOAD_TYPE = new IntegerLoadType("passengers");
	private static final double STOP_DURATION = 10;
	private static final double DRIVE_TIME = 50;

	private final Link from = link("from");
	private final Link to = link("to");
	private final Link accessFar = link("accessFar");

	private final DrtRouteConstraints constraints = new DrtRouteConstraints(Double.POSITIVE_INFINITY,
			Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0, false);

	private final DrtRequest request = DrtRequest.newBuilder()
			.id(Id.create("r", org.matsim.contrib.dvrp.optimizer.Request.class))
			.passengerIds(List.of(Id.createPersonId("p")))
			.fromLink(from)
			.toLink(to)
			.earliestDepartureTime(100)
			.constraints(constraints)
			.load(LOAD_TYPE.fromInt(1))
			.stopCandidates(new DrtStopCandidates(60, 0, List.of(new DrtStopCandidate(accessFar, 180)), List.of()))
			.build();

	private final InsertionGenerator generator = new InsertionGenerator(
			new PrebookingStopTimeCalculator(StaticPassengerStopDurationProvider.of(STOP_DURATION, STOP_DURATION)),
			(fromLink, toLink, departureTime) -> fromLink == toLink ? 0 : DRIVE_TIME);

	@Test
	void insertionsForAllVariants() {
		var entry = emptyEntry();
		var insertions = generator.generateInsertions(request, entry);

		// empty vehicle: one insertion (0, 0) per variant
		assertThat(insertions).hasSize(2);
		assertThat(insertions.get(0).insertion.getRequest()).isSameAs(request);
		assertThat(insertions.get(0).insertion.getVariant().index()).isZero();
		assertThat(insertions.get(1).insertion.getVariant().index()).isEqualTo(1);
		assertThat(insertions.get(1).insertion.getVariant().accessWalkDuration()).isEqualTo(180);
		assertThat(insertions.get(1).insertion.pickup.newWaypoint.getLink()).isSameAs(accessFar);
	}

	@Test
	void resultEqualsGeneratingEachVariantSeparately() {
		var entry = emptyEntry();
		var variants = RequestVariants.of(request);
		var expected = new ArrayList<InsertionWithDetourData>();
		for (RequestVariant variant : variants) {
			expected.addAll(generator.generateInsertionsForVariant(variant, entry));
		}

		assertThat(generator.generateInsertions(variants, entry)).usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(expected);
	}

	@Test
	void pickupNotBeforeArrivalOfPassengerAtVariantStop() {
		var insertions = generator.generateInsertions(request, emptyEntry());

		// the vehicle could be at both stops after 50 s, but the passenger arrives at 100 (preferred) or 220 (far stop)
		assertThat(insertions.get(0).detourTimeInfo.pickupDetourInfo.requestPickupTime).isEqualTo(100 + STOP_DURATION);
		assertThat(insertions.get(1).detourTimeInfo.pickupDetourInfo.requestPickupTime).isEqualTo(220 + STOP_DURATION);
	}

	private VehicleEntry emptyEntry() {
		Link depot = link("depot");
		var vehicle = new DvrpVehicleImpl(ImmutableDvrpVehicleSpecification.newBuilder()
				.id(Id.create("v1", DvrpVehicle.class))
				.capacity(LOAD_TYPE.fromInt(4))
				.startLinkId(depot.getId())
				.serviceBeginTime(0)
				.serviceEndTime(24 * 3600)
				.build(), depot);
		var start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad());
		var slackTimes = new double[2];
		Arrays.fill(slackTimes, Double.POSITIVE_INFINITY);
		return new VehicleEntry(vehicle, start, ImmutableList.of(), slackTimes, List.of(), 0);
	}

	private static Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}
}
