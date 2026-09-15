package org.matsim.contrib.drt.extension.operations.operationFacilities;

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

    /**
     * Tries to register the vehicle at the facility and returns true if successful.
     * If the vehicle was already registered before it also returns true.
     * Returns false, if the vehicle could not be registered.
     */
    boolean register(Id<DvrpVehicle> id);

    boolean deregisterVehicle(Id<DvrpVehicle> id);

    List<Id<Charger>> getChargers();

    OperationFacilityType getType();

    Set<Id<DvrpVehicle>> getRegisteredVehicles();

    static Id<OperationFacility> id(String id) {
        return Id.create(id, OperationFacility.class);
    }
}
