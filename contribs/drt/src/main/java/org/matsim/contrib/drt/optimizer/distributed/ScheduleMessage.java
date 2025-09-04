package org.matsim.contrib.drt.optimizer.distributed;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds schedules of vehicle fleet.
 */
public class ScheduleMessage implements Message {

	private final Map<Id<DvrpVehicle>, Schedule> schedules = new LinkedHashMap<>();

	public void addSchedule(Id<DvrpVehicle> id, Schedule schedule) {
		schedules.put(id, schedule);
	}

	public Map<Id<DvrpVehicle>, Schedule> getSchedules() {
		return schedules;
	}
}
