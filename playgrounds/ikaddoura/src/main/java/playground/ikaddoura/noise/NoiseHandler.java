/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.noise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.collections.Tuple;

public class NoiseHandler implements LinkLeaveEventHandler , ActivityEndEventHandler , ActivityStartEventHandler {

	private static final Logger log = Logger.getLogger(NoiseHandler.class);
	
	Scenario scenario;
	
	static Map<Id,Map<Id,Map<Integer,Tuple<Double,Double>>>> receiverPointId2personId2actNumber2activityStartAndActivityEnd = new HashMap<Id,Map<Id,Map<Integer,Tuple<Double,Double>>>>();
//	static Map<Id,Map<Id,List<double[]>>> receiverPointId2personId2activityStartAndActivityEnd = new HashMap<Id, Map<Id,List<double[]>>>();
	static Map<Id,Map<Integer,Map<Id,Tuple<Double, Double>>>> personId2actNumber2receiverPointId2activityStartAndActivityEnd = new HashMap<Id,Map<Integer,Map<Id,Tuple<Double, Double>>>>();
	static Map<Id,Map<Integer,String>> personId2actNumber2actType = new HashMap<Id, Map<Integer,String>>();
	static Map<Id,Integer> personId2actualActNumber = new HashMap<Id, Integer>();
	static Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits = new HashMap<Id, Map<Double,Double>>();
	static Map<Id,Map<Double,Map<Id,Map<Integer,Tuple<Double,String>>>>> receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap<Id, Map<Double,Map<Id,Map<Integer,Tuple<Double,String>>>>>();
	static Map<Id,List<Id>> receiverPointId2ListOfHomeAgents = new HashMap<Id, List<Id>>();
	
	static Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost = new HashMap<Id, Map<Double,Double>>();
	static Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCostPerAffectedAgentUnit = new HashMap<Id, Map<Double,Double>>();
	public static Map<Id,Map<Double,Double>> linkId2timeInterval2noiseEmission = new HashMap<Id, Map<Double,Double>>();
	static Map<Id,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission = new HashMap<Id, Map<Double,Double>>();
	static Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission = new HashMap<Id, Map<Double,Map<Id,Double>>>();
	
	static List<LinkLeaveEvent> linkLeaveEvents = new ArrayList<LinkLeaveEvent>();
	static List<LinkLeaveEvent> linkLeaveEventsCar = new ArrayList<LinkLeaveEvent>();
	static List<LinkLeaveEvent> linkLeaveEventsHdv = new ArrayList<LinkLeaveEvent>();
	
	static List<Id> hdvVehicles = new ArrayList<Id>();
	static Map<Id,List<LinkLeaveEvent>> linkId2linkLeaveEvents = new HashMap<Id, List<LinkLeaveEvent>>();
	static Map<Id,List<LinkLeaveEvent>> linkId2linkLeaveEventsCar = new HashMap<Id, List<LinkLeaveEvent>>();
	static Map<Id,List<LinkLeaveEvent>> linkId2linkLeaveEventsHdv = new HashMap<Id, List<LinkLeaveEvent>>();
	static Map<Id, Map<Double,List<LinkLeaveEvent>>> linkId2timeInterval2linkLeaveEvents = new HashMap<Id, Map<Double,List<LinkLeaveEvent>>>();
	static Map<Id, Map<Double,List<LinkLeaveEvent>>> linkId2timeInterval2linkLeaveEventsCar = new HashMap<Id, Map<Double,List<LinkLeaveEvent>>>();
	static Map<Id, Map<Double,List<LinkLeaveEvent>>> linkId2timeInterval2linkLeaveEventsHdv = new HashMap<Id, Map<Double,List<LinkLeaveEvent>>>();
	
	static int counterRouteA = 0;
	static int counterRouteB = 0;
	static int counterRouteC = 0;
	int iterationNumber = 0;
	
	Map<Integer,Integer[]> iteration2routeChoices = new HashMap<Integer, Integer[]>();
	
	public NoiseHandler (Scenario scenario) {
		this.scenario = scenario;
	}
	
