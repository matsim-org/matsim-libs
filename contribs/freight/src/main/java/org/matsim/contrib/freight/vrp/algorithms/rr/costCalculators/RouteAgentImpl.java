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

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.InsertionData;
import org.matsim.contrib.freight.vrp.basics.InsertionData.NoInsertionFound;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleFleetManager;
import org.matsim.contrib.freight.vrp.basics.VehicleFleetManagerImpl;
import org.matsim.contrib.freight.vrp.basics.VehicleFleetManagerImpl.DefaultFleetManager;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl.NoVehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

/**
 * 
 * @author stefan schroeder
 *
 */

class RouteAgentImpl implements RouteAgent {
	
	private static Logger logger = Logger.getLogger(RouteAgentImpl.class);
	
	private VehicleRoute vehicleRoute;

	private TourStateCalculator tourCalculator;

	private JobInsertionCalculator insertionCostCalculator;
	
	private VehicleFleetManager vehicleFleetManager = VehicleFleetManagerImpl.createDefaultFleetManager();
	
	private TourImpl copiedTour;
	
	private Vehicle selectedVehicle;
	
	private Driver selectedDriver;
	
	RouteAgentImpl(VehicleRoute vehicleRoute, JobInsertionCalculator insertionCostCalculator, TourStateCalculator tourCalculator){
		if(vehicleRoute.getVehicle() == null){
			vehicleRoute.setVehicle(new NoVehicle());
		}
//		this.copiedTour = vehicleRoute.getTour().duplicate();
		this.selectedVehicle = vehicleRoute.getVehicle();
		this.selectedDriver = vehicleRoute.getDriver();
		
		this.tourCalculator = tourCalculator;
		this.insertionCostCalculator = insertionCostCalculator;
		this.vehicleRoute = vehicleRoute;
	}
	
	public VehicleRoute getVehicleRoute(){
		return new VehicleRoute(copiedTour, selectedDriver, selectedVehicle);
	}

	public void setVehicleFleetManager(VehicleFleetManager vehicleFleetManager) {
		this.vehicleFleetManager = vehicleFleetManager;
	}

	public InsertionData calculateBestInsertion(Job job, double bestKnownPrice) {
		Vehicle selectedVehicle = vehicleRoute.getVehicle();
		Driver selectedDriver = vehicleRoute.getDriver();
		InsertionData bestIData = new NoInsertionFound();
		double bestKnownCost = bestKnownPrice;
		Vehicle bestVehicle = vehicleRoute.getVehicle();
		if(!(selectedVehicle instanceof NoVehicle)){
			bestIData = insertionCostCalculator.calculate(vehicleRoute, job, selectedVehicle, selectedDriver, bestKnownCost);
			if(!(bestIData instanceof NoInsertionFound)){
				bestVehicle = vehicleRoute.getVehicle();
				if(bestIData.getInsertionCost() < bestKnownCost){
					bestKnownCost = bestIData.getInsertionCost();
				}
			}
		}
		for(String type : vehicleFleetManager.getAvailableVehicleTypes()){
			if(!(vehicleRoute.getVehicle() instanceof NoVehicle)){
				if(type.equals(vehicleRoute.getVehicle().getType().typeId)){
					continue;
				}
			}
			Vehicle v = vehicleFleetManager.getEmptyVehicle(type);
			InsertionData iData = insertionCostCalculator.calculate(vehicleRoute, job, v, selectedDriver, bestKnownCost);
			if(iData.getInsertionCost() < bestKnownCost){
				if(vehicleRoute.getTour().getLoad() + job.getCapacityDemand() > v.getCapacity()) throw new IllegalStateException("this must not be");
				bestIData = iData;
				bestVehicle = v;
				bestKnownCost = iData.getInsertionCost();
			}
		}
		bestIData.setVehicle(bestVehicle);	
		return bestIData;
	}

