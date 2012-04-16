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

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.core.utils.misc.Counter;


/**
 * It does not consider time dependent travel times. It does not make that much sense to 
 * enable this with it, since if start time does not matter, one should conduct the tour at 
 * night where we have no traffic. If start time matters, we have can formulate a vrp-problem 
 * with time windows.
 * 
 * @author stefan schroeder
 *
 */

public class TourCostProcessor implements TourStatusProcessor{
	
	private static Logger logger = Logger.getLogger(TourCostProcessor.class);
	
	private Costs costs;
	
	public Counter counter;
	
	public TourCostProcessor(Costs costs) {
		super();
		this.costs = costs;
		counter = new Counter("#tour processors (tourCostProcessor)");
	}
	
	@Override
	public void process(Tour tour){
		tour.getTourStats().reset();
		if(tour.isEmpty()){
			return;
		}
//		counter.incCounter();
//		counter.printCounter();
		TourActivity prevAct = null;
		for(TourActivity currAct : tour.getActivities()){
			if(prevAct == null){
				prevAct = currAct;
				continue;
			}
			updateLoad(tour, prevAct, currAct);
			
			tour.getTourStats().transportCosts += (costs.getTransportCost(prevAct.getLocationId(),currAct.getLocationId(), 0.0));
			tour.getTourStats().transportTime  += costs.getTransportTime(prevAct.getLocationId(),currAct.getLocationId(), 0.0);
			
			prevAct = currAct;
		}
	}

	private void updateLoad(Tour tour, TourActivity prevAct, TourActivity currAct) {
		if(currAct instanceof JobActivity){
			currAct.setCurrentLoad(prevAct.getCurrentLoad() + ((JobActivity)currAct).getCapacityDemand());
			if(currAct instanceof Pickup){
				tour.getTourStats().totalLoad += ((Pickup) currAct).getCapacityDemand();
			}
		}
		else{
			currAct.setCurrentLoad(prevAct.getCurrentLoad());
		}
	}


}
