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
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

class SingleDepotDistributionLeastCostTourCalculator extends LeastCostTourCalculator{
	
	private static boolean warned = false;
	
	private static Logger logger = Logger.getLogger(SingleDepotDistributionLeastCostTourCalculator.class);
	
	private LeastCostInsertionCalculator marginalInsertionCostCalculator;

	private VehicleRoutingCosts costs;
	
	SingleDepotDistributionLeastCostTourCalculator(VehicleRoutingCosts costs) {
		super();
		this.costs = costs;
	}
	
	public void setMarginalCostCalculator(LeastCostInsertionCalculator marginalInsertionCostCalculator){
		this.marginalInsertionCostCalculator = marginalInsertionCostCalculator;
	}

	@Override
	InsertionData calculateLeastCostTour(Job job, Vehicle vehicle, TourImpl tour, Driver driver, double bestKnownCosts) {
		Double bestPenalty = bestKnownCosts;
		Shipment shipment = (Shipment)job;
		if(!checkCapacity(tour,shipment,vehicle)){
			return InsertionData.createNoInsertionFound();
		}
		
		Pickup pickup = new Pickup(shipment);
		TourImpl tourCopy = tour;
		int startListIterator = tour.getActivities().size()/2-1;
		boolean pickupInserted = false;
		
		if(pickup.getOperationTime() > 0){
			if(!warned){
				logger.warn("pickup operation start-time decrease performance of singleDepotVrpProblems significantly. it might be better to vary driver/vehicleEarliestStartTime and assume a loaded vehicle");
				warned = true;
			}
			startListIterator++;
			tourCopy = new TourImpl(tour);
			tourCopy.getActivities().add(1, pickup);
			pickupInserted = true;
			boolean isFeasible = updateTour(tourCopy, vehicle, driver);
			if(!isFeasible){
				return InsertionData.createNoInsertionFound();
			}
		}
		
		Delivery deliveryAct = new Delivery(shipment);
		Integer insertionIndex = null;
		Iterator<TourActivity> actIter = tourCopy.getActivities().listIterator(startListIterator);
		TourActivity startAct = actIter.next();
		assertCorrectStartAct(startAct);
		TourActivity prevAct = startAct;
		
		while(actIter.hasNext()){
			TourActivity currAct = actIter.next();
			if(!anotherPreCheck(prevAct,currAct,deliveryAct)){
				prevAct = currAct;
				continue;
			}
			double mc = marginalInsertionCostCalculator.calculateLeastCost(tourCopy, prevAct, currAct, deliveryAct, driver, vehicle);			
			double penalty = mc;
			if(penalty < bestPenalty){
				bestPenalty = penalty;
				insertionIndex = tourCopy.getActivities().indexOf(currAct);
				if(insertionIndex == -1){
					throw new IllegalStateException("this cannot happen. activity " + currAct + " not in tour " + tour);
				}
			}
			prevAct = currAct;
		}
		if(insertionIndex == null){
			return InsertionData.createNoInsertionFound();
		}
		int finalInsertionIndex = insertionIndex;
		if(pickupInserted){
			finalInsertionIndex--;
		}
		return new InsertionData(bestPenalty, new int[]{1, finalInsertionIndex});
	}

	private boolean updateTour(TourImpl tourCopy, Vehicle vehicle, Driver driver) {
		boolean isFeasible = new TourCostAndTWProcessor(costs).process(tourCopy, vehicle, driver);
		return isFeasible;
	}
	
	private void assertCorrectStartAct(TourActivity startAct) {
		if(startAct instanceof Start || startAct instanceof Pickup){
			return;
		}
		throw new IllegalStateException("insertion should start with the startAct or the last pickupAct." + startAct);
		
	}

	private boolean checkCapacity(TourImpl tour, Shipment shipment, Vehicle vehicle) {
		if(tour.tourData.totalLoad + shipment.getSize() > vehicle.getCapacity()){
			return false;
		}
		return true;
	}
	
	private boolean anotherPreCheck(TourActivity prevAct, TourActivity currAct, TourActivity deliveryAct) {
		if(deliveryAct.getLatestOperationStartTime() < prevAct.getEarliestOperationStartTime()){
			return false;
		}
		if(deliveryAct.getEarliestOperationStartTime() > currAct.getLatestOperationStartTime()){
			return false;
		}
		return true;
	}
}
