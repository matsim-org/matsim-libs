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

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.*;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.passenger.events.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.locationchoice.router.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.misc.Time;

/**
 * @author michalm
 */
public class InsertionDrtOptimizer extends AbstractDrtOptimizer implements MobsimBeforeCleanupListener {
	private final ParallelMultiVehicleInsertionProblem insertionProblem;
	private final EventsManager eventsManager;
	private final boolean printWarnings;

	public InsertionDrtOptimizer(DrtOptimizerContext optimContext, DrtConfigGroup drtCfg) {
		super(optimContext, new TreeSet<DrtRequest>(Requests.ABSOLUTE_COMPARATOR));
		this.eventsManager = optimContext.eventsManager;
		printWarnings = drtCfg.isPrintDetailedWarnings();

		// TODO bug: cannot cast ImaginaryNode to RoutingNetworkNode
		// PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		// preProcessDijkstra.run(optimContext.network);
		PreProcessDijkstra preProcessDijkstra = null;
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessDijkstra)
				.createRoutingNetwork(optimContext.network);
		RoutingNetwork inverseRoutingNetwork = new InverseArrayRoutingNetworkFactory(preProcessDijkstra)
				.createRoutingNetwork(optimContext.network);

		SingleVehicleInsertionProblem[] singleVehicleInsertionProblems = new SingleVehicleInsertionProblem[drtCfg
				.getNumberOfThreads()];
		for (int i = 0; i < singleVehicleInsertionProblems.length; i++) {
			FastMultiNodeDijkstra router = new FastMultiNodeDijkstra(routingNetwork, optimContext.travelDisutility,
					optimContext.travelTime, preProcessDijkstra, fastRouterFactory, true);
			BackwardFastMultiNodeDijkstra backwardRouter = new BackwardFastMultiNodeDijkstra(inverseRoutingNetwork,
					optimContext.travelDisutility, optimContext.travelTime, preProcessDijkstra, fastRouterFactory,
					true);
			singleVehicleInsertionProblems[i] = new SingleVehicleInsertionProblem(router, backwardRouter,
					optimContext.scheduler.getParams().stopDuration, drtCfg.getMaxWaitTime(), optimContext.timer);
		}

		insertionProblem = new ParallelMultiVehicleInsertionProblem(singleVehicleInsertionProblems,
				optimContext.vehicleFilter);
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		insertionProblem.shutdown();
	}

	@Override
	protected void scheduleUnplannedRequests() {
		if (getUnplannedRequests().isEmpty()) {
			return;
		}

		VehicleData vData = new VehicleData(getOptimContext(), getOptimContext().fleet.getVehicles().values());

		Iterator<DrtRequest> reqIter = getUnplannedRequests().iterator();
		while (reqIter.hasNext()) {

			DrtRequest req = reqIter.next();
			BestInsertion best = insertionProblem.findBestInsertion(req, vData);
			if (best == null) {
				eventsManager
						.processEvent(new DrtRequestRejectedEvent(getOptimContext().timer.getTimeOfDay(), req.getId()));
				if (printWarnings) {
					Logger.getLogger(getClass())
							.warn("No vehicle found for drt request from passenger \t" + req.getPassenger().getId()
									+ "\tat\t" + Time.writeTime(req.getSubmissionTime()) + "\tfrom Link\t"
									+ req.getFromLink().getId());
				}
			} else {
				getOptimContext().scheduler.insertRequest(best.vehicleEntry, req, best.insertion);
				vData.updateEntry(best.vehicleEntry);
				eventsManager.processEvent(new DrtRequestScheduledEvent(getOptimContext().timer.getTimeOfDay(),
						req.getId(), best.vehicleEntry.vehicle.getId(), req.getPickupTask().getEndTime(),
						req.getDropoffTask().getBeginTime()));
			}
			reqIter.remove();
		}
	}
}
