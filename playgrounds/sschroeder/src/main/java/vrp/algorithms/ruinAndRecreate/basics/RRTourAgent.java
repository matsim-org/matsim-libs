/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
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

import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer;
import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourBuilder;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.Vehicle;


/**
 * 
 * @author stefan schroeder
 *
 */

class RRTourAgent implements TourAgent {
	
	private static Logger logger = Logger.getLogger(RRTourAgent.class);
	
	private Tour tour;
	
	private Vehicle vehicle;

	private Double currentCost = null;
	
	private Constraints constraint;
	
	private Offer openOffer = null;
	
	private Double costOfOfferedTour = null; 
	
	private Tour tourOfLastOffer = null;
	
	private TourActivityStatusUpdater activityStatusUpdater;
	
	private TourBuilder tourBuilder;
	
	RRTourAgent(Costs costs, Tour tour, Vehicle vehicle, TourActivityStatusUpdater updater) {
		super();
		this.tour = tour;
		this.vehicle = vehicle;
		this.activityStatusUpdater = updater;
		updater.update(tour);
		currentCost = tour.costs.generalizedCosts;
	}

	public void setTourBuilder(TourBuilder tourBuilder) {
		this.tourBuilder = tourBuilder;
	}


	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getConstraint()
	 */
	Constraints getConstraint() {
		return constraint;
	}
	
	@Override
	public boolean tourIsValid(){
		activityStatusUpdater.update(tour);
		currentCost = tour.costs.generalizedCosts;
		if(constraint.judge(tour,vehicle)){
			return true;
		}
		else{
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#setConstraint(api.basic.Constraints)
	 */
	@Override
	public void setConstraint(Constraints constraint) {
		this.constraint = constraint;
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#requestService(core.basic.Node)
	 */
	
	@Override
	public Offer requestService(Shipment shipment, double bestKnownPrice){
		Tour newTour = tourBuilder.addShipmentAndGetTour(tour, shipment, bestKnownPrice);
		if(newTour != null){
			double marginalCosts = newTour.costs.generalizedCosts - tour.costs.generalizedCosts;
			Offer offer = new Offer(this, marginalCosts);
			openOffer = offer;
			tourOfLastOffer = newTour;
			costOfOfferedTour = newTour.costs.generalizedCosts;
			return offer;
		}
		else{
			return null;
		}
	}
	

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getTourSize()
	 */
	@Override
	public int getTourSize(){
		return tour.getActivities().size();
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#offerGranted(core.basic.Node)
	 */
	@Override
	public void offerGranted(Shipment shipment){
		if(tourOfLastOffer != null){
			tour = tourOfLastOffer;
			currentCost = costOfOfferedTour;
			logger.debug("granted offer: " + openOffer);
			logger.debug("");
			openOffer = null;
			tourOfLastOffer = null;
			costOfOfferedTour = null;
		}
		else {
			throw new IllegalStateException("cannot grant offer where no offer has been given");
		}
	}
	
	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#offerRejected(core.algorithms.ruinAndRecreate.RuinAndRecreate.Offer)
	 */
	@Override
	public void offerRejected(Offer offer){
		openOffer = null;
		tourOfLastOffer = null;
		costOfOfferedTour = null;
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getTotalCost()
	 */
	@Override
	public double getTotalCost(){
		return currentCost;
	}
	
	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#removeNode(core.basic.Node)
	 */
	@Override
	public void removeCustomer(Customer customer){
//		logger.debug("remove node: " + node);
		for(TourActivity c : tour.getActivities()){
			if(c.getCustomer().getId().equals(customer.getId())){
				tour.getActivities().remove(c);
				activityStatusUpdater.update(tour);
				currentCost = tour.costs.generalizedCosts;
				break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#hasNode(core.basic.Node)
	 */
	@Override
	public boolean hasCustomer(Customer customer) {
		for(TourActivity c : tour.getActivities()){
			if(c.getCustomer().getId().equals(customer.getId())){
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getVehicleCapacity()
	 */
	@Override
	public int getVehicleCapacity() {
		return vehicle.getCapacity();
	}
	
	@Override
	public Collection<TourActivity> getTourActivities(){
		return Collections.unmodifiableCollection(tour.getActivities());
	}
	
	@Override
	public String toString() {
		return tour.toString();
	}

	@Override
	public Tour getTour() {
		return tour;
	}

	@Override
	public Offer getOpenOffer() {
		return openOffer;
	}

}
