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

package playground.ikaddoura.parkAndRide.pRstrategy;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideConstants;
import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */
public class ParkAndRideChangeLocationStrategy implements PlanStrategyModule {
	private static final Logger log = Logger.getLogger(ParkAndRideChangeLocationStrategy.class);

	ScenarioImpl sc;
	Network net;
	Population pop;
	private List<ParkAndRideFacility> prFacilities = new ArrayList<ParkAndRideFacility>();

	public ParkAndRideChangeLocationStrategy(Controler controler, List<ParkAndRideFacility> prFacilities) {
		this.sc = controler.getScenario();
		this.net = this.sc.getNetwork();
		this.pop = this.sc.getPopulation();
		this.prFacilities = prFacilities;

	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
			
			List<PlanElement> planElements = plan.getPlanElements();
			boolean hasParkAndRide = false;
			
			for (int i = 0; i < planElements.size(); i++) {
				PlanElement pe = planElements.get(i);
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (act.toString().contains(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
						hasParkAndRide = true;
					}
				}
			}
			
			if (hasParkAndRide == false){
				log.info("Plan doesn't contain Park and Ride.");
			}
			else {
				log.info("Plan contains Park and Ride. Changing the Park and Ride Location...");
							
				List<Integer> planElementIndex = getPlanElementIndex(planElements);
				if (planElementIndex.size() > 2) throw new RuntimeException("More than two ParkAndRideActivities, don't know what's happening...");
				
				Activity parkAndRide = createParkAndRideActivity(Math.random());

				for (int i = 0; i < planElements.size(); i++) {
					if (i==planElementIndex.get(0)){ // first Park and Ride Activity
						planElements.set(i, parkAndRide);
					}
					else if (i==planElementIndex.get(1)){ // second Park and Ride Activity
						planElements.set(i, parkAndRide);
					}
				}
			}
	}
	
	private Activity createParkAndRideActivity(double random) {

		int max = this.prFacilities.size();
	    int rndInt = (int) (random * max);
		Id rndLinkId = this.prFacilities.get(rndInt).getPrLink2in();
		Link rndParkAndRideLink = this.net.getLinks().get(rndLinkId);
		
		Activity parkAndRide = new ActivityImpl(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE, rndParkAndRideLink.getToNode().getCoord(), rndLinkId); 
		parkAndRide.setMaximumDuration(120.0);
		
		return parkAndRide;
	}

	private List<Integer> getPlanElementIndex(List<PlanElement> planElements) {
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < planElements.size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (act.getType().toString().equals(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
					list.add(i);
				}
			}
		}
		return list;
	}

	@Override
	public void prepareReplanning() {
	}
	
}
