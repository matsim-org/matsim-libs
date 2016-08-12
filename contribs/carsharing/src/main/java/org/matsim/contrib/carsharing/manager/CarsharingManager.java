package org.matsim.contrib.carsharing.manager;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
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
		
		if (type.equals("freefloating")) {
			
			return this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted, "freefloating");
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
		//TODO: reset CSPersonVehicles also
	}
}
