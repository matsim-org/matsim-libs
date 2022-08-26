package org.matsim.contrib.drt.extension.shifts.operationFacilities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Set;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationFacility extends Identifiable<OperationFacility>, Facility {

	int getCapacity();

    boolean hasCapacity();

    boolean register(Id<DvrpVehicle> id);

    boolean deregisterVehicle(Id<DvrpVehicle> id);

    List<Id<Charger>> getChargers();

    OperationFacilityType getType();

    Set<Id<DvrpVehicle>> getRegisteredVehicles();
}
