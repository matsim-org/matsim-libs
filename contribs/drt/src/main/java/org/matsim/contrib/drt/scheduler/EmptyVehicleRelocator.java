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

package org.matsim.contrib.drt.scheduler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class EmptyVehicleRelocator {
	private final TravelTime travelTime;
	private final MobsimTimer timer;
	private final DrtTaskFactory taskFactory;
	private final LeastCostPathCalculator router;

	public EmptyVehicleRelocator(Network network, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			TravelDisutility travelDisutility, MobsimTimer timer, DrtTaskFactory taskFactory) {
		this.travelTime = travelTime;
		this.timer = timer;
		this.taskFactory = taskFactory;
		router = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	public void relocateVehicle(DvrpVehicle vehicle, Link link) {
		DrtStayTask currentTask = (DrtStayTask)vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();

		if (currentLink != link) {
			VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(currentLink, link, timer.getTimeOfDay(), router,
					travelTime);
			if (path.getArrivalTime() < vehicle.getServiceEndTime()) {
				relocateVehicleImpl(vehicle, path);
			}
		}
	}

	private void relocateVehicleImpl(DvrpVehicle vehicle, VrpPathWithTravelData vrpPath) {
		Schedule schedule = vehicle.getSchedule();
		DrtStayTask stayTask = (DrtStayTask)schedule.getCurrentTask();
		if (stayTask.getTaskIdx() != schedule.getTaskCount() - 1) {
			throw new IllegalStateException("The current STAY task is not last. Not possible without prebooking");
		}

		stayTask.setEndTime(vrpPath.getDepartureTime()); // finish STAY
		schedule.addTask(taskFactory.createDriveTask(vehicle, vrpPath)); // add DRIVE
		// append STAY
		schedule.addTask(taskFactory.createStayTask(vehicle, vrpPath.getArrivalTime(), vehicle.getServiceEndTime(),
				vrpPath.getToLink()));
	}
}
