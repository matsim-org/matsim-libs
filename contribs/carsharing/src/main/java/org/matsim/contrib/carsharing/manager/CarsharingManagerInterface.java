package org.matsim.contrib.carsharing.manager;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

public interface CarsharingManagerInterface {
	
	
	public boolean parkVehicle(String vehicleId, Id<Link> linkId);
	public List<PlanElement> reserveAndrouteCarsharingTrip(Plan plan, String carsharingType, Leg legToBeRouted, Double time);

}
