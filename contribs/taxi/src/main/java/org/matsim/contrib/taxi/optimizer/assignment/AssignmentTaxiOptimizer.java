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

import java.util.*;

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiries;
import org.matsim.contrib.locationchoice.router.*;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;

import com.google.common.collect.Iterables;

/**
 * @author michalm
 */
public class AssignmentTaxiOptimizer extends AbstractTaxiOptimizer {
	private final AssignmentTaxiOptimizerParams params;
	private final FastMultiNodeDijkstra router;
	private final BackwardFastMultiNodeDijkstra backwardRouter;
	private final FastAStarEuclidean euclideanRouter;
	private final VehicleAssignmentProblem<TaxiRequest> assignmentProblem;
	private final TaxiToRequestAssignmentCostProvider assignmentCostProvider;

	public AssignmentTaxiOptimizer(TaxiOptimizerContext optimContext, AssignmentTaxiOptimizerParams params) {
		super(optimContext, params, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), true, true);
		this.params = params;

		// TODO bug: cannot cast ImaginaryNode to RoutingNetworkNode
		// PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		// preProcessDijkstra.run(optimContext.network);
		PreProcessDijkstra preProcessDijkstra = null;
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessDijkstra)
				.createRoutingNetwork(optimContext.network);
		router = new FastMultiNodeDijkstra(routingNetwork, optimContext.travelDisutility, optimContext.travelTime,
				preProcessDijkstra, fastRouterFactory, true);

		RoutingNetwork inverseRoutingNetwork = new InverseArrayRoutingNetworkFactory(preProcessDijkstra)
				.createRoutingNetwork(optimContext.network);
		backwardRouter = new BackwardFastMultiNodeDijkstra(inverseRoutingNetwork, optimContext.travelDisutility,
				optimContext.travelTime, preProcessDijkstra, fastRouterFactory, true);

		PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(optimContext.travelDisutility);
		preProcessEuclidean.run(optimContext.network);

		RoutingNetwork euclideanRoutingNetwork = new ArrayRoutingNetworkFactory(preProcessEuclidean)
				.createRoutingNetwork(optimContext.network);
		euclideanRouter = new FastAStarEuclidean(euclideanRoutingNetwork, preProcessEuclidean,
				optimContext.travelDisutility, optimContext.travelTime,
				optimContext.scheduler.getParams().AStarEuclideanOverdoFactor, fastRouterFactory);

		assignmentProblem = new VehicleAssignmentProblem<>(optimContext.travelTime, getRouter(), getBackwardRouter(),
				euclideanRouter, params.nearestRequestsLimit, params.nearestVehiclesLimit);

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
		return router;
	}

	protected BackwardMultiNodePathCalculator getBackwardRouter() {
		return backwardRouter;
	}
}
