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
package org.matsim.contrib.freight.vrp.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;

public class VrpBuilder {

	private VehicleRoutingCosts costs;

	private Collection<Job> jobs;

	private Collection<Vehicle> vehicles;

	public VrpBuilder(VehicleRoutingCosts costs) {
		super();
		this.costs = costs;
		jobs = new ArrayList<Job>();
		vehicles = new ArrayList<Vehicle>();
	}

	public void addJob(Job job) {
		jobs.add(job);
	}

	public void addVehicle(Vehicle vehicle) {
		vehicles.add(vehicle);
	}

	public VehicleRoutingProblem build() {
		verify();
		VehicleRoutingProblem vrp = new VehicleRoutingProblem(jobs,vehicles, costs);
		return vrp;
	}

	private void verify() {

	}
}
