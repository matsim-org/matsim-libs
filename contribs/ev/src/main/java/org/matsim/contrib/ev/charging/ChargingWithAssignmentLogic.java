package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.fleet.ElectricVehicle;

import java.util.Collection;

public interface ChargingWithAssignmentLogic extends ChargingLogic {
	void assignVehicle(ElectricVehicle ev, ChargingStrategy strategy);

	void unassignVehicle(ElectricVehicle ev);

	Collection<ChargingVehicle> getAssignedVehicles();

	boolean isAssigned(ElectricVehicle ev);
}
