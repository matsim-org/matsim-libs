package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.apache.commons.lang.math.IntRange;
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

    /**
     * True if you can fit _one more_ reservation covering the full
     * timeRange, i.e. max concurrent reservations in that window < capacity.
     */
    boolean hasCapacity(IntRange timeRange);


    /**
     * Reserve one slot for vehicleId over the entire timeRange
     * (inclusive). Returns true only if, at all times in that
     * range, #existing reservations < capacity.
     */
    boolean register(Id<DvrpVehicle> vehicleId, IntRange timeRange);

    /**
     * Remove all reservations ever made for this vehicle.
     */
    boolean deregisterVehicle(Id<DvrpVehicle> id);

    List<Id<Charger>> getChargers();

    OperationFacilityType getType();

    Set<Id<DvrpVehicle>> getRegisteredVehicles();
}
