package org.matsim.contrib.drt.prebooking.unscheduler;

import com.google.common.base.VerifyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleRequestUnschedulerTest {
	@Mock
	private DvrpVehicleLookup vehicleLookup;
	@Mock
	private DvrpVehicle vehicle;
	@Mock
	private Schedule schedule;

	private SimpleRequestUnscheduler unscheduler;

	@BeforeEach
	void setUp() {
		unscheduler = new SimpleRequestUnscheduler(vehicleLookup);
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
		when(schedule.getTasks()).thenReturn((List) tasks);
		when(schedule.getTaskCount()).thenReturn(tasks.size());

		unscheduler.unscheduleRequest(0.0, vehicle.getId(), requestId);

		verify(pickup).removePickupRequest(requestId);
		verify(dropoff).removeDropoffRequest(requestId);
	}

	@Test
	void startedSchedule_searchesFromCurrentTask() {
		Id<Request> requestId = Id.create("req", Request.class);

		DrtStopTask alreadyDone = stopWithRequest(0, Id.create("other", Request.class), true);
		DrtStopTask pickup = stopWithRequest(1, requestId, true);
		DrtStopTask dropoff = stopWithRequest(2, requestId, false);

		List<Task> tasks = new ArrayList<>();
		tasks.add(alreadyDone);
		tasks.add(pickup);
		tasks.add(dropoff);

		when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
		when(schedule.getCurrentTask()).thenReturn(alreadyDone);
		when(alreadyDone.getTaskIdx()).thenReturn(0);
		when(schedule.getTasks()).thenReturn((List) tasks);
		when(schedule.getTaskCount()).thenReturn(tasks.size());

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
		pickups.put(other, org.mockito.Mockito.mock(AcceptedDrtRequest.class));
		dropoffs.put(other, org.mockito.Mockito.mock(AcceptedDrtRequest.class));
		if (pickup) {
			pickups.put(requestId, org.mockito.Mockito.mock(AcceptedDrtRequest.class));
		} else {
			dropoffs.put(requestId, org.mockito.Mockito.mock(AcceptedDrtRequest.class));
		}

		when(stop.getPickupRequests()).thenReturn(pickups);
		when(stop.getDropoffRequests()).thenReturn(dropoffs);

		return stop;
	}
}
