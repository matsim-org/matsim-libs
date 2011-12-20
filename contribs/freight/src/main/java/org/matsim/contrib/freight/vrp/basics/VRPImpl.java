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
package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class VRPImpl implements VehicleRoutingProblem{

	private static Logger logger = Logger.getLogger(VRPImpl.class);
	
	private Costs costs;
	
	private Constraints constraints;
	
	private Map<String,Job> jobs;
	
	private Collection<Vehicle> vehicles;

	public VRPImpl(Collection<? extends Job> jobs, Collection<Vehicle> vehicles, Costs costs, Constraints constraints) {
		this.jobs = new HashMap<String, Job>();
		mapJobs(jobs);
		this.vehicles = vehicles;
		this.costs = costs;
		this.constraints = constraints;
		
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
	public Constraints getConstraints() {
		return constraints;
	}

	@Override
	public Costs getCosts() {
		return costs;
	}

}