	@Override
	public void reset(int iteration) {
		
//		if(iteration==0) {
//			//Bilden des Samples
			double sample = 1.00;
//			for(Id personId : scenario.getPopulation().getPersons().keySet()) {
//				System.out.println(personId);
//				//decide if the person will be removed
//				double random = Math.random();
//				if(random>sample) {
//					scenario.getPopulation().getPersons().remove(personId);
//				} else {
//					//do nothing
//				}
//			}
			
			//TODO: capacity for samples
//			if ( iteration == 0) {
//				for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
//					double adaptedCapacity = (scenario.getNetwork().getLinks().get(linkId).getCapacity())/(1./sample);
//					scenario.getNetwork().getLinks().get(linkId).setCapacity(adaptedCapacity);
//				}
//			}
//		}
		
		// TODO resetting/clearing
//		receiverPointId2personId2activityStartAndActivityEnd.clear();
		receiverPointId2personId2actNumber2activityStartAndActivityEnd.clear();
		personId2actNumber2receiverPointId2activityStartAndActivityEnd.clear();
		personId2actNumber2actType.clear();
		personId2actualActNumber.clear();
		receiverPointId2timeInterval2affectedAgentUnits.clear();
		receiverPointId2timeInterval2damageCost.clear();
		receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.clear();
		linkId2timeInterval2noiseEmission.clear();
		receiverPointId2timeInterval2noiseImmission.clear();
		receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.clear();
		linkLeaveEvents.clear();
		linkLeaveEventsCar.clear();
		linkLeaveEventsHdv.clear();
		hdvVehicles.clear();
		linkId2linkLeaveEvents.clear();
		linkId2linkLeaveEventsCar.clear();
		linkId2linkLeaveEvents.clear();
		linkId2timeInterval2linkLeaveEvents.clear();
		linkId2timeInterval2linkLeaveEventsCar.clear();
		linkId2timeInterval2linkLeaveEventsHdv.clear();
		
		counterRouteA = 0;
		counterRouteB = 0;
		counterRouteC = 0;
		if(iteration>0) {
			log.info("iteration2routeChoices ("+iterationNumber+"): "+iteration2routeChoices.get(iterationNumber)[0]+" , "+iteration2routeChoices.get(iterationNumber)[1]+" , "+iteration2routeChoices.get(iterationNumber)[2]);
		}
		iterationNumber = iteration;
		Integer[] newEntry = {0,0,0};
		iteration2routeChoices.put(iterationNumber, newEntry);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if(event.getLinkId().toString().equals("linkA3")) {
			int valueBeforeA = iteration2routeChoices.get(iterationNumber)[0];
			int valueBeforeB = iteration2routeChoices.get(iterationNumber)[1];
			int valueBeforeC = iteration2routeChoices.get(iterationNumber)[2];
			
			int newValueA = valueBeforeA + 1;
			int newValueB = valueBeforeB;
			int newValueC = valueBeforeC;
			
			Integer[] adaptedEntry = {newValueA,newValueB,newValueC};
			iteration2routeChoices.put(iterationNumber, adaptedEntry);
		} else if(event.getLinkId().toString().equals("linkB3")) {
			int valueBeforeA = iteration2routeChoices.get(iterationNumber)[0];
			int valueBeforeB = iteration2routeChoices.get(iterationNumber)[1];
			int valueBeforeC = iteration2routeChoices.get(iterationNumber)[2];
			
			int newValueA = valueBeforeA;
			int newValueB = valueBeforeB + 1;
			int newValueC = valueBeforeC;

			Integer[] adaptedEntry = {newValueA,newValueB,newValueC};
			iteration2routeChoices.put(iterationNumber, adaptedEntry);
		} else if(event.getLinkId().toString().equals("linkC3")) {
			int valueBeforeA = iteration2routeChoices.get(iterationNumber)[0];
			int valueBeforeB = iteration2routeChoices.get(iterationNumber)[1];
			int valueBeforeC = iteration2routeChoices.get(iterationNumber)[2];
			
			int newValueA = valueBeforeA;
			int newValueB = valueBeforeB;
			int newValueC = valueBeforeC + 1;

			Integer[] adaptedEntry = {newValueA,newValueB,newValueC};
			iteration2routeChoices.put(iterationNumber, adaptedEntry);
		}
		
		if(linkId2linkLeaveEvents.containsKey(event.getLinkId())) {
			List<LinkLeaveEvent> listTmp = linkId2linkLeaveEvents.get(event.getLinkId());
			listTmp.add(event);
			linkId2linkLeaveEvents.put(event.getLinkId(), listTmp);
		} else {
			List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>();
			listTmp.add(event);
			linkId2linkLeaveEvents.put(event.getLinkId(), listTmp);
		}
		linkLeaveEvents.add(event);
		
		if(hdvVehicles.contains(event.getVehicleId())) {
			// hdv vehicle
			if (linkId2linkLeaveEventsHdv.containsKey(event.getLinkId())) {
				List<LinkLeaveEvent> listTmp = linkId2linkLeaveEventsHdv.get(event.getLinkId());
				listTmp.add(event);
				linkId2linkLeaveEventsHdv.put(event.getLinkId(), listTmp);
			} else {
				List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>();
				listTmp.add(event);
				linkId2linkLeaveEventsHdv.put(event.getLinkId(), listTmp);
			}
		} else {
			// car
			if (linkId2linkLeaveEventsCar.containsKey(event.getLinkId())) {
				List<LinkLeaveEvent> listTmp = linkId2linkLeaveEventsCar.get(event.getLinkId());
				listTmp.add(event);
				linkId2linkLeaveEventsCar.put(event.getLinkId(), listTmp);
			} else {
				List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>();
				listTmp.add(event);
				linkId2linkLeaveEventsCar.put(event.getLinkId(), listTmp);
			}
		}
		
//		// TODO: OLD LOESCHEN
//		if(linkId2linkLeaveEvents.containsKey(event.getLinkId())) {
//			List<LinkLeaveEvent> listTmp = linkId2linkLeaveEvents.get(event.getLinkId());
//			listTmp.add(event);
//			linkId2linkLeaveEvents.put(event.getLinkId(), listTmp);
//		} else {
//			List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>();
//			listTmp.add(event);
//			linkId2linkLeaveEvents.put(event.getLinkId(), listTmp);
//		}
//		linkLeaveEvents.add(event);
	}

