package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.common.util.reservation.ReservationManager.ReservationInfo;
import org.matsim.contrib.common.util.reservation.ReservationManager.ReservationStatus;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.common.collect.ImmutableMap;

/**
 * Facility reservations handled by the dispatcher: a vehicle waiting at a facility holds a reservation until its
 * service end, which is shortened once its next shift is assigned; a break reservation is shortened when the break
 * starts; and a reservation is released when the last task using it ends.
 */
public class DrtShiftDispatcherReservationTest {

	private static final double PREVIOUS_SHIFT_END_TIME = 10 * 3600;
	private static final double SERVICE_END_TIME = 30 * 3600;
	private static final double SHIFT_START_TIME = 16 * 3600;

	private final Id<DvrpVehicle> vehicleId = Id.create("vehicle", DvrpVehicle.class);
	private final Id<OperationFacility> hubId = Id.create("hub", OperationFacility.class);
	private final Id<ReservationManager.Reservation> reservationId = Id.create("hub_0",
			ReservationManager.Reservation.class);

	private MobsimTimer timer;
	private OperationFacility hub;
	private Schedule schedule;
	private WaitForShiftTask waitTask;
	private Queue<DrtShift> vehicleShifts;
	private ShiftDvrpVehicle vehicle;
	private OperationFacilityReservationManager reservationManager;

	@BeforeEach
	public void setUp() {
		timer = mock(MobsimTimer.class);

		hub = mock(OperationFacility.class);
		when(hub.getId()).thenReturn(hubId);
		when(hub.getRegisteredVehicles()).thenReturn(Set.of(vehicleId));
		when(hub.getChargers()).thenReturn(List.of());

		// the vehicle waits at the hub after its previous shift (changeover and wait share the reservation)
		when(hub.register(vehicleId)).thenReturn(true);
		when(hub.deregisterVehicle(vehicleId)).thenReturn(true);

		waitTask = mock(WaitForShiftTask.class);
		when(waitTask.getFacilityId()).thenReturn(hubId);
		when(waitTask.getReservationId()).thenReturn(Optional.of(reservationId));
		when(waitTask.getTaskIdx()).thenReturn(0);

		schedule = mock(Schedule.class);
		when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
		when(schedule.getCurrentTask()).thenReturn(waitTask);
		doReturn(List.of(waitTask)).when(schedule).getTasks();
		when(schedule.getTaskCount()).thenReturn(1);

		vehicleShifts = new ArrayDeque<>();
		vehicle = mock(ShiftDvrpVehicle.class);
		when(vehicle.getId()).thenReturn(vehicleId);
		when(vehicle.getServiceEndTime()).thenReturn(SERVICE_END_TIME);
		when(vehicle.getSchedule()).thenReturn(schedule);
		when(vehicle.getShifts()).thenReturn(vehicleShifts);
		doAnswer(invocation -> vehicleShifts.add(invocation.getArgument(0))).when(vehicle).addShift(any());

		reservationManager = mock(OperationFacilityReservationManager.class);
	}

	private DrtShiftDispatcherImpl createDispatcher() {
		DrtShift shift = mock(DrtShift.class);
		when(shift.getId()).thenReturn(Id.create("shift", DrtShift.class));
		when(shift.getStartTime()).thenReturn(SHIFT_START_TIME);
		when(shift.getEndTime()).thenReturn(SHIFT_START_TIME + 8 * 3600);
		when(shift.getOperationFacilityId()).thenReturn(Optional.empty());
		when(shift.getDesignatedVehicleId()).thenReturn(Optional.empty());
		when(shift.getBreak()).thenReturn(Optional.empty());
		when(shift.getShiftType()).thenReturn(Optional.empty());

		Fleet fleet = mock(Fleet.class);
		doReturn(ImmutableMap.of(vehicleId, vehicle)).when(fleet).getVehicles();

		OperationFacilities facilities = mock(OperationFacilities.class);
		when(facilities.getFacilities()).thenReturn(ImmutableMap.of(hubId, hub));

		ShiftScheduler shiftScheduler = mock(ShiftScheduler.class);
		when(shiftScheduler.initialSchedule()).thenReturn(ImmutableMap.of(Id.create("shift", DrtShift.class), shift));
		when(shiftScheduler.schedule(anyDouble(), any())).thenReturn(List.of());

		DrtShiftDispatcherImpl dispatcher = new DrtShiftDispatcherImpl("drt", fleet, timer, facilities,
				mock(OperationFacilityFinder.class), mock(ShiftTaskScheduler.class), mock(EventsManager.class),
				new ShiftsParams(), shiftEntry -> true, ShiftEndLogic.NEVER, (v, s) -> true, shiftScheduler,
				reservationManager);
		dispatcher.initialize();
		return dispatcher;
	}

