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

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.scheduler.DrtScheduler;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

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
	public DefaultUnplannedRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer,
			EventsManager eventsManager, DrtScheduler scheduler, PrecalculatablePathDataProvider pathDataProvider) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.mobsimTimer = mobsimTimer;
		this.eventsManager = eventsManager;
		this.scheduler = scheduler;

		insertionProblem = new ParallelMultiVehicleInsertionProblem(pathDataProvider, drtCfg, mobsimTimer);
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

		VehicleData vData = new VehicleData(mobsimTimer.getTimeOfDay(), fleet.getVehicles().values().stream());

		Iterator<DrtRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {
			DrtRequest req = reqIter.next();
			Optional<BestInsertion> best = insertionProblem.findBestInsertion(req, vData.getEntries());
			if (!best.isPresent()) {
				req.setRejected(true);
				eventsManager.processEvent(new DrtRequestRejectedEvent(mobsimTimer.getTimeOfDay(), req.getId()));
				if (drtCfg.isPrintDetailedWarnings()) {
					Logger.getLogger(getClass())
							.warn("No vehicle found for drt request from passenger \t" + req.getPassenger().getId()
									+ "\tat\t" + Time.writeTime(req.getSubmissionTime()) + "\tfrom Link\t"
									+ req.getFromLink().getId());
				}
			} else {
				BestInsertion bestInsertion = best.get();
				scheduler.insertRequest(bestInsertion.vehicleEntry, req, bestInsertion.insertion);
				vData.updateEntry(bestInsertion.vehicleEntry.vehicle);
				eventsManager.processEvent(new DrtRequestScheduledEvent(mobsimTimer.getTimeOfDay(), req.getId(),
						bestInsertion.vehicleEntry.vehicle.getId(), req.getPickupTask().getEndTime(),
						req.getDropoffTask().getBeginTime()));
			}
			reqIter.remove();
		}
	}
}
