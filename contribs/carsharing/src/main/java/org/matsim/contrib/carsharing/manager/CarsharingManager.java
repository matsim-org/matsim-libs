package org.matsim.contrib.carsharing.manager;

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
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

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
		Link endLink = network.getLinks().get(legToBeRouted.getRoute().getEndLinkId());
		//=== it is assumed that the freefloating vehicle will be parked at the destination link upon ending the rental===
		//=== the case when we want it parked somewhere else would need a return information from the router===
		//=== if not, other approach could be that csPersonVehicles is updated somewhere else, when the agent exits vehicle===
		
		if (type.equals("freefloating")) {
			
			CSVehicle vehicle = getVehicleAtLocation(startLink,	plan.getPerson(), type);		
			
			if (vehicle != null) {
				this.csPersonVehicles.getVehicleLocationForType(person.getId(), type).remove(startLink.getId());
								
				if (keepTheCarModel.keepTheCarDuringNextACtivity(0.0, plan.getPerson())) {
					this.csPersonVehicles.getVehicleLocationForType(person.getId(), type).put(endLink.getId(), vehicle);
					return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, "freefloating", vehicle,
							startLink, true, true);
				}
				else
					return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, "freefloating", vehicle,
							startLink, false, true);


			}
			else {
				CSVehicle newVehicle = findClosestAvailableCar(startLink);
				Link stationLink = this.carsharingStationsData.getFfvehiclesMap().get(newVehicle);
				this.carsharingStationsData.getFfvehiclesMap().remove(vehicle);

				if (newVehicle == null)
					return null;
				eventsManager.processEvent(new StartRentalEvent(time, startLink, stationLink, person.getId(), newVehicle.getVehicleId()));
			
				if (keepTheCarModel.keepTheCarDuringNextACtivity(0.0, plan.getPerson())) {
					this.csPersonVehicles.getVehicleLocationForType(person.getId(), type).put(endLink.getId(), vehicle);

					return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, "freefloating", newVehicle, 
							stationLink, true, false);
				}
				else
					return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, "freefloating", newVehicle, 
							stationLink, false, false);			
			}		
		}
		return null;		
	}
	
	public void returnCarsharingVehicle(Id<Person> personId, Id<Link> linkId, double time, String ffVehicleId) {

		Network network = scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		CSVehicle ffVehicle = this.carsharingStationsData.getFfvehicleIdMap().get(ffVehicleId);
		this.carsharingStationsData.getFfVehicleLocationQuadTree().put(link.getCoord().getX(), link.getCoord().getY(), ffVehicle);
		this.carsharingStationsData.getFfvehiclesMap().put(ffVehicle, link);
		
		eventsManager.processEvent(new EndRentalEvent(time, linkId, personId, ffVehicle.getVehicleId()));		
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
	
}
