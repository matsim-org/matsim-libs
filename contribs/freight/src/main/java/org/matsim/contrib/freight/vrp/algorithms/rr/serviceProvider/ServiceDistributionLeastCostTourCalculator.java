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

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

class ServiceDistributionLeastCostTourCalculator extends LeastCostTourCalculator{
	
	private static Logger logger = Logger.getLogger(ServiceDistributionLeastCostTourCalculator.class);
	
	private LeastCostInsertionCalculator marginalInsertionCostCalculator;

	public void setMarginalCostCalculator(LeastCostInsertionCalculator marginalInsertionCostCalculator){
		this.marginalInsertionCostCalculator = marginalInsertionCostCalculator;
	}

	@Override
	InsertionData calculateLeastCostTour(Job job, Vehicle vehicle, TourImpl tour, Driver driver, double bestKnownCosts) {
		Double bestCost = bestKnownCosts;
		Service service = (Service)job;
		if(!checkCapacity(tour,service.getDemand(),vehicle)){
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
			double mc = marginalInsertionCostCalculator.calculateLeastCost(tour, prevAct, currAct, deliveryAct, driver, vehicle);			
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
