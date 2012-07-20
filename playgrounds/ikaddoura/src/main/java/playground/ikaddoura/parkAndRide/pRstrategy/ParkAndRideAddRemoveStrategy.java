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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.core.gbl.MatsimRandom;
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

	private ScenarioImpl sc;
	private Network net;
	private Population pop;
	private Map<Id, ParkAndRideFacility> id2prFacility = new HashMap<Id, ParkAndRideFacility>();
	private Map<Id, List<PrWeight>> personId2prWeights = new HashMap<Id, List<PrWeight>>();
	private int nrOfPrFacilitiesForReplanning = 0; // 0 means all P+R-Facilities are used for replanning
	
	public ParkAndRideAddRemoveStrategy(Controler controler, Map<Id, ParkAndRideFacility> id2prFacility, Map<Id, List<PrWeight>> personId2prWeights) {
		this.sc = controler.getScenario();
		this.net = this.sc.getNetwork();
		this.pop = this.sc.getPopulation();
		this.id2prFacility = id2prFacility;
		this.personId2prWeights = personId2prWeights;
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		
			List<PlanElement> planElements = plan.getPlanElements();
			
			PlanIndicesAnalyzer planIndices = new PlanIndicesAnalyzer(plan);
			planIndices.setIndices();
			
			boolean hasParkAndRide = planIndices.hasParkAndRide();
			boolean hasHomeActivity = planIndices.hasHomeActivity();
			boolean hasWorkActivity = planIndices.hasWorkActivity();
			
			if (hasHomeActivity == true && hasWorkActivity == true) {
				log.info("Plan contains Home and Work Activity. Proceeding...");

				if (hasParkAndRide == false){
					log.info("Plan doesn't contain ParkAndRide. Adding ParkAndRide...");

					// create ParkAndRideActivity (zufällige Auswahl einer linkID aus den eingelesenen P+R-LinkIDs bzw. der prFacilities)
					Activity parkAndRide = createParkAndRideActivity(plan);
					
					// First P+R //////////////////////////////
//					System.out.println("First P+R Leg...");

					planIndices.setIndices();

//					System.out.println("work " + planIndices.getFirstWorkIndex());
//					System.out.println("home " + planIndices.getHomeIndexBeforeWorkActivity());

					int transformIntoPRLeg1 = 0;

					if ((planIndices.getFirstWorkIndex() - planIndices.getHomeIndexBeforeWorkActivity()) == 2){
						// no activities between home and work
						transformIntoPRLeg1 = planIndices.getHomeIndexBeforeWorkActivity() + 1;
					} else {
						// activities between home and work, randomly chose a leg
						List<Integer> legIndex = new ArrayList<Integer>();
						for (Integer actIndex : planIndices.getActsplanElementIndex()){
							if (actIndex > planIndices.getHomeIndexBeforeWorkActivity() && actIndex < planIndices.getFirstWorkIndex()){
								legIndex.add(actIndex - 1); // before that activity
								legIndex.add(actIndex + 1); // after that activity
							}
						}
						int index = (int)(MatsimRandom.getRandom().nextDouble()*legIndex.size());
						transformIntoPRLeg1 = legIndex.get(index);
					}
//					System.out.println("Leg Index to be transformed into P+R (1): " + transformIntoPRLeg1);

					planElements.remove(transformIntoPRLeg1);
					planElements.add(transformIntoPRLeg1, pop.getFactory().createLeg(TransportMode.car));
					planElements.add(transformIntoPRLeg1 + 1, parkAndRide);
					planElements.add(transformIntoPRLeg1 + 2, pop.getFactory().createLeg(TransportMode.pt));
					
					// Second P+R ////////////////////////////////////////////////////////
//					System.out.println("Second P+R Leg...");

					planIndices.setIndices();
//					System.out.println("work " + planIndices.getLastWorkIndex());
//					System.out.println("home: " + planIndices.getHomeIndexAfterWorkActivity());
					
					int transformIntoPRLeg2 = 0;
					if ((planIndices.getHomeIndexAfterWorkActivity() - planIndices.getLastWorkIndex()) == 2){
						// no activities between home and work
						transformIntoPRLeg2 = planIndices.getHomeIndexAfterWorkActivity() - 1;
					} else {
						// activities between home and work, randomly chose a leg
						List<Integer> legIndex = new ArrayList<Integer>();
						for (Integer actIndex : planIndices.getActsplanElementIndex()){
							if (actIndex < planIndices.getHomeIndexAfterWorkActivity() && actIndex > planIndices.getLastWorkIndex()){
								legIndex.add(actIndex - 1); // before that activity
								legIndex.add(actIndex + 1); // after that activity
							}
						}
						int index = (int)(MatsimRandom.getRandom().nextDouble()*legIndex.size());
						transformIntoPRLeg2 = legIndex.get(index);
					}
//					System.out.println("Leg Index to be transformed into P+R (2): " + transformIntoPRLeg2);

					planElements.remove(transformIntoPRLeg2);
					planElements.add(transformIntoPRLeg2, pop.getFactory().createLeg(TransportMode.pt));
					planElements.add(transformIntoPRLeg2 + 1, parkAndRide);
					planElements.add(transformIntoPRLeg2 + 2, pop.getFactory().createLeg(TransportMode.car));
					
					// adjust leg modes
					planIndices.setIndices();
					
					if (planIndices.getpRplanElementIndex().size() > 2) throw new RuntimeException("More than two ParkAndRide Activities, don't know what's happening...");
					
					for (int i = 0; i < planElements.size(); i++) {
						PlanElement pe = planElements.get(i);
						if (i > planIndices.getHomeIndexBeforeWorkActivity() && i < planIndices.getpRplanElementIndex().get(0)) {
							// car!
							if (pe instanceof Leg){
								Leg leg = (Leg) pe;
								if (TransportMode.car.equals(leg.getMode())){
									leg.setMode(TransportMode.car);
								}
							}
						} else if (i > planIndices.getpRplanElementIndex().get(0) && i < planIndices.getpRplanElementIndex().get(1)) {
							// pt!
							if (pe instanceof Leg){
								Leg leg = (Leg) pe;
								if (TransportMode.car.equals(leg.getMode())){
									leg.setMode(TransportMode.pt);
								}
							}
						} else if (i > planIndices.getpRplanElementIndex().get(1) && i < planIndices.getHomeIndexAfterWorkActivity()) {
							// car!
							if (pe instanceof Leg){
								Leg leg = (Leg) pe;
								if (TransportMode.car.equals(leg.getMode())){
									leg.setMode(TransportMode.car);
								}
							}
						}
					}
					
				}
				else {
					log.info("Plan contains a ParkAndRide Activity. Removing the ParkAndRide Activity and the belonging pt Leg...");
										
					if (planIndices.getpRplanElementIndex().size() > 2) throw new RuntimeException("More than two ParkAndRide Activities, don't know what's happening...");
					 
					for (int i = 0; i < planElements.size(); i++) {
						if (i==planIndices.getpRplanElementIndex().get(0)){
							planElements.remove(i); // first Park and Ride Activity
							planElements.remove(i); // following ptLeg
						}
						else if (i==planIndices.getpRplanElementIndex().get(1)){
							planElements.remove(i-2); // second Park and Ride Activity
							planElements.remove(i-3); // ptLeg before
						}
					}
				}
			} else {
				log.info("Plan doesn't contain Home or Work Activity.");
				if (hasWorkActivity == false){
					log.warn("Plan doesn't contain Work Activity. This should not be possible. Not adding Park'n'Ride...");
				} else if (hasHomeActivity == false){
					log.info("Plan doesn't contain Home Activity. Not adding Park'n'Ride...");
				}
			}	
	}
	
	private Activity createParkAndRideActivity(Plan plan) {
		List<PrWeight> prWeights;
		EllipseSearch ellipseSearch = new EllipseSearch();

		if (this.personId2prWeights.get(plan.getPerson().getId()) == null){
			log.info("Weights for ParkAndRide Facilities for person " + plan.getPerson().getId().toString() + " not calculated before. Calculate Weights...");
			prWeights = ellipseSearch.getPrWeights(this.nrOfPrFacilitiesForReplanning, this.net, this.id2prFacility, plan);
			this.personId2prWeights.put(plan.getPerson().getId(), prWeights);
		} else {
			log.info("Weights for ParkAndRide Facilities for person " + plan.getPerson().getId().toString() + " already calculated before.");
			prWeights = this.personId2prWeights.get(plan.getPerson().getId());
		}
		
		log.info("Chose ParkAndRide Facility depending on weight...");
		Link rndPrLink = ellipseSearch.getRndPrLink(this.net, this.id2prFacility, prWeights);
		
		Activity parkAndRide = new ActivityImpl(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE, rndPrLink.getToNode().getCoord(), rndPrLink.getId()); 
		parkAndRide.setMaximumDuration(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_DURATION);
		
		return parkAndRide;
	}

	@Override
	public void prepareReplanning() {
	}
	
}
