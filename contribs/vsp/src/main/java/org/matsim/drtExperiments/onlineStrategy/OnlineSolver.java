package org.matsim.drtExperiments.onlineStrategy;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.drtExperiments.basicStructures.OnlineVehicleInfo;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.List;
import java.util.Map;

public interface OnlineSolver {
    Id<DvrpVehicle> insert(DrtRequest request, Map<Id<DvrpVehicle>, List<TimetableEntry>> timetables,
                           Map<Id<DvrpVehicle>, OnlineVehicleInfo> realTimeVehicleInfoMap);
}
