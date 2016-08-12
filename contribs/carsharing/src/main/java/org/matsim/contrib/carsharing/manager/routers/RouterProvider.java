package org.matsim.contrib.carsharing.manager.routers;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

public interface RouterProvider {
	
	public List<PlanElement> routeCarsharingTrip(Plan plan, double time, Leg legToBeRouted, String carsharingType);

}
