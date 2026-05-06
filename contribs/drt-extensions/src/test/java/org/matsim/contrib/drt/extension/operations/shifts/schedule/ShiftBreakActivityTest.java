package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtStopActivity;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.evrp.ChargingTask;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ShiftBreakActivityTest {

    @Mock private PassengerHandler passengerHandler;
    @Mock private DynAgent driver;
    @Mock private ShiftBreakTask shiftBreakTask;
    @Mock private ChargingTask chargingTask;
    @Mock private ChargingWithAssignmentLogic chargingLogic;
    @Mock private ElectricVehicle electricVehicle;
    
    private ShiftBreakActivity activity;
    private final double endTime = 3600.0; // 1 hour

    @BeforeEach
    public void setUp() {
        when(shiftBreakTask.getEndTime()).thenReturn(endTime);
        Map<Id<Request>, AcceptedDrtRequest> emptyRequests = new HashMap<>();
        activity = new ShiftBreakActivity(passengerHandler, driver, shiftBreakTask, emptyRequests, emptyRequests);
    }

    @Test
    public void testGetActivityType() {
        assertEquals("Shift break", activity.getActivityType());
    }



    @Test
    public void testDoSimStep_noCharging() {
        double now = 1800.0;
        activity.doSimStep(now);
        
        // Only the bus stop delegate should be called, no charging interactions
        verify(shiftBreakTask, times(2)).getChargingTask();
    }

    @Test
    public void testChargingInitialization_atConstruction() {
        when(shiftBreakTask.getChargingTask()).thenReturn(Optional.of(chargingTask));
        when(chargingTask.getChargingLogic()).thenReturn(chargingLogic);

        Map<Id<Request>, AcceptedDrtRequest> emptyRequests = new HashMap<>();
        ShiftBreakActivity activityWithCharging = new ShiftBreakActivity(
                passengerHandler, driver, shiftBreakTask, emptyRequests, emptyRequests);
        
        // Charging should be initialized
        double now = 1800.0;
        activityWithCharging.doSimStep(now);
        
        // Verify doSimStep was called, meaning charging was initialized
        verify(shiftBreakTask, times(2)).getChargingTask();
    }

    @Test
    public void testChargingInitialization_duringSimStep() {
        // First return empty, then return charging task on second call
        when(shiftBreakTask.getChargingTask())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(chargingTask));
        when(chargingTask.getChargingLogic()).thenReturn(chargingLogic);
        
        double now = 1800.0;
        activity.doSimStep(now);
        
        // First call should check for charging task
        verify(shiftBreakTask, times(2)).getChargingTask();
        
        // Second sim step, now with charging
        activity.doSimStep(now);
        
        // Should have checked for charging task again
        verify(shiftBreakTask, times(3)).getChargingTask();
    }

    @Test
    public void testFinalizeAction_withChargingInProgress() {
        when(shiftBreakTask.getChargingTask()).thenReturn(Optional.of(chargingTask));
        when(chargingTask.getChargingLogic()).thenReturn(chargingLogic);
        when(chargingTask.getElectricVehicle()).thenReturn(electricVehicle);
        
        Map<Id<Request>, AcceptedDrtRequest> emptyRequests = new HashMap<>();
        ShiftBreakActivity activityWithCharging = new ShiftBreakActivity(
                passengerHandler, driver, shiftBreakTask, emptyRequests, emptyRequests);
        
        double now = endTime - 300; // 5 minutes before end
        activityWithCharging.finalizeAction(now);
        
        // Should remove the vehicle from charging
        verify(chargingLogic).removeVehicle(electricVehicle, now);
    }

    @Test
    public void testFinalizeAction_chargingAlreadyCompleted() {
        when(shiftBreakTask.getChargingTask()).thenReturn(Optional.of(chargingTask));

        Map<Id<Request>, AcceptedDrtRequest> emptyRequests = new HashMap<>();
        ShiftBreakActivity activityWithCharging = new ShiftBreakActivity(
                passengerHandler, driver, shiftBreakTask, emptyRequests, emptyRequests);
        
        double now = endTime; // At end time
        activityWithCharging.finalizeAction(now);
        
        // Should not interact with charging logic
        verify(chargingTask, never()).getChargingLogic();
    }
}