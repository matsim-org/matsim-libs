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
package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import java.util.Iterator;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.core.utils.misc.Counter;


/**
 * 
 * @author stefan schroeder
 *
 */

public class CalculatesCostAndTWs implements TourStateCalculator{
	
	public static Counter counter = new Counter("#updateTWProcesses: ");
	
	private VehicleRoutingCosts costs;
	
	public boolean ensureFeasibilityOfTours = true;
	
	public CalculatesCostAndTWs(VehicleRoutingCosts costs) {
		super();
		this.costs = costs;
	}

	public boolean calculate(TourImpl tour, Vehicle vehicle, Driver driver) {
//		counter.incCounter();
//		counter.printCounter();
		reset(tour);
		if(tour.isEmpty()){
			return true;
		}
		boolean tourIsFeasible = updateTimeWindowsAndLoadsAtTourActivities(tour,vehicle,driver);
		return tourIsFeasible;
	}


	private boolean updateTimeWindowsAndLoadsAtTourActivities(TourImpl tour, Vehicle vehicle, Driver driver) {
		updateEarliestArrivalTimes(tour,vehicle,driver);
		boolean tourIsFeasible = updateLatestArrivalTimes(tour,vehicle,driver);
		return tourIsFeasible;
	}
	
	private void updateEarliestArrivalTimes(TourImpl tour, Vehicle vehicle, Driver driver) {

		int load = 0;
		Iterator<TourActivity> actIter = tour.getActivities().iterator();
		TourActivity prevAct = actIter.next();

		
		while(actIter.hasNext()){
			TourActivity currentAct = actIter.next();
			
			load += getLoad(currentAct);
			
			double startTimeAtPrevAct = prevAct.getEarliestOperationStartTime() + prevAct.getOperationTime();
			double transportTime = getTransportTime(prevAct, currentAct, startTimeAtPrevAct, driver, vehicle);
			double earliestArrTimeAtCurrAct = startTimeAtPrevAct + transportTime; 
			double earliestOperationStartTime = Math.max(currentAct.getTheoreticalEarliestOperationStartTime(), earliestArrTimeAtCurrAct);
			currentAct.setEarliestOperationStartTime(earliestOperationStartTime);
	
			double transportCost = this.costs.getTransportCost(prevAct.getLocationId(), currentAct.getLocationId(), startTimeAtPrevAct, driver, vehicle);
			
			tour.tourData.transportCosts += transportCost;
			tour.tourData.transportTime += transportTime;
			
			currentAct.setCurrentCost(tour.tourData.transportCosts);
			
			prevAct = currentAct;
		}
		tour.setLoad(load);
		tour.setTotalCost(tour.tourData.transportCosts);
	}

	private boolean updateLatestArrivalTimes(TourImpl tour, Vehicle vehicle, Driver driver) {
		TourActivity prevAct = tour.getActivities().get(tour.getActivities().size()-1);
		double startAtPrevAct = prevAct.getTheoreticalLatestOperationStartTime();
		for(int i=tour.getActivities().size()-2;i>=0;i--){
			TourActivity currAct = tour.getActivities().get(i);
			double backwardArrAtCurrAct = startAtPrevAct - getBackwardTransportTime(currAct, prevAct, startAtPrevAct, driver,vehicle);
			double potentialLatestOperationStartTimeAtCurrAct = backwardArrAtCurrAct - currAct.getOperationTime();
			double latestOperationStartTime = Math.min(currAct.getTheoreticalLatestOperationStartTime(), potentialLatestOperationStartTimeAtCurrAct);
			currAct.setLatestOperationStartTime(latestOperationStartTime);
			if(ensureFeasibilityOfTours){
				if(currAct.getEarliestOperationStartTime() > currAct.getLatestOperationStartTime()){
					return false;
				}
			}
			prevAct = currAct;
			startAtPrevAct = latestOperationStartTime;
		}
		return true;
		
	}
	
	private double getLoad(TourActivity currentAct) {
		if(currentAct instanceof JobActivity){
			if(currentAct instanceof Pickup){
				return ((JobActivity) currentAct).getJob().getCapacityDemand();
			}
		}
		return 0;
	}


	private void updateLoad(TourImpl tour, TourActivity prevAct, TourActivity currentAct) {
		if(currentAct instanceof JobActivity){
			currentAct.setCurrentLoad(prevAct.getCurrentLoad() + ((JobActivity)currentAct).getCapacityDemand());
			if(((JobActivity) currentAct).getJob() instanceof Service){
				tour.tourData.totalLoad += ((Service)((JobActivity) currentAct).getJob()).getCapacityDemand(); 
			}
			else if(currentAct instanceof Pickup){
				tour.tourData.totalLoad += ((Pickup) currentAct).getCapacityDemand();
			}
		}
		else{
			currentAct.setCurrentLoad(prevAct.getCurrentLoad());
		}
	}

	private void reset(TourImpl tour) {
		tour.tourData.reset();
	}

	private double getTransportTime(TourActivity act1, TourActivity act2, double departureTime, Driver driver, Vehicle vehicle) {
		return costs.getTransportTime(act1.getLocationId(), act2.getLocationId(), departureTime, driver, vehicle);
	}
	
	private double getBackwardTransportTime(TourActivity act1, TourActivity act2, double arrivalTime, Driver driver, Vehicle vehicle) {
		return costs.getBackwardTransportTime(act1.getLocationId(), act2.getLocationId(), arrivalTime, driver, vehicle);
	}


}
