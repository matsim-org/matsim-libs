package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;

import java.util.List;

public interface RuinSelector {
    List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules);
}
