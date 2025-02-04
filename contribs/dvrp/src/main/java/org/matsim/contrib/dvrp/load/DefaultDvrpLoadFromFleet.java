package org.matsim.contrib.dvrp.load;

import java.util.Collections;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DefaultDvrpLoadFromFleet implements DvrpLoadFromFleet {
    private final DvrpLoadType loadType;
    private final String slot;

    public DefaultDvrpLoadFromFleet(DvrpLoadType loadType, String mappedSlot) {
        this.loadType = loadType;
        this.slot = mappedSlot;
    }

    @Override
    public DvrpLoad getDvrpVehicleLoad(int capacity, Id<DvrpVehicle> vehicleId) {
        return loadType.fromMap(Collections.singletonMap(slot, capacity));
    }
}
