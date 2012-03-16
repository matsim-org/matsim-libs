/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.ikaddoura.parkAndRide.strategyTest;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.PtConstants;

public class ParkAndRidePlanStrategyModule implements PlanStrategyModule {
	private static final Logger log = Logger.getLogger(ParkAndRidePlanStrategyModule.class);

	ScenarioImpl sc;
	Network net;
	Population pop;

	public ParkAndRidePlanStrategyModule(Controler controler) {
		this.sc = controler.getScenario();
		this.net = this.sc.getNetwork();
		this.pop = this.sc.getPopulation();
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		if (plan.getPerson().getId().toString().contains("car")) { // checks if car is available
			log.info("Car is available --> Adding park and ride to the plan.");
			
			List<PlanElement> planElements = plan.getPlanElements();
			
			// checks if plan contains already Park and Ride
			boolean hasParkAndRide = false;
			for (int i = 0; i < planElements.size(); i++) {
				PlanElement pe = planElements.get(i);
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (act.toString().contains("parkAndRide")){
						hasParkAndRide = true;
					}
				}
			}
			
			if (hasParkAndRide == false){ // if plan doesn't contain Park and Ride
				
				double xCoord1 = 2500;
				double yCoord1 = 0;
				Coord parkAndRideCoord1 = this.sc.createCoord(xCoord1, yCoord1);
				ActivityImpl parkAndRide1 = new ActivityImpl("parkAndRide", parkAndRideCoord1, new IdImpl("4to5")); 
				parkAndRide1.setMaximumDuration(0.0);
				
				double xCoord2 = 2000;
				double yCoord2 = 0;
				Coord parkAndRideCoord2 = this.sc.createCoord(xCoord2, yCoord2);
				ActivityImpl parkAndRide2 = new ActivityImpl("parkAndRide", parkAndRideCoord2, new IdImpl("5to4")); 
				parkAndRide2.setMaximumDuration(0.0);

				// splits first Leg after homeActivity into carLeg - parkAndRideActivity - ptLeg
				for (int i = 0; i < planElements.size(); i++) {
					PlanElement pe = planElements.get(i);
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (act.getType().equals("home") && i==0){
							planElements.remove(1);
							planElements.add(1, pop.getFactory().createLeg(TransportMode.car));
							planElements.add(2, parkAndRide1);
							planElements.add(3, pop.getFactory().createLeg(TransportMode.pt));
						}
					}
				}
				
				// splits first Leg before homeActivity into ptLeg - parkAndRideActivity - carLeg
				int size = planElements.size();
				for (int i = 0; i < size; i++) {
					PlanElement pe = planElements.get(i);
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (act.getType().equals("home") && i==planElements.size()-1) {
							planElements.remove(size-2);
							planElements.add(size-2, pop.getFactory().createLeg(TransportMode.car));
							planElements.add(size-2, parkAndRide2);
							planElements.add(size-2, pop.getFactory().createLeg(TransportMode.pt));	
						}
					} 
				}
				
				// change all carLegs between parkAndRideActivities to ptLegs
				List<Integer> parkAndRidePlanElementIndex = new ArrayList<Integer>();
				for (int i = 0; i < planElements.size(); i++) {
					PlanElement pe = planElements.get(i);
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (act.getType().toString().equals("parkAndRide")){
							parkAndRidePlanElementIndex.add(i);
						}
					}
				}
				if (parkAndRidePlanElementIndex.size() > 2) throw new RuntimeException("More than two ParkAndRideActivities, don't know what's happening...");
				for (int i = 0; i < planElements.size(); i++) {
					PlanElement pe = planElements.get(i);
					if (i>parkAndRidePlanElementIndex.get(0) && i < parkAndRidePlanElementIndex.get(1)){
						if (pe instanceof Leg){
							Leg leg = (Leg) pe;
							if (TransportMode.car.equals(leg.getMode())){
								leg.setMode(TransportMode.pt);
							}
						}
					}
				}
				
			}
			else {
				log.info("Plan contains already a parkAndRide Activity.");
			}
			
		}
		else {
			log.info("Person has no car.");
		}
	}

	@Override
	public void prepareReplanning() {
	}
	
}