	/*
	 * (non-Javadoc)
	 * @see org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent#removeJob(org.matsim.contrib.freight.vrp.basics.Job)
	 * !! hier kann es auch zu inkonsistenzen kommen. wenn ein job entfernt wird, dann ist der tourStatus nicht mehr aktuell.
	 * 
	 * sowieso: momentan hängen die kosten an der tour. die kosten sind allerdings auch abhängig vom fahrer u. dem fzg. ich würde die kosten
	 * deshalb eher an der vehicleRoute erwarten.
	 * hier muss noch nachgearbeitet werden.
	 */
	public boolean removeJobWithoutTourUpdate(Job job) {
		boolean removed = vehicleRoute.getTour().removeJob(job);
		if(vehicleRoute.getTour().isEmpty() && !(vehicleFleetManager instanceof DefaultFleetManager)){
			vehicleFleetManager.unlock(vehicleRoute.getVehicle());
			vehicleRoute.setVehicle(VehicleImpl.createNoVehicle());
		}
		return removed;
	}
	
	public boolean removeJob(Job job){
		boolean removed = removeJobWithoutTourUpdate(job);
		if(removed) updateTour();
		return removed;
	}
	
	public void updateTour(){
		if(vehicleRoute.getVehicle() instanceof NoVehicle){
			vehicleRoute.getTour().reset();
			if(!vehicleRoute.getTour().isEmpty()){
				throw new IllegalStateException("tour is not empty, but has no vehicle. this should not be");
			}
			return;
		}
		boolean tourIsFeasible = tourCalculator.calculate(vehicleRoute.getTour(), vehicleRoute.getVehicle(), vehicleRoute.getDriver());
		if(!tourIsFeasible){
			throw new IllegalStateException("at this point tour should be feasible. but it is not. " + vehicleRoute.getTour());
		}
	}

	@Override
	public void insertJobWithoutTourUpdate(Job job, InsertionData insertionData) {
		if(insertionData == null || (insertionData instanceof NoInsertionFound)) throw new IllegalStateException("insertionData null. cannot insert job.");
		if(job == null) throw new IllegalStateException("cannot insert null-job");
		if(job instanceof Shipment){
			assert insertionData.getInsertionIndeces().length == 2 : "a shipment needs two insertionIndeces. a pickupInsertionIndex and a deliveryInsertionIndex";
			try{
				insertJob((Shipment)job, insertionData.getInsertionIndeces()[0], insertionData.getInsertionIndeces()[1]);
			}
			catch(IndexOutOfBoundsException e){
				throw new IllegalStateException("insertionData are invalid for this tour. " + e);
			}
		}
		else if(job instanceof Service){
			assert insertionData.getInsertionIndeces().length == 1 : "a service needs one insertionIndeces.";
			try{
				insertJob((Service)job, insertionData.getInsertionIndeces()[0]);
			}
			catch(IndexOutOfBoundsException e){
				throw new IllegalStateException("insertionData are invalid for this tour. " + e);
			}
			
		}
		else{
			throw new IllegalStateException("a job must either be a shipment or a service");
		}
		if(insertionData.getSelectedVehicle() != vehicleRoute.getVehicle() && !(vehicleFleetManager instanceof DefaultFleetManager)){
			try{
				vehicleFleetManager.unlock(vehicleRoute.getVehicle());
			}
			catch(IllegalStateException e){
				throw new IllegalStateException();
			}
			vehicleFleetManager.lock(insertionData.getSelectedVehicle());
			vehicleRoute.setVehicle(insertionData.getSelectedVehicle());
		}
	}
	
	public void insertJob(Job job, InsertionData insertionData){
		insertJobWithoutTourUpdate(job, insertionData);
		updateTour();
	}

	@Override
	public String toString() {
		return vehicleRoute.getTour().toString();
	}

	private void insertJob(Service service, int serviceInsertionIndex) {
		vehicleRoute.getTour().addActivity(serviceInsertionIndex, new Delivery(service));
	}

	private void insertJob(Shipment shipment, int pickupInsertionIndex, int deliveryInsertionIndex) {
		vehicleRoute.getTour().addActivity(deliveryInsertionIndex,new Delivery(shipment));
		vehicleRoute.getTour().addActivity(pickupInsertionIndex,new Pickup(shipment));
	}

}
