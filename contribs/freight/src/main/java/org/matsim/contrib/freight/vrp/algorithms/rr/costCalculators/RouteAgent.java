package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.InsertionData;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

public interface RouteAgent {
	
	public interface RouteAgentListener {
		
	}
	
	public interface JobRemovedListener extends RouteAgentListener{
		public void removed(VehicleRoute route, Job job);
	}
	
	public interface JobInsertedListener extends RouteAgentListener{
		public void inserted(VehicleRoute route, Job job);
	}
	
	public interface VehicleSwitchedListener extends RouteAgentListener{
		public void vehicleSwitched(Vehicle oldVehicle, Vehicle newVehicle);
	}
	
	public InsertionData calculateBestInsertion(Job job, double bestKnownPrice);
		
	public boolean removeJobWithoutTourUpdate(Job job);
	
	public boolean removeJob(Job job);
	
	public void insertJobWithoutTourUpdate(Job job, InsertionData insertionData);
	
	public void insertJob(Job job, InsertionData insertionData);
	
	public void updateTour();


}
