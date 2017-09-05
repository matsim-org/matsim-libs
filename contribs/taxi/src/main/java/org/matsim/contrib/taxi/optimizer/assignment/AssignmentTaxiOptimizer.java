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

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiries;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstraFactory;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.FastMultiNodeDijkstra;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.MultiNodePathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator;

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

	public AssignmentTaxiOptimizer(TaxiOptimizerContext optimContext, AssignmentTaxiOptimizerParams params) {
		super(optimContext, params, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), true, true);
		this.params = params;

		multiNodeRouter = (FastMultiNodeDijkstra)new FastMultiNodeDijkstraFactory(true)
				.createPathCalculator(optimContext.network, optimContext.travelDisutility, optimContext.travelTime);

		backwardMultiNodeRouter = (BackwardFastMultiNodeDijkstra)new BackwardFastMultiNodeDijkstraFactory(true)
				.createPathCalculator(optimContext.network, optimContext.travelDisutility, optimContext.travelTime);

		router = new FastAStarEuclideanFactory().createPathCalculator(optimContext.network,
				optimContext.travelDisutility, optimContext.travelTime);

		assignmentProblem = new VehicleAssignmentProblem<>(optimContext.travelTime, getRouter(), getBackwardRouter(),
				router, params.nearestRequestsLimit, params.nearestVehiclesLimit);

		assignmentCostProvider = new TaxiToRequestAssignmentCostProvider(params);
	}

	@Override
	protected void scheduleUnplannedRequests() {
		// advance request not considered => horizon==0
		AssignmentRequestData rData = new AssignmentRequestData(getOptimContext(), 0, getUnplannedRequests());
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
			getOptimContext().scheduler.scheduleRequest(a.vehicle, a.destination, a.path);
			getUnplannedRequests().remove(a.destination);
		}
	}

	private VehicleData initVehicleData(AssignmentRequestData rData) {
		int idleVehs = Iterables.size(Iterables.filter(getOptimContext().fleet.getVehicles().values(),
				ScheduleInquiries.createIsIdle(getOptimContext().scheduler)));
		double vehPlanningHorizon = idleVehs < rData.getUrgentReqCount() ? //
				params.vehPlanningHorizonUndersupply : params.vehPlanningHorizonOversupply;
		return new VehicleData(getOptimContext(), getOptimContext().fleet.getVehicles().values(), vehPlanningHorizon);
	}

	protected MultiNodePathCalculator getRouter() {
		return multiNodeRouter;
	}

	protected BackwardMultiNodePathCalculator getBackwardRouter() {
		return backwardMultiNodeRouter;
	}
}
