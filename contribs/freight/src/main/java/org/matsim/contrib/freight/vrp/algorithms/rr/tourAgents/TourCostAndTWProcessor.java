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
		if(tour.getActivities().size() <= 2){
			reset(tour);
			return;
		}
		updateTimeWindowsAndLoadsAtTourActivities(tour);
	}
	
	private void updateTimeWindowsAndLoadsAtTourActivities(Tour tour) {
		reset(tour);		
		int nOfActivities = tour.getActivities().size();
		TourActivity prevAct = tour.getActivities().getFirst();
		TourActivity nextAct = tour.getActivities().getLast();
		
		for(int i=1,j=nOfActivities-2;i<nOfActivities;i++,j--){
			
			TourActivity currActFromBehind = tour.getActivities().get(j);
			TourActivity currActFromFront = tour.getActivities().get(i);
			
			double latestArrAtCurrActFromBehind = nextAct.getLatestArrTime() - tour.getActivities().get(j).getServiceTime() - 
					getBackwardTransportTime(tour.getActivities().get(j), nextAct, nextAct.getLatestArrTime());
			
			double late = Math.min(tour.getActivities().get(j).getLatestArrTime(), latestArrAtCurrActFromBehind);
			tour.getActivities().get(j).setLatestArrTime(late);
			
			if(currActFromFront instanceof JobActivity){
				currActFromFront.setCurrentLoad(prevAct.getCurrentLoad() + ((JobActivity)currActFromFront).getCapacityDemand());
				if(currActFromFront instanceof Pickup){
					tour.costs.totalLoad += ((Pickup) currActFromFront).getCapacityDemand();
				}
			}
			else{
				currActFromFront.setCurrentLoad(prevAct.getCurrentLoad());
			}
			
			double earliestStartTimeAtLastActivity = prevAct.getEarliestArrTime() + prevAct.getServiceTime();
			double earliestArrivalTimeAtCurrentActivity = earliestStartTimeAtLastActivity + getTransportTime(prevAct, currActFromFront, earliestStartTimeAtLastActivity); 
			double early = Math.max(currActFromFront.getEarliestArrTime(), earliestArrivalTimeAtCurrentActivity);
			currActFromFront.setEarliestArrTime(early);
			double waitingTime = 0.0;
			if(earliestArrivalTimeAtCurrentActivity < currActFromFront.getEarliestArrTime()){
				waitingTime = currActFromFront.getEarliestArrTime() - earliestArrivalTimeAtCurrentActivity;
			}
			
			double serviceTime = currActFromFront.getServiceTime();
			double transportTime = this.costs.getTransportTime(prevAct.getLocationId(), currActFromFront.getLocationId(), earliestStartTimeAtLastActivity);
		
			tour.costs.transportCosts += this.costs.getTransportCost(prevAct.getLocationId(), currActFromFront.getLocationId(), earliestStartTimeAtLastActivity);
			tour.costs.transportTime  += transportTime;
			tour.costs.serviceTime += serviceTime;
			tour.costs.waitingTime += waitingTime; 	
			tour.costs.totalDutyTime += waitingTime + serviceTime + transportTime;
			
			nextAct = tour.getActivities().get(j);
			prevAct = currActFromFront;
		}
	}
	
	private void reset(Tour tour) {
		tour.costs.transportCosts = 0.0;
		tour.costs.transportTime = 0.0;
		tour.costs.serviceTime = 0.0;
		tour.costs.waitingTime = 0.0;
		tour.costs.totalDutyTime = 0.0;
		tour.costs.totalLoad = 0;
	}

	private double getTransportTime(TourActivity act1, TourActivity act2, double departureTime) {
		return costs.getTransportTime(act1.getLocationId(), act2.getLocationId(), departureTime);
	}
	
	private double getBackwardTransportTime(TourActivity act1, TourActivity act2, double arrivalTime) {
		return costs.getBackwardTransportTime(act1.getLocationId(), act2.getLocationId(), arrivalTime);
	}
}
