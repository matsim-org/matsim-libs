package org.matsim.contrib.drt.optimizer.rebalancing.toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;

public class VehicleInfoCollector {

	private Fleet fleet;
	private DrtZonalSystem zonalSystem;

	public VehicleInfoCollector(Fleet fleet, DrtZonalSystem zonalSystem) {
		this.fleet = fleet;
		this.zonalSystem = zonalSystem;
	}

	// also include vehicles being right now relocated or recharged
	public Map<DrtZone, List<DvrpVehicle>> groupSoonIdleVehicles(double time, double maxTimeBeforeIdle,
			double minServiceTime) {
		Map<DrtZone, List<DvrpVehicle>> soonIdleVehiclesPerZone = new HashMap<>();
		for (DvrpVehicle v : fleet.getVehicles().values()) {
			Schedule s = v.getSchedule();
			StayTask stayTask = (StayTask) Schedules.getLastTask(s);
			if (stayTask.getStatus() == TaskStatus.PLANNED && stayTask.getBeginTime() < time + maxTimeBeforeIdle
					&& v.getServiceEndTime() > time + minServiceTime) {
				DrtZone zone = zonalSystem.getZoneForLinkId(stayTask.getLink().getId());
				if (zone != null) {
					soonIdleVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
				}
			}
		}
		return soonIdleVehiclesPerZone;
	}

	public Map<DrtZone, List<DvrpVehicle>> groupRebalancableVehicles(Stream<? extends DvrpVehicle> rebalancableVehicles,
			double time, double minServiceTime) {
		Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = new HashMap<>();
		rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + minServiceTime).forEach(v -> {
			Link link = ((StayTask) v.getSchedule().getCurrentTask()).getLink();
			DrtZone zone = zonalSystem.getZoneForLinkId(link.getId());
			if (zone != null) {
				// zonePerVehicle.put(v.getId(), zone);
				rebalancableVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
			}
		});
		return rebalancableVehiclesPerZone;
	}

}
