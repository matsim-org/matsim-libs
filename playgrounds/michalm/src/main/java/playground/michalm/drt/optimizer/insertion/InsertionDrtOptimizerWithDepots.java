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

package playground.michalm.drt.optimizer.insertion;

import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;

import playground.michalm.drt.optimizer.DrtOptimizerContext;
import playground.michalm.drt.run.DrtConfigGroup;
import playground.michalm.drt.schedule.*;
import playground.michalm.drt.schedule.NDrtTask.NDrtTaskType;

/**
 * @author michalm
 */
public class InsertionDrtOptimizerWithDepots extends InsertionDrtOptimizer {
	private final Set<Link> startLinks = new HashSet<>();
	private final FastAStarEuclidean router;

	public InsertionDrtOptimizerWithDepots(DrtOptimizerContext optimContext, DrtConfigGroup drtCfg,
			InsertionDrtOptimizerParams params) {
		super(optimContext, drtCfg, params);

		for (Vehicle v : optimContext.fleet.getVehicles().values()) {
			startLinks.add(v.getStartLink());
		}

		RoutingNetwork network = getRoutingNetwork();
		PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(optimContext.travelDisutility);
		preProcessEuclidean.run(network);

		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		router = new FastAStarEuclidean(network, preProcessEuclidean, optimContext.travelDisutility,
				optimContext.travelTime, 2., fastRouterFactory);
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		super.nextTask(vehicle);

		NDrtTask currentTask = (NDrtTask)vehicle.getSchedule().getCurrentTask();

		// current task is STAY
		if (currentTask != null && currentTask.getDrtTaskType() == NDrtTaskType.STAY) {
			int previousTaskIdx = currentTask.getTaskIdx() - 1;

			// previous task is STOP
			if (previousTaskIdx >= 0 && ((NDrtTask)vehicle.getSchedule().getTasks().get(previousTaskIdx))
					.getDrtTaskType() == NDrtTaskType.STOP) {

				Link currentLink = ((NDrtStayTask)currentTask).getLink();
				Link bestStartLink = findBestStartLink(currentLink);
				if (bestStartLink != null) {
					System.err.println("sending vehicle to depot");
					VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(currentLink, bestStartLink,
							currentTask.getBeginTime(), router, getOptimContext().travelTime);
					getOptimContext().scheduler.relocateEmptyVehicle(vehicle, path);
				}
			}
		}
	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra should be the ultimate solution
	private Link findBestStartLink(Link fromLink) {
		if (startLinks.contains(fromLink)) {
			return null;// stay where it is
		}

		Coord fromCoord = fromLink.getCoord();
		double minDistance = Double.MAX_VALUE;
		Link bestLink = null;
		for (Link l : startLinks) {
			double distance = DistanceUtils.calculateSquaredDistance(fromCoord, l.getCoord());
			if (distance < minDistance) {
				bestLink = l;
			}
		}

		return bestLink;
	}
}
