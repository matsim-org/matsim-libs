package org.matsim.contrib.drt.extension.operations.shifts.scheduler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.EvShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftBreakStopWaypoint;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.optimizer.StopWaypoint;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.IntegerLoad;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author nkuehnel / MOIA
 */
@ExtendWith(MockitoExtension.class)
public class ShiftTaskSchedulerImplTest {

    private ShiftTaskScheduler shiftTaskScheduler;
    private Network network;
    private List<Link> links;

    @Mock private ShiftDrtTaskFactory taskFactory;
    @Mock private ChargingInfrastructure chargingInfrastructure;
    @Mock private ChargingStrategy.Factory chargingStrategyFactory;

    @Mock private ShiftDvrpVehicle standardVehicle;
    @Mock private EvShiftDvrpVehicle evVehicle;
    @Mock private ElectricVehicle ev;
    @Mock private Battery battery;
    @Mock private Schedule schedule;
    @Mock private WaitForShiftTask waitForShiftTask;
    @Mock private ShiftBreakTask breakTask;
    @Mock private OperationFacility facility;
    @Mock private DrtShiftDispatcher.ShiftEntry shiftEntry;
    @Mock private ChargingTask chargingTask;

    private ShiftsParams shiftsParams;
    private final double now = 3600.0;

    @Mock private OperationFacilities operationFacilities;
    @Mock private OperationFacilityReservationManager facilityReservationManager;
    @Mock private TravelDisutility travelDisutility;
    @Mock private TravelTime travelTime;
    @Mock private OperationFacilityFinder operationFacilityFinder;
    @Mock private VehicleEntry.EntryFactory vEntryFactory;

    @Mock private DrtShiftDispatcher.ShiftEntry mockShiftEntry;
    @Mock private ShiftDvrpVehicle mockVehicle;
    @Mock private Schedule mockSchedule;

    @BeforeEach
    public void setUp() {

        shiftsParams = new ShiftsParams();
        shiftsParams.setChangeoverDuration(300);
        shiftsParams.setBreakChargerType("fast_charger");

        network = createTestNetwork();
        links = new ArrayList<>(network.getLinks().values());

        shiftTaskScheduler = new ShiftTaskSchedulerImpl(
                operationFacilities,
                taskFactory,
                network,
                facilityReservationManager,
                shiftsParams,
                travelDisutility,
                travelTime,
                operationFacilityFinder,
                vEntryFactory,
                new ScheduleTimingUpdater() {
                    @Override
                    public void updateBeforeNextTask(DvrpVehicle vehicle) {}

                    @Override
                    public void updateTimings(DvrpVehicle vehicle) {}

                    @Override
                    public void updateTimingsStartingFromTaskIdx(DvrpVehicle vehicle, int startIdx, double newBeginTime) {}
                },
                chargingStrategyFactory, chargingInfrastructure);
    }

