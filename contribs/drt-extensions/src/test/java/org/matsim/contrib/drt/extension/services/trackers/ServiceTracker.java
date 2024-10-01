package org.matsim.contrib.drt.extension.services.trackers;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.services.events.DrtServiceScheduledEvent;
import org.matsim.contrib.drt.extension.services.events.DrtServiceScheduledEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author steffenaxer
*/public class ServiceTracker implements DrtServiceScheduledEventHandler  {
	public Map<Id<DvrpVehicle>, List<DrtServiceScheduledEvent>> serviceTracker = new HashMap<>();

	@Override
	public void handleEvent(DrtServiceScheduledEvent event) {
		serviceTracker.computeIfAbsent(event.getVehicleId(), k -> new ArrayList<>()).add(event);
	}

	@Override
	public void reset(int iteration)
	{
		this.serviceTracker.clear();
	}
}
