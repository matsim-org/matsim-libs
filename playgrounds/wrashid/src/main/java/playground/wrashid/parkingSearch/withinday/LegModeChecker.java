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

package playground.wrashid.parkingSearch.withinday;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Checks whether a plan contains car trips. If car trips are found, it is
 * ensured that a car is available for every car trip.
 * 
 * Based on a random number it is decided whether the car or non-car trips are replaced.
 * The maxDistance parameter defines how far a agent is willing to walk to reach its
 * parked car. 
 * 
 * @author cdobler
 */
public class LegModeChecker extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private Scenario scenario;
	private double maxDistance = 2000.0;	// allowed distance between an activity location and the place where the car is parked
	private double toCarProbability = 0.5;
	private Random random = MatsimRandom.getLocalInstance();
	private PlanAlgorithm routingAlgorithm;
	private String[] validNonCarModes = {TransportMode.bike, TransportMode.pt, TransportMode.walk};
	
	public LegModeChecker(Scenario scenario, PlanAlgorithm routingAlgorithm) {
		this.scenario = scenario;
		this.routingAlgorithm = routingAlgorithm;
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
	
	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}
	
	public double getMaxDistance() {
		return this.maxDistance;
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
//		Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
//		boolean isRoundTrip = firstActivity.getFacilityId().equals(lastActivity.getFacilityId());
		
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
				 * the coordinate of the home location of an agent.
				 */
				Coord carCoord = ((MutableScenario) scenario).getActivityFacilities().getFacilities().get(firstActivity.getFacilityId()).getCoord();
				
				/*
				 * Where does the first car trip start?
				 */
				double r = random.nextDouble();
				if (r < toCarProbability) checkLegModes(plan, carCoord, true);
				else checkLegModes(plan, carCoord, false);
				
				/*
				 * Finally ensure that all routes in the plan are valid.
				 */
				for (int i = 1; i < plan.getPlanElements().size() - 2; i = i + 2) {
					
					Leg leg = (Leg) plan.getPlanElements().get(i);
					Activity previousActivity = (Activity) plan.getPlanElements().get(i - 1);
					Activity nextActivity = (Activity) plan.getPlanElements().get(i + 1);
					
					// if the route is null, create a new one
					if (leg.getRoute() == null) {
						
						PlanImpl newPlan = new PlanImpl(plan.getPerson());
						newPlan.addActivity(previousActivity);
						newPlan.addLeg(leg);
						newPlan.addActivity(nextActivity);
						routingAlgorithm.run(newPlan);						
					}
				}
			}
			
			// only car trips, therefore we don't have to adapt the plan
			else return;
		}
	}

	private void checkLegModes(Plan plan, Coord initialCarCoord, boolean toCar) {
		
		Coord carCoord = initialCarCoord;
		
		for (int i = 1; i < plan.getPlanElements().size(); i = i + 2) {
			Leg leg = (Leg) plan.getPlanElements().get(i);
			
			/*
			 * If it is a car leg we have to check whether the car is available
			 * and update the position of the car after the leg.
			 */
			if (leg.getMode().equals(TransportMode.car)) {
				
				Activity previousActivity = (Activity) plan.getPlanElements().get(i - 1);
				Activity nextActivity = (Activity) plan.getPlanElements().get(i + 1);
				
				Coord previousCoord = ((MutableScenario) scenario).getActivityFacilities().getFacilities().get(previousActivity.getFacilityId()).getCoord();
				Coord nextCoord = ((MutableScenario) scenario).getActivityFacilities().getFacilities().get(nextActivity.getFacilityId()).getCoord();
				
				/*
				 * Check the distance to the car. 
				 */
				double distance = CoordUtils.calcEuclideanDistance(carCoord, previousCoord);
				if (distance > maxDistance) {
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
					checkLegModes(plan, initialCarCoord, toCar);
					
				} else {
					carCoord = nextCoord;
				}
			}
		}
	}
}
