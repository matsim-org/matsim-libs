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
import org.matsim.contrib.freight.vrp.basics.CarrierCostFunction;
import org.matsim.contrib.freight.vrp.basics.Delivery;
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

public class RRTourAgent {
	

	public static class Offer {
		
		private RRTourAgent agent;
		
		private double price;
		
		private double marginalCosts;

		public Offer(RRTourAgent agent, double price, double mc) {
			super();
			this.agent = agent;
			this.price = price;
			this.marginalCosts = mc;
		}

		public double getMarginalCosts() {
			return marginalCosts;
		}

		public RRTourAgent getTourAgent() {
			return agent;
		}

		public double getPrice() {
			return price;
		}
		
		@Override
		public String toString() {
			return "currentTour=" + agent + "; marginalInsertionCosts=" + price;
		}
		
	}
	
	private static Logger logger = Logger.getLogger(RRTourAgent.class);
	
	private Tour tour;
	
	private String id;
	
	private Vehicle vehicle;

	private TourStatusProcessor tourActivityStatusUpdater;

	private boolean tourStatusOutOfSync = true;
	
	private Map<String, Job> jobs = new HashMap<String, Job>();
	
	private CarrierCostFunction costFunction;

	private OfferMaker offerMaker;

	private OfferData lastOffer;
	
	public void setOfferMaker(OfferMaker offerMaker) {
		this.offerMaker = offerMaker;
	}


	public RRTourAgent(Vehicle vehicle, Tour tour, TourStatusProcessor tourStatusProcessor, CarrierCostFunction carrierCostFunction) {
		super();
		this.tour = tour;
		this.tourActivityStatusUpdater=tourStatusProcessor;
		this.costFunction = carrierCostFunction;
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
			cost+=costFunction.costParams.cost_per_vehicle;
			cost+=tour.getTourStats().transportCosts;
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
