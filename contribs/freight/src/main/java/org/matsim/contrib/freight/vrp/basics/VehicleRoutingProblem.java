package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;
import java.util.Map;



public interface VehicleRoutingProblem{
	
	public Collection<Vehicle> getVehicles();
	
	public Map<String,Job> getJobs();
	
	public Constraints getConstraints();
	
	public Costs getCosts();


}
