package playground.balac.onewaycarsharingredisgned.router;


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
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

public class OneWayCarsharingRDRoutingModule implements RoutingModule{
	public OneWayCarsharingRDRoutingModule() {		
		
	}
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		final Leg startWalkLeg = PopulationUtils.createLeg("walk_ow_sb");
		GenericRouteImpl startWalkRoute = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		startWalkLeg.setRoute(startWalkRoute);
		trip.add( startWalkLeg );
		
		final Leg csLeg = PopulationUtils.createLeg("onewaycarsharing");
		LinkNetworkRouteImpl csRoute = new LinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		csLeg.setRoute(csRoute);
		trip.add( csLeg );	
		
		final Leg endWalkLeg = PopulationUtils.createLeg("walk_ow_sb");
		GenericRouteImpl endWalkRoute = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		endWalkLeg.setRoute(endWalkRoute);
		trip.add( endWalkLeg );
		
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		
		return EmptyStageActivityTypes.INSTANCE;
	}
}
