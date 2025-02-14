package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public class PlanBasedParkingCapacityInitializer implements ParkingCapacityInitializer {
    @Override
    public Map<Id<Link>, ParkingInitialCapacity> initialize() {
        return Map.of();
    }
}
