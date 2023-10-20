package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.FleetSchedules;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.GeneralRequest;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.OnlineVehicleInfo;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;
import java.util.Map;

public interface VrpSolver {
    FleetSchedules calculate(FleetSchedules previousSchedules,
							 Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap,
							 List<GeneralRequest> newRequests, double time);
}
