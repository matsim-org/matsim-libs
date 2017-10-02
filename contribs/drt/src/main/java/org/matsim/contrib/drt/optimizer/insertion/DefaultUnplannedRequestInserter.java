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

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.optimizer.insertion.filter.DrtVehicleFilter;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.scheduler.DrtScheduler;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstraFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.FastMultiNodeDijkstra;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class DefaultUnplannedRequestInserter implements UnplannedRequestInserter, MobsimBeforeCleanupListener {
	private final DrtConfigGroup drtCfg;
	private final Fleet fleet;
	private final MobsimTimer mobsimTimer;
	private final EventsManager eventsManager;
	private final DrtScheduler scheduler;

	private final ParallelMultiVehicleInsertionProblem insertionProblem;

	@Inject
	public DefaultUnplannedRequestInserter(DrtConfigGroup drtCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
			Fleet fleet, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			@Named(DefaultDrtOptimizer.DRT_OPTIMIZER) TravelDisutility travelDisutility,
			MobsimTimer mobsimTimer, DrtVehicleFilter vehicleFilter, EventsManager eventsManager,
			DrtScheduler scheduler) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.mobsimTimer = mobsimTimer;
		this.eventsManager = eventsManager;
		this.scheduler = scheduler;

		SingleVehicleInsertionProblem[] singleVehicleInsertionProblems = new SingleVehicleInsertionProblem[drtCfg
				.getNumberOfThreads()];
		for (int i = 0; i < singleVehicleInsertionProblems.length; i++) {
			FastMultiNodeDijkstra router = (FastMultiNodeDijkstra)new FastMultiNodeDijkstraFactory(true)
					.createPathCalculator(network, travelDisutility, travelTime);
			BackwardFastMultiNodeDijkstra backwardRouter = (BackwardFastMultiNodeDijkstra)new BackwardFastMultiNodeDijkstraFactory(
					true).createPathCalculator(network, travelDisutility, travelTime);
			singleVehicleInsertionProblems[i] = new SingleVehicleInsertionProblem(router, backwardRouter,
					drtCfg.getStopDuration(), drtCfg.getMaxWaitTime(), mobsimTimer);
		}

		insertionProblem = new ParallelMultiVehicleInsertionProblem(singleVehicleInsertionProblems, vehicleFilter);
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		insertionProblem.shutdown();
	}

	@Override
	public void scheduleUnplannedRequests(Collection<DrtRequest> unplannedRequests) {
		if (unplannedRequests.isEmpty()) {
			return;
		}

		VehicleData vData = new VehicleData(mobsimTimer.getTimeOfDay(), fleet.getVehicles().values());

		Iterator<DrtRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {

			DrtRequest req = reqIter.next();
			BestInsertion best = insertionProblem.findBestInsertion(req, vData);
			if (best == null) {
				eventsManager.processEvent(new DrtRequestRejectedEvent(mobsimTimer.getTimeOfDay(), req.getId()));
				if (drtCfg.isPrintDetailedWarnings()) {
					Logger.getLogger(getClass())
							.warn("No vehicle found for drt request from passenger \t" + req.getPassenger().getId()
									+ "\tat\t" + Time.writeTime(req.getSubmissionTime()) + "\tfrom Link\t"
									+ req.getFromLink().getId());
				}
			} else {
				scheduler.insertRequest(best.vehicleEntry, req, best.insertion);
				vData.updateEntry(best.vehicleEntry);
				eventsManager.processEvent(new DrtRequestScheduledEvent(mobsimTimer.getTimeOfDay(), req.getId(),
						best.vehicleEntry.vehicle.getId(), req.getPickupTask().getEndTime(),
						req.getDropoffTask().getBeginTime()));
			}
			reqIter.remove();
		}
	}
}
