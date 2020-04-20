/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.taxi.optimizer.assignment;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstraFactory;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.MultiNodePathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class AssignmentRequestInserter implements UnplannedRequestInserter {
	private final Fleet fleet;
	private final TaxiScheduler scheduler;
	private final MobsimTimer timer;
	private final AssignmentTaxiOptimizerParams params;

	private final VehicleAssignmentProblem<TaxiRequest> assignmentProblem;
	private final TaxiToRequestAssignmentCostProvider assignmentCostProvider;

	public AssignmentRequestInserter(Fleet fleet, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, TaxiScheduler scheduler, AssignmentTaxiOptimizerParams params) {
		this(fleet, timer, travelTime, scheduler, params,
				(MultiNodePathCalculator)new FastMultiNodeDijkstraFactory(true).createPathCalculator(network,
						travelDisutility, travelTime),
				(BackwardMultiNodePathCalculator)new BackwardFastMultiNodeDijkstraFactory(true).createPathCalculator(
						network, travelDisutility, travelTime),
				new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility, travelTime));
	}

	public AssignmentRequestInserter(Fleet fleet, MobsimTimer timer, TravelTime travelTime, TaxiScheduler scheduler,
			AssignmentTaxiOptimizerParams params, MultiNodePathCalculator multiNodeRouter,
			BackwardMultiNodePathCalculator backwardMultiNodeRouter, LeastCostPathCalculator router) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.timer = timer;
		this.params = params;

		assignmentProblem = new VehicleAssignmentProblem<>(travelTime, multiNodeRouter, backwardMultiNodeRouter, router,
				params.getNearestRequestsLimit(), params.getNearestVehiclesLimit());

		assignmentCostProvider = new TaxiToRequestAssignmentCostProvider(params);
	}

	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		// advance request not considered => horizon==0
		AssignmentRequestData rData = AssignmentRequestData.create(timer.getTimeOfDay(), 0, unplannedRequests);
		if (rData.getSize() == 0) {
			return;
		}
		VehicleData vData = initVehicleData(rData);
		if (vData.getSize() == 0) {
			return;
		}

		AssignmentCost<TaxiRequest> cost = assignmentCostProvider.getCost(rData, vData);
		List<Dispatch<TaxiRequest>> assignments = assignmentProblem.findAssignments(vData, rData, cost);

		for (Dispatch<TaxiRequest> a : assignments) {
			scheduler.scheduleRequest(a.vehicle, a.destination, a.path);
			unplannedRequests.remove(a.destination);
		}
	}

	private VehicleData initVehicleData(AssignmentRequestData rData) {
		long idleVehs = fleet.getVehicles().values().stream().filter(scheduler::isIdle).count();
		double vehPlanningHorizon = idleVehs < rData.getUrgentReqCount() ?
				params.getVehPlanningHorizonUndersupply() :
				params.getVehPlanningHorizonOversupply();
		return new VehicleData(timer.getTimeOfDay(), scheduler, fleet.getVehicles().values().stream(),
				vehPlanningHorizon);
	}
}
