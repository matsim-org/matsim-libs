package org.matsim.contrib.drt.prebooking.unscheduler;

import com.google.common.base.VerifyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplexRequestUnschedulerTest {
	@Mock
	private DvrpVehicleLookup vehicleLookup;
	@Mock
	private VehicleEntry.EntryFactory entryFactory;
	@Mock
	private DrtTaskFactory taskFactory;
	@Mock
	private LeastCostPathCalculator router;
	@Mock
	private TravelTime travelTime;
	@Mock
	private org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater timingUpdater;
	@Mock
	private DvrpVehicle vehicle;
	@Mock
	private Schedule schedule;

	private ComplexRequestUnscheduler unscheduler;

	@BeforeEach
	void setUp() {
		unscheduler = new ComplexRequestUnscheduler(vehicleLookup, entryFactory, taskFactory, router, travelTime,
				timingUpdater, false);
		Id<DvrpVehicle> vehicleId = Id.create("veh", DvrpVehicle.class);
		when(vehicle.getId()).thenReturn(vehicleId);
		when(vehicleLookup.lookupVehicle(vehicleId)).thenReturn(vehicle);
		when(vehicle.getSchedule()).thenReturn(schedule);
	}

	@Test
	void plannedSchedule_removesRequestFromStops() {
		Id<Request> requestId = Id.create("req", Request.class);

		DrtStopTask pickup = stopWithRequest(0, requestId, true);
		DrtStopTask dropoff = stopWithRequest(1, requestId, false);

		List<Task> tasks = new ArrayList<>();
		tasks.add(pickup);
		tasks.add(dropoff);

		when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.PLANNED);
		when(schedule.getTasks()).thenAnswer(invocation -> tasks);

		unscheduler.unscheduleRequest(0.0, vehicle.getId(), requestId);

		verify(pickup).removePickupRequest(requestId);
		verify(dropoff).removeDropoffRequest(requestId);
	}

	@Test
	void startedSchedule_atStop_removesRequest() {
		Id<Request> requestId = Id.create("req", Request.class);

		DrtStopTask currentStop = stopWithRequest(0, Id.create("other", Request.class), true);
		DrtStopTask pickup = stopWithRequest(1, requestId, true);
		DrtStopTask dropoff = stopWithRequest(2, requestId, false);

		List<Task> tasks = new ArrayList<>();
		tasks.add(currentStop);
		tasks.add(pickup);
		tasks.add(dropoff);

		when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
		when(schedule.getCurrentTask()).thenReturn(currentStop);
		when(currentStop.getTaskIdx()).thenReturn(0);
		when(schedule.getTasks()).thenAnswer(invocation -> tasks);

		unscheduler.unscheduleRequest(0.0, vehicle.getId(), requestId);

		verify(pickup).removePickupRequest(requestId);
		verify(dropoff).removeDropoffRequest(requestId);
	}

	@Test
	void startedSchedule_whileDriving_stillFindsStops() {
		Id<Request> requestId = Id.create("req", Request.class);

		Task drive = mock(Task.class);
		when(drive.getTaskIdx()).thenReturn(0);

		DrtStopTask pickup = stopWithRequest(1, requestId, true);
		DrtStopTask dropoff = stopWithRequest(2, requestId, false);

		List<Task> tasks = new ArrayList<>();
		tasks.add(drive);
		tasks.add(pickup);
		tasks.add(dropoff);

		when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
		when(schedule.getCurrentTask()).thenReturn(drive);
		when(schedule.getTasks()).thenAnswer(invocation -> tasks);

		unscheduler.unscheduleRequest(0.0, vehicle.getId(), requestId);

		verify(pickup).removePickupRequest(requestId);
		verify(dropoff).removeDropoffRequest(requestId);
	}

	@Test
	void unplannedSchedule_throws() {
		when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.UNPLANNED);
		assertThrows(VerifyException.class,
				() -> unscheduler.unscheduleRequest(0.0, vehicle.getId(), Id.create("req", Request.class)));
	}

	@Test
	void completedSchedule_throws() {
		when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.COMPLETED);
		assertThrows(VerifyException.class,
				() -> unscheduler.unscheduleRequest(0.0, vehicle.getId(), Id.create("req", Request.class)));
	}

	private DrtStopTask stopWithRequest(int idx, Id<Request> requestId, boolean pickup) {
		DrtStopTask stop = org.mockito.Mockito.mock(DrtStopTask.class);
		Map<Id<Request>, AcceptedDrtRequest> pickups = new HashMap<>();
		Map<Id<Request>, AcceptedDrtRequest> dropoffs = new HashMap<>();
		Id<Request> other = Id.create("other-" + idx, Request.class);
		pickups.put(other, mock(AcceptedDrtRequest.class));
		dropoffs.put(other, mock(AcceptedDrtRequest.class));
		if (pickup) {
			pickups.put(requestId, mock(AcceptedDrtRequest.class));
		} else {
			dropoffs.put(requestId, mock(AcceptedDrtRequest.class));
		}

		when(stop.getPickupRequests()).thenReturn(pickups);
		when(stop.getDropoffRequests()).thenReturn(dropoffs);

		return stop;
	}
}
