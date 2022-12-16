package org.matsim.contrib.drt.optimizer;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedules;

public class DefaultSlackTimeCalculator implements SlackTimeCalculator {

	@Override
	public double[] computeSlackTimes(DvrpVehicle vehicle, double now, Waypoint.Stop[] stops) {
		double[] slackTimes = new double[stops.length + 1];

		//vehicle
		double slackTime = calcVehicleSlackTime(vehicle, now);
		slackTimes[stops.length] = slackTime;

		//stops
		for (int i = stops.length - 1; i >= 0; i--) {
			var stop = stops[i];
			slackTime = Math.min(stop.latestArrivalTime - stop.task.getBeginTime(), slackTime);
			slackTime = Math.min(stop.latestDepartureTime - stop.task.getEndTime(), slackTime);
			slackTimes[i] = slackTime;
		}
		return slackTimes;
	}


	private static double calcVehicleSlackTime(DvrpVehicle vehicle, double now) {
		var lastTask = Schedules.getLastTask(vehicle.getSchedule());
		//if the last task is started, take 'now', otherwise take the planned begin time
		double availableFromTime = Math.max(lastTask.getBeginTime(), now);
		//for an already delayed vehicle, assume slack is 0 (instead of a negative number)
		return Math.max(0, vehicle.getServiceEndTime() - availableFromTime);
	}
}