	private void assignShift(DrtShiftDispatcherImpl dispatcher) {
		// within shiftScheduleLookAhead (1800 s) before the shift start, so the shift is assigned but not started
		double now = SHIFT_START_TIME - 1001;
		when(timer.getTimeOfDay()).thenReturn(now);
		dispatcher.dispatch(now);
	}

	private void existingReservationUntil(double endTime) {
		when(reservationManager.findReservation(hubId, reservationId)).thenReturn(Optional.of(new ReservationInfo<>(
				reservationId, hub, vehicle, PREVIOUS_SHIFT_END_TIME, endTime, ReservationStatus.CONFIRMED)));
	}

	@Test
	public void testReservationIsShortenedToFollowUpShiftStart() {
		existingReservationUntil(SERVICE_END_TIME);
		DrtShiftDispatcherImpl dispatcher = createDispatcher();

		assignShift(dispatcher);

		verify(reservationManager).updateReservation(hubId, reservationId, PREVIOUS_SHIFT_END_TIME, SHIFT_START_TIME);
	}

	@Test
	public void testReservationIsKeptWhileVehicleServesAnotherShift() {
		existingReservationUntil(SERVICE_END_TIME);
		// the wait task is planned at the end of the schedule, but the vehicle is still serving its previous shift
		when(schedule.getCurrentTask()).thenReturn(mock(DrtStayTask.class));
		DrtShiftDispatcherImpl dispatcher = createDispatcher();

		assignShift(dispatcher);

		verify(reservationManager, never()).updateReservation(any(), any(), anyDouble(), anyDouble());
	}

	@Test
	public void testBreakReservationIsShortenedWhenBreakStarts() {
		Id<ReservationManager.Reservation> breakReservationId = Id.create("hub_1", ReservationManager.Reservation.class);
		double breakStart = 12 * 3600;
		double duration = 1800;
		double latestBreakEnd = 14 * 3600;
		when(reservationManager.findReservation(hubId, breakReservationId)).thenReturn(Optional.of(new ReservationInfo<>(
				breakReservationId, hub, vehicle, 11 * 3600, latestBreakEnd, ReservationStatus.CONFIRMED)));

		DrtShiftBreak shiftBreak = mock(DrtShiftBreak.class);
		when(shiftBreak.getDuration()).thenReturn(duration);
		Link link = mock(Link.class);
		when(link.getId()).thenReturn(Id.createLinkId("link"));
		ShiftBreakTask breakTask = mock(ShiftBreakTask.class);
		when(breakTask.getFacilityId()).thenReturn(hubId);
		when(breakTask.getReservationId()).thenReturn(Optional.of(breakReservationId));
		when(breakTask.getShiftBreak()).thenReturn(shiftBreak);
		when(breakTask.getEndTime()).thenReturn(breakStart + duration);
		when(breakTask.getLink()).thenReturn(link);
		vehicleShifts.add(mock(DrtShift.class));

		DrtShiftDispatcherImpl dispatcher = createDispatcher();
		when(timer.getTimeOfDay()).thenReturn(breakStart);
		dispatcher.startOperationalTask(vehicle, breakTask);

		verify(reservationManager).updateReservation(hubId, breakReservationId, 11 * 3600, breakStart + duration);
	}

	@Test
	public void testReservationIsReleasedWhenWaitEnds() {
		DrtShiftDispatcherImpl dispatcher = createDispatcher();

		dispatcher.endOperationalTask(vehicle, waitTask);

		verify(reservationManager).removeReservation(hubId, reservationId);
	}

	@Test
	public void testReservationIsKeptWhenChangeoverIsFollowedByWait() {
		ShiftChangeOverTask changeover = mock(ShiftChangeOverTask.class);
		when(changeover.getFacilityId()).thenReturn(hubId);
		when(changeover.getReservationId()).thenReturn(Optional.of(reservationId));
		when(changeover.getTaskIdx()).thenReturn(0);
		when(schedule.getCurrentTask()).thenReturn(changeover);
		doReturn(List.of(changeover, waitTask)).when(schedule).getTasks();
		when(schedule.getTaskCount()).thenReturn(2);
		DrtShiftDispatcherImpl dispatcher = createDispatcher();

		dispatcher.endOperationalTask(vehicle, changeover);

		verify(reservationManager, never()).removeReservation(any(), any());
	}

	@Test
	public void testReservationEndingBeforeShiftStartIsKept() {
		existingReservationUntil(SHIFT_START_TIME - 600);
		DrtShiftDispatcherImpl dispatcher = createDispatcher();

		assignShift(dispatcher);

		verify(reservationManager, never()).updateReservation(any(), any(), anyDouble(), anyDouble());
	}
}
