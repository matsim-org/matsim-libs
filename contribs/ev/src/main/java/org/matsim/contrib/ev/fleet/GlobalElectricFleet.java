package org.matsim.contrib.ev.fleet;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.Map;
import java.util.stream.Collectors;

public class GlobalElectricFleet implements ElectricFleet {

	private final Map<Id<Vehicle>, ElectricVehicle> electricVehicles;

	@Inject
	GlobalElectricFleet(Vehicles scenarioVehicles, DriveEnergyConsumption.Factory driveEnergyConsumptionFactory, AuxEnergyConsumption.Factory auxEnergyConsumptionFactory, ChargingPower.Factory chargingPowerFactory) {
		this.electricVehicles = scenarioVehicles.getVehicles().values().stream()
			.filter(v -> ElectricFleetUtils.isElectricVehicleType(v.getType()))
			.map(ElectricVehicleSpecificationDefaultImpl::new)
			.map(s -> ElectricFleetUtils.create(s, driveEnergyConsumptionFactory, auxEnergyConsumptionFactory, chargingPowerFactory))
			.collect(Collectors.toMap(ElectricVehicle::getId, v -> v));
	}
	
	@Override
	public ElectricVehicle getVehicle(Id<Vehicle> vehicleId) {
		return electricVehicles.get(vehicleId);
	}

	@Override
	public boolean hasVehicle(Id<Vehicle> vehicleId) {
		// yes, we already have that vehicle
		return electricVehicles.containsKey(vehicleId);
	}
}