    @Test
    public void testUpdateShiftBreak_withStopWaypointCurrentTask() {
        double now = 28800; // 8:00 AM
        DrtShiftBreak shiftBreak = createTestShiftBreak(now + 3600, now + 7200, 1800);

        when(mockShiftEntry.vehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getSchedule()).thenReturn(mockSchedule);

        // Current task is a STOP task
        DrtStopTask stopTask = mock(DrtStopTask.class);
        when(stopTask.getTaskIdx()).thenReturn(0);
        when(stopTask.getEndTime()).thenReturn(now + 600); // 8:10

        // Place the current task on the schedule
        when(mockSchedule.getCurrentTask()).thenReturn(stopTask);

        // Waypoints
        StopWaypoint stopWaypoint = mock(StopWaypoint.class);
        when(stopWaypoint.getTask()).thenReturn(stopTask);
        when(stopWaypoint.getLink()).thenReturn(links.getFirst());
        when(stopWaypoint.getDepartureTime()).thenReturn(now + 600);

        ShiftBreakTask breakTask = mock(ShiftBreakTask.class);
        Id<OperationFacility> facility1Id = Id.create("facility1", OperationFacility.class);
        Id<ReservationManager.Reservation> reservation1Id = Id.create("reservation1", ReservationManager.Reservation.class);
        when(breakTask.getFacilityId()).thenReturn(facility1Id);
        when(breakTask.getShiftBreak()).thenReturn(shiftBreak);
        when(breakTask.getReservationId()).thenReturn(Optional.of(reservation1Id));
        when(breakTask.getStatus()).thenReturn(Task.TaskStatus.PLANNED);

        ShiftBreakStopWaypoint breakWaypoint = mock(ShiftBreakStopWaypoint.class);
        when(breakWaypoint.getTask()).thenReturn(breakTask);

        DrtStopTask endTask = mock(DrtStopTask.class);
        when(endTask.getTaskIdx()).thenReturn(3); // Set the correct task index
        when(endTask.getBeginTime()).thenReturn(now + 7200); // 10:00

        StopWaypoint endWaypoint = mock(StopWaypoint.class);
        when(endWaypoint.getLink()).thenReturn(links.get(4));
        when(endWaypoint.getLatestArrivalTime()).thenReturn(now + 10800); // 11:00
        when(endWaypoint.getTask()).thenReturn(endTask);
        when(endWaypoint.scheduleWaitBeforeDrive()).thenReturn(false);

        // ==== REAL VehicleEntry instead of mocking fields ====
        Waypoint.Start start = new Waypoint.Start(stopTask, links.get(0), now, IntegerLoad.fromValue(0));
        ImmutableList<StopWaypoint> stops = ImmutableList.of(stopWaypoint, breakWaypoint, endWaypoint);
        double[] slackTimes = new double[stops.size() + 2];
        List<Double> precedingStayTimes = Arrays.asList(0.0, 0.0, 0.0);

        VehicleEntry realEntry = new VehicleEntry(
                mockVehicle, start, stops, slackTimes, precedingStayTimes, now);

        when(vEntryFactory.create(eq(mockVehicle), eq(now))).thenReturn(realEntry);


        List<Task> scheduleTasks = new ArrayList<>();
        scheduleTasks.add(stopTask);
        scheduleTasks.add(mock(Task.class));
        scheduleTasks.add(breakTask);
        scheduleTasks.add(endTask);
        doReturn(scheduleTasks).when(mockSchedule).getTasks();

        // Facility and path to facility
        OperationFacility facility = mock(OperationFacility.class);
        when(facility.getId()).thenReturn(Id.create("facility2", OperationFacility.class));
        when(facility.getLinkId()).thenReturn(links.get(2).getId());

        double arrivalAtFacility = now + 1200; // 8:20

        VrpPathWithTravelData pathToFacility = mock(VrpPathWithTravelData.class);
        when(pathToFacility.getFromLink()).thenReturn(links.get(0));
        when(pathToFacility.getToLink()).thenReturn(links.get(2));
        when(pathToFacility.getArrivalTime()).thenReturn(arrivalAtFacility);
        when(pathToFacility.getTravelTime()).thenReturn(600.0);
        when(pathToFacility.withDepartureTime(anyDouble())).thenAnswer(inv -> pathToFacility);

        OperationFacilityFinder.FacilityWithPath facilityWithPath =
                new OperationFacilityFinder.FacilityWithPath(facility, pathToFacility);

        when(operationFacilityFinder.findFacilityForTime(
                any(), eq(mockVehicle), anyDouble(), anyDouble(), anyDouble(), any())
        ).thenReturn(Optional.of(facilityWithPath));

        double continuationArrivalAtFacility = now + 6000; // 8:20


        // Continuation path from facility -> endWaypoint (calc via static VrpPaths)
        VrpPathWithTravelData continuationPath = mock(VrpPathWithTravelData.class);

        when(continuationPath.getArrivalTime()).thenReturn(continuationArrivalAtFacility); // 9:40


        // Reservation
        @SuppressWarnings("unchecked")
        ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle> reservationInfo =
                (ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle>) mock(ReservationManager.ReservationInfo.class);
        when(reservationInfo.reservationId()).thenReturn(Id.create("new_reservation", ReservationManager.Reservation.class));
        when(facilityReservationManager.addReservation(eq(facility), eq(mockVehicle), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(reservationInfo));

        // Factory returns
        DrtStayTask waitTask = mock(DrtStayTask.class);
        when(waitTask.getTaskIdx()).thenReturn(1);

        DrtDriveTask driveToFacilityTask = mock(DrtDriveTask.class);
        when(driveToFacilityTask.getTaskIdx()).thenReturn(2);
        when(driveToFacilityTask.getEndTime()).thenReturn(arrivalAtFacility);
        ShiftBreakTask newBreakTask = mock(ShiftBreakTask.class);
        when(newBreakTask.getTaskIdx()).thenReturn(3);


        DrtDriveTask continuationDriveTask = mock(DrtDriveTask.class);
        when(continuationDriveTask.getTaskIdx()).thenReturn(4);
        when(continuationDriveTask.getEndTime()).thenReturn(continuationArrivalAtFacility);

        when(taskFactory.createStayTask(eq(mockVehicle), anyDouble(), anyDouble(), any(Link.class)))
                .thenReturn(waitTask);

        when(taskFactory.createDriveTask(eq(mockVehicle), any(VrpPathWithTravelData.class), any()))
                .thenReturn(driveToFacilityTask, continuationDriveTask);

        when(taskFactory.createShiftBreakTask(
                eq(mockVehicle), anyDouble(), anyDouble(), any(Link.class),
                eq(shiftBreak), any(Id.class), any(Id.class)))
                .thenReturn(newBreakTask);

        try (MockedStatic<VrpPaths> mockedVrpPaths = mockStatic(VrpPaths.class)) {
            mockedVrpPaths.when(() -> VrpPaths.calcAndCreatePath(
                    any(Link.class),                 // fromLink
                    any(Link.class),                 // toLink
                    anyDouble(),                     // departureTime
                    any(),                           // LeastCostPathCalculator (router)
                    any(TravelTime.class)            // travelTime
            )).thenReturn(continuationPath);

            // Execute
            shiftTaskScheduler.updateShiftBreak(mockShiftEntry, now);
        }

        // Verifications
        verify(facilityReservationManager).removeReservation(
                eq(facility1Id),
                eq(reservation1Id));

        verify(taskFactory, times(2)).createDriveTask(eq(mockVehicle), any(VrpPathWithTravelData.class), any());
        verify(taskFactory).createShiftBreakTask(eq(mockVehicle), anyDouble(), anyDouble(), any(Link.class),
                eq(shiftBreak), any(Id.class), any(Id.class));
        verify(mockSchedule, atLeast(1)).addTask(anyInt(), any(Task.class));
    }

    @Test
    public void testUpdateWaitingVehicleWithCharging_standardVehicle() {
        boolean result = shiftTaskScheduler.updateWaitingVehicleWithCharging(standardVehicle, now);

        // Standard vehicle shouldn't be updated with charging
        assertFalse(result);
        verifyNoInteractions(chargingInfrastructure);
    }

    @Test
    public void testUpdateVehicleWithCharging_evVehicleNotOnWaitTask() {
        when(evVehicle.getSchedule()).thenReturn(schedule);
        when(schedule.getCurrentTask()).thenReturn(mock(Task.class)); // Not a wait task
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);

        boolean result = shiftTaskScheduler.updateWaitingVehicleWithCharging(evVehicle, now);

        // EV not on break task shouldn't be updated
        assertFalse(result);
    }

