package org.matsim.contrib.drt.extension.operations.shifts.charging;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.ev.charging.ChargingEstimations;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;

import java.util.List;
import java.util.Optional;

/**
 * Central utility for making charging decisions for shift operations.
 * 
 * @author nkuehnel / MOIA
 */
public class ShiftChargingLogic {
    
    private final ShiftsParams shiftsParams;
    private final ChargingInfrastructure chargingInfrastructure;
    private final ChargingStrategy.Factory chargingStrategyFactory;
    
    public ShiftChargingLogic(
            ShiftsParams shiftsParams,
            ChargingInfrastructure chargingInfrastructure,
            ChargingStrategy.Factory chargingStrategyFactory) {
        this.shiftsParams = shiftsParams;
        this.chargingInfrastructure = chargingInfrastructure;
        this.chargingStrategyFactory = chargingStrategyFactory;
    }
    
    /**
     * Record to hold charger and strategy information
     */
    public record ChargerWithStrategy(Charger charger, ChargingStrategy strategy) {}
    
    /**
     * Finds an available charger at the given facility with no wait time
     * and creates a strategy for it.
     * 
     * @param facility The operation facility
     * @param ev The electric vehicle
     * @return Optional containing the charger and strategy if found, empty otherwise
     */
    public Optional<ChargerWithStrategy> findAvailableCharger(OperationFacility facility, ElectricVehicle ev) {
        // Check if there's an appropriate charger available
        List<Id<Charger>> chargerIds = facility.getChargers();
        if (chargerIds.isEmpty() || chargingInfrastructure == null) {
            return Optional.empty();
        }
        
        // Find a charger with no wait time
        Optional<Charger> selectedCharger = chargerIds.stream()
                .map(id -> chargingInfrastructure.getChargers().get(id))
                .filter(charger -> charger != null && 
                        shiftsParams.getBreakChargerType().equals(charger.getChargerType()))
                .filter(charger -> ChargingEstimations.estimateMaxWaitTimeForNextVehicle(charger) == 0)
                .findFirst();
        
        if (selectedCharger.isPresent()) {
            Charger charger = selectedCharger.get();
            ChargingStrategy strategy = chargingStrategyFactory.createStrategy(charger.getSpecification(), ev);
            
            // Don't bother charging if already fully charged according to strategy
            if (strategy.isChargingCompleted()) {
                return Optional.empty();
            }
            
            return Optional.of(new ChargerWithStrategy(charger, strategy));
        }
        
        return Optional.empty();
    }
}