	public void calculateFinalNoiseEmissions() {
//		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
//			Map<Double,List<LinkLeaveEvent>> timeInterval2linkLeaveEvents = new HashMap<Double, List<LinkLeaveEvent>>();
//			Map<Double,List<LinkLeaveEvent>> timeInterval2linkLeaveEventsCar = new HashMap<Double, List<LinkLeaveEvent>>();
//			Map<Double,List<LinkLeaveEvent>> timeInterval2linkLeaveEventsHdv = new HashMap<Double, List<LinkLeaveEvent>>();
//			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()) {
//				List<LinkLeaveEvent> listLinkLeaveEvents = new ArrayList<LinkLeaveEvent>();
//				List<LinkLeaveEvent> listLinkLeaveEventsCar = new ArrayList<LinkLeaveEvent>();
//				List<LinkLeaveEvent> listLinkLeaveEventsHdv = new ArrayList<LinkLeaveEvent>();
//				timeInterval2linkLeaveEvents.put(timeInterval, listLinkLeaveEvents);
//				timeInterval2linkLeaveEventsCar.put(timeInterval, listLinkLeaveEventsCar);
//				timeInterval2linkLeaveEventsHdv.put(timeInterval, listLinkLeaveEventsHdv);
//			}
//			linkId2timeInterval2linkLeaveEvents.put(linkId, timeInterval2linkLeaveEvents);
//			linkId2timeInterval2linkLeaveEventsCar.put(linkId, timeInterval2linkLeaveEventsCar);
//			linkId2timeInterval2linkLeaveEventsHdv.put(linkId, timeInterval2linkLeaveEventsHdv);
//		}
//		
//		
//		// sort the linkLeaveEvents by linkIds and timeIntervals
//		for(Id linkId : linkId2linkLeaveEvents.keySet()) {
//			Map<Double,List<LinkLeaveEvent>> timeInterval2linkLeaveEvents = new HashMap<Double, List<LinkLeaveEvent>>();
//			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
//				List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>(); 
//				timeInterval2linkLeaveEvents.put(timeInterval, listTmp);
//			}
//			for(LinkLeaveEvent linkLeaveEvent : linkId2linkLeaveEvents.get(linkId)) {
//				double time = linkLeaveEvent.getTime();
//				double timeInterval = 0.;
//				if((time % Configurations.getIntervalLength()) == 0) {
//					timeInterval = time;
//				} else {
//					timeInterval = (((int)(time/Configurations.getIntervalLength()))*Configurations.getIntervalLength()) + Configurations.getIntervalLength();
//				}
//				List<LinkLeaveEvent> linkLeaveEvents = timeInterval2linkLeaveEvents.get(timeInterval);
//				linkLeaveEvents.add(linkLeaveEvent);
//				timeInterval2linkLeaveEvents.put(timeInterval, linkLeaveEvents);
//			}
//			linkId2timeInterval2linkLeaveEvents.put(linkId, timeInterval2linkLeaveEvents);
//		}
//		
//		for(Id linkId : scenario.getNetwork().getLinks().keySet()){
//			Map<Double,Double> timeInterval2NoiseEmission = new HashMap<Double, Double>();
//			double vCar = (scenario.getNetwork().getLinks().get(linkId).getFreespeed())*3.6;
//			double vLorry = vCar;
//				for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
//					double noiseEmission = 0.;
//
//					int M_car = linkId2timeInterval2linkLeaveEventsCar.get(linkId).get(timeInterval).size();
//					int M_hdv = linkId2timeInterval2linkLeaveEventsHdv.get(linkId).get(timeInterval).size();
//					int M = M_car + M_hdv;
//					double p = 0;
//					if(!(M == 0)) {
//						p = M_hdv / M;
//					}
//					if(!(M == 0)) {
//						noiseEmission = Emissionspegel.calculateEmissionspegel(M, p, vCar, vLorry);
//					}	
//					timeInterval2NoiseEmission.put(timeInterval, noiseEmission);
//				}
//				linkId2timeInterval2noiseEmission.put(linkId , timeInterval2NoiseEmission);
//		}
		
//		// TODO: OLD LOESCHEN
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double,List<LinkLeaveEvent>> timeInterval2linkLeaveEvents = new HashMap<Double, List<LinkLeaveEvent>>();
			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()) {
				List<LinkLeaveEvent> listLinkLeaveEvents = new ArrayList<LinkLeaveEvent>();
				timeInterval2linkLeaveEvents.put(timeInterval, listLinkLeaveEvents);
			}
			linkId2timeInterval2linkLeaveEvents.put(linkId, timeInterval2linkLeaveEvents);
		}
		
		
		// sort the linkLeaveEvents by linkIds and timeIntervals
		for(Id linkId : linkId2linkLeaveEvents.keySet()) {
			Map<Double,List<LinkLeaveEvent>> timeInterval2linkLeaveEvents = new HashMap<Double, List<LinkLeaveEvent>>();
			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				List<LinkLeaveEvent> listTmp = new ArrayList<LinkLeaveEvent>(); 
				timeInterval2linkLeaveEvents.put(timeInterval, listTmp);
			}
			for(LinkLeaveEvent linkLeaveEvent : linkId2linkLeaveEvents.get(linkId)) {
				double time = linkLeaveEvent.getTime();
				double timeInterval = 0.;
				if((time % Configurations.getIntervalLength()) == 0) {
					timeInterval = time;
				} else {
					timeInterval = (((int)(time/Configurations.getIntervalLength()))*Configurations.getIntervalLength()) + Configurations.getIntervalLength();
				}
				List<LinkLeaveEvent> linkLeaveEvents = timeInterval2linkLeaveEvents.get(timeInterval);
				linkLeaveEvents.add(linkLeaveEvent);
				timeInterval2linkLeaveEvents.put(timeInterval, linkLeaveEvents);
			}
			linkId2timeInterval2linkLeaveEvents.put(linkId, timeInterval2linkLeaveEvents);
		}
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()){
			Map<Double,Double> timeInterval2NoiseEmission = new HashMap<Double, Double>();
			double vCar = (scenario.getNetwork().getLinks().get(linkId).getFreespeed())*3.6;
			double vLorry = vCar;
				for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
					double noiseEmission = 0.;

					int M_car = linkId2timeInterval2linkLeaveEvents.get(linkId).get(timeInterval).size();
					int M_hdv = 0;
					int M = M_car + M_hdv;
					double p = 0;
					if(!(M == 0)) {
						p = M_hdv / M;
					}
					if(!(M == 0)) {
//						noiseEmission = Emissionspegel.calculateEmissionspegel(M, p, vCar, vLorry);
						noiseEmission = Emissionspegel.calculateEmissionspegel((int)((Configurations.getScaleFactor())*M), p, vCar, vLorry);
					}	
					timeInterval2NoiseEmission.put(timeInterval, noiseEmission);
				}
				linkId2timeInterval2noiseEmission.put(linkId , timeInterval2NoiseEmission);
