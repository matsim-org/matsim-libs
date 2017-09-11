/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.List;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiries;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstraFactory;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.FastMultiNodeDijkstra;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.MultiNodePathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.Iterables;

/**
 * @author michalm
 */
public class AssignmentTaxiOptimizer extends AbstractTaxiOptimizer {
	private final AssignmentTaxiOptimizerParams params;
	private final FastMultiNodeDijkstra multiNodeRouter;
	private final BackwardFastMultiNodeDijkstra backwardMultiNodeRouter;
	private final LeastCostPathCalculator router;
	private final VehicleAssignmentProblem<TaxiRequest> assignmentProblem;
	private final TaxiToRequestAssignmentCostProvider assignmentCostProvider;
	private final MobsimTimer timer;

	public AssignmentTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, Network network, MobsimTimer timer,
			TravelTime travelTime, TravelDisutility travelDisutility, TaxiScheduler scheduler,
			AssignmentTaxiOptimizerParams params) {
		super(taxiCfg, fleet, scheduler, params, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), true, true);
		this.timer = timer;
		this.params = params;

		multiNodeRouter = (FastMultiNodeDijkstra)new FastMultiNodeDijkstraFactory(true).createPathCalculator(network,
				travelDisutility, travelTime);

		backwardMultiNodeRouter = (BackwardFastMultiNodeDijkstra)new BackwardFastMultiNodeDijkstraFactory(true)
				.createPathCalculator(network, travelDisutility, travelTime);

		router = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility, travelTime);

		assignmentProblem = new VehicleAssignmentProblem<>(travelTime, getRouter(), getBackwardRouter(), router,
				params.nearestRequestsLimit, params.nearestVehiclesLimit);

		assignmentCostProvider = new TaxiToRequestAssignmentCostProvider(params);
	}

	@Override
	protected void scheduleUnplannedRequests() {
		// advance request not considered => horizon==0
		AssignmentRequestData rData = new AssignmentRequestData(timer, 0, getUnplannedRequests());
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
			getScheduler().scheduleRequest(a.vehicle, a.destination, a.path);
			getUnplannedRequests().remove(a.destination);
		}
	}

	private VehicleData initVehicleData(AssignmentRequestData rData) {
		int idleVehs = Iterables.size(
				Iterables.filter(getFleet().getVehicles().values(), ScheduleInquiries.createIsIdle(getScheduler())));
		double vehPlanningHorizon = idleVehs < rData.getUrgentReqCount() ? //
				params.vehPlanningHorizonUndersupply : params.vehPlanningHorizonOversupply;
		return new VehicleData(timer, getScheduler(), getFleet().getVehicles().values(), vehPlanningHorizon);
	}

	protected MultiNodePathCalculator getRouter() {
		return multiNodeRouter;
	}

	protected BackwardMultiNodePathCalculator getBackwardRouter() {
		return backwardMultiNodeRouter;
	}

	protected MobsimTimer getTimer() {
		return timer;
	}
}
