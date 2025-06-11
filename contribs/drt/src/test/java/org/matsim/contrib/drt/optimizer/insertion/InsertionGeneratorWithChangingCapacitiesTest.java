package org.matsim.contrib.drt.optimizer.insertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtCapacityChangeTask;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.stops.DefaultStopTimeCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.IntegersLoadType;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public class InsertionGeneratorWithChangingCapacitiesTest {

	private static final int STOP_DURATION = 10;

	private final Link depotLink = link("depot");

	private final Link fromLink = link("from");

	private final Link toLink = link("to");

	private final DvrpVehicleSpecification vehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder()
		.id(Id.create("v1", DvrpVehicle.class))
		.capacity(STARTING_VEHICLE_CAPACITY)
		.startLinkId(depotLink.getId())
		.serviceBeginTime(0)
		.serviceEndTime(24 * 3600)
		.build();
	private final DvrpVehicle vehicle = new DvrpVehicleImpl(vehicleSpecification, depotLink);

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	@SuppressWarnings("SameParameterValue")
	private Waypoint.Stop stop(double beginTime, Link link, DvrpLoad outgoingOccupancy) {
		return new Waypoint.Stop(new DefaultDrtStopTask(beginTime, beginTime + STOP_DURATION, link), outgoingOccupancy, customLoadType);
	}

	@SuppressWarnings("SameParameterValue")
	private Waypoint.Stop stopWithCapacityChange(double beginTime, Link link, DvrpLoad newCapacity) {
		return new Waypoint.Stop(new DefaultDrtCapacityChangeTask(beginTime, beginTime + STOP_DURATION, link, newCapacity), customLoadType);
	}

	private VehicleEntry entry(Waypoint.Start start, Waypoint.Stop... stops) {
		List<Double> precedingStayTimes = Collections.nCopies(stops.length, 0.0);
		return entry(start, precedingStayTimes, stops);
	}

	private VehicleEntry entry(Waypoint.Start start, List<Double> precedingStayTimes, Waypoint.Stop... stops) {
		var slackTimes = new double[stops.length + 2];
		Arrays.fill(slackTimes, Double.POSITIVE_INFINITY);
		return new VehicleEntry(vehicle, start, ImmutableList.copyOf(stops), slackTimes, precedingStayTimes, 0);
	}

	private static IntegersLoadType customLoadType = new IntegersLoadType("A", "B");

	private static final DvrpLoad STARTING_VEHICLE_CAPACITY = customLoadType.fromArray(4, 0);
	private static final DvrpLoad CHANGED_VEHICLE_CAPACITY = customLoadType.fromArray(0, 4);
	private final DrtRequest drtRequestA = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).passengerIds(List.of(Id.createPersonId("personA"))).load(customLoadType.fromArray(1, 0)).build();
	private final DrtRequest drtRequestB = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).passengerIds(List.of(Id.createPersonId("personB"))).load(customLoadType.fromArray(0, 1)).build();

	@Test
	void startEmpty_capacityChange_oneRequestPerCapacityType() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, customLoadType.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stopWithCapacityChange(0, link("stop0"), CHANGED_VEHICLE_CAPACITY);//pick up 4 pax (full)
		VehicleEntry entry = entry(start, stop0);
		assertInsertionsOnly(drtRequestA, entry,
			//pickup after start
			new InsertionGenerator.Insertion(drtRequestA, entry, 0, 0));

		assertInsertionsOnly(drtRequestB, entry,
			//pickup after stop 1
			new InsertionGenerator.Insertion(drtRequestB, entry, 1, 1));
	}

	@Test
	void startEmpty_atCapacityChange_oneRequestPerCapacityType() {
		Waypoint.Start start = new Waypoint.Start(new DefaultDrtCapacityChangeTask(0, 60, link("start"), CHANGED_VEHICLE_CAPACITY), link("start"), 0, customLoadType.getEmptyLoad());
		VehicleEntry entry = entry(start);
		assertInsertionsOnly(drtRequestA, entry);
		assertInsertionsOnly(drtRequestB, entry, new InsertionGenerator.Insertion(drtRequestB, entry, 0, 0));
	}

	@Test
	void startOccupied_capacityChange_oneRequestPerCapacityType() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, customLoadType.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), customLoadType.fromArray(1, 0)); //pickup
		Waypoint.Stop stop1 = stop(0, link("stop1"), customLoadType.getEmptyLoad()); // dropoff
		Waypoint.Stop stop2 = stopWithCapacityChange(0, link("stop2"), CHANGED_VEHICLE_CAPACITY);

		VehicleEntry entry = entry(start, stop0, stop1, stop2);

		assertInsertionsOnly(drtRequestA, entry,
			new InsertionGenerator.Insertion(drtRequestA, entry, 0, 0),
			new InsertionGenerator.Insertion(drtRequestA, entry, 0, 1),
			new InsertionGenerator.Insertion(drtRequestA, entry, 0, 2),
			new InsertionGenerator.Insertion(drtRequestA, entry, 1, 1),
			new InsertionGenerator.Insertion(drtRequestA, entry, 1, 2),
			new InsertionGenerator.Insertion(drtRequestA, entry, 2, 2));

		assertInsertionsOnly(drtRequestB, entry,
			new InsertionGenerator.Insertion(drtRequestB, entry, 3, 3));
	}

	@Test
	void startEmpty_capacityChangeThenRequest_oneRequestPerCapacityType() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, customLoadType.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stopWithCapacityChange(0, link("stop0"), CHANGED_VEHICLE_CAPACITY);
		Waypoint.Stop stop1 = stop(0, link("stop1"), customLoadType.fromArray(0, 1)); //pickup
		Waypoint.Stop stop2 = stop(0, link("stop2"), customLoadType.getEmptyLoad()); // dropoff

		VehicleEntry entry = entry(start, stop0, stop1, stop2);

		assertInsertionsOnly(drtRequestA, entry,
			new InsertionGenerator.Insertion(drtRequestA, entry, 0, 0));

		assertInsertionsOnly(drtRequestB, entry,
			new InsertionGenerator.Insertion(drtRequestB, entry, 1, 1),
			new InsertionGenerator.Insertion(drtRequestB, entry, 1, 2),
			new InsertionGenerator.Insertion(drtRequestB, entry, 1, 3),
			new InsertionGenerator.Insertion(drtRequestB, entry, 2, 2),
			new InsertionGenerator.Insertion(drtRequestB, entry, 2, 3),
			new InsertionGenerator.Insertion(drtRequestB, entry, 3, 3));
	}

	private void assertInsertionsOnly(DrtRequest drtRequest, VehicleEntry entry, InsertionGenerator.Insertion... expectedInsertions) {
		int stopCount = entry.stops.size();
		DvrpLoad endOccupancy = stopCount > 0 ? entry.stops.get(stopCount - 1).outgoingOccupancy : entry.start.occupancy;
		Preconditions.checkArgument(endOccupancy.isEmpty());//make sure the input is valid

		DetourTimeEstimator timeEstimator = (from, to, departureTime) -> 0;

		var actualInsertions = new InsertionGenerator(new DefaultStopTimeCalculator(STOP_DURATION), timeEstimator).generateInsertions(drtRequest,
			entry);
		assertThat(actualInsertions.stream().map(i -> i.insertion)).usingRecursiveFieldByFieldElementComparator()
			.containsExactly(expectedInsertions);
	}


}