//				log.info(linkId2timeInterval2noiseEmission.get(linkId));
		}
	}

	public void calculateImmissionSharesPerReceiverPointPerTimeInterval() {
		for(Id coordId : GetNearestReceiverPoint.receiverPoints.keySet()) {
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2isolatedImmission = new HashMap<Double, Map<Id,Double>>();
			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()) {
			 	Map<Id,Double> noiseLinks2isolatedImmission = new HashMap<Id, Double>();
				for(Id linkId : GetNearestReceiverPoint.receiverPointId2relevantLinkIds.get(coordId)) {
					double distanceToRoad = GetNearestReceiverPoint.receiverPointId2RelevantLinkIds2Distances.get(coordId).get(linkId);
//			 		log.info(linkId);
//					log.info(distanceToRoad);
//					log.info(timeInterval);
					double noiseEmission = linkId2timeInterval2noiseEmission.get(linkId).get(timeInterval);			 		
//			 		log.info(noiseEmission);
					double noiseImmission = NoiseImmissionCalculator.calculateNoiseImmission(scenario , linkId , distanceToRoad, noiseEmission , GetNearestReceiverPoint.receiverPoints.get(coordId) , GetNearestReceiverPoint.receiverPoints.get(coordId).getX() , GetNearestReceiverPoint.receiverPoints.get(coordId).getY() , scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX() , scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY() , scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX() , scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY());
//			 		log.info("noiseImmissionCalculated: "+noiseImmission);
//			 		double noiseImmission = NoiseImmissionCalculator.calculateNoiseImmission2(coordId,linkId,noiseEmission);
//			 		System.out.println("+++: "+noiseImmission);
					noiseLinks2isolatedImmission.put(linkId,noiseImmission);
			 	}
				timeIntervals2noiseLinks2isolatedImmission.put(timeInterval, noiseLinks2isolatedImmission);
			}
			receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.put(coordId, timeIntervals2noiseLinks2isolatedImmission);
		}
	}
	
	public void calculateFinalNoiseImmissions() {
		for(Id coordId : GetNearestReceiverPoint.receiverPoints.keySet()) {
			Map<Double,Double> timeInterval2noiseImmission = new HashMap<Double, Double>();
			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()) {
				List<Double> noiseImmissions = new ArrayList<Double>();
				for(Id linkId : receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).keySet()) {
					if(!(linkId2timeInterval2linkLeaveEvents.get(linkId).get(timeInterval).size()==0.)) {
						noiseImmissions.add(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).get(linkId));
					}
//					log.info("noiseImmissionX: "+(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).get(linkId)));
//					log.info("coordId: "+coordId+" timeInterval: "+timeInterval+" linkId: "+linkId);
//					if(linkId2timeInterval2linkLeaveEvents.containsKey(linkId)) {
//						if(linkId2timeInterval2linkLeaveEvents.get(linkId).containsKey(timeInterval)) {
//							noiseImmissions.add(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).get(linkId));
//						}
//					}
				}	
				double resultingNoiseImmission = NoiseImmissionCalculator.calculateResultingNoiseImmission(noiseImmissions);
				timeInterval2noiseImmission.put(timeInterval, resultingNoiseImmission);
			}
			receiverPointId2timeInterval2noiseImmission.put(coordId, timeInterval2noiseImmission);
		}
	}

	public void calculateDurationOfStay() {
//		log.info(GetNearestReceiverPoint.activityCoord2receiverPointId);
//		for (Id id : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()){
//			log.info(receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(id));
//		}
		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for(Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for(Integer actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
					Tuple<Double,Double> actStartAndActEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber);
					String actType = personId2actNumber2actType.get(personId).get(actNumber);
					double actStart = Double.MAX_VALUE;
					double actEnd = Double.MIN_VALUE;
					if(actStartAndActEnd.getFirst() == 0.) {
						// home activity (morning)
						actStart = 0.;
						actEnd = actStartAndActEnd.getSecond();
					} else if(actStartAndActEnd.getSecond() == 30*3600) {
						// home activity (evening)
						actStart = actStartAndActEnd.getFirst();
						actEnd = 30*3600;
					} else {
						// other activity
						actStart = actStartAndActEnd.getFirst();
						actEnd = actStartAndActEnd.getSecond();
					}
					// now calculation for the time shares of the intervals
					for(double intervalEnd = Configurations.getIntervalLength() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + Configurations.getIntervalLength()) {
						double intervalStart = intervalEnd - Configurations.getIntervalLength();
//						
						double durationOfStay = 0.;
//						
						if(actEnd <= intervalStart || actStart >= intervalEnd) {
							durationOfStay = 0.;
						} else if(actStart <= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = Configurations.getIntervalLength();
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - intervalStart;
						} else if(actStart >= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = intervalEnd - actStart;
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - actStart;
						}
						
//						log.info("00: "+durationOfStay+" - - - in interval "+intervalEnd);
//						log.info("11: "+receiverPointId);
//						log.info("22: "+personId);
//						log.info("33: "+actNumber);
//						log.info("44: "+actType);
//						log.info("55: "+actStartAndActEnd);
//						log.info("66: "+intervalEnd);
//						log.info("77: "+receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						
						// calculation for the individual noiseEventsAffected
						// list for all receiver points and all time intervals for each agent the time, ...
						Map <Double , Map <Id,Map<Integer,Tuple<Double,String>>>> timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Double , Map <Id,Map<Integer,Tuple<Double,String>>>>();
						if(receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(receiverPointId)) {
//							log.info("AA");
							timeInterval2personId2actNumber2affectedAgentUnitsAndActType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId);
						} else {
//							log.info("BB");
						}
						Map <Id,Map<Integer,Tuple<Double,String>>> personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Id,Map<Integer,Tuple<Double,String>>>();
						if(timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(intervalEnd)) {
//							log.info("CC");
							personId2actNumber2affectedAgentUnitsAndActType = timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(intervalEnd);
						} else {
//							log.info("DD");
						}
						Map<Integer,Tuple<Double,String>> actNumber2affectedAgentUnitsAndActType = new HashMap <Integer,Tuple<Double,String>>();
						if(personId2actNumber2affectedAgentUnitsAndActType.containsKey(personId)) {
//							log.info("EE");
							actNumber2affectedAgentUnitsAndActType = personId2actNumber2affectedAgentUnitsAndActType.get(personId);
						} else {
//							log.info("FF");
						}
						Tuple <Double,String> affectedAgentUnitsAndActType = new Tuple<Double, String>((durationOfStay/Configurations.getIntervalLength()), actType);
//						log.info("GG :"+affectedAgentUnitsAndActType);
						actNumber2affectedAgentUnitsAndActType.put(actNumber,affectedAgentUnitsAndActType);
//						log.info("HH :"+actNumber2affectedAgentUnitsAndActType);
						personId2actNumber2affectedAgentUnitsAndActType.put(personId,actNumber2affectedAgentUnitsAndActType);
//						log.info("II :"+personId2actNumber2affectedAgentUnitsAndActType);
						timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(intervalEnd,personId2actNumber2affectedAgentUnitsAndActType);
//						log.info("JJ :"+timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(receiverPointId,timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						
						// calculation for the individual noiseEventsAffected (home-based-oriented)
						
						
						// calculation for the damage
//						double affectedAgentUnits = (durationOfStay/Configurations.getIntervalLength();
						double affectedAgentUnits = (Configurations.getScaleFactor())* (durationOfStay/Configurations.getIntervalLength());
						if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
							if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(intervalEnd)) {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, timeInterval2affectedAgentUnits.get(intervalEnd)+affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							} else {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							}
						} else {
							Map<Double,Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();
							timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
							receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
						}	
					}
				}
			}
		}
//		log.info(receiverPointId2timeInterval2affectedAgentUnits);
		
		//for homogeneous distribution of the affected agents
//		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
////		for(Id id : receiverPointId2timeInterval2affectedAgentUnits.keySet()) {
//			Map <Double,Double> mapTmp = new HashMap<Double, Double>();
//			for(double intervalEnd = Configurations.getIntervalLength() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + Configurations.getIntervalLength()) {
//				mapTmp.put(intervalEnd, 60.);
//			}
//			receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId,mapTmp);
//		}
	}
	
