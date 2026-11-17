package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

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
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeoverTaskImpl;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.common.collect.ImmutableMap;

/**
 * A vehicle whose shift ends within {@code shiftEndLookAhead} has moved from the active to the ending shifts. It
 * should still be reusable for a follow-up shift, and once it holds a follow-up shift that starts at a fixed facility,
 * the changeover of the ending shift must not be moved elsewhere.
 * <p>
 * Default lookaheads: shifts are assigned 1800 s before their start and become ending shifts 3600 s before their end,
 * so a follow-up shift is always assigned after its predecessor has become an ending shift.
 */
public class DrtShiftDispatcherReuseTest {

	private static final double FIRST_START = 8 * 3600;
	private static final double FIRST_END = 16 * 3600;
	// starts 1200 s after the first shift ends (changeover takes 900 s), so it is assigned 600 s before that end
	private static final double SECOND_START = FIRST_END + 1200;
	private static final double SECOND_END = 22 * 3600;
	private static final double SERVICE_END_TIME = 30 * 3600;

	private final Id<DvrpVehicle> vehicleId = Id.create("vehicle", DvrpVehicle.class);
	private final Id<OperationFacility> hubId = Id.create("hub", OperationFacility.class);

	private MobsimTimer timer;
	private OperationFacility hub;
	private Schedule schedule;
	private ShiftDvrpVehicle vehicle;
	private OperationFacilityFinder finder;

	@BeforeEach
	public void setUp() {
		timer = mock(MobsimTimer.class);
		finder = mock(OperationFacilityFinder.class);

		hub = mock(OperationFacility.class);
		when(hub.getId()).thenReturn(hubId);
		when(hub.getRegisteredVehicles()).thenReturn(Set.of(vehicleId));
		when(hub.getChargers()).thenReturn(List.of());

		Link link = mock(Link.class);
		when(link.getId()).thenReturn(Id.createLinkId("link"));

		WaitForShiftTask waitTask = mock(WaitForShiftTask.class);
		when(waitTask.getFacilityId()).thenReturn(hubId);
		when(waitTask.getTaskIdx()).thenReturn(0);
		when(waitTask.getLink()).thenReturn(link);
		when(waitTask.getTaskType()).thenReturn(WaitForShiftTask.TYPE);

		// the changeover of the first shift is scheduled at the hub
		ShiftChangeOverTask changeover = mock(ShiftChangeOverTask.class);
		when(changeover.getFacilityId()).thenReturn(hubId);
		when(changeover.getTaskIdx()).thenReturn(1);
		when(changeover.getTaskType()).thenReturn(ShiftChangeoverTaskImpl.TYPE);
		when(changeover.getBeginTime()).thenReturn(FIRST_END);

		schedule = mock(Schedule.class);
		when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
		when(schedule.getCurrentTask()).thenReturn(waitTask);
		doReturn(List.of(waitTask, changeover)).when(schedule).getTasks();
		doAnswer(invocation -> Stream.of(waitTask, changeover)).when(schedule).tasks();

		Queue<DrtShift> vehicleShifts = new ArrayDeque<>();
		vehicle = mock(ShiftDvrpVehicle.class);
		when(vehicle.getId()).thenReturn(vehicleId);
		when(vehicle.getServiceEndTime()).thenReturn(SERVICE_END_TIME);
		when(vehicle.getSchedule()).thenReturn(schedule);
		when(vehicle.getShifts()).thenReturn(vehicleShifts);
		doAnswer(invocation -> vehicleShifts.add(invocation.getArgument(0))).when(vehicle).addShift(any());
	}

	private DrtShift shift(String id, double start, double end, Optional<Id<OperationFacility>> facility) {
		Id<DrtShift> shiftId = Id.create(id, DrtShift.class);
		DrtShift shift = mock(DrtShift.class);
		when(shift.getId()).thenReturn(shiftId);
		when(shift.getStartTime()).thenReturn(start);
		when(shift.getEndTime()).thenReturn(end);
		when(shift.getOperationFacilityId()).thenReturn(facility);
		when(shift.getDesignatedVehicleId()).thenReturn(Optional.empty());
		when(shift.getBreak()).thenReturn(Optional.empty());
		when(shift.getShiftType()).thenReturn(Optional.empty());
		when(shift.compareTo(any())).thenAnswer(invocation -> shiftId.compareTo(((DrtShift) invocation.getArgument(0)).getId()));
		return shift;
	}

	private DrtShiftDispatcherImpl createDispatcher(DrtShift first, DrtShift second) {
		Fleet fleet = mock(Fleet.class);
		doReturn(ImmutableMap.of(vehicleId, vehicle)).when(fleet).getVehicles();

		OperationFacilities facilities = mock(OperationFacilities.class);
		when(facilities.getFacilities()).thenReturn(ImmutableMap.of(hubId, hub));

		ImmutableMap<Id<DrtShift>, DrtShift> initialSchedule = ImmutableMap.of(first.getId(), first, second.getId(),
				second);
		ShiftScheduler shiftScheduler = mock(ShiftScheduler.class);
		when(shiftScheduler.initialSchedule()).thenReturn(initialSchedule);
		when(shiftScheduler.schedule(anyDouble(), any())).thenReturn(List.of());

		DrtShiftDispatcherImpl dispatcher = new DrtShiftDispatcherImpl("drt", fleet, timer, facilities, finder,
				mock(ShiftTaskScheduler.class), mock(EventsManager.class), new ShiftsParams(), shiftEntry -> true,
				ShiftEndLogic.NEVER, (v, s) -> true, shiftScheduler, mock(OperationFacilityReservationManager.class));
		dispatcher.initialize();
		return dispatcher;
	}

	private void dispatch(DrtShiftDispatcherImpl dispatcher, double now) {
		when(timer.getTimeOfDay()).thenReturn(now);
		dispatcher.dispatch(now);
	}

	/**
	 * Assigns the first shift to the (only, idle) vehicle, starts it, and then dispatches at the time the follow-up
	 * shift becomes assignable. The vehicle is no longer idle, so it can only be found as a reuse vehicle.
	 */
	private void runUntilFollowUpAssignment(DrtShiftDispatcherImpl dispatcher) {
		dispatch(dispatcher, FIRST_START - 1);
		dispatch(dispatcher, FIRST_START);
		dispatch(dispatcher, SECOND_START - 1800);
	}

	@Test
	public void testVehicleOfEndingShiftIsReused() {
		DrtShift first = shift("first", FIRST_START, FIRST_END, Optional.of(hubId));
		DrtShift second = shift("second", SECOND_START, SECOND_END, Optional.empty());
		DrtShiftDispatcherImpl dispatcher = createDispatcher(first, second);

		runUntilFollowUpAssignment(dispatcher);

		assertThat(vehicle.getShifts()).containsExactly(first, second);
	}

	@Test
	public void testShiftEndIsNotMovedAwayFromFixedStartOfFollowUpShift() {
		DrtShift first = shift("first", FIRST_START, FIRST_END, Optional.empty());
		DrtShift second = shift("second", SECOND_START, SECOND_END, Optional.of(hubId));
		DrtShiftDispatcherImpl dispatcher = createDispatcher(first, second);

		runUntilFollowUpAssignment(dispatcher);
		assertThat(vehicle.getShifts()).containsExactly(first, second);

		// next shift end update (every 180 s: 57060 s = 317 * 180 s), within shiftEndRescheduleLookAhead of the first
		// shift's end
		dispatch(dispatcher, FIRST_END - 540);

		verify(finder, never()).findFacilityForTime(any(), any(), anyDouble(), anyDouble(), anyDouble(), any());
	}
}
