package org.matsim.contrib.drt.optimizer.insertion;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTaskWithVehicleCapacityChange;
import org.matsim.contrib.drt.stops.DefaultStopTimeCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoadType;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoad;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoadType;
import org.matsim.testcases.fakes.FakeLink;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tarek Chouaki (tkchouaki)
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
		return new Waypoint.StopWithPickupAndDropoff(new DefaultDrtStopTask(beginTime, beginTime + STOP_DURATION, link), outgoingOccupancy);
	}

	@SuppressWarnings("SameParameterValue")
	private Waypoint.StopWithCapacityChange stopWithCapacityChange(double beginTime, Link link, DvrpLoad newCapacity) {
		return new Waypoint.StopWithCapacityChange(new DefaultDrtStopTaskWithVehicleCapacityChange(beginTime, beginTime + STOP_DURATION, link, newCapacity));
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

	private static class TestIntegerLoadTypeA extends IntegerLoadType {
		public TestIntegerLoadTypeA() {
			super(Id.create("loadA", DvrpLoadType.class), "A");
		}

		@Override
		public IntegerLoad fromInt(int load) {
			return new IntegerLoad(load, this);
		}
	}

	private static class TestIntegerLoadTypeB extends IntegerLoadType {

		public TestIntegerLoadTypeB() {
			super(Id.create("loadB", DvrpLoadType.class), "B");
		}

		@Override
		public IntegerLoad fromInt(int load) {
			return new IntegerLoad(load, this);
		}
	}

	private static final TestIntegerLoadTypeA FACTORY_A = new TestIntegerLoadTypeA();
	private static final TestIntegerLoadTypeB FACTORY_B = new TestIntegerLoadTypeB();

	private static final DvrpLoad STARTING_VEHICLE_CAPACITY = FACTORY_A.fromInt(4);
	private static final DvrpLoad CHANGED_VEHICLE_CAPACITY = FACTORY_B.fromInt(4);
	private final DrtRequest drtRequestA = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).passengerIds(List.of(Id.createPersonId("personA"))).load(FACTORY_A.fromInt(1)).build();
	private final DrtRequest drtRequestB = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).passengerIds(List.of(Id.createPersonId("personB"))).load(FACTORY_B.fromInt(1)).build();

	@Test
	void startEmpty_capacityChange_oneRequestPerCapacityType() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, FACTORY_A.getEmptyLoad()); //empty
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
	void startOccupied_capacityChange_oneRequestPerCapacityType() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, FACTORY_A.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), FACTORY_A.fromInt(1)); //pickup
		Waypoint.Stop stop1 = stop(0, link("stop1"), FACTORY_A.getEmptyLoad()); // dropoff
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
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, FACTORY_A.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stopWithCapacityChange(0, link("stop0"), CHANGED_VEHICLE_CAPACITY);
		Waypoint.Stop stop1 = stop(0, link("stop1"), FACTORY_B.fromInt(1)); //pickup
		Waypoint.Stop stop2 = stop(0, link("stop2"), FACTORY_B.fromInt(0)); // dropoff

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
