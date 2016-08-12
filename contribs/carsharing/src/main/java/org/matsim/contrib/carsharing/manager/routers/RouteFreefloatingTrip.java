package org.matsim.contrib.carsharing.manager.routers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.manager.CSPersonVehicle;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
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
	@Inject private CarSharingVehiclesNew carSharingVehicles;
	@Inject private CSPersonVehicle csPersonVehicles;
	@Inject private KeepingTheCarModel keepCarModel;
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;
	
	@Inject private Map<String, TravelTime> travelTimes ;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactories ;
	@Inject private EventsManager events;
		
	@Override
	public List<PlanElement> routeCarsharingTrip(Plan plan, Leg legToBeRouted, double now) {	
		
		PopulationFactory pf = scenario.getPopulation().getFactory();
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutility travelDisutility = travelDisutilityFactories.get( TransportMode.car ).createTravelDisutility(travelTime) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(scenario.getNetwork(),
				travelDisutility, travelTime ) ;
		Map<CSVehicle, Link> ffvehiclesmap = this.carSharingVehicles.getFfvehiclesMap();
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();		

		Person person = plan.getPerson();
		NetworkRoute route = (NetworkRoute) legToBeRouted.getRoute();
		final Link currentLink = scenario.getNetwork().getLinks().get(route.getStartLinkId());
		final Link destinationLink = scenario.getNetwork().getLinks().get(route.getEndLinkId());
		
		if (hasffVehicleAtLocation(currentLink, person)) {
			//=== car leg
			
			CSVehicle ffVehicle = this.csPersonVehicles.getVehicleLocationForType(person.getId(), "freefloating").get(currentLink.getId());			
			

			trip.add(RouterUtils.createCarLeg(pf, pathCalculator,
					person, currentLink, destinationLink, "freefloating", 
					ffVehicle.getVehicleId(), now));
			
			//=== check if the person wants to keep the car for later ===
			
			if (keepCarModel.keepTheCarDuringNextACtivity(0.0, person)) {
				
				csPersonVehicles.removeVehicle(person.getId(), currentLink, null, "freefloating");
				csPersonVehicles.addVehicle(person.getId(), destinationLink, ffVehicle, "freefloating");
							
			}
			else {
				
				csPersonVehicles.removeVehicle(person.getId(), currentLink, null, "freefloating");
				
				trip.add( RouterUtils.createWalkLeg(pf, 
						destinationLink, destinationLink, "egress_walk_ff", now) );

			}
		}
		else {
			
			CSVehicle vehicleToBeUsed = findClosestAvailableCar(currentLink);
			if (vehicleToBeUsed == null) {			
				return null;
			}
			String ffVehId = vehicleToBeUsed.getVehicleId();
			final Link stationLink = ffvehiclesmap.get(vehicleToBeUsed) ;
			Coord coordStation = stationLink.getCoord();
			
			this.carSharingVehicles.getFfvehiclesMap().remove(vehicleToBeUsed);
			this.carSharingVehicles.getFfVehicleLocationQuadTree().remove(coordStation.getX(),
					coordStation.getY(), vehicleToBeUsed); 
			
			trip.add( RouterUtils.createWalkLeg(scenario.getPopulation().getFactory(),
					currentLink, stationLink, "access_walk_ff", now) );

			// === car leg: ===							
			
			trip.add(RouterUtils.createCarLeg(pf, pathCalculator,
					person, stationLink, destinationLink, "freefloating",
					ffVehId, now));
			
			
			if (keepCarModel.keepTheCarDuringNextACtivity(0.0, person)) {
				
				if (this.csPersonVehicles.getVehicleLocationForType(person.getId(), "freefloating") != null) {
					this.csPersonVehicles.addVehicle(person.getId(), destinationLink, vehicleToBeUsed, "freefloating");

					
				}
				else {
					this.csPersonVehicles.addNewPersonInfo(person.getId());
					this.csPersonVehicles.addVehicle(person.getId(), destinationLink, vehicleToBeUsed, "freefloating");
					
				}	
							
			}
			else {			
				trip.add( RouterUtils.createWalkLeg(pf, 
						destinationLink, destinationLink, "egress_walk_ff", now) );
			}
			
			events.processEvent(new StartRentalEvent(now, currentLink, stationLink, person.getId(), ffVehId));
		}			
		return trip;
	}

	private boolean hasffVehicleAtLocation(Link currentLink, Person person) {

		if (this.csPersonVehicles.getVehicleLocationForType(person.getId(), "freefloating") != null &&
				this.csPersonVehicles.getVehicleLocationForType(person.getId(), "freefloating").containsKey(currentLink.getId()))
			return true;
		return false;
		
	}
	
	private CSVehicle findClosestAvailableCar(Link link) {
		CSVehicle vehicle = this.carSharingVehicles.getFfVehicleLocationQuadTree()
				.getClosest(link.getCoord().getX(), link.getCoord().getY());
		return vehicle;
	}
	
}
