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
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;


/**
 * Updates time windows of a given tour. Earliest arrival times are computed by starting the tour right from the beginning. 
 * Latest arr. times are computed by starting computation from the end of a tour.
 * 
 * It is important to note that this class can consider time dependent cost. However, a conflict occurs when updating latest arrival time.
 * Basically, computations is conducted by going back in time. For time dependent vehicle routing however, one requires a start time for travel
 * time calculation. The start time is unfortunately exacly the variable, computation is done for. For now, travel time is thus calculated 
 * based on the first time slice (assuming that we have approximately free flow speed in that slice).
 * 
 * @author stefan schroeder
 *
 */

public class TourCostAndTWProcessor implements TourStatusProcessor{

	private Costs costs;
	
	public TourCostAndTWProcessor(Costs costs) {
		super();
		this.costs = costs;
	}

	@Override
	public void process(Tour tour) {
		updateTimeWindowsAndLoadsAtTourActivities(tour);
	}
	
	private void updateTimeWindowsAndLoadsAtTourActivities(Tour tour) {
		reset(tour);		
		int nOfActivities = tour.getActivities().size();
		TourActivity nextActivityBackwardInTime = tour.getActivities().getFirst();
		TourActivity nextActivityForwardInTime = tour.getActivities().getLast();
		
		for(int i=1,j=nOfActivities-2;i<nOfActivities;i++,j--){
			/*
			 * hier beißt sich die katze in den schwanz
			 * ich versuch ne startZeit zu bestimmen. dazu brauch ich die reisezeit. für die reisezeit benötige ich aber die startZeit.
			 * deshalb berechne ich das erstmal mit freeFlowSpeed bzw. mit der ersten zeitscheibe. 
			 */
			double late = Math.min(tour.getActivities().get(j).getLatestArrTime(), nextActivityForwardInTime.getLatestArrTime() - tour.getActivities().get(j).getServiceTime() - 
					getBackwardTransportTime(tour.getActivities().get(j), nextActivityForwardInTime, nextActivityForwardInTime.getLatestArrTime()));
			tour.getActivities().get(j).setLatestArrTime(late);
			

			double earliestStartTimeAtLastActivity = nextActivityBackwardInTime.getEarliestArrTime() + nextActivityBackwardInTime.getServiceTime();
			double arrivalTimeAtCurrentActivity = earliestStartTimeAtLastActivity + getTransportTime(nextActivityBackwardInTime, tour.getActivities().get(i), earliestStartTimeAtLastActivity); 
			double early = Math.max(tour.getActivities().get(i).getEarliestArrTime(), arrivalTimeAtCurrentActivity);
			tour.getActivities().get(i).setEarliestArrTime(early);
			
			tour.costs.generalizedCosts += this.costs.getGeneralizedCost(nextActivityBackwardInTime.getLocationId(), tour.getActivities().get(i).getLocationId(), earliestStartTimeAtLastActivity);
			tour.costs.distance += this.costs.getDistance(nextActivityBackwardInTime.getLocationId(), tour.getActivities().get(i).getLocationId(), earliestStartTimeAtLastActivity);
			tour.costs.time  += this.costs.getTransportTime(nextActivityBackwardInTime.getLocationId(), tour.getActivities().get(i).getLocationId(), earliestStartTimeAtLastActivity);
			
			nextActivityForwardInTime = tour.getActivities().get(j);
			nextActivityBackwardInTime = tour.getActivities().get(i);
		}
	}
	
	private void reset(Tour tour) {
		tour.costs.generalizedCosts = 0.0;
		tour.costs.distance = 0.0;
		tour.costs.time = 0.0;
	}

	private double getTransportTime(TourActivity act1, TourActivity act2, double departureTime) {
		return costs.getTransportTime(act1.getLocationId(), act2.getLocationId(), departureTime);
	}
	
	private double getBackwardTransportTime(TourActivity act1, TourActivity act2, double arrivalTime) {
		return costs.getBackwardTransportTime(act1.getLocationId(), act2.getLocationId(), arrivalTime);
	}
}
