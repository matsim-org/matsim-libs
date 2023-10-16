package org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.vrpSolver.ruinAndRecreate;

import org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.basicStructures.FleetSchedules;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.basicStructures.GeneralRequest;

import java.util.List;

public interface RuinSelector {
    List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules);
}
