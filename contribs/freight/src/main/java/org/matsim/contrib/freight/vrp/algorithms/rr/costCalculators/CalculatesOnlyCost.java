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

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
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

public class CalculatesOnlyCost implements TourStateCalculator{
	
	private static Logger logger = Logger.getLogger(CalculatesOnlyCost.class);
	
	private VehicleRoutingCosts costs;
	
	public Counter counter;
	
	public CalculatesOnlyCost(VehicleRoutingCosts costs) {
		super();
		this.costs = costs;
		counter = new Counter("#tour processors (tourCostProcessor)");
	}
	
	@Override
	public boolean calculate(TourImpl tour, Vehicle vehicle, Driver driver){
		if(tour == null){
			throw new IllegalStateException("tour is null. this must not be.");
		}
		tour.reset();
		if(tour.isEmpty()){
			return true;
		}
		int load = 0;
		Iterator<TourActivity> actIter = tour.getActivities().iterator();
		TourActivity prevAct = actIter.next();

		while(actIter.hasNext()){
			TourActivity currAct = actIter.next();
			load += getLoad(currAct);

			double transportCost = costs.getTransportCost(prevAct.getLocationId(),currAct.getLocationId(), 0.0, driver, vehicle);
			tour.tourData.transportCosts += transportCost;
			tour.tourData.transportTime  += costs.getTransportTime(prevAct.getLocationId(),currAct.getLocationId(), 0.0, driver, vehicle);
			currAct.setCurrentCost(tour.tourData.transportCosts);
			
			prevAct = currAct;
		}
		tour.setLoad(load);
		tour.setTotalCost(tour.tourData.transportCosts);
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


}
