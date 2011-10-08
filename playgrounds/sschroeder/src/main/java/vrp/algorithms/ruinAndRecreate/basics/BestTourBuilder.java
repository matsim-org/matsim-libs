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
package vrp.algorithms.ruinAndRecreate.basics;

import org.apache.log4j.Logger;

import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.algorithms.ruinAndRecreate.api.TourBuilder;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.Vehicle;
import vrp.basics.VrpUtils;

/**
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
	
	private Tour buildTour(Tour tour, Customer customer, double bestKnownPrice){
		double bestMarginalCost = bestKnownPrice;
		Tour bestTour = null;
		for(int i=1;i<tour.getActivities().size();i++){	
				double marginalCost = getCosts(getActLocation(tour, i-1), customer.getLocation()) + 
					getCosts(customer.getLocation(), getActLocation(tour, i)) - getCosts(getActLocation(tour, i-1), getActLocation(tour, i));
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
	
	private void verify() {
		if(tourActivityUpdater == null){
			throw new IllegalStateException("tourActivityStatusUpdater is not set. this cannot be.");
		}
		if(costs == null){
			throw new IllegalStateException("costsObj is not set. this cannot be");
		}
		
	}

	private Tour buildTourWithEnRoutePickupAndDelivery(Tour tour, Shipment shipment, double bestKnownPrice) {
		Node fromLocation = shipment.getFrom().getLocation();
		Node toLocation = shipment.getTo().getLocation();
		Double bestMarginalCost = bestKnownPrice;
		Tour bestTour = null;
		for(int i=1;i<tour.getActivities().size();i++){
			double marginalCostComp1 = getCosts(getActLocation(tour, i-1),fromLocation) + getCosts(fromLocation,getActLocation(tour, i)) - getCosts(getActLocation(tour, i-1),getActLocation(tour, i));
			for(int j=i;j<tour.getActivities().size();j++){
				double marginalCost;
				if(i == j){
					marginalCost = getCosts(getActLocation(tour, i-1),fromLocation) + getCosts(fromLocation,toLocation) + getCosts(toLocation,getActLocation(tour, i)) -
						getCosts(getActLocation(tour, i-1),getActLocation(tour, i));
				}
				else{
					double marginalCostComp2 = getCosts(getActLocation(tour, j-1),toLocation) + getCosts(toLocation,getActLocation(tour, j)) - getCosts(getActLocation(tour, j-1),getActLocation(tour, j));
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

	private double getCosts(Node from, Node to) {
		return costs.getCost(from, to);
	}

	private Node getActLocation(Tour tour, int i) {
		return tour.getActivities().get(i).getLocation();
	}


}
