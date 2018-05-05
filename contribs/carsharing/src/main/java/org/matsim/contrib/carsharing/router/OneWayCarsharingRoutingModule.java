package org.matsim.contrib.carsharing.router;


/**
 * @author balacm
 * 
 **/


import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

public class OneWayCarsharingRoutingModule implements RoutingModule{
	public OneWayCarsharingRoutingModule() {		
		
	}
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();		
		
		final Leg csLeg = PopulationUtils.createLeg("oneway");
//		NetworkRoute csRoute = RouteUtils.createLinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		CarsharingRoute csRoute = new CarsharingRoute(fromFacility.getLinkId(), toFacility.getLinkId());
		csLeg.setRoute(csRoute);
		trip.add( csLeg );	
	
		
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		
		return EmptyStageActivityTypes.INSTANCE;
	}
}
