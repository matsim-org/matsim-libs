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

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.contrib.freight.vrp.constraints.Constraints;


public class VrpBuilder {
	
	private Costs costs;
	
	private Constraints constraints;
	
	private Collection<Job> jobs;
	
	private Collection<Vehicle> vehicles;

	private String depotId;

	private double early;

	private double late;
	
	public VrpBuilder(Costs costs, Constraints constraints) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		jobs = new ArrayList<Job>();
		vehicles = new ArrayList<Vehicle>();
	}
	
	public void addJob(Job job){
		jobs.add(job);
	}
	
	public void addVehicle(Vehicle vehicle){
		vehicles.add(vehicle);
	}

	public VehicleRoutingProblem build(){
		verify();
		VRPImpl vrp = new VRPImpl(jobs, vehicles, costs, constraints);
		return vrp;
	}

	private void verify() {
	
	}
}
