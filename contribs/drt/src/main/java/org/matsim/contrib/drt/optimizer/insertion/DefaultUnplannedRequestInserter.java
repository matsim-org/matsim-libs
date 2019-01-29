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
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

/**
 * @author michalm
 */
public class DefaultUnplannedRequestInserter implements UnplannedRequestInserter, MobsimBeforeCleanupListener {
	private static final Logger log = Logger.getLogger(DefaultUnplannedRequestInserter.class);
	public static final String NO_INSERTION_FOUND_CAUSE = "no_insertion_found";

	private final DrtConfigGroup drtCfg;
	private final Fleet fleet;
	private final MobsimTimer mobsimTimer;
	private final EventsManager eventsManager;
	private final RequestInsertionScheduler insertionScheduler;
	private final VehicleData.EntryFactory vehicleDataEntryFactory;

	private final ForkJoinPool forkJoinPool;
	private final ParallelMultiVehicleInsertionProblem insertionProblem;

	public DefaultUnplannedRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer,
			EventsManager eventsManager, RequestInsertionScheduler insertionScheduler,
			VehicleData.EntryFactory vehicleDataEntryFactory, PrecalculablePathDataProvider pathDataProvider,
			InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.mobsimTimer = mobsimTimer;
		this.eventsManager = eventsManager;
		this.insertionScheduler = insertionScheduler;
		this.vehicleDataEntryFactory = vehicleDataEntryFactory;

		forkJoinPool = new ForkJoinPool(drtCfg.getNumberOfThreads());
		insertionProblem = new ParallelMultiVehicleInsertionProblem(pathDataProvider, drtCfg, mobsimTimer, forkJoinPool,
				penaltyCalculator);
		insertionScheduler.initSchedules(drtCfg.isChangeStartLinkToLastLinkInSchedule());
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

		VehicleData vData = new VehicleData(mobsimTimer.getTimeOfDay(), fleet.getVehicles().values().stream(),
				vehicleDataEntryFactory, forkJoinPool);

		Iterator<DrtRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {
			DrtRequest req = reqIter.next();
			Optional<BestInsertion> best = insertionProblem.findBestInsertion(req, vData.getEntries());
			if (!best.isPresent()) {
				req.setRejected(true);
				eventsManager.processEvent(
						new PassengerRequestRejectedEvent(mobsimTimer.getTimeOfDay(), drtCfg.getMode(), req.getId(),
								NO_INSERTION_FOUND_CAUSE));
				eventsManager.processEvent(new PersonStuckEvent(mobsimTimer.getTimeOfDay(), req.getPassengerId(),
						req.getFromLink().getId(), req.getMode()));
				if (drtCfg.isPrintDetailedWarnings()) {
					log.warn("No insertion found for drt request "
							+ req
							+ " from passenger id="
							+ req.getPassengerId()
							+ " fromLinkId="
							+ req.getFromLink().getId());
				}
			} else {
				BestInsertion bestInsertion = best.get();
				insertionScheduler.scheduleRequest(bestInsertion.vehicleEntry, req, bestInsertion.insertion);
				vData.updateEntry(bestInsertion.vehicleEntry.vehicle);
				eventsManager.processEvent(
						new DrtRequestScheduledEvent(mobsimTimer.getTimeOfDay(), drtCfg.getMode(), req.getId(),
								bestInsertion.vehicleEntry.vehicle.getId(), req.getPickupTask().getEndTime(),
								req.getDropoffTask().getBeginTime()));
			}
			reqIter.remove();
		}
	}
}
