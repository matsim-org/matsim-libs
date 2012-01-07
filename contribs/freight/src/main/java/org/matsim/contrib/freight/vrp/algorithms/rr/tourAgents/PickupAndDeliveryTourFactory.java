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
import org.matsim.contrib.freight.vrp.basics.Constraints;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;


/**
 * This class inserts a new shipment in a given tour such that insertion costs (based on 
 * generalized costs) are minimal. Insertion is done subject to various constraints (e.g. time and capacity constraints).
 * 
 * It is possible to consider time dependent costs.
 * Depot [08:00 - 09:00] - Customer1 [10:00 - 11:00] - Customer2 [12:00 - 17:00] - Depot [13:00 - 18:00] 
 * 
 * Insertion of a new Shipment (NewCustomerFrom [00:00 - 18:00], New CustomerTo [00:00 - 18:00]) after Customer1 
 * earliest: mc_earliest = cost(cust1,newCust,10:00) + cost(newCust,cust2,10:00+tt(cust1,newCust,earliest)+serviceTime) - cost(cust1,cust2,10:00)
 * latest: mc_latest = cost(cust1,newCust,11:00) + cost(newCust,cust2,11:00+tt(cust1,newCust,earliest)+serviceTime) - cost(cust1,cust2,11:00)
 * mc = mc_earliest 
 *  
 * @author stefan schroeder
 *
 */

public class PickupAndDeliveryTourFactory implements TourFactory {
	
	private static Logger logger = Logger.getLogger(PickupAndDeliveryTourFactory.class);
	
	private Costs costs;
	
	private Constraints constraints;

	private TourStatusProcessor tourActivityUpdater;

	public PickupAndDeliveryTourFactory(Costs costs, Constraints constraints, TourStatusProcessor tourActivityUpdater) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		this.tourActivityUpdater = tourActivityUpdater;
	}
	
	public Tour createTour(Vehicle vehicle, Tour oldTour, Job job, double bestKnownPrice){
		Tour newTour = null;
		newTour = buildTourWithNewShipment(vehicle,oldTour,(Shipment)job, bestKnownPrice);
		return newTour;
	}
	
	private Tour buildTourWithNewShipment(Vehicle vehicle, Tour tour, Shipment shipment, double bestKnownPrice) {
			Pickup pickup = createPickup(shipment);
			Delivery delivery = createDelivery(shipment);
			Double bestMarginalCost = bestKnownPrice;
			Tour bestTour = null;
			for(int i=1;i<tour.getActivities().size();i++){
				double mc1 = getMarginalInsertionCosts(getActivity(tour,i-1), getActivity(tour,i), pickup);
				for(int j=i;j<tour.getActivities().size();j++){
					double marginalCost;
					double mc2;
					if(i == j){
						mc2 = getMarginalInsertionCosts(pickup, getActivity(tour,i), delivery);
					}
					else{
						mc2 = getMarginalInsertionCosts(getActivity(tour,j-1), getActivity(tour,j), delivery);
					}
					marginalCost = mc1 + mc2;
					if(marginalCost < bestMarginalCost){
						Tour newTour = buildTour(tour,shipment,i,j);
						if(this.constraints.judge(newTour,vehicle)){
							bestMarginalCost = marginalCost;
							bestTour = newTour;
						}
					}
				}
			}
			if(bestTour != null){
				return bestTour;
			}
			return null;
		}

	private double getMarginalInsertionCosts(TourActivity act_i, TourActivity act_j, TourActivity newAct) {
		double tt_acti2newAct = getTravelTime(act_i.getLocationId(), newAct.getLocationId(), 
				act_i.getEarliestArrTime() + act_i.getServiceTime());
		double earliestArrTimeAtNewAct = act_i.getEarliestArrTime() + act_i.getServiceTime() + tt_acti2newAct;
	 
		double marginalCost = getGeneralizedCosts(act_i.getLocationId(), newAct.getLocationId(), 
				act_i.getEarliestArrTime()+act_i.getServiceTime()) +
			getGeneralizedCosts(newAct.getLocationId(), act_j.getLocationId(), earliestArrTimeAtNewAct + newAct.getServiceTime()) -
			getGeneralizedCosts(act_i.getLocationId(), act_j.getLocationId(), act_i.getEarliestArrTime() + act_i.getServiceTime());
		
		return marginalCost;
	}

	private Tour buildTour(Tour tour, Shipment shipment, int insertionIndexForPickup, int insertionIndexForDelivery) {
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		for(int i=0;i<tour.getActivities().size();i++){
			if(i == insertionIndexForPickup){
				tourBuilder.schedulePickup(shipment);
			}
			if(i == insertionIndexForDelivery){
				tourBuilder.scheduleDelivery(shipment);
			}
			tourBuilder.scheduleActivity(tour.getActivities().get(i));
		}
		Tour newTour = tourBuilder.build();
		tourActivityUpdater.process(newTour);
		return newTour;
	}

	private Delivery createDelivery(Shipment shipment) {
		return new Delivery(shipment);
	}

	private Pickup createPickup(Shipment shipment) {
		return new Pickup(shipment);
	}

	private double getTravelTime(String fromId, String toId, double time) {
		return costs.getTransportTime(fromId, toId, time);
	}

	private double getGeneralizedCosts(String fromId, String toId, double time) {
		return costs.getGeneralizedCost(fromId, toId, time);
	}

	private TourActivity getActivity(Tour tour, int i) {
		return tour.getActivities().get(i);
	}

}
