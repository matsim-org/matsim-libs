///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2011 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.ikaddoura.parkAndRide.pRstrategy;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Activity;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.PlanElement;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.api.core.v01.replanning.PlanStrategyModule;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.population.ActivityImpl;
//import org.matsim.core.scenario.ScenarioImpl;
//
//import playground.ikaddoura.parkAndRide.pR.ParkAndRideConstants;
//import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;
//
///**
// * @author Ihab
// *
// */
//public class ParkAndRideChangeLocationStrategy implements PlanStrategyModule {
//	private static final Logger log = Logger.getLogger(ParkAndRideChangeLocationStrategy.class);
//
//	private ScenarioImpl sc;
//	private Network net;
//	private Population pop;
//	private Map<Id, ParkAndRideFacility> id2prFacility = new HashMap<Id, ParkAndRideFacility>();
//	private Map<Id, List<PrWeight>> personId2prWeights = new HashMap<Id, List<PrWeight>>();
//	private int nrOfPrFacilitiesForReplanning = 0; // 0 means all P+R-Facilities are used for replanning
//
//
//	public ParkAndRideChangeLocationStrategy(Controler controler, Map<Id, ParkAndRideFacility> id2prFacility, Map<Id, List<PrWeight>> personId2prWeights) {
//		this.sc = controler.getScenario();
//		this.net = this.sc.getNetwork();
//		this.pop = this.sc.getPopulation();
//		this.id2prFacility = id2prFacility;
//		this.personId2prWeights = personId2prWeights;
//	}
//
//	@Override
//	public void finishReplanning() {
//	}
//
//	@Override
//	public void handlePlan(Plan plan) {
//			
//			List<PlanElement> planElements = plan.getPlanElements();
//			boolean hasParkAndRide = false;
//			
//			for (int i = 0; i < planElements.size(); i++) {
//				PlanElement pe = planElements.get(i);
//				if (pe instanceof Activity) {
//					Activity act = (Activity) pe;
//					if (act.toString().contains(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
//						hasParkAndRide = true;
//					}
//				}
//			}
//			
//			if (hasParkAndRide == false){
//				log.info("Plan doesn't contain ParkAndRide.");
//			}
//			else {
//				log.info("Plan contains ParkAndRide. Changing the ParkAndRide Location...");
//							
//				List<Integer> planElementIndex = getPlanElementIndex(planElements);
//				if (planElementIndex.size() > 2) throw new RuntimeException("More than two ParkAndRide Activities, can't interpret this. Aborting...");
//				
//				Activity parkAndRide = createParkAndRideActivity(plan);
//
//				for (int i = 0; i < planElements.size(); i++) {
//					if (i==planElementIndex.get(0)){ // first Park and Ride Activity
//						planElements.set(i, parkAndRide);
//					}
//					else if (i==planElementIndex.get(1)){ // second Park and Ride Activity
//						planElements.set(i, parkAndRide);
//					}
//				}
//			}
//	}
//	
//	private Activity createParkAndRideActivity(Plan plan) {
//		List<PrWeight> prWeights;
//		EllipseSearch ellipseSearch = new EllipseSearch();
//
//		if (this.personId2prWeights.get(plan.getPerson().getId()) == null){
//			log.info("Weights for ParkAndRide Facilities for person " + plan.getPerson().getId().toString() + " not calculated before. Calculate Weights...");
//			prWeights = ellipseSearch.getPrWeights(this.nrOfPrFacilitiesForReplanning, this.net, this.id2prFacility, plan);
//			this.personId2prWeights.put(plan.getPerson().getId(), prWeights);
//		} else {
//			log.info("Weights for ParkAndRide Facilities for person " + plan.getPerson().getId().toString() + " already calculated before.");
//			prWeights = this.personId2prWeights.get(plan.getPerson().getId());
//		}
//		
//		log.info("Chose ParkAndRide Facility depending on weight...");
//		Link rndPrLink = ellipseSearch.getRndPrLink(this.net, this.id2prFacility, prWeights);
//		
//		Activity parkAndRide = new ActivityImpl(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE, rndPrLink.getToNode().getCoord(), rndPrLink.getId()); 
//		parkAndRide.setMaximumDuration(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_DURATION);
//		
//		return parkAndRide;
//	}
//
//	private List<Integer> getPlanElementIndex(List<PlanElement> planElements) {
//		List<Integer> list = new ArrayList<Integer>();
//		for (int i = 0; i < planElements.size(); i++) {
//			PlanElement pe = planElements.get(i);
//			if (pe instanceof Activity) {
//				Activity act = (Activity) pe;
//				if (act.getType().toString().equals(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
//					list.add(i);
//				}
//			}
//		}
//		return list;
//	}
//
//	@Override
//	public void prepareReplanning() {
//	}
//	
//}
