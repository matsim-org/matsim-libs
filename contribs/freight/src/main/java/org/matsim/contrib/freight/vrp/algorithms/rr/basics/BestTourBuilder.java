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

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.api.TourActivityStatusUpdater;
import org.matsim.contrib.freight.vrp.algorithms.rr.api.TourBuilder;
import org.matsim.contrib.freight.vrp.api.Constraints;
import org.matsim.contrib.freight.vrp.api.Costs;
import org.matsim.contrib.freight.vrp.api.Customer;
import org.matsim.contrib.freight.vrp.api.Node;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;


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

public class BestTourBuilder implements TourBuilder {
	
	private static Logger logger = Logger.getLogger(BestTourBuilder.class);
	
	private Costs costs;
	
	private Vehicle vehicle;
	
	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	private Constraints constraints = new Constraints(){

		@Override
		public boolean judge(Tour tour, Vehicle vehicle) {
			return true;
		}
		
	};
	
	private TourActivityStatusUpdater tourActivityUpdater;
	
	public void setTourActivityStatusUpdater(TourActivityStatusUpdater tourActivityUpdater) {
		this.tourActivityUpdater = tourActivityUpdater;
	}
	
	public void setCosts(Costs costs){
		this.costs = costs;
	}
	
	public void setConstraints(Constraints constraints){
		this.constraints = constraints;
	}
	
	/*
	 * Assumption: driver starts from customer as early as possible
	 */
	@Override
	public Tour addShipmentAndGetTour(Tour tour, Shipment shipment, double bestKnownPrice){
		verify();
		Tour newTour = null;
		if(isDepot(tour,shipment.getFrom())){
			newTour = buildTour(tour, shipment.getTo(), bestKnownPrice);
		}
		else if(isDepot(tour,shipment.getTo())){
			newTour = buildTour(tour, shipment.getFrom(), bestKnownPrice);
		}
		else{
			newTour = buildTourWithEnRoutePickupAndDelivery(tour,shipment,bestKnownPrice);
		}
		return newTour;
	}

	private Tour buildTour(Tour tour, Customer customer, double bestKnownPrice){
		double bestMarginalCost = bestKnownPrice;
		Tour bestTour = null;
		for(int i=1;i<tour.getActivities().size();i++){
			TourActivity fromAct = getActivity(tour,i-1);
			TourActivity toAct = getActivity(tour,i);
			double marginalCost = getMarginalInsertionCosts(fromAct, toAct, customer);
			if(marginalCost < bestMarginalCost){
				Tour newTour = VrpUtils.createEmptyCustomerTour();
				for(TourActivity tA : tour.getActivities()){
					newTour.getActivities().add(VrpUtils.createTourActivity(tA.getCustomer()));
				}
				newTour.getActivities().add(i,VrpUtils.createTourActivity(customer));
				assertCustomerIsOnlyOnceInTour(newTour,customer);
				tourActivityUpdater.update(newTour);
				if(this.constraints.judge(newTour,vehicle)){
					bestMarginalCost = marginalCost; 
					bestTour = newTour;
				}
			}
		}
		if(bestTour != null){
			return bestTour;
		}
		return null;
	}

	private double getMarginalInsertionCosts(TourActivity fromAct, TourActivity toAct, Customer customer) {
		double transportTime_fromAct2Customer = getTravelTime(fromAct.getLocation(), customer.getLocation(), fromAct.getEarliestArrTime() + fromAct.getServiceTime());
		double earliestArrTimeFromCustomer = fromAct.getEarliestArrTime() + fromAct.getServiceTime() + transportTime_fromAct2Customer;
	 
		double marginalCost = getGeneralizedCosts(fromAct.getLocation(), customer.getLocation(), fromAct.getEarliestArrTime()+fromAct.getServiceTime()) +
			getGeneralizedCosts(customer.getLocation(), toAct.getLocation(), earliestArrTimeFromCustomer + customer.getServiceTime()) -
			getGeneralizedCosts(fromAct.getLocation(), toAct.getLocation(), fromAct.getEarliestArrTime() + fromAct.getServiceTime());
		
		return marginalCost;
	}
	
