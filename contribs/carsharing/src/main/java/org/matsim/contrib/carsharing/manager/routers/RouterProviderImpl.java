package org.matsim.contrib.carsharing.manager.routers;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

public class RouterProviderImpl implements RouterProvider {

	@Inject private RouteFreefloatingTrip freefloatingRouter;
//	@Inject private RouteOneWayTrip onewayRouter;
	
	@Override
	public List<PlanElement> routeCarsharingTrip(Plan plan, double time, Leg legToBeRouted, String carsharingType) {

		if (carsharingType.equals("freefloating"))
			return freefloatingRouter.routeCarsharingTrip(plan, legToBeRouted, time);
	//	else if (carsharingType.equals("onewaycarsharing"))
	//		return onewayRouter.routeCarsharingTrip(plan, legToBeRouted, time);
		
		return null;
	}
	
	

}
