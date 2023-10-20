package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.ruinAndRecreate;

import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.FleetSchedules;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.GeneralRequest;

import java.util.List;

public interface RuinSelector {
    List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules);
}
