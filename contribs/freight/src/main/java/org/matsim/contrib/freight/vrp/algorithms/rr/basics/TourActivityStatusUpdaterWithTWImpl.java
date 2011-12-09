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
package org.matsim.contrib.freight.vrp.algorithms.rr.basics;

import org.matsim.contrib.freight.vrp.algorithms.rr.api.TourActivityStatusUpdater;
import org.matsim.contrib.freight.vrp.api.Costs;
import org.matsim.contrib.freight.vrp.basics.DeliveryFromDepot;
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

public class TourActivityStatusUpdaterWithTWImpl implements TourActivityStatusUpdater{

	private Costs costs;
	
	public TourActivityStatusUpdaterWithTWImpl(Costs costs) {
		super();
		this.costs = costs;
	}

	@Override
	public void update(Tour tour) {
		updateTimeWindowsAndLoadsAtTourActivities(tour);
	}
	
	private void updateTimeWindowsAndLoadsAtTourActivities(Tour tour) {
		reset(tour);
		TourActivity nextCustomer = null;
		TourActivity lastCustomer = null;
		double costs = 0.0;
		int loadsAtDepot = getLoadAtDepot(tour);
		int nOfCustomers = tour.getActivities().size(); 
		int j=nOfCustomers-1;
		for(int i=0;i<nOfCustomers;i++){
			if(nextCustomer == null){
				nextCustomer = tour.getActivities().get(j);
			}
			else{
				TourActivity currentAct = tour.getActivities().get(j);
				/*
				 * hier beißt sich die katze in den schwanz
				 * ich versuch ne startZeit zu bestimmen. dazu brauch ich die reisezeit. für die reisezeit benötige ich aber die startZeit.
				 * deshalb berechne ich das erstmal mit freeFlowSpeed bzw. mit der ersten zeitscheibe. 
				 */
				double late = Math.min(currentAct.getLatestArrTime(), nextCustomer.getLatestArrTime() - currentAct.getServiceTime() - getTransportTime(currentAct,nextCustomer,0.0));
				currentAct.setLatestArrTime(late);
				nextCustomer = currentAct;
			}
			if(lastCustomer == null){
				lastCustomer = tour.getActivities().get(i);
				lastCustomer.setCurrentLoad(loadsAtDepot);
			}
			else{
				TourActivity currentAct = tour.getActivities().get(i);
				double earliestStartTime = lastCustomer.getEarliestArrTime() + lastCustomer.getServiceTime();
				double early = Math.max(currentAct.getEarliestArrTime(), earliestStartTime + getTransportTime(lastCustomer,currentAct, earliestStartTime));
				currentAct.setEarliestArrTime(early);
				int currentLoad = lastCustomer.getCurrentLoad() + currentAct.getCustomer().getDemand();
				currentAct.setCurrentLoad(currentLoad);
				costs += this.costs.getGeneralizedCost(lastCustomer.getLocation(), currentAct.getCustomer().getLocation(), earliestStartTime);
				tour.costs.generalizedCosts += this.costs.getGeneralizedCost(lastCustomer.getLocation(), currentAct.getCustomer().getLocation(), earliestStartTime);
				tour.costs.distance += this.costs.getDistance(lastCustomer.getLocation(), currentAct.getCustomer().getLocation(), earliestStartTime);
				tour.costs.time  += this.costs.getTransportTime(lastCustomer.getLocation(), currentAct.getCustomer().getLocation(), earliestStartTime);
				lastCustomer = currentAct;
			}
			j--;
		}
	}
	
	private void reset(Tour tour) {
		tour.costs.generalizedCosts = 0.0;
		tour.costs.distance = 0.0;
		tour.costs.time = 0.0;
		
	}

	private double getTransportTime(TourActivity act1, TourActivity act2, double time) {
		return costs.getTransportTime(act1.getLocation(), act2.getLocation(), time);
	}

	private int getLoadAtDepot(Tour tour) {
		int loadAtDepot = 0;
		for(TourActivity tA : tour.getActivities()){
			if(tA instanceof DeliveryFromDepot){
				loadAtDepot += tA.getCustomer().getDemand();
			}
		}
		return loadAtDepot*-1;
	}
}
