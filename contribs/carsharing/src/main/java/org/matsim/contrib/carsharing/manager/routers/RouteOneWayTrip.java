package org.matsim.contrib.carsharing.manager.routers;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.manager.CSPersonVehiclesContainer;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class RouteOneWayTrip implements RouteCarsharingTrip {

	private Scenario scenario;
	private CarSharingVehiclesNew carSharingVehicles;
	private Map<Id<Person>, CSPersonVehiclesContainer> vehicleInfoPerPerson;
	private KeepingTheCarModel keepCarModel;
	private LeastCostPathCalculator pathCalculator;
	private EventsManager events;
	
	public RouteOneWayTrip(LeastCostPathCalculator pathCalculator, 
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
	public List<PlanElement> routeCarsharingTrip(Plan plan, Leg legToBeRouted, double time) {
		// TODO Auto-generated method stub
		return null;
	}

}
