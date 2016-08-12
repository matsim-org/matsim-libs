package org.matsim.contrib.carsharing.manager.routers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
/** 
 * 
 * @author balac
 */
public class RouteFreefloatingTrip implements RouteCarsharingTrip {

	@Inject private Scenario scenario;
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;
	
	@Inject private Map<String, TravelTime> travelTimes ;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactories ;
		

	
	
	@Override
	public List<PlanElement> routeCarsharingTrip(Plan plan, Leg legToBeRouted, double time, 
			CSVehicle vehicle, Link vehicleLinkLocation, boolean keepTheCarForLaterUse, boolean hasVehicle) {
		PopulationFactory pf = scenario.getPopulation().getFactory();
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutility travelDisutility = travelDisutilityFactories.get( TransportMode.car ).createTravelDisutility(travelTime) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(scenario.getNetwork(),
				travelDisutility, travelTime ) ;
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();		

		Person person = plan.getPerson();
		NetworkRoute route = (NetworkRoute) legToBeRouted.getRoute();
		final Link currentLink = scenario.getNetwork().getLinks().get(route.getStartLinkId());
		final Link destinationLink = scenario.getNetwork().getLinks().get(route.getEndLinkId());
		
		if (hasVehicle) {
			//=== car leg			

			trip.add(RouterUtils.createCarLeg(pf, pathCalculator,
					person, currentLink, destinationLink, "freefloating", 
					vehicle.getVehicleId(), time));		
			
			if (!keepTheCarForLaterUse) 			

				trip.add( RouterUtils.createWalkLeg(pf, 
						destinationLink, destinationLink, "egress_walk_ff", time) );							
		
		}
		else {		
			
			String ffVehId = vehicle.getVehicleId();			
			trip.add( RouterUtils.createWalkLeg(scenario.getPopulation().getFactory(),
					currentLink, vehicleLinkLocation, "access_walk_ff", time) );

			// === car leg: ===							
			
			trip.add(RouterUtils.createCarLeg(pf, pathCalculator,
					person, vehicleLinkLocation, destinationLink, "freefloating",
					ffVehId, time));			
			
			if (!keepTheCarForLaterUse)  {			
				trip.add( RouterUtils.createWalkLeg(pf, 
						destinationLink, destinationLink, "egress_walk_ff", time) );
			}
			
		}			
		return trip;
	}}
