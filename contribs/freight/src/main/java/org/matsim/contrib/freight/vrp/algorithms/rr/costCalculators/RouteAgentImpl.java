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
package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.InsertionData;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleFleetManager;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.InsertionData.NoInsertionFound;

/**
 * 
 * @author stefan schroeder
 *
 */

class RouteAgentImpl implements RouteAgent {
	
	private static Logger logger = Logger.getLogger(RouteAgentImpl.class);
	
	private TourImpl tour;
	
	private String id;
	
	private Vehicle selectedVehicle;
	
	private Driver driver;

	private TourStateCalculator tourCalculator;

	private JobInsertionCalculator insertionCostCalculator;
	
	private VehicleFleetManager vehicleFleetManager = VehicleFleetManager.createDefaultFleetManager();

	private boolean tourStatusOutOfSync = true;
	
	public void setVehicleFleetManager(VehicleFleetManager vehicleFleetManager) {
		this.vehicleFleetManager = vehicleFleetManager;
	}

	private Map<String, Job> jobs = new HashMap<String, Job>();
	
	private TourCost tourCost = new TourCost() {
		
		@Override
		public double getTourCost(TourImpl tour, Driver driver, Vehicle vehicle) {
			double cost = 0.0;
			cost+=vehicle.getType().vehicleCostParams.fix;
			cost+=tour.tourData.transportCosts;
			return cost;
		}
	};
	
	RouteAgentImpl(VehicleRoute vehicleRoute, JobInsertionCalculator insertionCostCalculator, TourStateCalculator tourCalculator){
		this.tour = vehicleRoute.getTour();
		this.selectedVehicle = vehicleRoute.getVehicle();
		this.driver = vehicleRoute.getDriver();
		this.tourCalculator = tourCalculator;
		this.insertionCostCalculator = insertionCostCalculator;
	}
	
	RouteAgentImpl(final Vehicle vehicle, Driver driver, TourImpl tour) {
		super();
		this.tour = tour;
		this.selectedVehicle = vehicle;
		this.driver = driver;
		id = vehicle.getId();
		iniJobs();
	}

	public void setJobInsertionCalculator(JobInsertionCalculator jobInsertionCalculator) {
		this.insertionCostCalculator = jobInsertionCalculator;
	}

	public void setTourStateCalculator(TourStateCalculator tourStateCalculator){
		this.tourCalculator = tourStateCalculator;
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

	public double getCost(){
		syncTour();
		return cost(tour);
	}
	
	private double cost(TourImpl tour){
		if(!tour.isEmpty()){
			return tourCost.getTourCost(tour,driver,selectedVehicle);
		}
		else{
			return 0.0;
		}
	}

	public TourImpl getTour() {
		syncTour();
		return tour;
	}

	
	public InsertionData calculateBestInsertion(Job job, double bestKnownPrice) {
		syncTour();
		VehicleRoute currentVehicleRoute = getRoute();
		InsertionData bestIData = insertionCostCalculator.calculate(currentVehicleRoute, job, selectedVehicle, driver, bestKnownPrice);
		Vehicle bestVehicle = selectedVehicle;
		for(String type : vehicleFleetManager.getAvailableVehicleTypes()){
			if(type.equals(selectedVehicle.getType().typeId)){
				continue;
			}
			Vehicle v = vehicleFleetManager.getEmptyVehicle(type);
			InsertionData iData = insertionCostCalculator.calculate(currentVehicleRoute, job, v, driver, bestKnownPrice);
			if(iData.getInsertionCost() < bestIData.getInsertionCost()){
				bestIData = iData;
				bestVehicle = v;
			}
		}
		bestIData.setVehicle(bestVehicle);
		if(bestIData instanceof NoInsertionFound) return bestIData;
		assert bestIData.getInsertionIndeces() != null : "no insertionIndeces set";		
		return bestIData;
	}

	private void syncTour() {
		if(tourStatusOutOfSync){
			boolean tourIsFeasible = tourCalculator.calculate(tour, selectedVehicle, driver);
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
		return selectedVehicle;
	}
	
	public Driver getDriver() {
		return driver;
	}

	
	private void insertJob(Service service, int serviceInsertionIndex) {
		tour.getActivities().add(serviceInsertionIndex, new Delivery(service));
	}

	
	private void insertJob(Shipment shipment, int pickupInsertionIndex, int deliveryInsertionIndex) {
		tour.getActivities().add(deliveryInsertionIndex, new Delivery(shipment));
		tour.getActivities().add(pickupInsertionIndex, new Pickup(shipment));
	}

	@Override
	public void updateTour() {
		syncTour();
	}

	@Override
	public void insertJob(Job job, InsertionData insertionData) {
		jobs.put(job.getId(), job);
		if(job instanceof Shipment){
			assert insertionData.getInsertionIndeces().length == 2 : "a shipment needs two insertionIndeces. a pickupInsertionIndex and a deliveryInsertionIndex";
			insertJob((Shipment)job, insertionData.getInsertionIndeces()[0], insertionData.getInsertionIndeces()[1]);
		}
		else if(job instanceof Service){
			assert insertionData.getInsertionIndeces().length == 1 : "a service needs one insertionIndeces.";
			insertJob((Service)job, insertionData.getInsertionIndeces()[0]);
		}
		else{
			throw new IllegalStateException("a job must either be a shipment or a service");
		}
		tourStatusOutOfSync = true;
		updateTour();
	}

	@Override
	public VehicleRoute getRoute() {
		return new VehicleRoute(tour,driver,selectedVehicle);
	}
}