//	public void calculateDurationOfStay() {
//		for(Id receiverPointId : receiverPointId2personId2activityStartAndActivityEnd.keySet()) {
//			for(Id personId : receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
//				for(double[] actStartAndActEnd : receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId).get(personId)) {
//					double actStart = Double.MAX_VALUE;
//					double actEnd = Double.MIN_VALUE;
//					if(actStartAndActEnd[0] == 0) {
//						// home activity (morning)
//						actStart = 0.;
//						actEnd = actStartAndActEnd[1];
//					} else if(actStartAndActEnd[1] == 30*3600) {
//						// home activity (evening)
//						actStart = actStartAndActEnd[0];
//						actEnd = 30*3600;
//					} else {
//						// other activity
//						actStart = actStartAndActEnd[0];
//						actEnd = actStartAndActEnd[1];
//					}
//					// now calculation for the time shares of the intervals
//					for(double intervalEnd = Configurations.getIntervalLength() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + Configurations.getIntervalLength()) {
//						double intervalStart = intervalEnd - Configurations.getIntervalLength();
//						
//						double durationOfStay = 0.;
//						
//						if(actEnd <= intervalStart || actStart >= intervalEnd) {
//							durationOfStay = 0.;
//						} else if(actStart <= intervalStart && actEnd >= intervalEnd) {
//							durationOfStay = Configurations.getIntervalLength();
//						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
//							durationOfStay = actEnd - intervalStart;
//						} else if(actStart >= intervalStart && actEnd >= intervalEnd) {
//							durationOfStay = intervalEnd - actStart;
//						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
//							durationOfStay = actEnd - actStart;
//						}
//						
//						double affectedAgentUnits = durationOfStay/Configurations.getIntervalLength();
//						if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
//							if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(intervalEnd)) {
//								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
//								timeInterval2affectedAgentUnits.put(intervalEnd, timeInterval2affectedAgentUnits.get(intervalEnd)+affectedAgentUnits);
//								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
//							} else {
//								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
//								timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
//								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
//							}
//						} else {
//							Map<Double,Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();
//							timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
//							receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
//						}	
//					}
//				}
//			}
//		}
//		
//	}

	public void calculateDurationOfStayOnlyHomeActivity() {
//		log.info(GetNearestReceiverPoint.activityCoord2receiverPointId);
//		for (Id id : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()){
//			log.info(receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(id));
//		}
		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for(Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for(Integer actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
					Tuple<Double,Double> actStartAndActEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber);
					String actType = personId2actNumber2actType.get(personId).get(actNumber);
					double actStart = Double.MAX_VALUE;
					double actEnd = Double.MIN_VALUE;
					if(actStartAndActEnd.getFirst() == 0.) {
						// home activity (morning)
						actStart = 0.;
						actEnd = actStartAndActEnd.getSecond();
//						log.info("111111");
					} else if(actStartAndActEnd.getSecond() == 30*3600) {
						// TODO: !!! actStartAndActEnd.getSecond() == 30*3600 ?? Right Adaption before?!
						// home activity (evening)
						actStart = actStartAndActEnd.getFirst();
						actEnd = 30*3600;
//						log.info("222222");
					} else {
						// other activity
						actStart = actStartAndActEnd.getFirst();
						actEnd = actStartAndActEnd.getSecond();
//						log.info("333333");
					}
					
					if(!(actType.toString().equals("home"))) {
						// activity duration is zero if it is a home activity
						actEnd = actStart;
					}
					
//					log.info("");
//					log.info("++++++++++++++++++++++");
//					log.info("actType: "+actType);
//					log.info("start: "+actStart);
//					log.info("end: "+actEnd);
//					log.info("duration: "+(actEnd-actStart));
//					log.info("++++++++++++++++++++++");
//					log.info("");
					
					// now calculation for the time shares of the intervals
					for(double intervalEnd = Configurations.getIntervalLength() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + Configurations.getIntervalLength()) {
						double intervalStart = intervalEnd - Configurations.getIntervalLength();
//						
						double durationOfStay = 0.;
//						
						if(actEnd <= intervalStart || actStart >= intervalEnd) {
							durationOfStay = 0.;
						} else if(actStart <= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = Configurations.getIntervalLength();
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - intervalStart;
						} else if(actStart >= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = intervalEnd - actStart;
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - actStart;
						}
						
//						log.info("00: "+durationOfStay+" - - - in interval "+intervalEnd);
//						log.info("11: "+receiverPointId);
//						log.info("22: "+personId);
//						log.info("33: "+actNumber);
//						log.info("44: "+actType);
//						log.info("55: "+actStartAndActEnd);
//						log.info("66: "+intervalEnd);
//						log.info("77: "+receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						
						// calculation for the individual noiseEventsAffected
						// list for all receiver points and all time intervals for each agent the time, ...
						Map <Double , Map <Id,Map<Integer,Tuple<Double,String>>>> timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Double , Map <Id,Map<Integer,Tuple<Double,String>>>>();
						if(receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(receiverPointId)) {
//							log.info("AA");
							timeInterval2personId2actNumber2affectedAgentUnitsAndActType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId);
						} else {
//							log.info("BB");
						}
						Map <Id,Map<Integer,Tuple<Double,String>>> personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Id,Map<Integer,Tuple<Double,String>>>();
						if(timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(intervalEnd)) {
//							log.info("CC");
							personId2actNumber2affectedAgentUnitsAndActType = timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(intervalEnd);
						} else {
//							log.info("DD");
						}
						Map<Integer,Tuple<Double,String>> actNumber2affectedAgentUnitsAndActType = new HashMap <Integer,Tuple<Double,String>>();
						if(personId2actNumber2affectedAgentUnitsAndActType.containsKey(personId)) {
//							log.info("EE");
							actNumber2affectedAgentUnitsAndActType = personId2actNumber2affectedAgentUnitsAndActType.get(personId);
						} else {
//							log.info("FF");
						}
						Tuple <Double,String> affectedAgentUnitsAndActType = new Tuple<Double, String>((durationOfStay/Configurations.getIntervalLength()), actType);
//						log.info("GG :"+affectedAgentUnitsAndActType);
						actNumber2affectedAgentUnitsAndActType.put(actNumber,affectedAgentUnitsAndActType);
//						log.info("HH :"+actNumber2affectedAgentUnitsAndActType);
						personId2actNumber2affectedAgentUnitsAndActType.put(personId,actNumber2affectedAgentUnitsAndActType);
//						log.info("II :"+personId2actNumber2affectedAgentUnitsAndActType);
						timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(intervalEnd,personId2actNumber2affectedAgentUnitsAndActType);
//						log.info("JJ :"+timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(receiverPointId,timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						
						// calculation for the individual noiseEventsAffected (home-based-oriented)
						
						
						// calculation for the damage
//						double affectedAgentUnits = (durationOfStay/Configurations.getIntervalLength();
						double affectedAgentUnits = (Configurations.getScaleFactor())* (durationOfStay/Configurations.getIntervalLength());
						if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
							if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(intervalEnd)) {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, timeInterval2affectedAgentUnits.get(intervalEnd)+affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							} else {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							}
						} else {
							Map<Double,Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();
							timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
							receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
						}	
					}
				}
			}
		}
//		log.info(receiverPointId2timeInterval2affectedAgentUnits);
		
		//for homogeneous distribution of the affected agents
