package org.matsim.contrib.freight.vrp.basics;

import java.util.ArrayList;
import java.util.Collection;


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