    @Test
    public void testUpdateWaitingVehicleWithCharging_evVehicleOnBreakWithoutFacility() {
        when(evVehicle.getSchedule()).thenReturn(schedule);
        when(schedule.getCurrentTask()).thenReturn(waitForShiftTask); // Is a ShiftBreakTask
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED); // Is a ShiftBreakTask
        when(waitForShiftTask.getStatus()).thenReturn(Task.TaskStatus.STARTED);
        when(waitForShiftTask.getFacilityId()).thenReturn(Id.create("facility1", OperationFacility.class));
        when(operationFacilities.getFacilities()).thenReturn(ImmutableMap.of()); // Facility not found
        when(evVehicle.getElectricVehicle()).thenReturn(ev);
        when(ev.getBattery()).thenReturn(battery);

        boolean result = shiftTaskScheduler.updateWaitingVehicleWithCharging(evVehicle, now);

        // No facility found, shouldn't update
        assertFalse(result);
    }

    @Test
    public void testUpdateWaitingVehicleWithCharging_evVehicleAlreadyHasCharging() {
        when(evVehicle.getSchedule()).thenReturn(schedule);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
        when(schedule.getCurrentTask()).thenReturn(waitForShiftTask);
        when(waitForShiftTask.getStatus()).thenReturn(Task.TaskStatus.STARTED);
        when(waitForShiftTask.getChargingTask()).thenReturn(Optional.of(chargingTask));

        boolean result = shiftTaskScheduler.updateWaitingVehicleWithCharging(evVehicle, now);

        // Already has charging, shouldn't update
        assertFalse(result);
    }

    @Test
    public void testUpdateWaitingVehicleWithCharging_evVehicleWithChargerButFullyCharged() {
        // This test simulates the situation where there's a charger available but the vehicle is fully charged

        when(evVehicle.getSchedule()).thenReturn(schedule);
        when(schedule.getCurrentTask()).thenReturn(waitForShiftTask);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
        when(waitForShiftTask.getChargingTask()).thenReturn(Optional.empty()); // No charging yet
        when(waitForShiftTask.getStatus()).thenReturn(Task.TaskStatus.STARTED);
        when(evVehicle.getElectricVehicle()).thenReturn(ev);
        when(ev.getBattery()).thenReturn(battery);
        when(battery.getCharge()).thenReturn(100.0); // High charge
        when(battery.getCapacity()).thenReturn(100.0); // Max capacity
        //when(shiftsParams.getChargeDuringBreakThreshold()).thenReturn(0.2); // 20% threshold

        boolean result = shiftTaskScheduler.updateWaitingVehicleWithCharging(evVehicle, now);

        // Battery is at 100%, shouldn't need charging
        assertFalse(result);

        // Should still check the SOC but not proceed to find chargers
        verify(battery).getCharge();
        verify(battery).getCapacity();
        verify(facility, never()).getChargers();
    }

    @Test
    public void testUpdateShiftBreak_standardVehicle() {
        when(shiftEntry.vehicle()).thenReturn(standardVehicle);

        // Just verify that it doesn't throw an exception and delegates correctly
        // The complex behavior is tested in ShiftTaskSchedulerImplTest
        boolean result = shiftTaskScheduler.updateShiftBreak(shiftEntry, now);

        // Should interact with vEntryFactory at minimum
        verify(vEntryFactory).create(eq(standardVehicle), anyDouble());
    }



    
    // ---------------- helpers ----------------

    private Network createTestNetwork() {
        Network network = NetworkUtils.createNetwork();

        Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(0, 0));
        Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(1000, 0));
        Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord(2000, 0));
        Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord(3000, 0));
        Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord(4000, 0));

        NetworkUtils.createAndAddLink(network, Id.createLinkId("1_2"), node1, node2, 1000, 30 / 3.6, 2000, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("2_3"), node2, node3, 1000, 30 / 3.6, 2000, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("3_4"), node3, node4, 1000, 30 / 3.6, 2000, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("4_5"), node4, node5, 1000, 30 / 3.6, 2000, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("5_1"), node5, node1, 4000, 30 / 3.6, 8000, 1);

        return network;
    }

    private DrtShiftBreak createTestShiftBreak(double earliestStartTime, double latestEndTime, double duration) {
        return new DrtShiftBreak() {
            @Override public double getEarliestBreakStartTime() { return earliestStartTime; }
            @Override public double getLatestBreakEndTime() { return latestEndTime; }
            @Override public double getDuration() { return duration; }
        };
    }
}
