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

package playground.vsp.parkAndRide.replanning;

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
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.parkAndRide.PRConstants;
import playground.vsp.parkAndRide.PRFacility;


/**
 * 
 * This strategy module adds park-and-ride to a randomly chosen home-work-home sequence.
 * 
 * @author ikaddoura
 *
 */
public class PRAddStrategyMod implements PlanStrategyModule {
	private static final Logger log = Logger.getLogger(PRAddStrategyMod.class);

	private MutableScenario sc;
	private Network net;
	private Population pop;
	private Map<Id<PRFacility>, PRFacility> id2prFacility = new HashMap<>();
	private int nrOfPrFacilitiesForReplanning = 0; // 0 means all P+R-Facilities are used for replanning
	private double gravity;
	private double typicalDuration;
	
	/**
	 * 
	 * @param controler
	 * @param id2prFacility
	 * @param gravity
	 * @param typicalDuration 
	 */
	public PRAddStrategyMod(MatsimServices controler, Map<Id<PRFacility>, PRFacility> id2prFacility, double gravity, double typicalDuration) {
		this.sc = (MutableScenario) controler.getScenario();
		this.net = this.sc.getNetwork();
		this.pop = this.sc.getPopulation();
		this.id2prFacility = id2prFacility;
		this.gravity = gravity;
		this.typicalDuration = typicalDuration;
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		
			List<PlanElement> planElements = plan.getPlanElements();
			
			PlanIndicesAnalyzer planAnalyzer = new PlanIndicesAnalyzer(plan);
			planAnalyzer.setIndices();
			
			boolean hasHomeActivity = planAnalyzer.hasHomeActivity();
			boolean hasWorkActivity = planAnalyzer.hasWorkActivity();
			
			if (hasHomeActivity == true && hasWorkActivity == true) {
				log.info("Plan contains Home and Work Activity. Proceeding...");

					final int rndWork = (int)(MatsimRandom.getRandom().nextDouble() * planAnalyzer.getWorkActs().size());
					log.info("PlanElements of possible Work Activities: " + planAnalyzer.getWorkActs() + ". Creating Park'n'Ride before and after Work Activity " + planAnalyzer.getWorkActs().get(rndWork) + "...");
					
					int workIndex;
					int maxHomeBeforeWork;
					int minHomeAfterWork;

					// Check if home-work-home sequence contains park-and-ride
					
					planAnalyzer.setIndices();
					
					workIndex = planAnalyzer.getWorkActs().get(rndWork);					
					minHomeAfterWork = planAnalyzer.getMinHomeAfterWork(workIndex);
					maxHomeBeforeWork = planAnalyzer.getMaxHomeBeforeWork(workIndex);

					List<Integer> indicesPRact = new ArrayList<Integer>();
					for (Integer prIndex : planAnalyzer.getPrActs()){
						if (prIndex > maxHomeBeforeWork && prIndex < minHomeAfterWork){
							indicesPRact.add(prIndex);
						}
					}
					
					if (indicesPRact.size() > 2) throw new RuntimeException("More than two ParkAndRide Activities in this Home-Work-Home Sequence. Aborting...");
					
					if (indicesPRact.isEmpty()){
						
						log.info("Home-Work-Home sequence doesn't contain Park'n'Ride. Adding Park'n'Ride to this Home-Work-Home sequence...");
						
						// create ParkAndRideActivity
						planAnalyzer.setIndices();
						Activity parkAndRide = createParkAndRideActivity(planAnalyzer, planAnalyzer.getWorkActs().get(rndWork), plan);
						
						// First P+R activity //////////////////////////////
						planAnalyzer.setIndices();
						workIndex = planAnalyzer.getWorkActs().get(rndWork);
						maxHomeBeforeWork = planAnalyzer.getMaxHomeBeforeWork(workIndex);

						int transformIntoPRLeg1 = 0;

						if ((workIndex - maxHomeBeforeWork) == 2){
							// no activities between home and work
							transformIntoPRLeg1 = maxHomeBeforeWork + 1;
						} else {
							// activities between home and work, randomly choose a leg
							List<Integer> legIndex = new ArrayList<Integer>();
							for (Integer actIndex : planAnalyzer.getAllActs()){
								if (actIndex > maxHomeBeforeWork && actIndex < workIndex){
									if (legIndex.contains(actIndex - 1)){
										// don't add!
									} else {
										legIndex.add(actIndex - 1); // before that activity
									}
									
									if (legIndex.contains(actIndex + 1)) {
										// don't add!
									} else {
										legIndex.add(actIndex + 1); // after that activity
									}
								}
							}
							int index = (int)(MatsimRandom.getRandom().nextDouble()*legIndex.size());
							transformIntoPRLeg1 = legIndex.get(index);
						}
//						System.out.println("Leg Index to be transformed into P+R (1): " + transformIntoPRLeg1);

						planElements.remove(transformIntoPRLeg1);
						planElements.add(transformIntoPRLeg1, pop.getFactory().createLeg(TransportMode.car));
						planElements.add(transformIntoPRLeg1 + 1, parkAndRide);
						planElements.add(transformIntoPRLeg1 + 2, pop.getFactory().createLeg(TransportMode.pt));
						
						// Second P+R activity /////////////////////////////////
						planAnalyzer.setIndices();
						workIndex = planAnalyzer.getWorkActs().get(rndWork);						
						minHomeAfterWork = planAnalyzer.getMinHomeAfterWork(workIndex);
												
						int transformIntoPRLeg2 = 0;
						if ((minHomeAfterWork - workIndex) == 2){
							// no activities between home and work
							transformIntoPRLeg2 = minHomeAfterWork - 1;
						} else {
							// activities between home and work, randomly choose a leg
							List<Integer> legIndex = new ArrayList<Integer>();
							for (Integer actIndex : planAnalyzer.getAllActs()){
								if (actIndex < minHomeAfterWork && actIndex > workIndex){
									if (legIndex.contains(actIndex - 1)){
										// don't add!
									} else {
										legIndex.add(actIndex - 1); // before that activity
									}
									
									if (legIndex.contains(actIndex + 1)) {
										// don't add!
									} else {
										legIndex.add(actIndex + 1); // after that activity
									}
								}
							}
							int index = (int)(MatsimRandom.getRandom().nextDouble()*legIndex.size());
							System.out.println("Possible legs: " + legIndex);
							transformIntoPRLeg2 = legIndex.get(index);
						}
//						System.out.println("Leg Index to be transformed into P+R (2): " + transformIntoPRLeg2);

						planElements.remove(transformIntoPRLeg2);
						planElements.add(transformIntoPRLeg2, pop.getFactory().createLeg(TransportMode.pt));
						planElements.add(transformIntoPRLeg2 + 1, parkAndRide);
						planElements.add(transformIntoPRLeg2 + 2, pop.getFactory().createLeg(TransportMode.car));
						
						// adjust leg modes
						planAnalyzer.setIndices();
						
						workIndex = planAnalyzer.getWorkActs().get(rndWork);						
						minHomeAfterWork = planAnalyzer.getMinHomeAfterWork(workIndex);
						maxHomeBeforeWork = planAnalyzer.getMaxHomeBeforeWork(workIndex);

						List<Integer> prIndices = new ArrayList<Integer>();
						for (Integer prIndex : planAnalyzer.getPrActs()){
							if (prIndex > maxHomeBeforeWork && prIndex < minHomeAfterWork){
								prIndices.add(prIndex);
							}
						}
						
						if (prIndices.size() > 2) throw new RuntimeException("More than two ParkAndRide Activities in this Home-Work-Home Sequence. Aborting...");
						
						for (int i = 0; i < planElements.size(); i++) {
							PlanElement pe = planElements.get(i);					
							if (i > prIndices.get(0) && i < prIndices.get(1)) {
								// trips between first P+R and second P+R --> pt if car, otherwise don't modify legMode
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
					log.info("Home-Work-Home sequence contains a ParkAndRide Activity. Not modifying the plan...");
				}
						
			} else {
				if (hasWorkActivity == false){
					log.info("Plan doesn't contain Work Activity. Not modifying the plan...");
				} else if (hasHomeActivity == false){
					log.info("Plan doesn't contain Home Activity. Not modifiying the plan...");
				}
			}	
	}
	
	private Activity createParkAndRideActivity(PlanIndicesAnalyzer planIndices, Integer workIndex, Plan plan) {
		List<PRWeight> prWeights;
		EllipseSearch ellipseSearch = new EllipseSearch();
		
		Activity firstHomeAct = (Activity) plan.getPlanElements().get(planIndices.getHomeActs().get(0)); // assuming that if an agent has more than one home activity, it always have the same coordinates...
		Activity workAct = (Activity) plan.getPlanElements().get(workIndex);
		log.info("Create Park'n'Ride Activity for planElements (homeIndex: " + planIndices.getHomeActs().get(0) + " / workIndex: " + workIndex +"): " + firstHomeAct.getCoord() + " / " + workAct.getCoord());
		log.info("Calculating Weights...");
		prWeights = ellipseSearch.getPrWeights(this.nrOfPrFacilitiesForReplanning, this.net, this.id2prFacility, firstHomeAct.getCoord(), workAct.getCoord(), this.gravity);
		log.info("Choose ParkAndRide Facility depending on weight...");
		Link rndPrLink = ellipseSearch.getRndPrLink(this.net, this.id2prFacility, prWeights);
		
		Activity parkAndRide = PopulationUtils.createActivityFromCoordAndLinkId(PRConstants.PARKANDRIDE_ACTIVITY_TYPE, rndPrLink.getToNode().getCoord(), rndPrLink.getId()); 
		parkAndRide.setMaximumDuration(this.typicalDuration);
		
		return parkAndRide;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub
		
	}
	
}
