/* *********************************************************************** *
 * project: org.matsim.*
 * AssignVehiclesToPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.vehicles;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.utils.EditRoutes;

import playground.christoph.evacuation.mobsim.LegModeChecker;

public class AssignVehiclesToPlans extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private static Logger log = Logger.getLogger(AssignVehiclesToPlans.class);
	
	private final Scenario scenario;
	private final TripRouter tripRouter;
	private final Counter assignedVehiclesCounter;
	private final Counter removedCarLegsCounter;
	private final Counter addedCarLegsCounter;
	private final Map<Id<Person>, Id<Vehicle>> mapping;	// <AgentId, VehicleId>
	private final LegModeChecker legModeChecker;
	private final EditRoutes editRoutes;
	
	public AssignVehiclesToPlans(Scenario scenario, TripRouter tripRouter) {
		this.scenario = scenario;
		this.tripRouter = tripRouter;
		
		this.assignedVehiclesCounter = new Counter("Assigned vehicles: ");
		this.removedCarLegsCounter = new Counter("Legs with mode changed from car to another mode: ");
		this.addedCarLegsCounter = new Counter("Legs with mode changed from another mode to car: ");
		this.mapping = new HashMap<Id<Person>, Id<Vehicle>>();
		
		this.legModeChecker = new LegModeChecker(tripRouter, scenario.getNetwork());
		this.editRoutes = new EditRoutes();
	}
	
	public void printStatistics() {
		assignedVehiclesCounter.printCounter();
		removedCarLegsCounter.printCounter();
		addedCarLegsCounter.printCounter();
	}
	
	public void run(Household household) {
		
		// get a household's persons and check whether they require a car
    	List<Person> persons = new ArrayList<Person>();
    	Queue<Person> vehicleRequiringPersons = new PriorityQueue<Person>(10, new CarLegsComparator(scenario.getNetwork()));
    	for (Id<Person> personId : household.getMemberIds()) {
    		Person p = scenario.getPopulation().getPersons().get(personId);
    		persons.add(p);
    		mapping.put(p.getId(), null);
    		
    		boolean requiresVehicle = requiresVehicle(p);
    		if (requiresVehicle) vehicleRequiringPersons.add(p);
    	}
    	
    	List<Id<Vehicle>> vehicleIds = household.getVehicleIds();
    	
    	/*
    	 * If the household requires more vehicles than available, change
    	 * mode of car legs to another mode. Sort agents according to 
    	 * the distance they travel by car.
    	 */
    	while (vehicleRequiringPersons.size() > vehicleIds.size()) {
    		Person p = vehicleRequiringPersons.poll();
    		run(p);
    		checkVehicleId(p.getSelectedPlan());
    	}
    	
    	/*
    	 * Assign vehicles to person's legs.
    	 */
    	int i = 0;
    	Person p = null;
    	while (vehicleRequiringPersons.peek() != null) {
    		p = vehicleRequiringPersons.poll();
    		Id vehicleId = vehicleIds.get(i);
    		assignVehicleToPerson(p, vehicleId);
    		mapping.put(p.getId(), vehicleId);
    		assignedVehiclesCounter.incCounter();
    		i++;
    	}
	}
	
	/*
	 * So far, vehicle Ids are deleted when a person's route is updated. Therefore
	 * we have to set the vehicles again.
	 */
	public void reassignVehicles() {
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Id vehicleId = mapping.get(p.getId());
			if (vehicleId != null) {
				assignVehicleToPerson(p, vehicleId);				
			}
		}
	}
	
	private void checkVehicleId(Plan plan) {
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(TransportMode.car)) {
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					Id vehicleId = route.getVehicleId();
					if (vehicleId == null) {
						log.warn("Person " + plan.getPerson().getId().toString() + ": Vehicle Id is null!");
					} else if(!vehicleId.toString().contains("_veh")) {
						log.warn("Person " + plan.getPerson().getId().toString() + " has an unexpected vehicle Id: " + vehicleId.toString());
					}
				}
			}
		}
	}

	/**
	 * Replace all car legs in a plan by legs with alternative transport modes.
	 * Trips with a crow-fly distance of
	 * <ui>
	 * <li>0.0 .. 2000.0 become walk legs</li>
	 * <li>2000.0 .. 5000.0 become bike legs</li>
	 * <li>5000.0 ... become pt legs</li> 
	 * </ui> 
	 * @param plan to be adapted
	 */
	@Override
	public void run(Plan plan) {
		for (int i = 1; i < plan.getPlanElements().size() - 1; i = i + 2) {
			Leg leg = (Leg) plan.getPlanElements().get(i);

			if (leg.getMode().equals(TransportMode.car)) {
				removedCarLegsCounter.incCounter();
				Id startLinkId = leg.getRoute().getStartLinkId();
				Id endLinkId = leg.getRoute().getEndLinkId();
				Link startLink = scenario.getNetwork().getLinks().get(startLinkId);
				Link endLink = scenario.getNetwork().getLinks().get(endLinkId);
				double distance = CoordUtils.calcDistance(startLink.getCoord(), endLink.getCoord());
				
				if (distance < 2000.0) leg.setMode(TransportMode.walk);
				else if (distance < 5000.0) leg.setMode(TransportMode.bike);
				else leg.setMode(TransportMode.pt);
				
				this.editRoutes.replanFutureLegRoute(leg, plan.getPerson(), scenario.getNetwork(), tripRouter);
				
//				/*
//				 * Create a new route for the given leg.
//				 */
//				Activity previousActivity = (Activity) plan.getPlanElements().get(i - 1);
//				Activity nextActivity = (Activity) plan.getPlanElements().get(i + 1);
//				PlanImpl newPlan = new PlanImpl(plan.getPerson());
//				newPlan.addActivity(previousActivity);
//				newPlan.addLeg(leg);
//				newPlan.addActivity(nextActivity);
//				routingAlgorithm.run(newPlan);
//				
//				/*
//				 * Replace route in existing leg. This is necessary since the router
//				 * creates an entirely new leg and not only replaces the route inside
//				 * the leg.
//				 */
//				if (newPlan.getPlanElements().get(1) != leg) {
//					leg.setRoute(((Leg) newPlan.getPlanElements().get(1)).getRoute());
//				}
			}
		}
	}

	/**
	 * Replace person's car legs with non-car legs.
	 * Legs with a crow fly distance:
	 * <ul>
	 * 	<li>below 2000m become walk legs.</li>
	 * 	<li>between 2000 and 5000m become bike legs.</li>
	 * 	<li>above 5000m become pt legs.</li>
	 * </ul>
	 */
	@Override
	public void run(Person person) {
		this.run(person.getSelectedPlan());
	}
	
	
	private boolean requiresVehicle(Person p) {
		for (PlanElement planElement : p.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Leg) {
				if (((Leg) planElement).getMode().equals(TransportMode.car)) return true;
			}
		}
		return false;
	}
		
	private void assignVehicleToPerson(Person p, Id vehicleId) {
		boolean checkLegModes = false;
		Plan plan = p.getSelectedPlan();
		for (int i = 1; i < plan.getPlanElements().size() - 1; i++) {
			PlanElement planElement = plan.getPlanElements().get(i);
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				String mode = leg.getMode();
				if (mode.equals(TransportMode.car)) {
					assignVehicleToLeg(leg, vehicleId);
				} 
//				else if (mode.equals(TransportMode.pt) || mode.equals(TransportMode.walk) || 
//						mode.equals(TransportMode.ride) || mode.equals(TransportMode.bike)) {
				else if (mode.equals(TransportMode.pt)) {
					/*
					 * Set transport mode to car and create a new route for the given leg.
					 */
					leg.setMode(TransportMode.car);
					
					this.editRoutes.replanFutureLegRoute(leg, plan.getPerson(), scenario.getNetwork(), tripRouter);
					
//					Activity previousActivity = (Activity) plan.getPlanElements().get(i - 1);
//					Activity nextActivity = (Activity) plan.getPlanElements().get(i + 1);
//					
//					PlanImpl newPlan = new PlanImpl(plan.getPerson());
//					newPlan.addActivity(previousActivity);
//					newPlan.addLeg(leg);
//					newPlan.addActivity(nextActivity);
//					routingAlgorithm.run(newPlan);
//					
//					/*
//					 * Replace route in existing leg. This is necessary since the router
//					 * creates an entirely new leg and not only replaces the route inside
//					 * the leg.
//					 */
//					if (newPlan.getPlanElements().get(1) != leg) {
//						leg.setRoute(((Leg) newPlan.getPlanElements().get(1)).getRoute());
//					}
					
					assignVehicleToLeg(leg, vehicleId);
					checkLegModes = true;
					addedCarLegsCounter.incCounter();
				}
			}
		}
		if (checkLegModes) {
			legModeChecker.run(p);
			/*
			 * re-run vehicle assignment since the leg mode checker might have created
			 * some additional car legs.
			 */
			assignVehicleToPerson(p, vehicleId);
		}
	}
	
	private void assignVehicleToLeg(Leg leg, Id vehicleId) {
		Route route = leg.getRoute();
		
		if (route instanceof NetworkRoute) {
			((NetworkRoute) route).setVehicleId(vehicleId);
		}
	}
	
	/*
	 * Compare the length of car-legs in a plan. 
	 */
	private class CarLegsComparator implements Comparator<Person>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;
		private final Network network;
		
		public CarLegsComparator(Network network) {
			this.network = network;
		}
		
		@Override
		public int compare(Person p1, Person p2) {
			double carLegLength1 = 0.0;
			double carLegLength2 = 0.0;

			for (PlanElement planElement : p1.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (leg.getMode().equals(TransportMode.car)) {
						carLegLength1 += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), network);
					}
				}
			}

			for (PlanElement planElement : p2.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (leg.getMode().equals(TransportMode.car)) {
						carLegLength2 += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), network);
					}
				}
			}
			
			if (carLegLength1 < carLegLength2) return -1;
			else if (carLegLength1 > carLegLength2) return 1;
			// if both values are equal, compare persons' Ids: the one with the larger Id should be first
			else {
				return p2.getId().compareTo(p1.getId());
			}
		}		
	}
}
