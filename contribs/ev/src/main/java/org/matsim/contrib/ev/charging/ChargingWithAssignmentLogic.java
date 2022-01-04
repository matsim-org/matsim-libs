package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.fleet.ElectricVehicle;

import java.util.Collection;

public interface ChargingWithAssignmentLogic extends ChargingLogic {
	void assignVehicle(ElectricVehicle ev);

	void unassignVehicle(ElectricVehicle ev);

	Collection<ElectricVehicle> getAssignedVehicles();
}
