package org.matsim.contrib.drt.extension.operations.shifts.charging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.ev.charging.ChargingEstimations;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ShiftChargingLogicTest {

    @Mock private ShiftsParams shiftsParams;
    @Mock private ChargingInfrastructure chargingInfrastructure;
    @Mock private ChargingStrategy.Factory chargingStrategyFactory;
    @Mock private OperationFacility facility;
    @Mock private ElectricVehicle ev;
    @Mock private Charger charger1;
    @Mock private Charger charger2;
    @Mock private ChargingStrategy chargingStrategy;
    
    private ShiftChargingLogic chargingLogic;
    private final String CHARGER_TYPE = "fast_charger";

    @BeforeEach
    public void setUp() {
        chargingLogic = new ShiftChargingLogic(shiftsParams, chargingInfrastructure, chargingStrategyFactory);
    }

    @Test
    public void testFindAvailableCharger_noChargersAtFacility() {
        when(facility.getChargers()).thenReturn(List.of());
        
        Optional<ShiftChargingLogic.ChargerWithStrategy> result = chargingLogic.findAvailableCharger(facility, ev);
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAvailableCharger_nullChargingInfrastructure() {
        // Create a new charging logic with null infrastructure
        chargingLogic = new ShiftChargingLogic(shiftsParams, null, chargingStrategyFactory);
        when(facility.getChargers()).thenReturn(List.of(Id.create("charger1", Charger.class)));
        
        Optional<ShiftChargingLogic.ChargerWithStrategy> result = chargingLogic.findAvailableCharger(facility, ev);
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAvailableCharger_wrongChargerType() {
        Id<Charger> chargerId = Id.create("charger1", Charger.class);
        when(facility.getChargers()).thenReturn(List.of(chargerId));
        when(chargingInfrastructure.getChargers()).thenReturn(ImmutableMap.of(chargerId, charger1));
        when(charger1.getChargerType()).thenReturn("slow_charger"); // Different from shiftsParams.getBreakChargerType()
        when(shiftsParams.getBreakChargerType()).thenReturn(CHARGER_TYPE);


        Optional<ShiftChargingLogic.ChargerWithStrategy> result = chargingLogic.findAvailableCharger(facility, ev);
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAvailableCharger_chargerWithWaitTime() {
        try (MockedStatic<ChargingEstimations> mockedEstimations = mockStatic(ChargingEstimations.class)) {
            Id<Charger> chargerId = Id.create("charger1", Charger.class);
            when(facility.getChargers()).thenReturn(List.of(chargerId));
            when(chargingInfrastructure.getChargers()).thenReturn(ImmutableMap.of(chargerId, charger1));
            when(charger1.getChargerType()).thenReturn(CHARGER_TYPE);
            when(shiftsParams.getBreakChargerType()).thenReturn(CHARGER_TYPE);


            // Simulate wait time
            mockedEstimations.when(() -> ChargingEstimations.estimateMaxWaitTimeForNextVehicle(charger1))
                    .thenReturn(300.0); // 5 minutes wait time
            
            Optional<ShiftChargingLogic.ChargerWithStrategy> result = chargingLogic.findAvailableCharger(facility, ev);
            
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testFindAvailableCharger_fullyChargedVehicle() {
        Id<Charger> chargerId = Id.create("charger1", Charger.class);
        when(facility.getChargers()).thenReturn(List.of(chargerId));
        when(chargingInfrastructure.getChargers()).thenReturn(ImmutableMap.of(chargerId, charger1));
        when(charger1.getChargerType()).thenReturn(CHARGER_TYPE);
        when(shiftsParams.getBreakChargerType()).thenReturn(CHARGER_TYPE);


        try (MockedStatic<ChargingEstimations> mockedEstimations = mockStatic(ChargingEstimations.class)) {
            mockedEstimations.when(() -> ChargingEstimations.estimateMaxWaitTimeForNextVehicle(charger1))
                    .thenReturn(0.0); // No wait time
                    
            when(chargingStrategyFactory.createStrategy(any(), eq(ev))).thenReturn(chargingStrategy);
            when(chargingStrategy.isChargingCompleted()).thenReturn(true); // Vehicle is fully charged
            
            Optional<ShiftChargingLogic.ChargerWithStrategy> result = chargingLogic.findAvailableCharger(facility, ev);
            
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testFindAvailableCharger_success() {
        Id<Charger> chargerId = Id.create("charger1", Charger.class);
        when(facility.getChargers()).thenReturn(List.of(chargerId));
        when(chargingInfrastructure.getChargers()).thenReturn(ImmutableMap.of(chargerId, charger1));
        when(charger1.getChargerType()).thenReturn(CHARGER_TYPE);
        when(shiftsParams.getBreakChargerType()).thenReturn(CHARGER_TYPE);


        try (MockedStatic<ChargingEstimations> mockedEstimations = mockStatic(ChargingEstimations.class)) {
            mockedEstimations.when(() -> ChargingEstimations.estimateMaxWaitTimeForNextVehicle(charger1))
                    .thenReturn(0.0); // No wait time
                    
            when(chargingStrategyFactory.createStrategy(any(), eq(ev))).thenReturn(chargingStrategy);
            when(chargingStrategy.isChargingCompleted()).thenReturn(false); // Vehicle needs charging
            
            Optional<ShiftChargingLogic.ChargerWithStrategy> result = chargingLogic.findAvailableCharger(facility, ev);
            
            assertTrue(result.isPresent());
            assertEquals(charger1, result.get().charger());
            assertEquals(chargingStrategy, result.get().strategy());
        }
    }

    @Test
    public void testFindAvailableCharger_multipleChargers_firstHasWaitTime() {
        Id<Charger> chargerId1 = Id.create("charger1", Charger.class);
        Id<Charger> chargerId2 = Id.create("charger2", Charger.class);
        when(facility.getChargers()).thenReturn(List.of(chargerId1, chargerId2));
        when(chargingInfrastructure.getChargers()).thenReturn(ImmutableMap.of(
                chargerId1, charger1,
                chargerId2, charger2
        ));
        when(charger1.getChargerType()).thenReturn(CHARGER_TYPE);
        when(charger2.getChargerType()).thenReturn(CHARGER_TYPE);
        when(shiftsParams.getBreakChargerType()).thenReturn(CHARGER_TYPE);


        try (MockedStatic<ChargingEstimations> mockedEstimations = mockStatic(ChargingEstimations.class)) {
            // First charger has wait time, second doesn't
            mockedEstimations.when(() -> ChargingEstimations.estimateMaxWaitTimeForNextVehicle(charger1))
                    .thenReturn(300.0); // 5 minutes wait time
            mockedEstimations.when(() -> ChargingEstimations.estimateMaxWaitTimeForNextVehicle(charger2))
                    .thenReturn(0.0); // No wait time
                    
            when(chargingStrategyFactory.createStrategy(any(), eq(ev))).thenReturn(chargingStrategy);
            when(chargingStrategy.isChargingCompleted()).thenReturn(false); // Vehicle needs charging
            
            Optional<ShiftChargingLogic.ChargerWithStrategy> result = chargingLogic.findAvailableCharger(facility, ev);
            
            assertTrue(result.isPresent());
            assertEquals(charger2, result.get().charger()); // Should select the second charger
            assertEquals(chargingStrategy, result.get().strategy());
        }
    }
}