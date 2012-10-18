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

	private TourImpl tour;

	private Vehicle vehicle;
	
	private Driver driver;

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

}
