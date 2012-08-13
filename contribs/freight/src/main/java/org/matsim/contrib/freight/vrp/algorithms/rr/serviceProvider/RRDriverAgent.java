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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

/**
 * 
 * @author stefan schroeder
 *
 */

class RRDriverAgent implements ServiceProviderAgent, TourAgent {
	
	private static Logger logger = Logger.getLogger(RRDriverAgent.class);
	
	private Tour tour;
	
	private String id;
	
	private final Vehicle vehicle;
	
	private final Driver driver;

	private TourStatusProcessor tourTimeWindowsAndCostUpdater;

	private boolean tourStatusOutOfSync = true;
	
	private Map<String, Job> jobs = new HashMap<String, Job>();
	
	private Map<String,TourData> offerMemory = new HashMap<String, TourData>();

	private LeastCostTourCalculator leastCostTourCalculator;
	
	private TourCost tourCost = new TourCost() {
		
		@Override
		public double getTourCost(Tour tour, Driver driver, Vehicle vehicle) {
			double cost = 0.0;
			cost+=vehicle.getType().vehicleCostParams.fix;
			cost+=tour.tourData.transportCosts;
			return cost;
		}
	};
	
	RRDriverAgent(final Vehicle vehicle, Driver driver, Tour tour) {
		super();
		this.tour = tour;
		this.vehicle = vehicle;
		this.driver = driver;
		id = vehicle.getId();
		iniJobs();
	}

	public void setBestJobInsertionFinder(LeastCostTourCalculator bestJobInsertionFinder) {
		this.leastCostTourCalculator = bestJobInsertionFinder;
	}

	public void setTourTimeWindowsAndCostUpdater(TourStatusProcessor tourTimeWindowAndCostUpdater){
		this.tourTimeWindowsAndCostUpdater = tourTimeWindowAndCostUpdater;
	}

	public void setTourCost(TourCost tourCost) {
		this.tourCost = tourCost;
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
			return tourCost.getTourCost(tour,driver,vehicle);
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
		if(offerMemory.containsKey(job.getId())) offerMemory.remove(job.getId());
		TourData leastCostTourData = leastCostTourCalculator.calculateLeastCostTour(job, vehicle, tour, driver, bestKnownPrice);
		if(leastCostTourData.tourFound()){
			memorize(job,leastCostTourData);
			return new Offer(this,leastCostTourData.penalty);
		}
		else{
			return new Offer(this,Double.MAX_VALUE);
		}
	}

	private void memorize(Job job, TourData insertionData) {
		offerMemory.put(job.getId(), insertionData);
	}

	public void offerGranted(Job job) {
		if(offerMemory.containsKey(job.getId())){
			jobs.put(job.getId(), job);
			insert(job);
			tourStatusOutOfSync = true;
			syncTour();
			offerMemory.clear();
		}
		else {
			throw new IllegalStateException("cannot grant offer where no offer has been given");
		}
	}

	private void insert(Job job) {
		if(job instanceof Shipment){
			Shipment shipment = (Shipment) job;
			TourData id = offerMemory.get(job.getId());
			tour.getActivities().add(id.deliveryInsertionIndex, new Delivery(shipment));
			tour.getActivities().add(id.pickupInsertionIndex, new Pickup(shipment));
		}
		else{
			Service service = (Service) job;
			TourData id = offerMemory.get(job.getId());
			tour.getActivities().add(id.deliveryInsertionIndex, new Delivery(service));
		}
	}


	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#offerRejected(core.algorithms.ruinAndRecreate.RuinAndRecreate.Offer)
	 */
	
	public void offerRejected(Offer offer){
	}

	private void syncTour() {
		if(tourStatusOutOfSync){
			boolean tourIsFeasible = tourTimeWindowsAndCostUpdater.process(tour, vehicle, driver);
			if(!tourIsFeasible){
				throw new IllegalStateException("at this point tour should be feasible. but it is not. " + tour);
			}
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

	@Override
	public Driver getDriver() {
		return driver;
	}
}
