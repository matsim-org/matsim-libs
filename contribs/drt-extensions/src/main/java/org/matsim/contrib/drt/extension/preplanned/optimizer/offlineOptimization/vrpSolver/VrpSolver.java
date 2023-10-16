package org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.vrpSolver;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.basicStructures.FleetSchedules;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.basicStructures.GeneralRequest;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.basicStructures.OnlineVehicleInfo;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;
import java.util.Map;

public interface VrpSolver {
    FleetSchedules calculate(FleetSchedules previousSchedules,
							 Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap,
							 List<GeneralRequest> newRequests, double time);
}
