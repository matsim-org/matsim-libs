/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.drt.scheduler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.tasks.DrtTask;
import org.matsim.contrib.drt.tasks.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;

/**
 * @author michalm
 */
public class DrtScheduler implements ScheduleInquiry {
	private final Fleet fleet;
	protected final DrtSchedulerParams params;
	private final MobsimTimer timer;

	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;

	public DrtScheduler(Scenario scenario, Fleet fleet, MobsimTimer timer, DrtSchedulerParams params,
			TravelTime travelTime, TravelDisutility travelDisutility) {
		this.fleet = fleet;
		this.params = params;
		this.timer = timer;
		this.travelTime = travelTime;

		PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(travelDisutility);
		preProcessEuclidean.run(scenario.getNetwork());

		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessEuclidean)
				.createRoutingNetwork(scenario.getNetwork());

		router = new FastAStarEuclidean(routingNetwork, preProcessEuclidean, travelDisutility, travelTime,
				params.AStarEuclideanOverdoFactor, fastRouterFactory);

		if (TaxiConfigGroup.get(scenario.getConfig()).isChangeStartLinkToLastLinkInSchedule()) {
			for (Vehicle veh : fleet.getVehicles().values()) {
				Vehicles.changeStartLinkToLastLinkInSchedule(veh);
			}
		}

		((FleetImpl)fleet).resetSchedules();

		for (Vehicle veh : fleet.getVehicles().values()) {
			veh.getSchedule().addTask(new TaxiStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
		}
	}

	@Override
	public boolean isIdle(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (timer.getTimeOfDay() >= vehicle.getT1() || schedule.getStatus() != ScheduleStatus.STARTED) {
			return false;
		}

		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();
		return Schedules.isLastTask(currentTask) && currentTask.getDrtTaskType() == DrtTaskType.STAY;
	}

}
