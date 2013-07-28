/* *********************************************************************** *
 * project: org.matsim.*
 * LegModeChecker.java
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

package playground.christoph.evacuation.mobsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.utils.EditRoutes;

/**
 * Checks whether a plan contains car trips. If car trips are found, it is
 * ensured that a car is available for every car trip.
 * 
 * Based on a random number it is decided whether the car or non-car trips are replaced.
 * The maxDistance parameter defines how far a agent is willing to walk to reach its
 * parked car.
 * 
 * It is assumed that cars are parked at an agents home location at the beginning of each day.
 * 
 * @author cdobler
 */
public class LegModeChecker extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private final TripRouter tripRouter;
	private final Network network;
	private final Counter counter;
	private final EditRoutes editRoutes;
	
	private double toCarProbability = 0.5;
	private Random random = MatsimRandom.getLocalInstance();
	private String[] validNonCarModes = {TransportMode.bike, TransportMode.pt, TransportMode.walk};
	
	public LegModeChecker(TripRouter tripRouter, Network network) {
		this.tripRouter = tripRouter;
		this.network = network;
		
		this.counter = new Counter("Adapted mode chains: ");
		this.editRoutes = new EditRoutes();
	}

	public void setValidNonCarModes(String[] validNonCarModes) {
		this.validNonCarModes = validNonCarModes;
	}
	
	public String[] getValidNonCarModes() {
		return this.validNonCarModes;
	}
	
	public void setToCarProbability(double toCarProbability) {
		this.toCarProbability = toCarProbability;
	}
	
	public double getToCarProbability() {
		return this.toCarProbability;
	}
	
	public void printStatistics() {
		counter.printCounter();
	}
	
	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) this.run(plan);
	}
	
	@Override
	public void run(Plan plan) {
		int firstCarTrip = -1;
		int lastCarTrip = -1;
		int firstNonCarTrip = -1;
		int lastNonCarTrip = -1;
		boolean hasCarTrip = false;
		boolean hasNonCarTrips = false;
		
		Activity firstActivity = (Activity) plan.getPlanElements().get(0);
		
		List<String> legModes = new ArrayList<String>();
		
		int index = 0;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				legModes.add(leg.getMode());
				if (leg.getMode().equals(TransportMode.car)) {
					hasCarTrip = true;
					
					if (firstCarTrip < 0) firstCarTrip = index;
					lastCarTrip = index;
				} else {
					hasNonCarTrips = true;
					
					if (firstCarTrip < 0) firstNonCarTrip = index;
					lastNonCarTrip = index;
				}
			}
			index++;
		}
		
		if (hasCarTrip) {
			
			if (hasNonCarTrips) {
				// he have to check...

				/*
				 * First n-trips are car trips - nothing has to be changed.
				 */
				if (firstCarTrip == 0 && lastCarTrip < firstNonCarTrip) return;
				
				/*
				 * Get position of the car. We assume that the car is located at
				 * the link of the agent's home facility.
				 */
				Id homeLinkId = firstActivity.getLinkId();
				
				/*
				 * Where does the first car trip start?
				 */
				double r = random.nextDouble();
				if (r < toCarProbability) checkLegModes(plan, homeLinkId, true);
				else checkLegModes(plan, homeLinkId, false);
				
				/*
				 * Finally ensure that all routes in the plan are valid.
				 */
				boolean adapted = false;
				for (int i = 1; i < plan.getPlanElements().size() - 2; i = i + 2) {
					
					Leg leg = (Leg) plan.getPlanElements().get(i);
//					Activity previousActivity = (Activity) plan.getPlanElements().get(i - 1);
//					Activity nextActivity = (Activity) plan.getPlanElements().get(i + 1);
					
					// if the route is null, create a new one
					if (leg.getRoute() == null) {
						adapted = true;
						initRoute(plan, i);
						this.editRoutes.replanFutureLegRoute(leg, plan.getPerson(), network, tripRouter);
					}
				}
				if (adapted) counter.incCounter();
			}
			
			// only car trips, therefore we don't have to adapt the plan
			else return;
		}
	}
	
	private void initRoute(Plan plan, int legIndex) {
		Leg leg = (Leg) plan.getPlanElements().get(legIndex);
		
		Activity previousActivity = null;
		Activity nextActivity = null;
		
		for (int i = legIndex; i >= 0; i--) {
			PlanElement planElement = plan.getPlanElements().get(i);
			if (planElement instanceof Activity) {
				previousActivity = (Activity) planElement;
				break;
			}
		}
		for (int i = legIndex; i < plan.getPlanElements().size(); i++) {
			PlanElement planElement = plan.getPlanElements().get(i);
			if (planElement instanceof Activity) {
				nextActivity = (Activity) planElement;
				break;
			}
		}
		
		/*
		 * We can use a GenericRoute since only its start and end link ids are 
		 * required before it is replaced.
		 */
		leg.setRoute(new GenericRouteImpl(previousActivity.getLinkId(), nextActivity.getLinkId()));
	}

	private void checkLegModes(Plan plan, Id initialLinkId, boolean toCar) {
		
		Id carLinkId = initialLinkId;
		
		for (int i = 1; i < plan.getPlanElements().size(); i = i + 2) {
			Leg leg = (Leg) plan.getPlanElements().get(i);
			
			/*
			 * If it is a car leg we have to check whether the car is available
			 * and update the position of the car after the leg.
			 */
			if (leg.getMode().equals(TransportMode.car)) {
				
				Activity previousActivity = (Activity) plan.getPlanElements().get(i - 1);
				Activity nextActivity = (Activity) plan.getPlanElements().get(i + 1);
				
				Id previousLinkId = previousActivity.getLinkId();
				Id nextLinkId = nextActivity.getLinkId();
				
				/*
				 * Check whether the car is located at the link of the leg's previous activity.
				 * If not, the plan has to be adapted.
				 */
				if (!carLinkId.equals(previousLinkId)) {
					/* 
					 * Idea:
					 * Use a boolean option per plan - toCar/toNonCar.
					 * If toCar -> Recursively set the mode of the previous leg to car
					 * Otherwise -> set mode of the current leg to nonCarMode
					 */
					Leg previousLeg = (Leg) plan.getPlanElements().get(i - 2);
					if (toCar) {
						previousLeg.setMode(TransportMode.car);
					} else {
						int modeType = random.nextInt(validNonCarModes.length);
						leg.setMode(validNonCarModes[modeType]);
					}
					previousLeg.setRoute(null);
					
					/*
					 * Recursively call this method until the plan is valid.
					 */
					checkLegModes(plan, initialLinkId, toCar);
					
				} else {
					carLinkId = nextLinkId;
				}
			}
		}
	}
}
