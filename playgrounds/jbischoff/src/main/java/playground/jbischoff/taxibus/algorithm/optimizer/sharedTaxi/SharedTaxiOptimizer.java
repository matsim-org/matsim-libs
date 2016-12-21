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

package playground.jbischoff.taxibus.algorithm.optimizer.sharedTaxi;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.util.Schedules2GIS;

import playground.jbischoff.drt.scheduler.tasks.DrtTask;
import playground.jbischoff.drt.scheduler.tasks.DrtTask.DrtTaskType;
import playground.jbischoff.taxibus.algorithm.optimizer.AbstractTaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizerContext;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;

/**
 * @author  jbischoff
 *
 */
public class SharedTaxiOptimizer extends AbstractTaxibusOptimizer {

	private final double maximumDetourFactor;
	private final SharedTaxiDispatchFinder dispatchFinder;
	
	public SharedTaxiOptimizer(TaxibusOptimizerContext optimContext, boolean doUnscheduleAwaitingRequests, double maximumDetourFactor) {
		super(optimContext, doUnscheduleAwaitingRequests);
		this.dispatchFinder = new SharedTaxiDispatchFinder(optimContext,maximumDetourFactor);
		this.maximumDetourFactor = maximumDetourFactor;
	}

	@Override
	protected void scheduleUnplannedRequests() {
		Set<TaxibusRequest> handledRequests = new HashSet<>();
		for (TaxibusRequest request : unplannedRequests){
			TaxibusDispatch bestPath = findBestVehicleForRequest(request);
			if (bestPath!= null){ 
				optimContext.scheduler.scheduleRequest(bestPath);
				handledRequests.add(request);}
			
		}
		
		unplannedRequests.removeAll(handledRequests);

	}

	private TaxibusDispatch findBestVehicleForRequest(TaxibusRequest req) {
		TaxibusDispatch bestPath = null;
		Link fromLink = req.getFromLink();
		Set<Vehicle> idleVehicles = new HashSet<>();
		Set<Vehicle> busyVehicles = new HashSet<>();
		for (Vehicle veh : this.optimContext.vrpData.getVehicles().values()){
			Schedule<DrtTask> schedule = (Schedule<DrtTask>)veh.getSchedule();
			if (optimContext.scheduler.isIdle(veh)){
				// empty vehicle = no customer onboard so far, we are adding those requests to a Set and let the ordinary 
				// BestDispatchFinder do the job
				idleVehicles.add(veh);
			}
			else if (optimContext.scheduler.isStarted(veh)){
				// busy vehicle = we are currently picking someone up, maximum of passengers for this optimizer = 2;
				DrtTaskType  type =  schedule.getCurrentTask().getDrtTaskType();
//				Logger.getLogger(getClass()).info(veh.getId() + " "+ type);
				if (schedule.getCurrentTask().getDrtTaskType().equals(DrtTaskType.DRIVE_EMPTY)){
					
					Set<TaxibusRequest> currentRequests = optimContext.scheduler.getCurrentlyPlannedRequests(schedule);
					if (currentRequests.size()<2){
						busyVehicles.add(veh);
					}
				}
		
			}
			
		}
		bestPath = dispatchFinder.findBestVehicleForRequest(req, busyVehicles, idleVehicles);
		if (bestPath!=null){
		if (busyVehicles.contains(bestPath.vehicle)){
			//Shared ride: We need to get rid of the previous planned objects in schedule. In our case we know it must be 3 (Stay,Drive,Dropoff)
			Schedule<DrtTask> schedule = (Schedule<DrtTask>) bestPath.vehicle.getSchedule();
			int oldcount = schedule.getTaskCount() ;
			for (int ix = oldcount ;ix>schedule.getCurrentTask().getTaskIdx()+2; ix--){
				schedule.removeLastTask();
			}
//			Logger.getLogger(getClass()).info(schedule.getTasks().get(schedule.getTaskCount()-1));

			
			for (TaxibusRequest all : bestPath.requests){
				all.setDropoffTask(null);
				all.getDriveWithPassengerTask().clear();
			}
		}}
		
		return bestPath;
	}

}
