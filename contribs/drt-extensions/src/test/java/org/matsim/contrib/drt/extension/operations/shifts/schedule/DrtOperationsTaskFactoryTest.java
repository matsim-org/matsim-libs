package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class DrtOperationsTaskFactoryTest {

    @Mock private DrtTaskFactory drtTaskFactory;
    @Mock private OperationFacilities operationFacilities;
    @Mock private OperationFacilityReservationManager reservationManager;
    @Mock private DvrpVehicle vehicle;
    @Mock private Link link;
    @Mock private Link startLink;
    @Mock private VrpPathWithTravelData path;
    @Mock private DrtTaskType taskType;
    @Mock private DrtShiftBreak shiftBreak;
    @Mock private DrtShift shift;
    @Mock private DrtDriveTask drtDriveTask;
    @Mock private DrtStayTask drtStayTask;
    @Mock private DrtStopTask drtStopTask;
    @Mock private OperationFacility facility;
    
    private DrtOperationsTaskFactory taskFactory;
    private final double beginTime = 3600.0;
    private final double endTime = 7200.0;

    @BeforeEach
    public void setUp() {
        taskFactory = new DrtOperationsTaskFactory(drtTaskFactory, operationFacilities, reservationManager);
    }

    @Test
    public void testCreateDriveTask() {
        when(drtTaskFactory.createDriveTask(vehicle, path, taskType)).thenReturn(drtDriveTask);
        
        DrtDriveTask result = taskFactory.createDriveTask(vehicle, path, taskType);
        
        assertSame(drtDriveTask, result);
        verify(drtTaskFactory).createDriveTask(vehicle, path, taskType);
    }

    @Test
    public void testCreateStayTask() {
        when(drtTaskFactory.createStayTask(vehicle, beginTime, endTime, link)).thenReturn(drtStayTask);
        
        DrtStayTask result = taskFactory.createStayTask(vehicle, beginTime, endTime, link);
        
        assertSame(drtStayTask, result);
        verify(drtTaskFactory).createStayTask(vehicle, beginTime, endTime, link);
    }

    @Test
    public void testCreateStopTask() {
        when(drtTaskFactory.createStopTask(vehicle, beginTime, endTime, link)).thenReturn(drtStopTask);
        
        DrtStopTask result = taskFactory.createStopTask(vehicle, beginTime, endTime, link);
        
        assertSame(drtStopTask, result);
        verify(drtTaskFactory).createStopTask(vehicle, beginTime, endTime, link);
    }

    @Test
    public void testCreateShiftBreakTask() {
        Id<OperationFacility> facilityId = Id.create("facility1", OperationFacility.class);
        Id<ReservationManager.Reservation> reservationId = Id.create("reservation1", ReservationManager.Reservation.class);
        
        ShiftBreakTask result = taskFactory.createShiftBreakTask(
                vehicle, beginTime, endTime, link, shiftBreak, facilityId, reservationId);
        
        assertNotNull(result);
        assertEquals(beginTime, result.getBeginTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(facilityId, result.getFacilityId());
        assertEquals(reservationId, result.getReservationId().get());
        assertEquals(shiftBreak, result.getShiftBreak());
        
        // Should NOT have charging initially
        assertTrue(result.getChargingTask().isEmpty());
    }

    @Test
    public void testCreateShiftChangeoverTask() {
        Id<OperationFacility> facilityId = Id.create("facility1", OperationFacility.class);
        Id<ReservationManager.Reservation> reservationId = Id.create("reservation1", ReservationManager.Reservation.class);
        
        ShiftChangeOverTask result = taskFactory.createShiftChangeoverTask(
                vehicle, beginTime, endTime, link, shift, facilityId, reservationId);
        
        assertNotNull(result);
        assertEquals(beginTime, result.getBeginTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(facilityId, result.getFacilityId());
        assertEquals(reservationId, result.getReservationId().get());
        assertEquals(shift, result.getShift());
    }

    @Test
    public void testCreateWaitForShiftStayTask() {
        Id<OperationFacility> facilityId = Id.create("facility1", OperationFacility.class);
        Id<ReservationManager.Reservation> reservationId = Id.create("reservation1", ReservationManager.Reservation.class);
        
        WaitForShiftTask result = taskFactory.createWaitForShiftStayTask(
                vehicle, beginTime, endTime, link, facilityId, reservationId);
        
        assertNotNull(result);
        assertEquals(beginTime, result.getBeginTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(facilityId, result.getFacilityId());
        assertEquals(reservationId, result.getReservationId().get());
    }

    @Test
    public void testCreateInitialTask_success() {
        // Mock vehicle
        when(vehicle.getStartLink()).thenReturn(startLink);
        when(vehicle.getServiceBeginTime()).thenReturn(beginTime);
        when(vehicle.getServiceEndTime()).thenReturn(endTime);
        when(vehicle.getId()).thenReturn(Id.create("vehicle1", DvrpVehicle.class));
        Id<Link> linkId = Id.create("link1", Link.class);
        when(startLink.getId()).thenReturn(linkId);
        
        // Mock facility lookup
        Id<OperationFacility> facilityId = Id.create("facility1", OperationFacility.class);
        when(facility.getId()).thenReturn(facilityId);
        when(facility.getLinkId()).thenReturn(linkId);
        ImmutableMap<Id<OperationFacility>, OperationFacility> map = ImmutableMap.of(facility.getId(), facility);
        when(operationFacilities.getFacilities()).thenReturn(map); // Then with full lookup

        // Mock reservation
        Id<ReservationManager.Reservation> reservationId = Id.create("reservation1", ReservationManager.Reservation.class);
        ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle> reservationInfo = 
                mock(ReservationManager.ReservationInfo.class);
        when(reservationInfo.reservationId()).thenReturn(reservationId);
        when(reservationManager.addReservation(facility, vehicle, beginTime, endTime))
                .thenReturn(Optional.of(reservationInfo));
        
        DefaultStayTask result = taskFactory.createInitialTask(vehicle, beginTime, endTime, link);
        
        assertNotNull(result);
        assertTrue(result instanceof WaitForShiftTask);
        verify(facility).register(vehicle.getId());
    }
}