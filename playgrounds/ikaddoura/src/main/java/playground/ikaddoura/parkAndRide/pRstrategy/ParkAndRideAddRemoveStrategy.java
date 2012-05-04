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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
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
public class ParkAndRideAddRemoveStrategy implements PlanStrategyModule {
	private static final Logger log = Logger.getLogger(ParkAndRideAddRemoveStrategy.class);

	ScenarioImpl sc;
	Network net;
	Population pop;
	private List<ParkAndRideFacility> prFacilities = new ArrayList<ParkAndRideFacility>();

	public ParkAndRideAddRemoveStrategy(Controler controler, List<ParkAndRideFacility> prFacilities) {
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
		if (plan.getPerson().getId().toString().contains("car")) { // checks if car is available
			
			log.info("Car is available. Park and Ride is possible.");
			
			List<PlanElement> planElements = plan.getPlanElements();
			List<Integer> planElementIndex = new ArrayList<Integer>();
			boolean hasParkAndRide = false;
	
			for (int i = 0; i < planElements.size(); i++) {
				PlanElement pe = planElements.get(i);
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (act.toString().contains(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
						hasParkAndRide = true;
						planElementIndex.add(i);
					}
				}
			}
			
			if (hasParkAndRide == false){
				log.info("Plan doesn't contain Park and Ride. Adding Park and Ride...");

				// erstelle ParkAndRideActivity (zufällige Auswahl einer linkID aus den eingelesenen P+R-LinkIDs bzw. der prFacilities)
				Activity parkAndRide = createParkAndRideActivity(Math.random());

				// splits first Leg after homeActivity into carLeg - parkAndRideActivity - ptLeg
				for (int i = 0; i < planElements.size(); i++) {
					PlanElement pe = planElements.get(i);
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (act.getType().equals("home") && i==0){
							planElements.remove(1);
							planElements.add(1, pop.getFactory().createLeg(TransportMode.car));
							planElements.add(2, parkAndRide);
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
							planElements.add(size-2, parkAndRide);
							planElements.add(size-2, pop.getFactory().createLeg(TransportMode.pt));	
						}
					} 
				}
				
				// change all carLegs between parkAndRideActivities to ptLegs
				List <Integer> parkAndRidePlanElementIndex = getPlanElementIndex(planElements);
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
		}
		else {
			log.info("Person has no car. Park and Ride is not possible.");
			// do nothing!
		}
	}
	
	private Activity createParkAndRideActivity(double random) {
		
		int max = this.prFacilities.size();
	    int rndInt = (int) (random * max);
		Id rndLinkId = this.prFacilities.get(rndInt).getPrLink3in();
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