//		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
////		for(Id id : receiverPointId2timeInterval2affectedAgentUnits.keySet()) {
//			Map <Double,Double> mapTmp = new HashMap<Double, Double>();
//			for(double intervalEnd = Configurations.getIntervalLength() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + Configurations.getIntervalLength()) {
//				mapTmp.put(intervalEnd, 60.);
//			}
//			receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId,mapTmp);
//		}
	}
	
	public void calculateDamagePerReceiverPoint() {
//		log.info(receiverPointId2timeInterval2noiseImmission);
//		log.info(receiverPointId2timeInterval2affectedAgentUnits);
		for(Id receiverPointId : receiverPointId2timeInterval2noiseImmission.keySet()) {
//			log.info(receiverPointId2timeInterval2noiseImmission);
//			log.info(receiverPointId2timeInterval2affectedAgentUnits);
			for(double timeInterval : receiverPointId2timeInterval2noiseImmission.get(receiverPointId).keySet()) {
				double noiseImmission = receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get(timeInterval);
				double affectedAgentUnits = 0.;
				if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
					if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(timeInterval)) {
						affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).get(timeInterval);
					} 	
				}
				double damageCost = ComputationFormulae.calculateDamageCosts(noiseImmission,affectedAgentUnits,timeInterval);
				double damageCostPerAffectedAgentUnit = ComputationFormulae.calculateDamageCosts(noiseImmission,1.,timeInterval);
//				damageCost = damageCostPerAffectedAgentUnit*80;
//				log.info("111: "+damageCost);
//				log.info("222: "+damageCostPerAffectedAgentUnit);
				if(receiverPointId2timeInterval2damageCost.containsKey(receiverPointId)) {
					Map<Double,Double> timeInterval2damageCost = receiverPointId2timeInterval2damageCost.get(receiverPointId);
					Map<Double,Double> timeInterval2damageCostPerAffectedAgentUnit = receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId);
					timeInterval2damageCost.put(timeInterval, damageCost);
					timeInterval2damageCostPerAffectedAgentUnit.put(timeInterval, damageCostPerAffectedAgentUnit);
					receiverPointId2timeInterval2damageCost.put(receiverPointId, timeInterval2damageCost);
					receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.put(receiverPointId, timeInterval2damageCostPerAffectedAgentUnit);
				} else {
					Map<Double,Double> timeInterval2damageCost = new HashMap<Double, Double>();
					Map<Double,Double> timeInterval2damageCostPerAffectedAgentUnit = new HashMap<Double, Double>();
					timeInterval2damageCost.put(timeInterval, damageCost);
					timeInterval2damageCostPerAffectedAgentUnit.put(timeInterval, damageCostPerAffectedAgentUnit);
					receiverPointId2timeInterval2damageCost.put(receiverPointId, timeInterval2damageCost);
					receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.put(receiverPointId, timeInterval2damageCostPerAffectedAgentUnit);
				}
			}
		}
//		log.info(linkId2timeInterval2noiseEmission);
//		log.info(receiverPointId2timeInterval2damageCost);
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!event.getActType().toString().equals("pt_interaction")) {
			Id personId = event.getPersonId();
			personId2actualActNumber.put(event.getPersonId(), personId2actualActNumber.get(event.getPersonId())+1);
			int actNumber = personId2actualActNumber.get(personId);
			double time = event.getTime();
			Coord coord = GetActivityCoords.personId2listOfCoords.get(personId).get(actNumber-1);
			Id receiverPointId = GetNearestReceiverPoint.activityCoord2receiverPointId.get(coord);
			
			double startTime = time;
			double EndTime = 30*3600;
			Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
			Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
			receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
			Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId);
			actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
			personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);
			
			String actType = event.getActType();
			Map <Integer,String> actNumber2actType = personId2actNumber2actType.get(personId);
			actNumber2actType.put(actNumber,actType);
			personId2actNumber2actType.put(personId,actNumber2actType);
			
			if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.containsKey(receiverPointId)) {
				// already at least one activity at this receiverPoint
				if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).containsKey(personId)) {
					// already at least the second activity of this person at this receiverPoint
//					double startTime = time;
//					double EndTime = 30*3600;
//					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId);
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
				} else {
					// the first activity of this person at this receiverPoint
//					double startTime = time;
//					double EndTime = 30*3600;
//					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
				}
			} else {
				// the first activity at this receiver Point
//				double startTime = time;
//				double EndTime = 30*3600;
//				Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
				Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
				actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
				Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = new HashMap<Id, Map<Integer,Tuple<Double,Double>>>();
				personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
				receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
			}
				
//			if(receiverPointId2personId2activityStartAndActivityEnd.containsKey(receiverPointId)) {
//				if(receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId).containsKey(personId)) {
//					List<double[]> activityStartAndActivityEnd = receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId).get(personId);
//					double[] actStartAndActEnd = new double[2];
//					actStartAndActEnd [0] = event.getTime();
//					actStartAndActEnd [1] = 30*3600;
//					activityStartAndActivityEnd.add(actStartAndActEnd);
//					Map<Id,List<double[]>> personId2activityStartAndActivityEnd = receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId);
//					personId2activityStartAndActivityEnd.put(event.getPersonId(),activityStartAndActivityEnd);
//					receiverPointId2personId2activityStartAndActivityEnd.put(receiverPointId, personId2activityStartAndActivityEnd);
//				} else {
//					List<double[]> activityStartAndActivityEnd = new ArrayList<double[]>();
//					double[] actStartAndActEnd = new double[2];
//					actStartAndActEnd [0] = event.getTime();
//					actStartAndActEnd [1] = 30*3600;
//					activityStartAndActivityEnd.add(actStartAndActEnd);
//					Map<Id,List<double[]>> personId2activityStartAndActivityEnd = receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId);
//					personId2activityStartAndActivityEnd.put(event.getPersonId(),activityStartAndActivityEnd);
//					receiverPointId2personId2activityStartAndActivityEnd.put(receiverPointId, personId2activityStartAndActivityEnd);
//				}
//			} else {
//				List<double[]> activityStartAndActivityEnd = new ArrayList<double[]>();
//				double[] actStartAndActEnd = new double[2];
//				actStartAndActEnd [0] = event.getTime();
//				actStartAndActEnd [1] = 30*3600;
//				activityStartAndActivityEnd.add(actStartAndActEnd);
//				Map<Id,List<double[]>> personId2activityStartAndActivityEnd = new HashMap<Id, List<double[]>>();
//				personId2activityStartAndActivityEnd.put(event.getPersonId(),activityStartAndActivityEnd);
//				receiverPointId2personId2activityStartAndActivityEnd.put(receiverPointId, personId2activityStartAndActivityEnd);
//			}
		} else {
			// do nothing
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(!event.getActType().toString().equals("pt_interaction")) {
			Id personId = event.getPersonId();
			if(!(personId2actualActNumber.containsKey(event.getPersonId()))) {
				personId2actualActNumber.put(event.getPersonId(), 1);
			}
			int actNumber = personId2actualActNumber.get(personId);
			double time = event.getTime();
			Coord coord = GetActivityCoords.personId2listOfCoords.get(personId).get(actNumber-1);
			Id receiverPointId = GetNearestReceiverPoint.activityCoord2receiverPointId.get(coord);
			
			if(personId2actNumber2receiverPointId2activityStartAndActivityEnd.containsKey(personId)) {
				// not the first activity
				double startTime = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId).get(actNumber).get(receiverPointId).getFirst();
				double EndTime = time;
				Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
				Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
				receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
				Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId);
				actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
				personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);
				
