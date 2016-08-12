package org.matsim.contrib.carsharing.manager.routers;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public class RouteOneWayTrip implements RouteCarsharingTrip {



	@Override
	public List<PlanElement> routeCarsharingTrip(Plan plan, Leg legToBeRouted, double time, CSVehicle vehicle,
			Link vehicleLinkLocation, boolean keepTheCarForLaterUse, boolean hasVehicle) {
		// TODO Auto-generated method stub
		return null;
	}

}
