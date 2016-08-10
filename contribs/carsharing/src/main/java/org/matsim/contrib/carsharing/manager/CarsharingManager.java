package org.matsim.contrib.carsharing.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteFreefloatingTrip;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.vehicles.FFVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

/** 
 * Class containing all the information about carsharing.
 * 
 * @author balac
 */
public class CarsharingManager implements IterationStartsListener, IterationEndsListener{
	
	private RouteCarsharingTrip freefloatingRouter;
	private RouteCarsharingTrip twowayRouter;
	private RouteCarsharingTrip onewayRouter;
	@Inject private Scenario scenario;
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;
	@Inject private Map<String,TravelDisutilityFactory> travelDisutilityFactories ;
	@Inject private Map<String,TravelTime> travelTimes ;
	@Inject private CarSharingVehiclesNew carsharingStationsData;
	@Inject private KeepingTheCarModel keepTheCarModel;
	@Inject private EventsManager eventsManager;
	private Map<Id<Person>, CSPersonVehiclesContainer> vehicleInfoPerPerson;
	
	public List<PlanElement> reserveAndrouteCarsharingTrip(Person person, Plan plan, String type, int index, Double time) {
		
		if (type.equals("freefloating")) {
			
			return this.freefloatingRouter.routeCarsharingTrip(person, plan, index, time);
		}
		return null;
		
	}
	
	public void returnCarsharingVehicle(Id<Person> personId, Id<Link> linkId, double time, String ffVehicleId) {

		Network network = scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		FFVehicle ffVehicle = this.carsharingStationsData.getFfvehicleIdMap().get(ffVehicleId);
		this.carsharingStationsData.getFfVehicleLocationQuadTree().put(link.getCoord().getX(), link.getCoord().getY(), ffVehicle);
		this.carsharingStationsData.getFfvehiclesMap().put(ffVehicle, link);
		
		eventsManager.processEvent(new EndRentalEvent(time, linkId, personId, ffVehicle.getVehicleId()));
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get( TransportMode.car ) ;
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime ) ;
		
		vehicleInfoPerPerson = new HashMap<Id<Person>, CSPersonVehiclesContainer>();
		this.freefloatingRouter = new RouteFreefloatingTrip(pathCalculator, carsharingStationsData, 
				scenario, this.eventsManager, keepTheCarModel, vehicleInfoPerPerson);
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		this.carsharingStationsData.readVehicleLocations();
	}

}
