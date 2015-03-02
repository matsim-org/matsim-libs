/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.kai.usecases.invertednetwork;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
class InvertedRoutingModule implements RoutingModule {

	RoutingModule router ;
	
	InvertedRoutingModule(Scenario sc, TravelDisutilityFactory tdf ) {
		
		TravelTime travelTime = null ; // new TravelTimeForInvertedNetwork() ;

		TravelDisutility travelDisutility = tdf.createTravelDisutility(
				travelTime, sc.getConfig().planCalcScore() 
				) ;
		
		Network invertedNetwork = null ;
		
//		this.router = LegRouterWrapper.createLegRouterWrapper(TransportMode.car, sc.getPopulation().getFactory(), new NetworkLegRouter(invertedNetwork, new Dijkstra(invertedNetwork, travelDisutility, travelTime) , 
//				((PopulationFactoryImpl)sc.getPopulation().getFactory()).getModeRouteFactory() 
//)) ;
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		return this.router.calcRoute(fromFacility, toFacility, departureTime, person) ;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return null ;
	}

}

class TravelTimeForInvertedNetwork implements TravelTime {
	
	TravelTimeForInvertedNetwork(Network net) {
		
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		// ...
		return 0. ;
	}
	
}
