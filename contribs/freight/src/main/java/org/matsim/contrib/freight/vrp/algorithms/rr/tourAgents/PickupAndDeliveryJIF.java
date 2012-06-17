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
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

class PickupAndDeliveryJIF implements JobInsertionFinder {

	private Costs costs;
	private Vehicle vehicle;
	private Tour tour;
	
	private TourActivityRecorder activityRecorder;

	PickupAndDeliveryJIF(Costs costs, Vehicle vehicle, Tour tour) {
		this.costs = costs;
		this.vehicle = vehicle;
		this.tour = tour;
		
	}

	public void setActivityRecorder(TourActivityRecorder activityRecorder) {
		this.activityRecorder = activityRecorder;
	}

	@Override
	public InsertionData find(Job job, double bestKnownPrice) {
		Shipment shipment = (Shipment)job;
		Pickup pickup = createPickup(shipment);
		Delivery delivery = createDelivery(shipment);
		
		activityRecorder.initialiseRecorder(vehicle, tour, pickup, delivery); 
		
		Double bestMarginalCost = bestKnownPrice;
		Integer bestPickupInsertionIndex = null;
		Integer bestDeliveryInsertionIndex = null;
		
		activityRecorder.insertionProcedureStarts();
		for(int pickupIndex=1;pickupIndex<tour.getActivities().size();pickupIndex++){
			activityRecorder.pickupInsertionAt(pickupIndex);
			if(activityRecorder.finishInsertionProcedure()){
				break;
			}
			if(activityRecorder.continueWithNextIndex()){
				continue;
			}
			double mc4pickup = getMarginalInsertionCosts(getActivity(tour,pickupIndex-1), getActivity(tour,pickupIndex), pickup);
			if(mc4pickup > bestMarginalCost){
				continue;
			}
			for(int deliveryIndex=pickupIndex;deliveryIndex<tour.getActivities().size();deliveryIndex++){
				activityRecorder.deliveryInsertionAt(deliveryIndex);
				if(activityRecorder.continueWithNextIndex()){
					continue;
				}
				if(activityRecorder.finishInsertionProcedure()){
					break;
				}
				double totalMarginalCost;
				double mc4delivery;
				if(pickupIndex == deliveryIndex){
					mc4delivery = getMarginalInsertionCosts(pickup, getActivity(tour,pickupIndex), delivery);
				}
				else{
					mc4delivery = getMarginalInsertionCosts(getActivity(tour,deliveryIndex-1), getActivity(tour,deliveryIndex), delivery);
				}
				totalMarginalCost = mc4pickup + mc4delivery;
				if(totalMarginalCost < bestMarginalCost){
					bestMarginalCost = totalMarginalCost;
					bestPickupInsertionIndex = pickupIndex;
					bestDeliveryInsertionIndex = deliveryIndex;
				}
			}
		}
		return new InsertionData(bestMarginalCost, bestPickupInsertionIndex, bestDeliveryInsertionIndex);
	}
	
	private double getMarginalInsertionCosts(TourActivity prevAct, TourActivity nextAct, TourActivity newAct) {
		return new LocalMCCalculator(costs).calculateMarginalCosts(prevAct, nextAct, newAct);
	}

	private TourActivity getActivity(Tour tour, int i) {
		return tour.getActivities().get(i);
	}

	private Delivery createDelivery(Shipment shipment) {
		return new Delivery(shipment);
	}

	private Pickup createPickup(Shipment shipment) {
		return new Pickup(shipment);
	}

}
