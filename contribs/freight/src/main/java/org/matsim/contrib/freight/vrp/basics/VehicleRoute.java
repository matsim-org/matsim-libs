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
package org.matsim.contrib.freight.vrp.basics;

public class VehicleRoute {
	
	public interface VehicleRouteCostCalculator{

		double calculate(TourImpl tour, Vehicle vehicle, Driver driver);
		
	}
			
	public static VehicleRouteCostCalculator VehicleRouteCostCalculator = new VehicleRouteCostCalculator() {
	
		@Override
		public double calculate(TourImpl tour, Vehicle vehicle, Driver driver) {
			return tour.getTotalCost();
		}
		
	};;; 

	private TourImpl tour;

	private Vehicle vehicle;
	
	private Driver driver;
	
	private double cost;
	
	public double getCost() {
		if(tour.isEmpty()){
			return 0.0;
		}
		return VehicleRouteCostCalculator.calculate(tour,vehicle,driver);
	}

	public VehicleRoute(TourImpl tour, Vehicle vehicle) {
		super();
		this.tour = tour;
		this.vehicle = vehicle;
		this.driver = null;
	}
	
	public VehicleRoute(TourImpl tour, Driver driver, Vehicle vehicle) {
		super();
		this.tour = tour;
		this.vehicle = vehicle;
		this.driver = driver;
	}

	public TourImpl getTour() {
		return tour;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public Driver getDriver() {
		return driver;
	}

	public void setTour(TourImpl tour) {
		this.tour = tour;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}
	
	

}