	private void assertCustomerIsOnlyOnceInTour(Tour newTour, Customer customer) {
		if(isDepot(newTour, customer)){
			return;
		}
		String customerId = customer.getId();
		int count = 0;
		for(TourActivity tA : newTour.getActivities()){
			if(tA.getCustomer().getId().equals(customerId)){
				count++;
			}
		}
		if(count<1 || count>1){
			logger.error(newTour + " this cannot happen");
			System.exit(1);
		}
		
	}

	private void verify() {
		if(tourActivityUpdater == null){
			throw new IllegalStateException("tourActivityStatusUpdater is not set. this cannot be.");
		}
		if(costs == null){
			throw new IllegalStateException("costsObj is not set. this cannot be");
		}
		
	}

	private Tour buildTourWithEnRoutePickupAndDelivery(Tour tour, Shipment shipment, double bestKnownPrice) {
		Customer fromCustomer = shipment.getFrom();
		Customer toCustomer = shipment.getTo();
		Double bestMarginalCost = bestKnownPrice;
		Tour bestTour = null;
		for(int i=1;i<tour.getActivities().size();i++){
			double marginalCostComp1 = getMarginalInsertionCosts(getActivity(tour,i-1), getActivity(tour,i), shipment.getFrom());
			for(int j=i;j<tour.getActivities().size();j++){
				double marginalCost;
				if(i == j){
					TourActivity fromAct = getActivity(tour,i-1);
					TourActivity toAct = getActivity(tour,i);
					double transportTime_fromAct2fromCustomer = getTravelTime(fromAct.getLocation(),fromCustomer.getLocation(),fromAct.getEarliestArrTime() + fromAct.getServiceTime()); 
					double earliestArrTimeAtFromCustomer = fromAct.getEarliestArrTime() + fromAct.getServiceTime() + transportTime_fromAct2fromCustomer;
					double transportTime_fromCustomer2toCustomer = getTravelTime(fromCustomer.getLocation(),toCustomer.getLocation(),earliestArrTimeAtFromCustomer + fromCustomer.getServiceTime()); 
					double earliestArrTimeAtToCustomer = earliestArrTimeAtFromCustomer + fromCustomer.getServiceTime() + transportTime_fromCustomer2toCustomer;
					
					marginalCost = getGeneralizedCosts(fromAct.getLocation(), fromCustomer.getLocation(), fromAct.getEarliestArrTime() + fromAct.getServiceTime()) +
						getGeneralizedCosts(fromCustomer.getLocation(), toCustomer.getLocation(), earliestArrTimeAtFromCustomer + fromCustomer.getServiceTime()) +
						getGeneralizedCosts(toCustomer.getLocation(), toAct.getLocation(), earliestArrTimeAtToCustomer + toCustomer.getServiceTime()) -
						getGeneralizedCosts(fromAct.getLocation(), toAct.getLocation(), fromAct.getEarliestArrTime() + fromAct.getServiceTime());
					 
				}
				else{
					double marginalCostComp2 = getMarginalInsertionCosts(getActivity(tour,j-1), getActivity(tour,j), toCustomer);
					marginalCost = marginalCostComp1 + marginalCostComp2;
				}
				if(marginalCost < bestMarginalCost){
					Tour newTour = buildTour(tour,shipment,i,j);
					tourActivityUpdater.update(newTour);
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

	private boolean isDepot(Tour tour, Customer customer) {
		if(customer.getId().equals(getDepot(tour).getId())){
			return true;
		}
		return false;
	}
	
	private Customer getDepot(Tour tour){
		return tour.getActivities().get(0).getCustomer();
	}

	private Tour buildTour(Tour tour, Shipment shipment, int i, int j) {
		Tour newTour = VrpUtils.createEmptyCustomerTour();
		for(TourActivity tA : tour.getActivities()){
			newTour.getActivities().add(VrpUtils.createTourActivity(tA.getCustomer()));
		}
		newTour.getActivities().add(i,VrpUtils.createTourActivity(shipment.getFrom()));
		newTour.getActivities().add(j+1,VrpUtils.createTourActivity(shipment.getTo()));
		return newTour;
	}

	private double getTravelTime(Node from, Node to, double time) {
		return costs.getTransportTime(from, to, time);
	}

	private double getGeneralizedCosts(Node from, Node to, double time) {
		return costs.getGeneralizedCost(from, to, time);
	}

	private TourActivity getActivity(Tour tour, int i) {
		return tour.getActivities().get(i);
	}


}
