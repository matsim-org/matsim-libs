/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxibus.algorithm.optimizer.sharedTaxi;

import java.util.*;

import org.matsim.contrib.taxibus.TaxibusRequest;
import org.matsim.contrib.taxibus.algorithm.optimizer.*;
import org.matsim.contrib.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;
import org.matsim.contrib.taxibus.tasks.TaxibusTask;
import org.matsim.contrib.taxibus.tasks.TaxibusTask.TaxibusTaskType;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;

/**
 * @author jbischoff
 *
 */
public class SharedTaxiOptimizer extends AbstractTaxibusOptimizer {

	private final SharedTaxiDispatchFinder dispatchFinder;

	public SharedTaxiOptimizer(TaxibusOptimizerContext optimContext, boolean doUnscheduleAwaitingRequests,
			double maximumDetourFactor) {
		super(optimContext, doUnscheduleAwaitingRequests);
		this.dispatchFinder = new SharedTaxiDispatchFinder(optimContext, maximumDetourFactor);
	}

	@Override
	protected void scheduleUnplannedRequests() {
		Set<TaxibusRequest> handledRequests = new HashSet<>();
		for (TaxibusRequest request : getUnplannedRequests()) {
			TaxibusDispatch bestPath = findBestVehicleForRequest(request);
			if (bestPath != null) {
				getOptimContext().scheduler.scheduleRequest(bestPath);
				handledRequests.add(request);
			}

		}

		getUnplannedRequests().removeAll(handledRequests);

	}

	private TaxibusDispatch findBestVehicleForRequest(TaxibusRequest req) {
		TaxibusDispatch bestPath = null;
		Set<Vehicle> idleVehicles = new HashSet<>();
		Set<Vehicle> busyVehicles = new HashSet<>();
		for (Vehicle veh : this.getOptimContext().vrpData.getVehicles().values()) {
			Schedule schedule = veh.getSchedule();
			if (getOptimContext().scheduler.isIdle(veh)) {
				// empty vehicle = no customer onboard so far, we are adding those requests to a Set and let the
				// ordinary
				// BestDispatchFinder do the job
				idleVehicles.add(veh);
			} else if (getOptimContext().scheduler.isStarted(veh)) {
				// busy vehicle = we are currently picking someone up, maximum of passengers for this optimizer = 2;
				TaxibusTaskType type = ((TaxibusTask)schedule.getCurrentTask()).getTaxibusTaskType();
				// Logger.getLogger(getClass()).info(veh.getId() + " "+ type);
				if (type.equals(TaxibusTaskType.DRIVE_EMPTY)) {

					Set<TaxibusRequest> currentRequests = getOptimContext().scheduler.getCurrentlyPlannedRequests(schedule);
					if (currentRequests.size() < 2) {
						busyVehicles.add(veh);
					}
				}

			}

		}
		bestPath = dispatchFinder.findBestVehicleForRequest(req, busyVehicles, idleVehicles);
		if (bestPath != null) {
			if (busyVehicles.contains(bestPath.vehicle)) {
				// Shared ride: We need to get rid of the previous planned objects in schedule. In our case we know it
				// must be 3 (Stay,Drive,Dropoff)
				Schedule schedule = bestPath.vehicle.getSchedule();
				int oldcount = schedule.getTaskCount();
				for (int ix = oldcount; ix > schedule.getCurrentTask().getTaskIdx() + 2; ix--) {
					schedule.removeLastTask();
				}
				// Logger.getLogger(getClass()).info(schedule.getTasks().get(schedule.getTaskCount()-1));

				for (TaxibusRequest all : bestPath.requests) {
					all.setDropoffTask(null);
				}
			}
		}

		return bestPath;
	}

}
