package org.matsim.contrib.carsharing.manager;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

/** 
 * Class containing all the information about carsharing supply and demand.
 *  
 * @author balac
 */
public class CarsharingManager implements IterationEndsListener{
	
	@Inject private Scenario scenario;
	@Inject private CarSharingVehiclesNew carsharingStationsData;
	@Inject private KeepingTheCarModel keepTheCarModel;
	@Inject private CSPersonVehicle csPersonVehicles;
	@Inject private EventsManager eventsManager;
	@Inject private RouterProvider routerProvider;
	
	public List<PlanElement> reserveAndrouteCarsharingTrip(Plan plan, String type, Leg legToBeRouted, Double time) {
		Network network = scenario.getNetwork();
		Person person = plan.getPerson();
		Link startLink = network.getLinks().get(legToBeRouted.getRoute().getStartLinkId());
		Link destinationLink = network.getLinks().get(legToBeRouted.getRoute().getEndLinkId());
		Link endLink = null;
		CarsharingStation parkingStation = null;
		CSVehicle vehicle = getVehicleAtLocation(startLink,	plan.getPerson(), type);		
		
		boolean keepTheCar = keepTheCarModel.keepTheCarDuringNextACtivity(0.0, plan.getPerson(), type);
		
		if (type.equals("oneway") && !keepTheCar) {
			
			parkingStation = this.findClosestAvailableParkingSpace(destinationLink);
			if (parkingStation == null)
				return null;
			endLink = network.getLinks().get(parkingStation.getLinkId());
		}
		
		if (vehicle != null) {

			this.csPersonVehicles.getVehicleLocationForType(person.getId(), type).remove(startLink.getId());
			
			if (keepTheCar) {
				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, type, vehicle,
						startLink, endLink, true, true);
			}
			else {
				if (type.equals("oneway"))
					this.carsharingStationsData.reserveParkingSlot(parkingStation);

				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, type, vehicle,
						startLink, endLink, false, true);
				
			}
			
			
		}
		
		else {
			vehicle = findClosestAvailableCar(startLink, type,"car");
			if (vehicle == null)
				return null;
			Link stationLink = this.carsharingStationsData.getLocationVehicle(vehicle);
			this.carsharingStationsData.reserveVehicle(vehicle);
			
			eventsManager.processEvent(new StartRentalEvent(time, startLink, stationLink, person.getId(), vehicle.getVehicleId()));
			
			if (keepTheCar) {

				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, type, vehicle, 
						stationLink, endLink, true, false);
			}
			else
				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, type, vehicle, 
						stationLink, endLink, false, false);
		}			
		
	}
	
	private CSVehicle findClosestAvailableOWCar(Link link, String vehicleType) {
		Network network = scenario.getNetwork();

		//find the closest available car and reserve it (make it unavailable)
		//if no cars within certain radius return null
		final OneWayCarsharingConfigGroup owConfigGroup = (OneWayCarsharingConfigGroup)
				scenario.getConfig().getModule("OneWayCarsharing");
		double distanceSearch = owConfigGroup.getsearchDistance() ;

		Collection<CarsharingStation> location = 
				this.carsharingStationsData.getOwvehicleLocationQuadTree().getDisk(link.getCoord().getX(), 
						link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		CarsharingStation closest = null;
		for(CarsharingStation station: location) {
			
			Coord coord = network.getLinks().get(station.getLinkId()).getCoord();
			
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), coord) < distanceSearch 
					&& ((OneWayCarsharingStation)station).getNumberOfVehicles(vehicleType) > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), coord);
			}
		}	
		
		if (closest != null) {
			//TODO: remove this vehicle from the station
			CSVehicle vehicleToBeUsed = ((OneWayCarsharingStation)closest).getVehicles(vehicleType).get(0);
			return vehicleToBeUsed;
		}
		
		return null;
	}

	private CarsharingStation findClosestAvailableParkingSpace(Link link) {
		Network network = scenario.getNetwork();

		//find the closest available parking space and reserve it (make it unavailable)
		//if there are no parking spots within search radius, return null
		OneWayCarsharingConfigGroup owConfigGroup = (OneWayCarsharingConfigGroup) 
				scenario.getConfig().getModule("OneWayCarsharing");
		
		double distanceSearch = owConfigGroup.getsearchDistance();

		Collection<CarsharingStation> location = 
				this.carsharingStationsData.getOwvehicleLocationQuadTree().getDisk(link.getCoord().getX(), 
						link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		CarsharingStation closest = null;
		for(CarsharingStation station: location) {
			
			Coord coord = network.getLinks().get(station.getLinkId()).getCoord();
			
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), coord) < distanceSearch 
					&& ((OneWayCarsharingStation)station).getAvaialbleParkingSpots() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), coord);
			}
		}
		return closest;
	}
	
	private CSVehicle findClosestAvailableCar(Link startLink, String csType, String vehicleType) {

		if (csType.equals("freefloating"))
			return findClosestAvailableCar(startLink);
		else if (csType.equals("oneway"))
			return findClosestAvailableOWCar(startLink, vehicleType);
		return null;
	}

	public void returnCarsharingVehicle(Id<Person> personId, Id<Link> linkId, double time, String vehicleId) {
		Network network = scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		Coord coord = link.getCoord();
		if (vehicleId.startsWith("FF")) {
		
			CSVehicle ffVehicle = this.carsharingStationsData.getFfvehicleIdMap().get(vehicleId);
			this.carsharingStationsData.getFfVehicleLocationQuadTree().put(link.getCoord().getX(), link.getCoord().getY(), ffVehicle);
			this.carsharingStationsData.getFfvehiclesMap().put(ffVehicle, link);
			
			eventsManager.processEvent(new EndRentalEvent(time, linkId, personId, ffVehicle.getVehicleId()));		
		}
		else if (vehicleId.startsWith("OW")) {
			
			CSVehicle owVehicle = this.carsharingStationsData.getOwvehicleIdMap().get(vehicleId);
			this.carsharingStationsData.getOwvehiclesMap().put(owVehicle, link);
			
			CarsharingStation station = this.carsharingStationsData.getOwvehicleLocationQuadTree().getClosest(coord.getX(), coord.getY());
			((OneWayCarsharingStation)station).addCar(((StationBasedVehicle)owVehicle).getVehicleType(),  owVehicle);
			eventsManager.processEvent(new EndRentalEvent(time, linkId, personId, owVehicle.getVehicleId()));		

			
		}
	}	

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		this.carsharingStationsData.readVehicleLocations();
		this.csPersonVehicles.reset();
	}
	
	private CSVehicle getVehicleAtLocation(Link currentLink, Person person, String carsharingType) {

		if (this.csPersonVehicles.getVehicleLocationForType(person.getId(), carsharingType) != null &&
				this.csPersonVehicles.getVehicleLocationForType(person.getId(), carsharingType).containsKey(currentLink.getId())) {

			CSVehicle vehicle = this.csPersonVehicles.getVehicleLocationForType(person.getId(), carsharingType).get(currentLink.getId());			
			return vehicle;
		}
		return null;
		
	}
	
	private CSVehicle findClosestAvailableCar(Link link) {
		CSVehicle vehicle = this.carsharingStationsData.getFfVehicleLocationQuadTree()
				.getClosest(link.getCoord().getX(), link.getCoord().getY());
		if (vehicle != null) {
			Link linkV = this.carsharingStationsData.getFfvehiclesMap().get(vehicle);	
			Coord coord = linkV.getCoord();
			
			this.carsharingStationsData.getFfVehicleLocationQuadTree().remove(coord.getX(), coord.getY(), vehicle);
		}
		return vehicle;
	}

	public void freeParkingSpot(Id<Link> linkId) {

		Network network = scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		Coord coord = link.getCoord();
		OneWayCarsharingStation station = (OneWayCarsharingStation) this.carsharingStationsData.getOwvehicleLocationQuadTree().getClosest(coord.getX(), coord.getY());
		station.freeParkingSpot();
		
	}
	
}
