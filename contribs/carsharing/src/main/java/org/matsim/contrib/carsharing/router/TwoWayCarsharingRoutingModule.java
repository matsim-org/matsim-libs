package org.matsim.contrib.carsharing.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

public class TwoWayCarsharingRoutingModule implements RoutingModule {
	
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		
		final Leg leg1 = PopulationUtils.createLeg("twowaycarsharing");
		LinkNetworkRouteImpl route1 = new LinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		leg1.setRoute(route1);
		
		trip.add(leg1);
		
		return trip;
	}
		
	@Override
	public StageActivityTypes getStageActivityTypes() {
		// TODO Auto-generated method stub
		
		return EmptyStageActivityTypes.INSTANCE;
	}
}
