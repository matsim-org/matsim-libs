package org.matsim.contrib.ev.charging;

import java.util.Collection;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public interface ChargingWithAssignmentLogic extends ChargingLogic {
  void assignVehicle(ElectricVehicle ev);

  void unassignVehicle(ElectricVehicle ev);

  Collection<ElectricVehicle> getAssignedVehicles();
}
