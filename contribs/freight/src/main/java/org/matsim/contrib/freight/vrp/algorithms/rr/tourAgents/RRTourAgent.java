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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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

	private Offer openOffer = null;
	
	private Tour tourOfLastOffer = null;
	
	private TourStatusProcessor activityStatusUpdater;

	private boolean tourStatusOutOfSync = true;
	
	private Map<String, Job> jobs = new HashMap<String, Job>();
	
	private TourFactory tourFactory;
	
	RRTourAgent(Vehicle vehicle, Tour tour, TourStatusProcessor tourStatusProcessor, TourFactory tourBuilder) {
		super();
		this.tour = tour;
		this.tourFactory=tourBuilder;
		this.activityStatusUpdater=tourStatusProcessor;
		this.vehicle = vehicle;
		iniJobs();
		syncTour();
	}

	public void setTourBuilder(TourFactory tourBuilder) {
		this.tourFactory = tourBuilder;
	}

	private void iniJobs() {
		for(TourActivity c : tour.getActivities()){
			if(c instanceof JobActivity){
				jobs.put(((JobActivity) c).getJob().getId(), ((JobActivity) c).getJob());
			}
		}
	}


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
		syncTour();
		return tour.getCosts().generalizedCosts;
	}
	
	@Override
	public Tour getTour() {
		syncTour();
		return tour;
	}

	
	@Override
	public String toString() {
		return tour.toString();
	}

	@Override
	public Offer requestService(Job job, double bestKnownPrice) {
		syncTour();
		Tour newTour = tourFactory.createTour(vehicle, tour, job, bestKnownPrice);
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

	private void syncTour() {
		if(tourStatusOutOfSync){
			tourStatusOutOfSync = false;
			activityStatusUpdater.process(tour);
		}
	}


	@Override
	public void offerGranted(Job job) {
		jobs.put(job.getId(), job);
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
		if(jobs.containsKey(job.getId())){
			return true;
		}
		return false;
	}

	@Override
	public void removeJob(Job job) {
		if(jobs.containsKey(job.getId())){
			List<TourActivity> acts = new ArrayList<TourActivity>(tour.getActivities());
			for(TourActivity c : acts){
				if(c instanceof JobActivity){
					if(job.getId().equals(((JobActivity) c).getJob().getId())){
						tour.getActivities().remove(c);
					}
				}
			}
			tourStatusOutOfSync = true;
			jobs.remove(job.getId());
		}
	}
}
