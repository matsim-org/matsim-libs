package org.matsim.contrib.drt.extension.operations.operationFacilities;

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

    private final Id<OperationFacility> id;
    private final Id<Link> linkId;
    private final Coord coord;
    private final int capacity;
    private final List<Id<Charger>> chargers;
    private final OperationFacilityType type;

    private final Set<Id<DvrpVehicle>> registeredVehicles = new LinkedHashSet<>();

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
        return registeredVehicles.size() < capacity;
    }

    @Override
    public boolean register(Id<DvrpVehicle> id) {
        if(registeredVehicles.contains(id)) {
            return true;
        }
        if(hasCapacity()) {
            registeredVehicles.add(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean deregisterVehicle(Id<DvrpVehicle> id) {
       return registeredVehicles.remove(id);
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
        return Collections.unmodifiableSet(registeredVehicles);
    }
}