//				actType should already be named!
//				String actType = event.getActType();
//				Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
			} else {
				// the first activity
				double startTime = 0.;
				double EndTime = time;
				Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
				Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
				receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
				Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = new HashMap<Integer, Map<Id,Tuple<Double,Double>>>();
				actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
				personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);

				String actType = event.getActType();
				Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
				actNumber2actType.put(actNumber,actType);
				personId2actNumber2actType.put(personId, actNumber2actType);
				
				if(receiverPointId2ListOfHomeAgents.containsKey(receiverPointId)) {
					List <Id> listOfHomeAgents = receiverPointId2ListOfHomeAgents.get(receiverPointId);
					listOfHomeAgents.add(personId);
					receiverPointId2ListOfHomeAgents.put(receiverPointId, listOfHomeAgents);
				} else {
					List <Id> listOfHomeAgents = new ArrayList<Id>();
					listOfHomeAgents.add(personId);
					receiverPointId2ListOfHomeAgents.put(receiverPointId, listOfHomeAgents);
				}
			}
			
			if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.containsKey(receiverPointId)) {
				// already at least one activity at this receiver point
				if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).containsKey(personId)) {
					// at least the second activity of this person at this receiver point
					double startTime = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber).getFirst();
					double EndTime = time;
					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId);
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);

//					actType should already be named!
//					String actType = event.getActType();
//					Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
				} else {
					// the first activity of this person at this receiver point
					double startTime = 0.; // this must be the home activity in the morning;
					double EndTime = time;
					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);

					String actType = event.getActType();
					Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
					actNumber2actType.put(actNumber,actType);
					personId2actNumber2actType.put(personId, actNumber2actType);
				}
			} else {
				// the first activity at this receiver point
				double startTime = 0.; // this must be the home activity in the morning;
				double EndTime = time;
				Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
				Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
				actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
				Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = new HashMap<Id, Map<Integer,Tuple<Double,Double>>>();
				personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
				receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);

				String actType = event.getActType();
				Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
				actNumber2actType.put(actNumber,actType);
				personId2actNumber2actType.put(personId, actNumber2actType);
			}
			
