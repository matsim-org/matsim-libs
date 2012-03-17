/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideRemoverStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.strategyTest;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;

/**
 * @author Ihab
 *
 */
public class ParkAndRideRemoverStrategy implements PlanStrategyModule {
	private static final Logger log = Logger.getLogger(ParkAndRideRemoverStrategy.class);

	public ParkAndRideRemoverStrategy(Controler controler) {
	}

	@Override
	public void prepareReplanning() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlePlan(Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();		
		List<Integer> planElementIndex = new ArrayList<Integer>();
		
		for (int i = 0; i < planElements.size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (act.toString().contains(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
					planElementIndex.add(i);
				}
			}
		}
		
		if (!planElementIndex.isEmpty()){
	
			log.info("Plan contains a parkAndRide Activity. Removing the ParkAndRide Activity and the belonging ptLeg...");
						
			if (planElementIndex.size() > 2) throw new RuntimeException("More than two ParkAndRideActivities, don't know what's happening...");
			 
			for (int i = 0; i < planElements.size(); i++) {
				if (i==planElementIndex.get(0)){
					planElements.remove(i); // first Park and Ride Activity
					planElements.remove(i); // following ptLeg
				}
				else if (i==planElementIndex.get(1)){
					planElements.remove(i-2); // second Park and Ride Activity
					planElements.remove(i-3); // ptLeg before
				}
			}
		}
		else {
			// Plan has no Park And Ride
		}
	
	}

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub

	}

}
