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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.OfferMaker.OfferData;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.DriverCostParams;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

/**
 * 
 * @author stefan schroeder
 *
 */

public class RRDriverAgent implements ServiceProviderAgent {
	

	private static Logger logger = Logger.getLogger(RRDriverAgent.class);
	
	private Tour tour;
	
	private String id;
	
	private Vehicle vehicle;

	private TourStatusProcessor tourActivityStatusUpdater;

	private boolean tourStatusOutOfSync = true;
	
	private Map<String, Job> jobs = new HashMap<String, Job>();
	
//	private DriverCostFunction driverCostFunction;
	
	private DriverCostParams driverCostParams;

	private OfferMaker offerMaker;

	private OfferData lastOffer;
	
	public void setOfferMaker(OfferMaker offerMaker) {
		this.offerMaker = offerMaker;
	}


	public RRDriverAgent(Vehicle vehicle, Tour tour, TourStatusProcessor tourStatusProcessor, DriverCostParams driverCostParams) {
		super();
		this.tour = tour;
		this.tourActivityStatusUpdater=tourStatusProcessor;
		this.driverCostParams = driverCostParams;
		this.vehicle = vehicle;
		id = vehicle.getId();
		iniJobs();
		syncTour();
	}

	public String getId() {
		return id;
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

	public double getTourCost(){
		syncTour();
		return cost(tour);
	}
	
	private double cost(Tour tour){
		if(isActive(tour)){
			double cost = 0.0;
			cost+=driverCostParams.fixCost_per_vehicleService;
			cost+=tour.tourData.transportCosts;
			return cost;
		}
		else{
			return 0.0;
		}
	}

	public Tour getTour() {
		syncTour();
		return tour;
	}

	
	public Offer requestService(Job job, double bestKnownPrice) {
		syncTour();
		OfferData offerData = offerMaker.makeOffer(vehicle,tour,job,bestKnownPrice);
		memorize(offerData);
		return new Offer(this,offerData.offer.price,offerData.offer.price);
	}

	private void memorize(OfferData offerData) {
		lastOffer = offerData;
	}

	public void offerGranted(Job job) {
		if(lastOffer.metaData != null){
			jobs.put(job.getId(), job);
			insertJob(job);
			tourActivityStatusUpdater.process(tour);
			lastOffer = null;
		}
		else {
			throw new IllegalStateException("cannot grant offer where no offer has been given");
		}
	}

	private void insertJob(Job job) {
		Shipment shipment = (Shipment)job;
		tour.getActivities().add(lastOffer.metaData.deliveryInsertionIndex, new Delivery(shipment));
		tour.getActivities().add(lastOffer.metaData.pickupInsertionIndex, new Pickup(shipment));
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#offerRejected(core.algorithms.ruinAndRecreate.RuinAndRecreate.Offer)
	 */
	
	public void offerRejected(Offer offer){
		lastOffer = null;
	}

	private void syncTour() {
		if(tourStatusOutOfSync){
			tourActivityStatusUpdater.process(tour);
			tourStatusOutOfSync = false;
		}
	}

	public boolean removeJob(Job job) {
		if(jobs.containsKey(job.getId())){
			int counter=0;
			List<TourActivity> acts = new ArrayList<TourActivity>(tour.getActivities());
			for(TourActivity c : acts){
				if(c instanceof JobActivity){
					if(job.getId().equals(((JobActivity) c).getJob().getId())){
						tour.getActivities().remove(c);
						counter++;
						if(counter == 2){
							break;
						}
					}
				}
			}
			tourStatusOutOfSync = true;
			jobs.remove(job.getId());
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return tour.toString();
	}

	public Vehicle getVehicle() {
		return vehicle;
	}
	
	private boolean isActive(Tour tour){
		return !tour.isEmpty();
	}
	
	public boolean isActive(){
		return isActive(tour);
	}
}
