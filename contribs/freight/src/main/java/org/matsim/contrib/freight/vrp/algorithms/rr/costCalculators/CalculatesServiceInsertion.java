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
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.InsertionData;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

public class CalculatesServiceInsertion implements JobInsertionCalculator{
	
	private static Logger logger = Logger.getLogger(CalculatesServiceInsertion.class);
	
	private ActivityInsertionCalculator actInsertionCalculator;

	public CalculatesServiceInsertion(ActivityInsertionCalculator actInsertionCalculator) {
		super();
		this.actInsertionCalculator = actInsertionCalculator;
	}

	@Override
	public InsertionData calculate(VehicleRoute vehicleRoute, Job job, Vehicle newVehicle, Driver newDriver, double bestKnownCosts) {
		TourImpl tour = vehicleRoute.getTour();
		Double bestCost = bestKnownCosts;
		Service service = (Service)job;
		if(!checkCapacity(tour,service.getCapacityDemand(),newVehicle)){
			return InsertionData.createNoInsertionFound();
		}
		Delivery deliveryAct = new Delivery(service);
		Integer insertionIndex = null;
		Iterator<TourActivity> actIter = tour.getActivities().iterator();
		TourActivity prevAct = actIter.next();
		while(actIter.hasNext()){
			TourActivity currAct = actIter.next();
			if(!checkTimeWindowConstraints(prevAct,currAct,deliveryAct)){
				prevAct = currAct;
				continue;
			}
			double mc = actInsertionCalculator.calculate(tour, prevAct, currAct, deliveryAct, newDriver, newVehicle);			
			if(mc < bestCost){
				bestCost = mc;
				insertionIndex = tour.getActivities().indexOf(currAct);
			}
			prevAct = currAct;
		}
		if(insertionIndex == null){
			return InsertionData.createNoInsertionFound();
		}
		return new InsertionData(bestCost, new int[]{insertionIndex});
	}

	private boolean checkCapacity(TourImpl tour, int demand, Vehicle vehicle) {
		if(tour.tourData.totalLoad + demand > vehicle.getCapacity()){
			return false;
		}
		return true;
	}
	
	private boolean checkTimeWindowConstraints(TourActivity prevAct, TourActivity currAct, TourActivity deliveryAct) {
		if(deliveryAct.getLatestOperationStartTime() < prevAct.getEarliestOperationStartTime()){
			return false;
		}
		if(deliveryAct.getEarliestOperationStartTime() > currAct.getLatestOperationStartTime()){
			return false;
		}
		return true;
	}
}
