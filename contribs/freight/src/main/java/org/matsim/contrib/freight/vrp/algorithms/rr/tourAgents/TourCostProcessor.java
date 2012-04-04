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
		if(tour.getActivities().size() <= 2){
			tour.getCosts().transportCosts = 0.0;
			tour.getCosts().transportTime = 0.0;
			return;
		}
		reset(tour);
		counter.incCounter();
		counter.printCounter();
		TourActivity prevAct = tour.getActivities().getFirst();
		for(int i=1;i<tour.getActivities().size();i++){
			TourActivity toAct = tour.getActivities().get(i);
			if(toAct instanceof JobActivity){
				toAct.setCurrentLoad(prevAct.getCurrentLoad() + ((JobActivity)toAct).getCapacityDemand());
			}
			else{
				toAct.setCurrentLoad(prevAct.getCurrentLoad());
			}
			tour.costs.transportCosts += costs.getTransportCost(prevAct.getLocationId(),toAct.getLocationId(), 0.0);
			tour.costs.transportTime  += costs.getTransportTime(prevAct.getLocationId(),toAct.getLocationId(), 0.0);
			prevAct = toAct;
		}
	}
	
	private void reset(Tour tour) {
		tour.costs.transportCosts = 0.0;
		tour.costs.transportTime = 0.0;
	}

}
