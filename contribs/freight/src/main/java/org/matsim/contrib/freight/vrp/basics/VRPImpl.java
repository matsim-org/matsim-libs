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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.constraints.Constraints;

public class VRPImpl implements VehicleRoutingProblem{

	private static Logger logger = Logger.getLogger(VRPImpl.class);
	
	private Costs costs;
	
	private Constraints globalConstraints;
	
	private Map<String,Job> jobs;
	
	private Collection<Vehicle> vehicles;

	private Locations locations;

	public VRPImpl(Collection<? extends Job> jobs, Collection<Vehicle> vehicles, Costs costs, Constraints globalConstraints) {
		this.jobs = new HashMap<String, Job>();
		mapJobs(jobs);
		this.vehicles = vehicles;
		this.costs = costs;
		this.globalConstraints = globalConstraints;
		
	}

	private void mapJobs(Collection<? extends Job> jobs) {
		for(Job j : jobs){
			this.jobs.put(j.getId(), j);
		}
	}

	public Map<String, Job> getJobs() {
		return jobs;
	}

	public Collection<Vehicle> getVehicles() {
		return vehicles;
	}

	@Override
	public Constraints getGlobalConstraints() {
		return globalConstraints;
	}

	@Override
	public Costs getCosts() {
		return costs;
	}

	@Override
	public Locations getLocations() {
		return this.locations;
	}

	public void setLocations(Locations locations) {
		this.locations = locations;
	}

}
