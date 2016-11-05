package org.matsim.contrib.carsharing.manager.routers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

public class RouterUtils {
	
	public static Leg createCarLeg(PopulationFactory pf, LeastCostPathCalculator pathCalculator,
			Person person, Link startLink, Link destinationLink, String mode, 
			String vehicleId, double now) {
		
		
		RouteFactories routeFactory = ((PopulationFactory)pf).getRouteFactories() ;
		
		Vehicle vehicle = null ;
		Path path = pathCalculator.calcLeastCostPath(startLink.getToNode(), destinationLink.getFromNode(), 
				now, person, vehicle ) ;
		
		NetworkRoute carRoute = routeFactory.createRoute(NetworkRoute.class, startLink.getId(), destinationLink.getId() );
		carRoute.setLinkIds(startLink.getId(), NetworkUtils.getLinkIds( path.links), destinationLink.getId());
		carRoute.setTravelTime( path.travelTime );		
		
		carRoute.setVehicleId( Id.create( (vehicleId), Vehicle.class) ) ;

		Leg carLeg = pf.createLeg(mode);
		carLeg.setTravelTime( path.travelTime );
		carLeg.setRoute(carRoute);
		
		return carLeg;
	}

	public static Leg createWalkLeg(PopulationFactory pf, Link startLink, 
			Link destinationLink, String mode, double now) {
		
		RouteFactories routeFactory = ((PopulationFactory)pf).getRouteFactories() ;
		
		Route routeWalk = routeFactory.createRoute( Route.class, startLink.getId(), destinationLink.getId() ) ; 
		
		double egressDist = CoordUtils.calcEuclideanDistance(startLink.getCoord(), destinationLink.getCoord()) * 1.3;
		egressDist = egressDist > 0 ? egressDist : 1; 
		routeWalk.setTravelTime( (egressDist / 1.0));
		routeWalk.setDistance(egressDist);	

		final Leg walkLeg = pf.createLeg( mode );
		walkLeg.setRoute(routeWalk);

		return walkLeg;		
	}
	
}
