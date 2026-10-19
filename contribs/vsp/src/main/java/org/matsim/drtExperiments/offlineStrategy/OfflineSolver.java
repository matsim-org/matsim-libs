package org.matsim.drtExperiments.offlineStrategy;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.OnlineVehicleInfo;

import java.util.List;
import java.util.Map;

public interface OfflineSolver {
    FleetSchedules calculate(FleetSchedules previousSchedules,
                             Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap,
                             List<GeneralRequest> newRequests, double time);
}
