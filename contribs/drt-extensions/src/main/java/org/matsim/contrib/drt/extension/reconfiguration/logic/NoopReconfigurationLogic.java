package org.matsim.contrib.drt.extension.reconfiguration.logic;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoad;

public class NoopReconfigurationLogic implements CapacityReconfigurationLogic {
    @Override
    public Optional<DvrpLoad> getUpdatedStartCapacity(DvrpVehicle vehicle) {
        return Optional.empty();
    }

    @Override
    public List<ReconfigurationItem> getCapacityUpdates(DvrpVehicle dvrpVehicle) {
        return Collections.emptyList();
    }
}
