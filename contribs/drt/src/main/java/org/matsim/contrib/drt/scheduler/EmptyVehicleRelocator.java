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

import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;

/**
 * @author michalm
 */
// TODO move to DrtScheduler ??????????????
public class EmptyVehicleRelocator {
	private final TravelTime travelTime;
	private final DrtScheduler scheduler;
	private final FastAStarEuclidean router;

	public EmptyVehicleRelocator(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
			DrtScheduler scheduler) {
		this.travelTime = travelTime;
		this.scheduler = scheduler;

		PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(travelDisutility);
		preProcessEuclidean.run(network);

		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessEuclidean)
				.createRoutingNetwork(network);

		router = new FastAStarEuclidean(routingNetwork, preProcessEuclidean, travelDisutility, travelTime, 2.,
				fastRouterFactory);
	}

	public void relocateVehicle(Vehicle vehicle, Link link, double time) {
		DrtStayTask currentTask = (DrtStayTask)vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();

		if (currentLink != link) {
			VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(currentLink, link, time, router, travelTime);
			if (path.getArrivalTime()<vehicle.getServiceEndTime()){
			scheduler.relocateEmptyVehicle(vehicle, path);
			}
		}
	}
}
