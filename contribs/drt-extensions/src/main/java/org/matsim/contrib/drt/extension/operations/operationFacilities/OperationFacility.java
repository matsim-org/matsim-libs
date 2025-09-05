package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationFacility extends Identifiable<OperationFacility>, Facility {

    record Registration(Id<Registration> registrationId, Id<OperationFacility> operationFacilityId,
                        Id<DvrpVehicle> vehicleId, double fromInclusive, double toExclusive) {}

    int getCapacity();

    /**
     * True if you can fit _one more_ reservation covering the full
     * timeRange, i.e. max concurrent reservations in that window < capacity.
     *
     * @param startInclusive inclusive
     * @param endExclusive exclusive
     */
    boolean hasCapacity(double startInclusive, double endExclusive);

    /**
     * True if you can fit _one more_ reservation, i.e. max concurrent reservations at start < capacity.
     *
     * @param startInclusive inclusive
     */
    boolean hasCapacity(double startInclusive);

    /**
     * Checks in a vehicle at the facility to convey presence. Requires a prior reservation.
     */
    void checkIn(Registration facilityRegistration, double time);

    /**
     * Checks out a vehicle at the facility. Will also deregister the vehicle's reservation.
     */
    void checkOut(Registration facilityRegistration, double time);

    /**
     * Reserve one slot for vehicleId over the entire timeRange
     * (inclusive). Returns true only if, at all times in that
     * range, #existing reservations < capacity.
     *
     * @param startInclusive inclusive
     * @param endExclusive   exclusive
     */
    Optional<Registration> registerVehicle(Id<DvrpVehicle> vehicleId, double startInclusive, double endExclusive);

     /**
     * Reserve one slot for vehicleId over the entire timeRange
     * (inclusive). Returns true only if, at all times after start, #existing reservations < capacity.
     *
     * @param startInclusive inclusive
     */
    Optional<Registration> registerVehicle(Id<DvrpVehicle> vehicleId, double startInclusive);

    /**
     * Remove reservations made for this vehicle.
     */
    boolean deregisterVehicle(Id<Registration> registrationId);

    List<Id<Charger>> getChargers();

    OperationFacilityType getType();

    Set<Id<DvrpVehicle>> getCheckedInVehicles();

}