//			if(receiverPointId2personId2activityStartAndActivityEnd.containsKey(receiverPointId)) {
//				if(receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId).containsKey(personId)) {
//					List<double[]> activityStartAndActivityEnd = receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId).get(personId);
//					int size = activityStartAndActivityEnd.size();
//					double[] actStartAndActEnd = activityStartAndActivityEnd.get(size-1);
//					actStartAndActEnd [1] = event.getTime();
//					activityStartAndActivityEnd.add(actStartAndActEnd);
//					Map<Id,List<double[]>> personId2activityStartAndActivityEnd = receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId);
//					personId2activityStartAndActivityEnd.put(event.getPersonId(),activityStartAndActivityEnd);
//					receiverPointId2personId2activityStartAndActivityEnd.put(receiverPointId, personId2activityStartAndActivityEnd);
//				} else {
//					List<double[]> activityStartAndActivityEnd = new ArrayList<double[]>();
//					double[] actStartAndActEnd = new double[2];
//					actStartAndActEnd [0] = 0;
//					actStartAndActEnd [1] = event.getTime();
//					activityStartAndActivityEnd.add(actStartAndActEnd);
//					Map<Id,List<double[]>> personId2activityStartAndActivityEnd = receiverPointId2personId2activityStartAndActivityEnd.get(receiverPointId);
//					personId2activityStartAndActivityEnd.put(event.getPersonId(),activityStartAndActivityEnd);
//					receiverPointId2personId2activityStartAndActivityEnd.put(receiverPointId, personId2activityStartAndActivityEnd);
//				}
//			} else {
//				List<double[]> activityStartAndActivityEnd = new ArrayList<double[]>();
//				double[] actStartAndActEnd = new double[2];
//				actStartAndActEnd [0] = 0;
//				actStartAndActEnd [1] = event.getTime();
//				activityStartAndActivityEnd.add(actStartAndActEnd);
//				Map<Id,List<double[]>> personId2activityStartAndActivityEnd = new HashMap<Id, List<double[]>>();
//				personId2activityStartAndActivityEnd.put(event.getPersonId(),activityStartAndActivityEnd);
//				receiverPointId2personId2activityStartAndActivityEnd.put(receiverPointId, personId2activityStartAndActivityEnd);
//			}
		} else { 
			// do nothing
		}
	}

	public void writeNoiseEmissionStats(String fileName) {
		File file = new File(fileName);
		
		Map<Id,Double> linkId2noiseEmissionDay = new HashMap<Id, Double>();
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;avg noiseEmission;avg noiseEmission (day);avg noiseEmission (night);avg noiseEmission (peak);avg noiseEmission (off-peak)");
			bw.newLine();
			
			List<Double> day = new ArrayList<Double>();
			for(double timeInterval = 6*3600 + Configurations.getIntervalLength() ; timeInterval<=22*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				day.add(timeInterval);
			}
			List<Double> night = new ArrayList<Double>();
			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				if(!(day.contains(timeInterval))) {
					night.add(timeInterval);
				}
			}
			
			List<Double> peak = new ArrayList<Double>();
			for(double timeInterval = 7*3600 + Configurations.getIntervalLength() ; timeInterval<=9*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				peak.add(timeInterval);
			}
			for(double timeInterval = 15*3600 + Configurations.getIntervalLength() ; timeInterval<=18*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				peak.add(timeInterval);
			}
			List<Double> offPeak = new ArrayList<Double>();
			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				if(!(peak.contains(timeInterval))) {
					offPeak.add(timeInterval);
				}
			}
			
			for (Id linkId : this.linkId2timeInterval2noiseEmission.keySet()){
				double avgNoise = 0;
				double avgNoiseDay = 0;
				double avgNoiseNight = 0;
				double avgNoisePeak = 0;
				double avgNoiseOffPeak = 0;
				
				double sumAvgNoise = 0;
				double sumAvgNoiseDay = 0;
				double sumAvgNoiseNight = 0;
				double sumAvgNoisePeak = 0;
				double sumAvgNoiseOffPeak = 0;
				
				for(double timeInterval : linkId2timeInterval2noiseEmission.get(linkId).keySet()) {
					double noiseValue = linkId2timeInterval2noiseEmission.get(linkId).get(timeInterval);
					
					if(timeInterval<24*3600) {
						sumAvgNoise = sumAvgNoise + noiseValue;
					}
					
					if(day.contains(timeInterval)) {
					sumAvgNoiseDay = sumAvgNoiseDay + noiseValue;
					}
					
					if(night.contains(timeInterval)) {
					sumAvgNoiseNight = sumAvgNoiseNight + noiseValue;
					}
				
					if(peak.contains(timeInterval)) {
					sumAvgNoisePeak = sumAvgNoisePeak + noiseValue;
					}
					
					if(offPeak.contains(timeInterval)) {
					sumAvgNoiseOffPeak = sumAvgNoiseOffPeak + noiseValue;
					}	
				}
				
				avgNoise = sumAvgNoise / (day.size() + night.size());
				avgNoiseDay = sumAvgNoiseDay / day.size();
				avgNoiseNight = sumAvgNoiseNight / night.size();
				avgNoisePeak = sumAvgNoisePeak / peak.size();
				avgNoiseOffPeak = sumAvgNoiseOffPeak / offPeak.size();
				
				linkId2noiseEmissionDay.put(linkId, avgNoiseDay);
				
				bw.write(linkId + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		analysis.shapes.IKNetworkPopulationWriter.exportNetwork2Shp(scenario.getNetwork(), linkId2noiseEmissionDay);
		
	}
	
	public void writeNoiseEmissionStatsPerHour(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;");
		
			for(int i = 0; i < 26 ; i++) {
				bw.write(";hour"+i+";leaving agents (per hour);noiseEmission;");
			}
			bw.newLine();
			
			for (Id linkId : this.linkId2timeInterval2noiseEmission.keySet()){
				bw.write(linkId.toString()+";"); 
				for(int i = 0 ; i < 26 ; i++) {
//					log.info(linkId2timeInterval2linkLeaveEvents.get(linkId).get((i+1)*3600.).size());
//					log.info(linkId2timeInterval2noiseEmission.get(linkId).get((i+1)*3600.));
					bw.write(";hour_"+i+";"+linkId2timeInterval2linkLeaveEvents.get(linkId).get((i+1)*3600.).size()+";"+linkId2timeInterval2noiseEmission.get(linkId).get((i+1)*3600.)+";");	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	public void writeNoiseImmissionStats(String fileName) {
		File file = new File(fileName);
		
		Map<Id,Double> receiverPointId2noiseImmission = new HashMap<Id, Double>();
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("receiver point;avg noiseImmission;avg noiseImmission (day);avg noiseImmission (night);avg noiseImmission (peak);avg noiseImmission (off-peak)");
			bw.newLine();
			
			List<Double> day = new ArrayList<Double>();
			for(double timeInterval = 6*3600 + Configurations.getIntervalLength() ; timeInterval<=22*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				day.add(timeInterval);
			}
			List<Double> night = new ArrayList<Double>();
			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				if(!(day.contains(timeInterval))) {
					night.add(timeInterval);
				}
			}
			
			List<Double> peak = new ArrayList<Double>();
			for(double timeInterval = 7*3600 + Configurations.getIntervalLength() ; timeInterval<=9*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				peak.add(timeInterval);
			}
			for(double timeInterval = 15*3600 + Configurations.getIntervalLength() ; timeInterval<=18*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				peak.add(timeInterval);
			}
			List<Double> offPeak = new ArrayList<Double>();
			for(double timeInterval = Configurations.getIntervalLength() ; timeInterval<=24*3600 ; timeInterval = timeInterval + Configurations.getIntervalLength()){
				if(!(peak.contains(timeInterval))) {
					offPeak.add(timeInterval);
				}
			}
			
			for (Id receiverPointId : this.receiverPointId2timeInterval2noiseImmission.keySet()){
				double avgNoise = 0;
				double avgNoiseDay = 0;
				double avgNoiseNight = 0;
				double avgNoisePeak = 0;
				double avgNoiseOffPeak = 0;
				
				double sumAvgNoise = 0;
				double sumAvgNoiseDay = 0;
				double sumAvgNoiseNight = 0;
				double sumAvgNoisePeak = 0;
				double sumAvgNoiseOffPeak = 0;
				
				for(double timeInterval : receiverPointId2timeInterval2noiseImmission.get(receiverPointId).keySet()) {
					double noiseValue = receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get(timeInterval);
					
					if(timeInterval<24*3600) {
						sumAvgNoise = sumAvgNoise + noiseValue;
					}
					
					if(day.contains(timeInterval)) {
					sumAvgNoiseDay = sumAvgNoiseDay + noiseValue;
					}
					
					if(night.contains(timeInterval)) {
					sumAvgNoiseNight = sumAvgNoiseNight + noiseValue;
					}
				
					if(peak.contains(timeInterval)) {
					sumAvgNoisePeak = sumAvgNoisePeak + noiseValue;
					}
					
					if(offPeak.contains(timeInterval)) {
					sumAvgNoiseOffPeak = sumAvgNoiseOffPeak + noiseValue;
					}	
				}
				
				avgNoise = sumAvgNoise / (day.size() + night.size());
				avgNoiseDay = sumAvgNoiseDay / day.size();
				receiverPointId2noiseImmission.put(receiverPointId,avgNoiseDay);
				avgNoiseNight = sumAvgNoiseNight / night.size();
				avgNoisePeak = sumAvgNoisePeak / peak.size();
				avgNoiseOffPeak = sumAvgNoiseOffPeak / offPeak.size();
				
				bw.write(receiverPointId + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		analysis.shapes.IKNetworkPopulationWriter.exportReceiverPoints2Shp(receiverPointId2noiseImmission);
		
	}
	
	public void writeNoiseImmissionStatsPerHour(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("receiverPoint;");
			
			for(int i = 0; i < 26 ; i++) {
				bw.write(";hour_"+i+";affectedAgentUnits;noiseImmission;");
			}
			bw.newLine();
			
			for (Id receiverPointId : this.receiverPointId2timeInterval2noiseImmission.keySet()){
				
				bw.write(receiverPointId.toString()+";"); 
				for(int i = 0 ; i < 26 ; i++) {
					log.info(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).get((i+1)*3600.));
					log.info(receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get((i+1)*3600.));
					bw.write(";hour_"+i+";"+receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).get((i+1)*3600.)+";"+receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get((i+1)*3600.)+";");	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
