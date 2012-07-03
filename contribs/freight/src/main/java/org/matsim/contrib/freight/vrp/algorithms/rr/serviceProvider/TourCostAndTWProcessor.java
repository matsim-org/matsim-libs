/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import java.util.Iterator;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.core.utils.misc.Counter;


/**
 * Updates time windows of a given tour. Earliest arrival times are computed by starting the tour right from the beginning. 
 * Latest arr. times are computed by starting computation from the end of a tour.
 * 
 * It is important to note that this class can consider time dependent cost. However, a conflict occurs when updating latest arrival time.
 * Basically, computations is conducted by going back in time. For time dependent vehicle routing however, one requires a start time for travel
 * time calculation. The start time is unfortunately exactly the variable, computation is done for. For now, travel time is thus calculated 
 * based on the first time slice (assuming that we have approximately free flow speed in that slice).
 * 
 * @author stefan schroeder
 *
 */

class TourCostAndTWProcessor extends TourStatusProcessor{
	
	public static Counter counter = new Counter("#updateTWProcesses: ");
	
	private VehicleRoutingCosts costs;
	
	public boolean ensureFeasibilityOfTours = true;
	
	public TourCostAndTWProcessor(VehicleRoutingCosts costs) {
		super();
		this.costs = costs;
	}

	@Override
	boolean process(Tour tour, Vehicle vehicle, Driver driver) {
		counter.incCounter();
//		counter.printCounter();
		reset(tour);
		if(tour.isEmpty()){
			return true;
		}
		boolean tourIsFeasible = updateTimeWindowsAndLoadsAtTourActivities(tour,vehicle,driver);
		return tourIsFeasible;
	}


	private boolean updateTimeWindowsAndLoadsAtTourActivities(Tour tour, Vehicle vehicle, Driver driver) {
		updateEarliestArrivalTimes(tour,vehicle,driver);
		boolean tourIsFeasible = updateLatestArrivalTimes(tour,vehicle,driver);
		return tourIsFeasible;
	}
	
	private void updateEarliestArrivalTimes(Tour tour, Vehicle vehicle, Driver driver) {
		TourActivity prevAct = null;
		for(TourActivity currentAct : tour.getActivities()){
			if(prevAct == null){
				prevAct = currentAct;
				continue;
			}
			updateLoad(tour,prevAct,currentAct);
			
			double startTimeAtPrevAct = prevAct.getEarliestOperationStartTime() + prevAct.getOperationTime();
			double transportTime = getTransportTime(prevAct, currentAct, startTimeAtPrevAct, driver, vehicle);
			double earliestArrTimeAtCurrAct = startTimeAtPrevAct + transportTime; 
			double earliestOperationStartTime = Math.max(currentAct.getEarliestOperationStartTime(), earliestArrTimeAtCurrAct);
			currentAct.setEarliestOperationStartTime(earliestOperationStartTime);
	
			tour.tourData.transportCosts += (this.costs.getTransportCost(prevAct.getLocationId(), currentAct.getLocationId(), startTimeAtPrevAct, driver, vehicle));
			tour.tourData.transportTime += transportTime;
			
			prevAct = currentAct;
		}
	}

	private boolean updateLatestArrivalTimes(Tour tour, Vehicle vehicle, Driver driver) {
		TourActivity prevAct = null;
		Iterator<TourActivity> actIterator = tour.getActivities().descendingIterator(); 
		while(actIterator.hasNext()){
			if(prevAct == null){
				prevAct = actIterator.next();
				continue;
			}
			TourActivity currAct = actIterator.next();
			double backwardArrAtCurrAct = prevAct.getLatestOperationStartTime() - getBackwardTransportTime(currAct, prevAct, prevAct.getLatestOperationStartTime(),driver,vehicle);
			double potentialLatestOperationStartTimeAtCurrAct = backwardArrAtCurrAct - currAct.getOperationTime();
			double latestOperationStartTime = Math.min(currAct.getLatestOperationStartTime(), potentialLatestOperationStartTimeAtCurrAct);
			currAct.setLatestOperationStartTime(latestOperationStartTime);
			if(ensureFeasibilityOfTours){
				if(currAct.getEarliestOperationStartTime() > currAct.getLatestOperationStartTime()){
//					throw new IllegalStateException("tour is not feasible");
					return false;
				}
			}
			prevAct = currAct;
		}
		return true;
		
	}

	private void updateLoad(Tour tour, TourActivity prevAct, TourActivity currentAct) {
		if(currentAct instanceof JobActivity){
			currentAct.setCurrentLoad(prevAct.getCurrentLoad() + ((JobActivity)currentAct).getCapacityDemand());
			if(currentAct instanceof Pickup){
				tour.tourData.totalLoad += ((Pickup) currentAct).getCapacityDemand();
			}
		}
		else{
			currentAct.setCurrentLoad(prevAct.getCurrentLoad());
		}
	}

	private void reset(Tour tour) {
		tour.tourData.reset();
	}

	private double getTransportTime(TourActivity act1, TourActivity act2, double departureTime, Driver driver, Vehicle vehicle) {
		return costs.getTransportTime(act1.getLocationId(), act2.getLocationId(), departureTime, driver, vehicle);
	}
	
	private double getBackwardTransportTime(TourActivity act1, TourActivity act2, double arrivalTime, Driver driver, Vehicle vehicle) {
		return costs.getBackwardTransportTime(act1.getLocationId(), act2.getLocationId(), arrivalTime, driver, vehicle);
	}


}
