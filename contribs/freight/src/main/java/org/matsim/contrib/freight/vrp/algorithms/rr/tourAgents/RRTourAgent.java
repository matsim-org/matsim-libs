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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Constraints;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;



/**
 * 
 * @author stefan schroeder
 *
 */

class RRTourAgent implements TourAgent {
	
	private static Logger logger = Logger.getLogger(RRTourAgent.class);
	
	private Tour tour;
	
	private Vehicle vehicle;

	private Constraints constraints;
	
	private Offer openOffer = null;
	
	private Tour tourOfLastOffer = null;
	
	private TourActivityStatusUpdater activityStatusUpdater;

	private Costs costs;
	
//	private TourBuilder tourBuilder;
	
	RRTourAgent(Costs costs, Tour tour, Vehicle vehicle, TourActivityStatusUpdater updater, Constraints constraints) {
		super();
		this.tour = tour;
		this.vehicle = vehicle;
		this.activityStatusUpdater = updater;
		updater.update(tour);
		this.costs = costs;
		this.constraints = constraints;
	}

//	public void setTourBuilder(TourBuilder tourBuilder) {
//		this.tourBuilder = tourBuilder;
//	}


	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getConstraint()
	 */
//	Constraints getConstraint() {
//		return constraint;
//	}
//
//	/* (non-Javadoc)
//	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#setConstraint(api.basic.Constraints)
//	 */
//	@Override
//	public void setConstraint(Constraints constraint) {
//		this.constraint = constraint;
//	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#offerRejected(core.algorithms.ruinAndRecreate.RuinAndRecreate.Offer)
	 */
	@Override
	public void offerRejected(Offer offer){
		openOffer = null;
		tourOfLastOffer = null;
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getTotalCost()
	 */
	@Override
	public double getTourCost(){
		return tour.getCosts().generalizedCosts;
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
	public Offer requestService(Job job, double bestKnownPrice) {
		Tour newTour = new BestTourBuilder(tour, costs, vehicle, constraints, activityStatusUpdater).buildTour(job, bestKnownPrice);
		if(newTour != null){
			double marginalCosts = newTour.costs.generalizedCosts - tour.costs.generalizedCosts;
			Offer offer = new Offer(this, marginalCosts);
			openOffer = offer;
			tourOfLastOffer = newTour;
			return offer;
		}
		else{
			return null;
		}
	}

	@Override
	public void offerGranted(Job job) {
		if(tourOfLastOffer != null){
			tour = tourOfLastOffer;
			logger.debug("granted offer: " + openOffer);
			logger.debug("");
			openOffer = null;
			tourOfLastOffer = null;
		}
		else {
			throw new IllegalStateException("cannot grant offer where no offer has been given");
		}
	}

	@Override
	public boolean hasJob(Job job) {
		for(TourActivity c : tour.getActivities()){
			if(c instanceof JobActivity){
				if(job.getId().equals(((JobActivity) c).getJob().getId())){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void removeJob(Job job) {
		List<TourActivity> acts = new ArrayList<TourActivity>(tour.getActivities());
		for(TourActivity c : acts){
			if(c instanceof JobActivity){
				if(job.getId().equals(((JobActivity) c).getJob().getId())){
					tour.getActivities().remove(c);
					activityStatusUpdater.update(tour);
				}
			}
		}
	}

}
