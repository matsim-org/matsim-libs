package org.matsim.contrib.drt.extension.shifts.operationFacilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.util.*;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilityImpl implements OperationFacility {

    private static final int DEFAULT_CAPACITY = 50;

    private final Id<OperationFacility> id;
    private final Id<Link> linkId;
    private final Coord coord;
    private final int capacity;
    private final List<Id<Charger>> chargers;
    private final OperationFacilityType type;

    private final Set<Id<DvrpVehicle>> reservedVehicles = new LinkedHashSet<>();

    public OperationFacilityImpl(Id<OperationFacility> id, Id<Link> linkId, Coord coord, int capacity,
                                 List<Id<Charger>> chargers, OperationFacilityType type) {
        this.id = id;
        this.linkId = linkId;
        this.coord = coord;
        this.capacity = capacity;
        this.chargers = Collections.unmodifiableList(chargers);
        this.type = type;
    }

    @Override
    public Id<OperationFacility> getId() {
        return id;
    }

    @Override
    public Id<Link> getLinkId() {
        return linkId;
    }

    @Override
    public Coord getCoord() {
        return coord;
    }

    @Override
    public Map<String, Object> getCustomAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean hasCapacity() {
        return reservedVehicles.size() < capacity;
    }

    @Override
    public boolean register(Id<DvrpVehicle> id) {
        if(hasCapacity()) {
            reservedVehicles.add(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean deregisterVehicle(Id<DvrpVehicle> id) {
       return reservedVehicles.remove(id);
    }

    @Override
    public List<Id<Charger>> getChargers() {
        return chargers;
    }

    @Override
    public OperationFacilityType getType() {
        return type;
    }

    @Override
    public Set<Id<DvrpVehicle>> getRegisteredVehicles() {
        return Collections.unmodifiableSet(reservedVehicles);
    }
}
