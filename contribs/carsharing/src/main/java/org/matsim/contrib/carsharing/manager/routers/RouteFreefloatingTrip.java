package org.matsim.contrib.carsharing.manager.routers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.manager.CSPersonVehiclesContainer;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.vehicles.FFVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;
/** 
 * 
 * @author balac
 */
public class RouteFreefloatingTrip implements RouteCarsharingTrip {

	private Scenario scenario;
	private CarSharingVehiclesNew carSharingVehicles;
	private Map<Id<Person>, CSPersonVehiclesContainer> vehicleInfoPerPerson;
	private KeepingTheCarModel keepCarModel;
	private LeastCostPathCalculator pathCalculator;
	private EventsManager events;
	
	public RouteFreefloatingTrip(LeastCostPathCalculator pathCalculator, 
			CarSharingVehiclesNew carSharingVehicles, Scenario scenario,
			EventsManager eventsManager, KeepingTheCarModel keepCarModel,
			Map<Id<Person>, CSPersonVehiclesContainer> vehicleInfoPerPerson) {
		
		this.scenario = scenario;
		this.pathCalculator = pathCalculator;
		this.carSharingVehicles = carSharingVehicles;
		this.events = eventsManager;
		this.keepCarModel = keepCarModel;
		this.vehicleInfoPerPerson = vehicleInfoPerPerson;
	}
	
	@Override
	public List<PlanElement> routeCarsharingTrip(Person person, Plan plan, int indexOfLegToBeModified, double now) {	
		
		Map<FFVehicle, Link> ffvehiclesmap = this.carSharingVehicles.getFfvehiclesMap();
		
		List<PlanElement> planElements = plan.getPlanElements();

		final List<PlanElement> trip = new ArrayList<PlanElement>();		

		NetworkRoute route = (NetworkRoute) ((Leg)planElements.get(indexOfLegToBeModified)).getRoute();
		final Link currentLink = scenario.getNetwork().getLinks().get(route.getStartLinkId());
		final Link destinationLink = scenario.getNetwork().getLinks().get(route.getEndLinkId());
		
		if (hasffVehicleAtLocation(currentLink, person)) {
			//=== car leg
			
			FFVehicle ffVehicle = this.vehicleInfoPerPerson.get(person.getId()).getVehicleOnLink(currentLink);			

			trip.add(createCarLeg(person, currentLink, destinationLink, "freefloating", ffVehicle.getVehicleId(), now));
			
			//=== check if the person wants to keep the car for later ===
			
			if (keepCarModel.keepTheCarDuringNextACtivity(0.0, person)) {
				
				CSPersonVehiclesContainer personVehicles = this.vehicleInfoPerPerson.get(person.getId());
				
				personVehicles.getFfvehicleIdLocation().put(destinationLink.getId(), ffVehicle);
				personVehicles.getFfvehicleIdLocation().remove(currentLink.getId());
			}
			else {
				CSPersonVehiclesContainer personVehicles = this.vehicleInfoPerPerson.get(person.getId());

				personVehicles.getFfvehicleIdLocation().remove(currentLink.getId());

				trip.add( createWalkLeg(destinationLink, destinationLink, "egress_walk_ff", now) );

			}

		}
		else {
			
			FFVehicle vehicleToBeUsed = findClosestAvailableCar(currentLink);
			if (vehicleToBeUsed == null) {			
				return null;
			}
			
			String ffVehId = vehicleToBeUsed.getVehicleId();
			final Link stationLink = ffvehiclesmap.get(vehicleToBeUsed) ;
			Coord coordStation = stationLink.getCoord();
			
			this.carSharingVehicles.getFfVehicleLocationQuadTree().remove(coordStation.getX(),
					coordStation.getY(), vehicleToBeUsed); 
			
			trip.add( createWalkLeg(currentLink, stationLink, "access_walk_ff", now) );

			// === car leg: ===		
			if (this.vehicleInfoPerPerson.get(person.getId()) != null) {
				CSPersonVehiclesContainer personCSInfo = this.vehicleInfoPerPerson.get(person.getId());
				personCSInfo.getFfvehicleIdLocation().put(stationLink.getId(), vehicleToBeUsed);
				
			}
			else {
				CSPersonVehiclesContainer personCSInfo = new CSPersonVehiclesContainer();
				personCSInfo.getFfvehicleIdLocation().put(stationLink.getId(), vehicleToBeUsed);
				this.vehicleInfoPerPerson.put(person.getId(), personCSInfo);
			}
			

			trip.add(createCarLeg(person, stationLink, destinationLink, "freefloating", ffVehId, now));
			
			trip.add( createWalkLeg(destinationLink, destinationLink, "egress_walk_ff", now) );
			//this.basicAgentDelegate.getEvents().processEvent(new StartRentalEvent(now, currentLink.getId(), stationLink.getId(),
			//		this.basicAgentDelegate.getPerson().getId(), ffVehId));
			events.processEvent(new StartRentalEvent(now, currentLink, stationLink, person.getId(), ffVehId));


		}	
		
		return trip;
	}

	private boolean hasffVehicleAtLocation(Link currentLink, Person person) {

		CSPersonVehiclesContainer personVehicles = this.vehicleInfoPerPerson.get(person.getId());
		
		if (personVehicles != null && 
				personVehicles.getFfvehicleIdLocation().containsKey(currentLink.getId()))
			return true;
		
		return false;
	}
	
	private FFVehicle findClosestAvailableCar(Link link) {
		FFVehicle vehicle = this.carSharingVehicles.getFfVehicleLocationQuadTree()
				.getClosest(link.getCoord().getX(), link.getCoord().getY());
		return vehicle;
	}
	
	private Leg createCarLeg(Person person, Link startLink, Link destinationLink, String mode, 
			String vehicleId, double now) {
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		RouteFactories routeFactory = ((PopulationFactory)pf).getRouteFactories() ;
		
		Vehicle vehicle = null ;
		Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), destinationLink.getFromNode(), 
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
	
	private Leg createWalkLeg(Link startLink, Link destinationLink, String mode, double now) {
		
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		RouteFactories routeFactory = ((PopulationFactory)pf).getRouteFactories() ;
		
		Route routeWalk = routeFactory.createRoute( Route.class, startLink.getId(), destinationLink.getId() ) ; 
		
		final double egressDist = CoordUtils.calcEuclideanDistance(startLink.getCoord(), destinationLink.getCoord()) * 1.3;
		routeWalk.setTravelTime( (egressDist / 1.0));
		routeWalk.setDistance(egressDist);	

		final Leg walkLeg = pf.createLeg( mode );
		walkLeg.setRoute(routeWalk);

		return walkLeg;		
	}



}
