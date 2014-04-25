package playground.jbischoff.energy.vehicles;

import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

public class BatteryElectricVehicleImpl extends BatteryElectricVehicle {
	/**
	 * 
	 * a fairly basic implementation of a BEV.
	 * 
	 * @param EnergyConsumptionModel
	 * @param battery capacity (in J)
	 * 
	 * 
	 */
	public BatteryElectricVehicleImpl(EnergyConsumptionModel ecm, double usableBatteryCapacityInJoules) {
		this.electricDriveEnergyConsumptionModel=ecm;
		this.usableBatteryCapacityInJoules=usableBatteryCapacityInJoules;
		this.socInJoules=usableBatteryCapacityInJoules;
	
	}

}
