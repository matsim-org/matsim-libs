package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableMap;

/**
 * Checks the reservation window that {@link DrtShiftDispatcherImpl} asks the
 * {@link OperationFacilityFinder} for when it looks for a facility to end a
 * shift. A shift changeover reserves the facility until the service end of the
 * vehicle, so the finder has to check availability until that (absolute) time.
 */
public class DrtShiftDispatcherImplTest {

	private static final double SERVICE_END_TIME = 30 * 3600;
	private static final double SHIFT_START_TIME = 8 * 3600;
	private static final double SHIFT_END_TIME = 16 * 3600;

	private final Id<DvrpVehicle> vehicleId = Id.create("vehicle", DvrpVehicle.class);

	private MobsimTimer timer;
	private OperationFacilityFinder finder;
	private ShiftDvrpVehicle vehicle;
	private Queue<DrtShift> vehicleShifts;
	private DrtShift shift;

	private DrtShift mockShift(String id, double start, double end) {
		DrtShift mockedShift = mock(DrtShift.class);
		when(mockedShift.getId()).thenReturn(Id.create(id, DrtShift.class));
		when(mockedShift.getStartTime()).thenReturn(start);
		when(mockedShift.getEndTime()).thenReturn(end);
		when(mockedShift.getOperationFacilityId()).thenReturn(Optional.empty());
		when(mockedShift.getDesignatedVehicleId()).thenReturn(Optional.empty());
		when(mockedShift.getBreak()).thenReturn(Optional.empty());
		when(mockedShift.getShiftType()).thenReturn(Optional.empty());
		return mockedShift;
	}

	@BeforeEach
	public void setUp() {
		timer = mock(MobsimTimer.class);
		finder = mock(OperationFacilityFinder.class);
		when(finder.findFacilityForTime(any(), any(), anyDouble(), anyDouble(), anyDouble(), anySet()))
				.thenReturn(Optional.empty());

		Link link = mock(Link.class);
		when(link.getId()).thenReturn(Id.createLinkId("hub"));

		// the vehicle is idle (staying) at the hub
		DrtStayTask stayTask = mock(DrtStayTask.class);
		when(stayTask.getTaskIdx()).thenReturn(0);
		when(stayTask.getTaskType()).thenReturn(DrtStayTask.TYPE);
		when(stayTask.getLink()).thenReturn(link);

		Schedule schedule = mock(Schedule.class);
		when(schedule.getCurrentTask()).thenReturn(stayTask);
		doReturn(List.of(stayTask)).when(schedule).getTasks();

		vehicleShifts = new ArrayDeque<>();
		vehicle = mock(ShiftDvrpVehicle.class);
		when(vehicle.getId()).thenReturn(vehicleId);
		when(vehicle.getServiceEndTime()).thenReturn(SERVICE_END_TIME);
		when(vehicle.getSchedule()).thenReturn(schedule);
		when(vehicle.getShifts()).thenReturn(vehicleShifts);
		doAnswer(invocation -> vehicleShifts.add(invocation.getArgument(0))).when(vehicle).addShift(any());

		shift = mockShift("shift", SHIFT_START_TIME, SHIFT_END_TIME);
	}

	private DrtShiftDispatcherImpl createDispatcher(ShiftEndLogic shiftEndLogic) {
		Fleet fleet = mock(Fleet.class);
		doReturn(ImmutableMap.of(vehicleId, vehicle)).when(fleet).getVehicles();

		Id<OperationFacility> hubId = Id.create("hub", OperationFacility.class);
		OperationFacility hub = mock(OperationFacility.class);
		when(hub.getId()).thenReturn(hubId);
		when(hub.getRegisteredVehicles()).thenReturn(Set.of(vehicleId));
		when(hub.getChargers()).thenReturn(List.of());

		OperationFacilities facilities = mock(OperationFacilities.class);
		when(facilities.getFacilities()).thenReturn(ImmutableMap.of(hubId, hub));

		ShiftScheduler shiftScheduler = mock(ShiftScheduler.class);
		when(shiftScheduler.initialSchedule()).thenReturn(ImmutableMap.of(Id.create("shift", DrtShift.class), shift));
		when(shiftScheduler.schedule(anyDouble(), any())).thenReturn(List.of());

		DrtShiftDispatcherImpl dispatcher = new DrtShiftDispatcherImpl("drt", fleet, timer, facilities, finder,
				mock(ShiftTaskScheduler.class), mock(EventsManager.class), new ShiftsParams(), shiftEntry -> true,
				shiftEndLogic, (v, s) -> true, shiftScheduler, mock(OperationFacilityReservationManager.class));
		dispatcher.initialize();
		return dispatcher;
	}

	private void dispatch(DrtShiftDispatcherImpl dispatcher, double now) {
		when(timer.getTimeOfDay()).thenReturn(now);
		dispatcher.dispatch(now);
	}

	private double capturedReservationEndTime(double latestArrival) {
		ArgumentCaptor<Double> reservationEndTime = ArgumentCaptor.forClass(Double.class);
		verify(finder).findFacilityForTime(any(), eq(vehicle), anyDouble(), eq(latestArrival),
				reservationEndTime.capture(), anySet());
		return reservationEndTime.getValue();
	}

	@Test
	public void testEarlyShiftEndChecksAvailabilityUntilServiceEnd() {
		DrtShiftDispatcherImpl dispatcher = createDispatcher((activeShift, now) -> true);

		dispatch(dispatcher, SHIFT_START_TIME - 1); // assigns the shift to the idle vehicle
		dispatch(dispatcher, SHIFT_START_TIME); // starts the shift
		dispatch(dispatcher, 10 * 3600 + 1); // the shift should end early

		assertEquals(SERVICE_END_TIME, capturedReservationEndTime(SERVICE_END_TIME), 0.0);
	}

	@Test
	public void testShiftEndUpdateChecksAvailabilityUntilServiceEnd() {
		DrtShiftDispatcherImpl dispatcher = createDispatcher(ShiftEndLogic.NEVER);

		dispatch(dispatcher, SHIFT_START_TIME - 1); // assigns the shift to the idle vehicle
		dispatch(dispatcher, SHIFT_START_TIME); // starts the shift

		// the shift end is only updated if a follow-up shift is assigned to the vehicle
		vehicleShifts.add(mockShift("next", SHIFT_END_TIME, SHIFT_END_TIME + 8 * 3600));

		// within shiftEndRescheduleLookAhead before the shift end, at a multiple of
		// updateShiftEndInterval (180 s)
		dispatch(dispatcher, SHIFT_END_TIME - 1440);

		assertEquals(SERVICE_END_TIME, capturedReservationEndTime(SHIFT_END_TIME), 0.0);
	}
}
