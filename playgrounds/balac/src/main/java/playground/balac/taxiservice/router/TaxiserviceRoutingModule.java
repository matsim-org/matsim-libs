package playground.balac.taxiservice.router;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;

public class TaxiserviceRoutingModule implements RoutingModule {

	private final Controler controler;
	public TaxiserviceRoutingModule (Controler controler) {
		
		this.controler = controler;
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		double travelTime = 0.0;
		List<Id<Link>> ids = new ArrayList<Id<Link>>();
		
		List<PlanElement> trip = new ArrayList<PlanElement>();
		
		Provider<TripRouter> tripRouterFactory = controler.getTripRouterProvider();
		
		TripRouter tripRouter = tripRouterFactory.get();

		for(PlanElement pe1: tripRouter.calcRoute("car", fromFacility, toFacility, departureTime, person)) {
	    	
			if (pe1 instanceof Leg) {
				ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
	    			travelTime += ((Leg) pe1).getTravelTime();
			}
		}
		
		Leg taxiLeg = new LegImpl("taxi");
		taxiLeg.setTravelTime( travelTime );
		LinkNetworkRouteImpl route = 
				(LinkNetworkRouteImpl) ((PopulationFactoryImpl)controler.getScenario().getPopulation().getFactory()).getModeRouteFactory().createRoute("car", fromFacility.getLinkId(), toFacility.getLinkId());
		route.setLinkIds( fromFacility.getLinkId(), ids, toFacility.getLinkId());
		route.setTravelTime( travelTime);
		taxiLeg.setRoute(route);
		
		trip.add(taxiLeg);
		return trip;
		
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}
	
	
	

}
