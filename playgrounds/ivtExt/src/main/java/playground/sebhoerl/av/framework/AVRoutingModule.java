package playground.sebhoerl.av.framework;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

import playground.sebhoerl.av.router.GenericLeg;

public class AVRoutingModule implements RoutingModule {

	@SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		Route route = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		
		route.setDistance(0.0);
		route.setTravelTime(0.0);
		
		Leg leg = new GenericLeg(AVModule.AV_MODE);
		
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(0.0);
		leg.setRoute(route);
		
		ArrayList<Leg> list = new ArrayList<Leg>();
		list.add(leg);
		
		return list;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}
}
