package org.matsim.contrib.carsharing.manager.routers;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

import com.google.inject.Inject;

public class RouterProviderImpl implements RouterProvider {

	@Inject private RouteFreefloatingTrip freefloatingRouter;
//	@Inject private RouteOneWayTrip onewayRouter;
	

	@Override
	public List<PlanElement> routeCarsharingTrip(Plan plan, double time, Leg legToBeRouted, String carsharingType,
			CSVehicle vehicle, Link vehicleLinkLocation,boolean keepTheCarForLaterUse, boolean hasVehicle) {
		if (carsharingType.equals("freefloating"))
			return freefloatingRouter.routeCarsharingTrip(plan, legToBeRouted, time, vehicle, 
					vehicleLinkLocation, keepTheCarForLaterUse, hasVehicle);
		return null;
	}
	
	

}
