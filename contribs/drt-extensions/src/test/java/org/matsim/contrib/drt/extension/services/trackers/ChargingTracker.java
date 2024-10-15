package org.matsim.contrib.drt.extension.services.trackers;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author steffenaxer
 */
public class ChargingTracker implements TaskStartedEventHandler {
	public Map<Id<DvrpVehicle>, List<TaskStartedEvent>> chargingTracker = new HashMap<>();

	@Override
	public void handleEvent(TaskStartedEvent event) {
		if (event.getTaskType().equals(EDrtChargingTask.TYPE))
		{
			chargingTracker.computeIfAbsent(event.getDvrpVehicleId(), k -> new ArrayList<>()).add(event);
		}
	}

	@Override
	public void reset(int iteration)
	{
		this.chargingTracker.clear();
	}
}
