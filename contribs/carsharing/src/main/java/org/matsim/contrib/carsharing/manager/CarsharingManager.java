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
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
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
public class CarsharingManager implements CarsharingManagerInterface, IterationEndsListener{
	
	@Inject private Scenario scenario;
	@Inject private CarSharingVehiclesNew carsharingStationsData;
	@Inject private KeepingTheCarModel keepTheCarModel;
	@Inject private CSPersonVehicle csPersonVehicles;
	@Inject private EventsManager eventsManager;
	@Inject private RouterProvider routerProvider;
	
	@Override
	public List<PlanElement> reserveAndrouteCarsharingTrip(Plan plan, String carsharingType, Leg legToBeRouted, Double time) {
		Network network = scenario.getNetwork();
		Person person = plan.getPerson();
		Link startLink = network.getLinks().get(legToBeRouted.getRoute().getStartLinkId());
		Link destinationLink = network.getLinks().get(legToBeRouted.getRoute().getEndLinkId());
		Link endLink = null;
		CarsharingStation parkingStation = null;
		CSVehicle vehicle = getVehicleAtLocation(startLink,	plan.getPerson(), carsharingType);		
		boolean willHaveATripFromLocation = willUseTheVehicleLaterFromLocation(destinationLink.getId(), plan, legToBeRouted);
		
		boolean keepTheCar = keepTheCarModel.keepTheCarDuringNextACtivity(0.0, plan.getPerson(), carsharingType);
		
		if (this.csPersonVehicles.getVehicleLocationForType(person.getId(), carsharingType) == null)				
			this.csPersonVehicles.addNewPersonInfo(person.getId());
		
		if (carsharingType.equals("oneway") && !(keepTheCar && willHaveATripFromLocation)) {
			
			parkingStation = this.findClosestAvailableParkingSpace(destinationLink);
			if (parkingStation == null)
				return null;
			endLink = network.getLinks().get(parkingStation.getLinkId());
		}		
		else if (carsharingType.equals("twoway") && !(keepTheCar && willHaveATripFromLocation)) {
			
			if (vehicle != null)
				parkingStation = this.carsharingStationsData.getTwowaycarsharingstationsMap().get(((StationBasedVehicle)vehicle).getStationId());
			else
				new RuntimeException("This should never happen!");
			endLink = network.getLinks().get(parkingStation.getLinkId());
		}
		if (vehicle != null) {

			this.csPersonVehicles.getVehicleLocationForType(person.getId(), carsharingType).remove(startLink.getId());
			
			if (willHaveATripFromLocation && keepTheCar) {
				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, carsharingType, vehicle,
						startLink, destinationLink, true, true);
			}
			else {
				if (carsharingType.equals("oneway"))
					this.carsharingStationsData.reserveParkingSlot(parkingStation);

				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, carsharingType, vehicle,
						startLink, endLink, false, true);				
			}			
		}		
		else {
			vehicle = findClosestAvailableCar(startLink, carsharingType, "car");
			if (vehicle == null)
				return null;
			//if (carsharingType.equals("twoway"))
			//	this.csPersonVehicles.addOriginForTW(person.getId(), startLink, vehicle);
			
			Link stationLink = this.carsharingStationsData.getLocationVehicle(vehicle);
			this.carsharingStationsData.reserveVehicle(vehicle);
			
			eventsManager.processEvent(new StartRentalEvent(time, startLink, stationLink, person.getId(), vehicle.getVehicleId()));
			if (willHaveATripFromLocation && keepTheCar) {
				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, carsharingType, vehicle,
						stationLink, destinationLink, true, false);
			}
			else {
				if (carsharingType.equals("oneway")) {
					parkingStation = this.findClosestAvailableParkingSpace(destinationLink);

					this.carsharingStationsData.reserveParkingSlot(parkingStation);
					destinationLink = network.getLinks().get(parkingStation.getLinkId());
				}

				return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, carsharingType, vehicle,
						startLink, destinationLink, false, false);				
			}			
				
		}					
	}
	
	private boolean willUseTheVehicleLaterFromLocation(Id<Link> linkId, Plan plan, Leg currentLeg) {
		boolean willUseVehicle = false;

		String mode = currentLeg.getMode();
		List<PlanElement> planElements = plan.getPlanElements();

		int index = planElements.indexOf(currentLeg) + 1;

		for (int i = index; i < planElements.size(); i++) {

			if (planElements.get(i) instanceof Leg) {

				if (((Leg)planElements.get(i)).getMode().equals(mode)) {

					if (((Leg)planElements.get(i)).getRoute().getStartLinkId().toString().equals(linkId.toString())) {

						willUseVehicle = true;
					}
				}
			}
		}
		return willUseVehicle;
	}
	
	private CSVehicle findClosestAvailableTWCar(Link link, String vehicleType) {
		Network network = scenario.getNetwork();

		//find the closest available car and reserve it (make it unavailable)
		//if no cars within certain radius return null
		final TwoWayCarsharingConfigGroup owConfigGroup = (TwoWayCarsharingConfigGroup)
				scenario.getConfig().getModule("TwoWayCarsharing");
		double distanceSearch = owConfigGroup.getsearchDistance() ;

		Collection<CarsharingStation> location = 
				this.carsharingStationsData.getTwvehicleLocationQuadTree().getDisk(link.getCoord().getX(), 
						link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		CarsharingStation closest = null;
		for(CarsharingStation station: location) {
			
			Coord coord = network.getLinks().get(station.getLinkId()).getCoord();
			
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), coord) < distanceSearch 
					&& ((TwoWayCarsharingStation)station).getNumberOfVehicles(vehicleType) > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), coord);
			}
		}
		
		if (closest != null) {
			CSVehicle vehicleToBeUsed = ((TwoWayCarsharingStation)closest).getVehicles(vehicleType).get(0);
			return vehicleToBeUsed;
		}
		
		return null;
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
		else if (csType.equals("twoway"))
			return findClosestAvailableTWCar(startLink, vehicleType);
		return null;
	}

	public void returnCarsharingVehicle(Id<Person> personId, Id<Link> linkId, double time, String vehicleId) {
		Network network = scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		this.carsharingStationsData.parkVehicle(vehicleId, link);
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

	@Override
	public boolean parkVehicle(String vehicleId, Id<Link> linkId) {
		
		Network network = scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		this.carsharingStationsData.parkVehicle(vehicleId, link);
		return false;
	}		
}
