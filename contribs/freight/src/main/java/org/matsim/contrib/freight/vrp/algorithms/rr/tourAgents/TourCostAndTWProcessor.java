/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import java.util.Iterator;

import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
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

public class TourCostAndTWProcessor implements TourStatusProcessor{
	
	private Costs costs;
	
	public Counter counter;
	
	public TourCostAndTWProcessor(Costs costs) {
		super();
		this.costs = costs;
		counter = new Counter("#tour processors ");
	}

	@Override
	public void process(Tour tour) {
//		counter.incCounter();
//		counter.printCounter();
		reset(tour);
		if(tour.isEmpty()){
			return;
		}
		updateTimeWindowsAndLoadsAtTourActivities(tour);
	}
	
	private void updateTimeWindowsAndLoadsAtTourActivities(Tour tour) {
		updateEarliestArrivalTimes(tour);
		updateLatestArrivalTimes(tour);
	}
	
	private void updateEarliestArrivalTimes(Tour tour) {
		TourActivity prevAct = null;
		for(TourActivity currentAct : tour.getActivities()){
			if(prevAct == null){
				prevAct = currentAct;
				continue;
			}
			updateLoad(tour,prevAct,currentAct);
			
			double startTimeAtPrevAct = prevAct.getEarliestOperationStartTime() + prevAct.getOperationTime();
			double transportTime = getTransportTime(prevAct, currentAct, startTimeAtPrevAct);
			double earliestArrTimeAtCurrAct = startTimeAtPrevAct + transportTime; 
			double earliestOperationStartTime = Math.max(currentAct.getEarliestOperationStartTime(), earliestArrTimeAtCurrAct);
			currentAct.setEarliestOperationStartTime(earliestOperationStartTime);
	
			tour.getTourStats().transportCosts += (this.costs.getTransportCost(prevAct.getLocationId(), currentAct.getLocationId(), startTimeAtPrevAct));
			tour.getTourStats().transportTime += transportTime;
			
			prevAct = currentAct;
		}
	
	}

	private void updateLatestArrivalTimes(Tour tour) {
		TourActivity prevAct = null;
		Iterator<TourActivity> actIterator = tour.getActivities().descendingIterator(); 
		while(actIterator.hasNext()){
			if(prevAct == null){
				prevAct = actIterator.next();
				continue;
			}
			TourActivity currAct = actIterator.next();
			
			double latestArrAtCurrAct = prevAct.getLatestOperationStartTime() - currAct.getOperationTime() - 
					getBackwardTransportTime(currAct, prevAct, prevAct.getLatestOperationStartTime());
			double latestOperationStartTime = Math.min(currAct.getLatestOperationStartTime(), latestArrAtCurrAct);
			currAct.setLatestOperationStartTime(latestOperationStartTime);
			
			prevAct = currAct;
		}
		
	}

	private void updateLoad(Tour tour, TourActivity prevAct, TourActivity currentAct) {
		if(currentAct instanceof JobActivity){
			currentAct.setCurrentLoad(prevAct.getCurrentLoad() + ((JobActivity)currentAct).getCapacityDemand());
			if(currentAct instanceof Pickup){
				tour.getTourStats().totalLoad += ((Pickup) currentAct).getCapacityDemand();
			}
		}
		else{
			currentAct.setCurrentLoad(prevAct.getCurrentLoad());
		}
	}

	private void reset(Tour tour) {
		tour.getTourStats().reset();
	}

	private double getTransportTime(TourActivity act1, TourActivity act2, double departureTime) {
		return costs.getTransportTime(act1.getLocationId(), act2.getLocationId(), departureTime);
	}
	
	private double getBackwardTransportTime(TourActivity act1, TourActivity act2, double arrivalTime) {
		return costs.getBackwardTransportTime(act1.getLocationId(), act2.getLocationId(), arrivalTime);
	}


}
