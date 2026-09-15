package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
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
public class ChangeoverActivityTest {

    @Mock private PassengerHandler passengerHandler;
    @Mock private DynAgent driver;
    @Mock private ShiftChangeOverTask changeoverTask;
    @Mock private ChargingTask chargingTask;
    @Mock private ChargingWithAssignmentLogic chargingLogic;
    @Mock private ElectricVehicle electricVehicle;
    
    private ChangeoverActivity activity;
    private final double endTime = 3600.0; // 1 hour

    @BeforeEach
    public void setUp() {
        when(changeoverTask.getEndTime()).thenReturn(endTime);
        Map<Id<Request>, AcceptedDrtRequest> emptyRequests = new HashMap<>();
        activity = new ChangeoverActivity(passengerHandler, driver, changeoverTask, emptyRequests, emptyRequests);
    }


    @Test
    public void testDoSimStep_noCharging() {
        double now = 1800.0;
        activity.doSimStep(now);
        
        // Only the stop delegate should be called, no charging interactions
        verify(changeoverTask, times(2)).getChargingTask();
    }

    @Test
    public void testChargingInitialization_atConstruction() {
        // Setup charging task with required mocks
        when(chargingTask.getChargingLogic()).thenReturn(chargingLogic);
        when(chargingTask.getElectricVehicle()).thenReturn(electricVehicle);
        
        // Make the task return the charging task during construction
        when(changeoverTask.getChargingTask()).thenReturn(Optional.of(chargingTask));

        // Create activity - charging should be initialized in constructor
        Map<Id<Request>, AcceptedDrtRequest> emptyRequests = new HashMap<>();
        ChangeoverActivity activityWithCharging = new ChangeoverActivity(
                passengerHandler, driver, changeoverTask, emptyRequests, emptyRequests);
        
        // Execute sim step to trigger charging delegate's sim step
        double now = 1800.0;
        activityWithCharging.doSimStep(now);
        
        // Charging delegate should handle the sim step too - but we can't verify this directly 
        // as it's a private field, but we can verify the task was requested
        verify(changeoverTask, times(2)).getChargingTask(); // Once in constructor, once in doSimStep
        
    }

    @Test
    public void testChargingInitialization_duringSimStep() {
        // First return empty, then return charging task on second call
        when(changeoverTask.getChargingTask())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(chargingTask));
        
        when(chargingTask.getChargingLogic()).thenReturn(chargingLogic);
        
        double now = 1800.0;
        activity.doSimStep(now);
        
        // First call should check for charging task
        verify(changeoverTask, times(2)).getChargingTask();
        
        // Second sim step, now with charging
        activity.doSimStep(now);
        
        // Should have checked for charging task again
        verify(changeoverTask, times(3)).getChargingTask();
    }

    @Test
    public void testFinalizeAction_withChargingInProgress() {
        when(changeoverTask.getChargingTask()).thenReturn(Optional.of(chargingTask));
        when(chargingTask.getChargingLogic()).thenReturn(chargingLogic);
        when(chargingTask.getElectricVehicle()).thenReturn(electricVehicle);
        
        Map<Id<Request>, AcceptedDrtRequest> emptyRequests = new HashMap<>();
        ChangeoverActivity activityWithCharging = new ChangeoverActivity(
                passengerHandler, driver, changeoverTask, emptyRequests, emptyRequests);
        
        double now = endTime - 300; // 5 minutes before end
        activityWithCharging.finalizeAction(now);
        
        // Should remove the vehicle from charging
        verify(chargingLogic).removeVehicle(electricVehicle, now);
    }

    @Test
    public void testFinalizeAction_chargingAlreadyCompleted() {
        when(changeoverTask.getChargingTask()).thenReturn(Optional.of(chargingTask));

        Map<Id<Request>, AcceptedDrtRequest> emptyRequests = new HashMap<>();
        ChangeoverActivity activityWithCharging = new ChangeoverActivity(
                passengerHandler, driver, changeoverTask, emptyRequests, emptyRequests);
        
        activityWithCharging.finalizeAction(endTime); // At end time
        
        // Should not interact with charging logic
        verify(chargingTask, never()).getChargingLogic();
    }
}