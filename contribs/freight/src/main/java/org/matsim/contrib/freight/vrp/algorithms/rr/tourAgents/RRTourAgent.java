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
import org.matsim.contrib.freight.vrp.basics.CostParams;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
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

	private Tour tourOfLastOffer = null;
	
	private TourStatusProcessor activityStatusUpdater;

	private boolean tourStatusOutOfSync = true;
	
	private Map<String, Job> jobs = new HashMap<String, Job>();
	
	private TourFactory tourFactory;
	
	private boolean active = false;
	
	private boolean keepScaling = false;
	
	public boolean scale = false;
	
	public boolean noFixedCosts = false;
	
	public double marginalCostScalingFactorForNewService = 1.0;
	
	public double fixCostsForService = 0.0;
	
	public CostParams costParams;
	
	public RRTourAgent(Vehicle vehicle, Tour tour, TourStatusProcessor tourStatusProcessor, TourFactory tourBuilder, CostParams costParams) {
		super();
		this.tour = tour;
		this.tourFactory=tourBuilder;
		this.activityStatusUpdater=tourStatusProcessor;
		this.vehicle = vehicle;
		id = vehicle.getId();
		this.costParams = costParams;
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

	public void offerRejected(Offer offer){
		tourOfLastOffer = null;
	}

	/* (non-Javadoc)
	 * @see core.algorithms.ruinAndRecreate.VehicleAgent#getTotalCost()
	 */

	public double getTourCost(){
		syncTour();
		return cost(tour);
	}
	
	private double cost(Tour tour){
		if(isActive(tour)){
			double fix = costParams.getCostPerVehicle();
			double waiting = tour.getCosts().waitingTime*costParams.getCostPerSecondWaiting();
			double service = tour.getCosts().serviceTime*costParams.getCostPerSecondService();
			double transportCosts = tour.getCosts().transportCosts;
			return fix + waiting + service + transportCosts;
		}
		else{
			return 0.0;
		}
	}

	public Tour getTour() {
		syncTour();
		return tour;
	}

	
	@Override
	public String toString() {
		return tour.toString();
	}


	public Offer requestService(Job job, double bestKnownPrice) {
		syncTour();
		Tour newTour = tourFactory.createTour(vehicle, tour, job, bestKnownPrice);
		if(newTour != null){
			double marginalCosts = cost(newTour) - cost(tour);
			double price;
//			if(noFixedCosts){
				price = marginalCosts;
//			}
//			else{
//				price = marginalCosts + shareOfFixedCosts(job,newTour);
//			}
			 
//			if(scale || !active || keepScaling){
//				price = scale(marginalCosts);
//				keepScaling = true;
//			}
			Offer offer = new Offer(this, price, marginalCosts);
			tourOfLastOffer = newTour;
			return offer;
		}
		else{
			return null;
		}
	}

	private double shareOfFixedCosts(Job job, Tour newTour) {
		return (((Shipment)job).getSize()/newTour.costs.totalLoad)*costParams.getCostPerVehicle();
	}

	private double scale(double marginalCosts) {
		return marginalCosts*marginalCostScalingFactorForNewService;
	}

	private void syncTour() {
		if(tourStatusOutOfSync){
			tourStatusOutOfSync = false;
			activityStatusUpdater.process(tour);
			if(tour.getActivities().size()<=2){
				active=false;
			}
			else{
				active=true;
			}
		}
	}


	public void offerGranted(Job job) {
		jobs.put(job.getId(), job);
		if(tourOfLastOffer != null){
			tour = tourOfLastOffer;
			tourOfLastOffer = null;
		}
		else {
			throw new IllegalStateException("cannot grant offer where no offer has been given");
		}
	}

	public boolean hasJob(Job job) {
		if(jobs.containsKey(job.getId())){
			return true;
		}
		return false;
	}

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

	public Vehicle getVehicle() {
		return vehicle;
	}
	
	private boolean isActive(Tour tour){
//		syncTour();
		return tour.getActivities().size()>2;
	}
	
	public boolean isActive(){
		return isActive(tour);
	}
}
