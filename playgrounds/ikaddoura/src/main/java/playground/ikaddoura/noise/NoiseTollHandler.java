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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

//
//import analysis.shapes.IKGISAnalyzerPostAnalysis;
//import analysis.shapes.IKNetworkPopulationWriter;

public class NoiseTollHandler implements NoiseEventHandler , NoiseEventAffectedHandler , LinkLeaveEventHandler {

	private static final Logger log = Logger.getLogger(NoiseTollHandler.class);
	
	private double timeBinSize = NoiseConfig.getTimeBinSize();
	
	private Scenario scenario;
	private EventsManager events;
	
	private List<NoiseEvent> noiseEvents = new ArrayList<NoiseEvent>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	private List<PersonMoneyEvent> moneyEvents = new ArrayList<PersonMoneyEvent>();
	
	private Map<Id, Map<Double, Double>> linkId2timeBin2tollSum = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Integer>> linkId2timeBin2leavingAgents = new HashMap<Id, Map<Double, Integer>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgToll = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Double>> linkId2timeBin2avgTollOldValue = new HashMap<Id, Map<Double, Double>>();
	
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCost = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCostPerCar = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCostPerHdv = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,List<Id>>> linkId2timeInterval2leavingAgents = new HashMap<Id, Map<Double,List<Id>>>();
	
	private Map<Id,Double> personId2tollSum = new HashMap<Id, Double>();
	private Map<Id,Double> personId2damageSum = new HashMap<Id, Double>();
	private Map<Id,Double> personId2homeBasedDamageSum = new HashMap<Id, Double>();
	private Map<Id,Double> personId2differenceTollDamage = new HashMap<Id, Double>();
	
	private Map<Id,Id> personId2homeReceiverPointId = new HashMap<Id, Id>();
	
	private Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2costShare = new HashMap<Id, Map<Double,Map<Id,Double>>>();
	
	private List<LinkLeaveEvent> linkLeaveEvents = new ArrayList<LinkLeaveEvent>();
	
	private double totalToll = 0;
	private double totalTollAffected = 0;
	
	private SpatialInfo spatialInfo;
	private NoiseHandler noiseHandler;
	
	private double vtts_car;
	
	private NoiseImmissionCalculator noiseImmissionCalculator = new NoiseImmissionCalculator(spatialInfo);
	
	public NoiseTollHandler (Scenario scenario , EventsManager events, SpatialInfo spatialInfo, NoiseHandler noiseHandler) {
		this.scenario = scenario;
		this.events = events;
		this.spatialInfo = spatialInfo;
		this.noiseHandler = noiseHandler;
		
		this.vtts_car = (scenario.getConfig().planCalcScore().getTraveling_utils_hr() - scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("VTTS_car: " + vtts_car);
	}
	
	@Override
	public void reset(int iteration) {
		
		linkId2timeBin2tollSum.clear();
		linkId2timeBin2leavingAgents.clear();
		
		linkId2timeBin2avgTollOldValue.clear();
		linkId2timeBin2avgTollOldValue.putAll(linkId2timeBin2avgToll);
		linkId2timeBin2avgToll.clear();
		
		// TODO resetten/clearen
		noiseEvents.clear();
		moneyEvents.clear();
		linkId2timeInterval2damageCost.clear();
		linkId2timeInterval2damageCostPerCar.clear();
		linkId2timeInterval2damageCostPerHdv.clear();
		linkId2timeInterval2leavingAgents.clear();
		receiverPointIds2timeIntervals2noiseLinks2costShare.clear();
		linkLeaveEvents.clear();
		personId2tollSum.clear();
		personId2damageSum.clear();
		personId2homeBasedDamageSum.clear();
		personId2differenceTollDamage.clear();

		log.info("totalToll in previous iteration: "+totalToll);
		totalToll = 0;
		log.info("totalTollAffected in previous iteration: "+totalTollAffected);
		totalTollAffected = 0;
	}

	public void calculateCostSharesPerLinkPerTimeInterval() {
		// preparation
		for(Id coordId : this.noiseHandler.getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission().keySet()) {
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2costShare = new HashMap<Double, Map<Id,Double>>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				Map<Id,Double> noiseLinks2costShare = new HashMap<Id, Double>();
				for(Id linkId : noiseHandler.getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission().get(coordId).get(timeInterval).keySet()) {
//					log.info(linkId);
//					log.info(NoiseHandler.receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).get(linkId));
					double noiseImmission = noiseHandler.getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission().get(coordId).get(timeInterval).get(linkId);
//					log.info("noiseImmission: "+noiseImmission);
					double resultingNoiseImmission = noiseHandler.getReceiverPointId2timeInterval2noiseImmission().get(coordId).get(timeInterval);
//					log.info("resultingNoiseImmission "+resultingNoiseImmission);
					double costShare = noiseImmissionCalculator.calculateShareOfResultingNoiseImmission(noiseImmission, resultingNoiseImmission);
//					log.info("costShare: "+costShare);
					double costs = costShare * noiseHandler.getReceiverPointId2timeInterval2damageCost().get(coordId).get(timeInterval);
//					log.info("costs: "+costs);
//					System.out.println("++++++++++++"+linkId2timeBin2leavingAgents);
//					System.out.println("vorher:    costs: "+costs+" , linkId: "+linkId+" , timeInterval: "+timeInterval);
//					if(linkId2timeBin2leavingAgents.containsKey(linkId)) {
//						if(linkId2timeBin2leavingAgents.get(linkId).containsKey(timeInterval)){
//						} else {
//							costs = 0.;
//						}
//					} else {
//						costs = 0.;
//					}
//					System.out.println("nachher:    costs: "+costs+" , linkId: "+linkId+" , timeInterval: "+timeInterval);
//					log.info(NoiseHandler.receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval));
//					log.info(costs);
					noiseLinks2costShare.put(linkId, costs);
//					log.info(linkId);
					if(costs>0){
//						log.info(linkId+"  "+costs+"  "+timeInterval);
//						log.info(GetNearestReceiverPoint.receiverPoints.get(coordId));
//						log.info(NoiseHandler.receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval));
					}
				}
				timeIntervals2noiseLinks2costShare.put(timeInterval, noiseLinks2costShare);
			}
			receiverPointIds2timeIntervals2noiseLinks2costShare.put(coordId, timeIntervals2noiseLinks2costShare);
		}
//		log.info(receiverPointIds2timeIntervals2noiseLinks2costShare);
		
		//summing up the link-based-costs
		//initializing the map:
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double,Double> timeInterval2damageCost = new HashMap<Double, Double>();
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				timeInterval2damageCost.put(timeInterval, 0.);
			}
			linkId2timeInterval2damageCost.put(linkId,timeInterval2damageCost);
		}
		
//		log.info("receiverPointIds2timeIntervals2noiseLinks2costShare: "+receiverPointIds2timeIntervals2noiseLinks2costShare);
		for(Id coordId : spatialInfo.getReceiverPoints().keySet()) {
			for(Id linkId : spatialInfo.getReceiverPointId2relevantLinkIds().get(coordId)) {
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					double costs = receiverPointIds2timeIntervals2noiseLinks2costShare.get(coordId).get(timeInterval).get(linkId);
//					log.info("costs: "+costs);
					double sumBefore = linkId2timeInterval2damageCost.get(linkId).get(timeInterval);
//					log.info("sumBefore: "+sumBefore);
					double sumNew = sumBefore + costs;
//					log.info("sumNew: "+sumNew);
					linkId2timeInterval2damageCost.get(linkId).put(timeInterval, sumNew);
				}
			}
		}
	}

	public void calculateCostsPerVehiclePerLinkPerTimeInterval() {
		// TODO differentiation between vehicle-types
//		log.info(linkId2timeInterval2damageCost);
		for(Id linkId : linkId2timeInterval2damageCost.keySet()) {
			Map<Double,Double> timeInterval2damageCostPerCar = new HashMap<Double, Double>();
			for(double timeInterval : linkId2timeInterval2damageCost.get(linkId).keySet()) {
				double damageCostSum = linkId2timeInterval2damageCost.get(linkId).get(timeInterval);
//				log.info("damageCostSum: "+damageCostSum);
				double numberOfCarsLeaving = noiseHandler.getLinkId2timeInterval2linkLeaveEvents().get(linkId).get(timeInterval).size();
//				log.info("numberOfCarsLeaving: "+numberOfCarsLeaving);
				
//				int nCar = NoiseHandler.linkId2timeInterval2linkLeaveEventsCar.get(linkId).get(timeInterval).size();
//				int nHdv = NoiseHandler.linkId2timeInterval2linkLeaveEventsCar.get(linkId).get(timeInterval).size();
//				
//				double shareCar = 0.;
//				double shareHdv = 0.;
//				shareCar = ((nCar * Math.pow(10, 0.1*lCar)) / (nCar * Math.pow(10, 0.1*lCar))+(nHdv * Math.pow(10, 0.1*lHdv)))
				
				double damageCostPerCar = 0.;
				if(!(numberOfCarsLeaving == 0)) {
					damageCostPerCar = damageCostSum/numberOfCarsLeaving;
				}
				timeInterval2damageCostPerCar.put(timeInterval,damageCostPerCar);
			}
			linkId2timeInterval2damageCostPerCar.put(linkId, timeInterval2damageCostPerCar);
		}
		
		// linkId2timeInterval2damageCostPerHdv
		//TODO: differentiation
		
	}
	
	@Override
	public void handleEvent(NoiseEvent event) {
		double amount = event.getAmount() *(-1);
		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getVehicleId(), amount);
		events.processEvent(moneyEvent);
		moneyEvents.add(moneyEvent);
		noiseEvents.add(event);
	}
	
	@Override
	public void handleEvent(NoiseEventAffected event) {
		noiseEventsAffected.add(event);
	}

	public void throwNoiseEvents() {
//		log.info(linkId2timeInterval2damageCostPerCar);
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				double amountCar = (linkId2timeInterval2damageCostPerCar.get(linkId).get(timeInterval))/(NoiseConfig.getScaleFactor());
				double amountHdv = 0;
				List<LinkLeaveEvent> listLinkLeaveEventsTmp = noiseHandler.getLinkId2timeInterval2linkLeaveEvents().get(linkId).get(timeInterval);
				List<Id>  listIdsTmp = new ArrayList<Id>();
				
				// calculate shares for the affected Agents

				
				for(LinkLeaveEvent event : listLinkLeaveEventsTmp) {
					listIdsTmp.add(event.getVehicleId());
				}
				for(Id vehicleId : listIdsTmp) {
					// TODO: by now, only cars (no Hdv)
					double amount = 0.;
					if(!(noiseHandler.getHdvVehicles().contains(vehicleId))) {
						amount = amountCar;
//						log.info(linkId2timeInterval2damageCostPerCar);
//						log.info("amount: "+amount);
					} else {
						amount = amountHdv;
					}
					double time = timeInterval-1; // TODO: the real leaving time should be adapted,
					// but for the routing the linkEnterTime or the ActivityEndTime is necessary!
					Id agentId = vehicleId;
						
//					NoiseEvent_Interface noiseEvent = new NoiseEventImpl(time,linkId,vehicleId,agentId,amount);
					NoiseEvent noiseEvent = new NoiseEvent(time,agentId,vehicleId,amount,linkId);
					// for any causing-affected relation, the noiseEvents should be thrown,
					// then the later computation would be easier
					events.processEvent(noiseEvent);
//					log.info("time: "+time);
//					events.processEvent((Event) noiseEvent);
					totalToll = totalToll+amount;
//					log.info("amount: "+amount);
//					log.info("totalToll: "+totalToll);
					
					if(personId2tollSum.containsKey(agentId)) {
						double newTollSum = personId2tollSum.get(agentId) + amount;
						personId2tollSum.put(agentId,newTollSum);
					} else {
						personId2tollSum.put(agentId,amount);
					}
				}
			}
		}
	}
	
	public void throwNoiseEventsAffected() {
		for(Id receiverPointId : noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().keySet()) {
			for(Id personId : noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(receiverPointId).keySet()) {
				for(int actNumber : noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(receiverPointId).get(personId).keySet()) {
					double actStart = noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(receiverPointId).get(personId).get(actNumber).getFirst();
					double actEnd = noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(receiverPointId).get(personId).get(actNumber).getSecond();
					
					for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
						double time = timeInterval;
						Id agentId = personId;
						String actType = noiseHandler.getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(receiverPointId).get(timeInterval).get(personId).get(actNumber).getSecond();
						double factor = noiseHandler.getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(receiverPointId).get(timeInterval).get(personId).get(actNumber).getFirst();
						double costPerUnit = noiseHandler.getReceiverPointId2timeInterval2damageCostPerAffectedAgentUnit().get(receiverPointId).get(timeInterval);
//						log.info("costPerUnit: "+costPerUnit);
						double amount = factor * costPerUnit;
						
						NoiseEventAffected noiseEventAffected = new NoiseEventAffected(time,agentId,amount,receiverPointId,actType);
						// for any causing-affected relation, the noiseEvents should be thrown,
						// then the later computation would be easier
						events.processEvent(noiseEventAffected);
						totalTollAffected = totalTollAffected+amount;
//						log.info("amount: "+amount);
//						log.info("totalTollAffected: "+totalTollAffected);
						
						if(personId2damageSum.containsKey(personId)) {
							double newTollSum = personId2damageSum.get(personId) + amount;
							personId2damageSum.put(personId,newTollSum);
						} else {
							personId2damageSum.put(personId,amount);
						}
					}
				}
			}
		}
		
//		log.info(NoiseHandler.receiverPointId2ListOfHomeAgents);
		//for comparison the home-based-oriented calculation
		for(Id receiverPointId : noiseHandler.getReceiverPointId2ListOfHomeAgents().keySet()) {
			for(Id personId : noiseHandler.getReceiverPointId2ListOfHomeAgents().get(receiverPointId)) {
				personId2homeReceiverPointId.put(personId,receiverPointId);
			}
		}
//		log.info(personId2homeReceiverPointId);
		for(Id personId : personId2homeReceiverPointId.keySet()) {
//			log.info("personId: "+personId+" - receiverPointId"+personId2homeReceiverPointId.get(personId));
		}
		for(Id personId : scenario.getPopulation().getPersons().keySet()) {
			Id receiverPointId = personId2homeReceiverPointId.get(personId);
			double homeBasedDamageSum = 0.;
			for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
				double cost = 0.;
				if(noiseHandler.getReceiverPointId2timeInterval2damageCostPerAffectedAgentUnit().get(receiverPointId).containsKey(timeInterval)) {
					cost = noiseHandler.getReceiverPointId2timeInterval2damageCostPerAffectedAgentUnit().get(receiverPointId).get(timeInterval);
				} else {
					cost = 0.;
				}
				homeBasedDamageSum = homeBasedDamageSum + cost;
			}
			personId2homeBasedDamageSum.put(personId, homeBasedDamageSum);
		}
		
	}

	public void setLinkId2timeBin2avgToll() {
		if (!this.linkId2timeBin2tollSum.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2tollSum should be empty!");
		} else {
			// calculate toll sum for each link and time bin
			setlinkId2timeBin2tollSum();
		}
		
		if (!this.linkId2timeBin2leavingAgents.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2leavingAgents should be empty!");
		} else {
			// calculate leaving agents for each link and time bin
			setlinkId2timeBin2leavingAgents();
		}
		
		if (!this.linkId2timeBin2avgToll.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2avgToll should be empty!");
		} else {
			// calculate average toll for each link and time bin
				
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()) {
				Map<Double, Double> timeBin2tollSum = this.linkId2timeBin2tollSum.get(linkId);
				Map<Double, Double> timeBin2avgToll = new HashMap<Double, Double>();

				for (Double timeBin : timeBin2tollSum.keySet()){
					double avgToll = 0.0;
					double tollSum = timeBin2tollSum.get(timeBin);
					if (tollSum == 0.) {
						// avg toll is zero for this time bin on that link
					} else {
//						log.info(linkId2timeBin2leavingAgents);
						int leavingAgents = 0;
//						log.info(linkId2timeBin2leavingAgents);
						if(linkId2timeBin2leavingAgents.get(linkId).containsKey(timeBin)) {
							leavingAgents = linkId2timeBin2leavingAgents.get(linkId).get(timeBin);
								if(!(this.linkId2timeBin2leavingAgents.get(linkId).get(timeBin) == null)) {
									avgToll = (tollSum/(NoiseConfig.getScaleFactor())) / leavingAgents;
									if(leavingAgents==0) {
										avgToll = 0.;
									}
								}
								avgToll = (tollSum/(NoiseConfig.getScaleFactor())) / leavingAgents;
							} else {
								avgToll = 0.;
							}
//						log.info("linkId: " + linkId + " // timeBin: " + Time.writeTime(timeBin, Time.TIMEFORMAT_HHMMSS) + " // toll sum: " + tollSum + " // leaving agents: " + leavingAgents + " // avg toll: " + avgToll);
					}
					timeBin2avgToll.put(timeBin, avgToll);
				}
				linkId2timeBin2avgToll.put(linkId , timeBin2avgToll);
			}
		}
	}
	
	private void setlinkId2timeBin2leavingAgents() {
//		log.info("leavingEvents: "+linkLeaveEvents);
		for (LinkLeaveEvent event : this.linkLeaveEvents){
			
			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())){
				// Tolls paid on this link.
				
				Map<Double, Integer> timeBin2leavingAgents = new HashMap<Double, Integer>();

				if (this.linkId2timeBin2leavingAgents.containsKey(event.getLinkId())) {
					// link already in map
					timeBin2leavingAgents = this.linkId2timeBin2leavingAgents.get(event.getLinkId());
					
					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							// update leaving agents on this link and in this time bin
							
							if (timeBin2leavingAgents.get(time) != null) {
								// not the first agent leaving this link in this time bin
								int leavingAgentsSoFar = timeBin2leavingAgents.get(time);
								int leavingAgents = leavingAgentsSoFar + 1;
								timeBin2leavingAgents.put(time, leavingAgents);
							} else {
								// first leaving agent leaving this link in this time bin
								timeBin2leavingAgents.put(time, 1);
							}
						}
					}
					linkId2timeBin2leavingAgents.put(event.getLinkId(), timeBin2leavingAgents);

				} else {
					// link not yet in map

					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							timeBin2leavingAgents.put(time, 1);
						}
					}
					linkId2timeBin2leavingAgents.put(event.getLinkId(), timeBin2leavingAgents);
				}	
			
			} else {
				// No tolls paid on that link. Skip that link.
		
			}
		}
		
	}
	
	private void setlinkId2timeBin2tollSum() {
//		log.info("noiseEvents: "+noiseEvents);
		for (NoiseEvent event : this.noiseEvents) {
			Map<Double, Double> timeBin2tollSum = new HashMap<Double, Double>();		
//			log.info(event.getAmount());
			double amount = 0.0;
			
			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())) {
				// link already in map
				timeBin2tollSum = this.linkId2timeBin2tollSum.get(event.getLinkId());

				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;
					
					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// noise event in time bin
						// update toll sum of this link and time bin
						
						if (timeBin2tollSum.get(time) != null) {
							// toll sum was calculated before for this time bin
							double sum = timeBin2tollSum.get(time);
						
							amount = event.getAmount();
							
							double sumNew = sum + amount;
							timeBin2tollSum.put(time, sumNew);
						
						} else {
							// toll sum was not calculated before for this time bin
							amount = event.getAmount();
						
							timeBin2tollSum.put(time, amount);
							
						}
					}
				}
				linkId2timeBin2tollSum.put(event.getLinkId(),timeBin2tollSum);

			} else {
				// link not yet in map
				
				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;

					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// noise event in time bin
						amount = event.getAmount();

						timeBin2tollSum.put(time, amount);
					}
				}
				linkId2timeBin2tollSum.put(event.getLinkId(),timeBin2tollSum);
			}
		}
	}
	
	public double getAvgToll(Link link, double time) {
		double avgToll = 0.;
		if (this.linkId2timeBin2avgTollOldValue.containsKey(link.getId())){
			Map<Double, Double> timeBin2avgToll = this.linkId2timeBin2avgTollOldValue.get(link.getId());
			for (Double timeBin : timeBin2avgToll.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgToll = timeBin2avgToll.get(timeBin);
				}
			}
		}
		return avgToll;
	}

	public Map<Id,Double> getPersonId2tollSum(String fileName) {
		File file = new File(fileName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;tollSum");
			bw.newLine();
			
			for (Id personId : this.personId2tollSum.keySet()){
				double toll = personId2tollSum.get(personId);
				
				bw.write(personId + ";" + toll);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		IKNetworkPopulationWriter.exportActivityConnection2DoubleValue(scenario , personId2tollSum , "toll");
		return personId2tollSum;
	}
	
	public Map<Id,Double> getPersonId2damageSum(String fileName) {

		File file = new File(fileName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;damageSum");
			bw.newLine();
			
			for (Id personId : this.personId2damageSum.keySet()){
				double damage = personId2damageSum.get(personId);
				
				bw.write(personId + ";" + damage);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		IKNetworkPopulationWriter.exportActivityConnection2DoubleValue(scenario , personId2damageSum , "damage");
		return personId2damageSum;
	}
	
	public Map<Id,Double> getPersonId2homeBasedDamageSum(String fileName) {
		File file = new File(fileName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;homeBasedDamageSum");
			bw.newLine();
			
			for (Id personId : this.personId2homeBasedDamageSum.keySet()){
				double difference = personId2homeBasedDamageSum.get(personId);
				
				bw.write(personId + ";" + difference);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		IKNetworkPopulationWriter.exportActivityConnection2DoubleValue(scenario , personId2homeBasedDamageSum , "homeBasedDamage");
		return personId2homeBasedDamageSum;
	}
	
	public Map<Id,Double> getPersonId2differenceTollDamage(String fileName) {
		for(Id personId : personId2tollSum.keySet()) {
			double toll = personId2tollSum.get(personId);
			double damage = personId2damageSum.get(personId);
			double difference = toll - damage;
			personId2differenceTollDamage.put(personId, difference);
		}
		File file = new File(fileName);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;differenceTollDamage");
			bw.newLine();
			
			for (Id personId : this.personId2differenceTollDamage.keySet()){
				double difference = personId2differenceTollDamage.get(personId);
				
				bw.write(personId + ";" + difference);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		IKNetworkPopulationWriter.exportActivityConnection2DoubleValue(scenario , personId2differenceTollDamage , "differenceTollDamage");
		return personId2differenceTollDamage;
	}
	
	public void writeTollStats(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;total toll (per day);leaving agents (per day);toll per agent;toll per agent per km;toll per agent per km (classified)");
			bw.newLine();
			
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()){
				double totalToll = 0.;
				int leavingAgents = 0;
				double tollPerAgent = 0.;
				double tollPerAgentPerKm = 0.;
				
				for (Double tollSum_timeBin : this.linkId2timeBin2tollSum.get(linkId).values()){
					totalToll = totalToll + tollSum_timeBin;
				}
				
				for (Integer leavingAgents_timeBin : this.linkId2timeBin2leavingAgents.get(linkId).values()){
					leavingAgents = leavingAgents + leavingAgents_timeBin;
				}
				
				tollPerAgent = totalToll/leavingAgents;
				tollPerAgentPerKm = 1000.*(tollPerAgent/scenario.getNetwork().getLinks().get(linkId).getLength());
				int tollPerAgentPerKmClassified = classifyTollPerAgentPerKm(tollPerAgentPerKm);
				
				bw.write(linkId + ";" + totalToll + ";" + leavingAgents + ";" + tollPerAgent + ";" + tollPerAgentPerKm +";" + tollPerAgentPerKmClassified);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsOnlyHomeActivities(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;total toll (per day);leaving agents (per day);toll per agent;toll per agent per km;toll per agent per km (classified)");
			bw.newLine();
			
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()){
				double totalToll = 0.;
				int leavingAgents = 0;
				double tollPerAgent = 0.;
				double tollPerAgentPerKm = 0.;
				
				for (Double tollSum_timeBin : this.linkId2timeBin2tollSum.get(linkId).values()){
					totalToll = totalToll + tollSum_timeBin;
				}
				
				for (Integer leavingAgents_timeBin : this.linkId2timeBin2leavingAgents.get(linkId).values()){
					leavingAgents = leavingAgents + leavingAgents_timeBin;
				}
				
				tollPerAgent = totalToll/leavingAgents;
				tollPerAgentPerKm = 1000.*(tollPerAgent/scenario.getNetwork().getLinks().get(linkId).getLength());
				int tollPerAgentPerKmClassified = classifyTollPerAgentPerKm(tollPerAgentPerKm);
				
				bw.write(linkId + ";" + totalToll + ";" + leavingAgents + ";" + tollPerAgent + ";" + tollPerAgentPerKm +";" + tollPerAgentPerKmClassified);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//LOESCHEN
	Map<Id,Map<Double,List<NoiseEvent>>> mapTMP = new HashMap<Id, Map<Double,List<NoiseEvent>>>();
	
	public void writeTollStatsPerHour(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write("link;hour0;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour1;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour2;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour3;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour4;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour5;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour6;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour7;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour8;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour9;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour10;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour11;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour12;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour13;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour14;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour15;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour16;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour17;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour18;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour19;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour20;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour21;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour22;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour23;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour24;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified);hour25;total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified)");
			bw.write("link");
			for(int i = 0; i < 26 ; i++) {
				bw.write(";hour"+i+";total toll (per hour);leaving agents (per hour);toll per agent;toll per agent per km;toll per agent per km (classified)");
			}
			bw.newLine();
			
			//LOESCHEN
			for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
				Map<Double,List<NoiseEvent>> timeInterval2noiseEvents = new HashMap<Double, List<NoiseEvent>>();
				for(double timeInterval = NoiseConfig.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfig.getIntervalLength()) {
					List<NoiseEvent> listNoiseEvents = new ArrayList<NoiseEvent>();
					timeInterval2noiseEvents.put(timeInterval, listNoiseEvents);
				}
				mapTMP.put(linkId, timeInterval2noiseEvents);
			}
			
			//LOESCHEN
			for(NoiseEvent ne : noiseEvents) {
				Id link_id = ne.getLinkId();
				double time_ = (((int) (ne.getTime()/3600.))+1)*3600 ;
				
				Map <Double, List<NoiseEvent>> mapTmp2 = mapTMP.get(link_id);
				List<NoiseEvent> listTmp2 = mapTmp2.get(time_);
				
				listTmp2.add(ne);
				mapTmp2.put(time_, listTmp2);
				
				mapTMP.put(link_id, mapTmp2);
			}
			
			
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()){
				
				Map<Integer,Double> hour2toll = new HashMap<Integer, Double>();
				Map<Integer,Integer> hour2leavingAgents = new HashMap<Integer, Integer>();
				Map<Integer,Double> hour2tollPerAgent = new HashMap<Integer, Double>();
				Map<Integer,Double> hour2tollPerAgentPerKm = new HashMap<Integer, Double>();
				Map<Integer,Integer> hour2tollPerAgentPerKmClassified = new HashMap<Integer, Integer>();
				
				for (int i = 0 ; i<31 ; i++) {
					hour2toll.put(i, 0.);
					hour2leavingAgents.put(i, 0);
				}
				
				Map<Double,Double> timeBin2tollSum = linkId2timeBin2tollSum.get(linkId);
				
				for (Double timeBin : timeBin2tollSum.keySet()){
					int hour = (int) (timeBin /3600);
					double toll = hour2toll.get(hour) + timeBin2tollSum.get(timeBin);
					hour2toll.put(hour, toll);
				}
				
				Map<Double,Integer> timeBin2leavingAgents = linkId2timeBin2leavingAgents.get(linkId);
				
				for (Double timeBin : timeBin2leavingAgents.keySet()){
					int hour = (int) (timeBin /3600);
					int leavingAgents = hour2leavingAgents.get(hour) + timeBin2leavingAgents.get(timeBin);
					hour2leavingAgents.put(hour, leavingAgents);
				}
				
				for (int i = 0 ; i<31 ; i++) {
					hour2tollPerAgent.put(i, hour2toll.get(i)/hour2leavingAgents.get(i));
					hour2tollPerAgentPerKm.put(i, 1000.*(hour2tollPerAgent.get(i)/scenario.getNetwork().getLinks().get(linkId).getLength()));
					hour2tollPerAgentPerKmClassified.put(i, classifyTollPerAgentPerKm(hour2tollPerAgentPerKm.get(i)));
				}
				
//				bw.write(linkId + ";" + "" + ";"  + hour2toll.get(0) + ";" + hour2leavingAgents.get(0) + ";" + hour2tollPerAgent.get(0) + ";" + hour2tollPerAgentPerKm.get(0) +";" + hour2tollPerAgentPerKmClassified.get(0) +";" + "" + ";"  + hour2toll.get(1) + ";" + hour2leavingAgents.get(1) + ";" + hour2tollPerAgent.get(1) + ";" + hour2tollPerAgentPerKm.get(1) +";" + hour2tollPerAgentPerKmClassified.get(1) +";" + "" + ";"  + hour2toll.get(2) + ";" + hour2leavingAgents.get(2) + ";" + hour2tollPerAgent.get(2) + ";" + hour2tollPerAgentPerKm.get(2) +";" + hour2tollPerAgentPerKmClassified.get(2) +";" + "" + ";"  + hour2toll.get(3) + ";" + hour2leavingAgents.get(3) + ";" + hour2tollPerAgent.get(3) + ";" + hour2tollPerAgentPerKm.get(3) +";" + hour2tollPerAgentPerKmClassified.get(3) +";" + "" + ";"  + hour2toll.get(4) + ";" + hour2leavingAgents.get(4) + ";" + hour2tollPerAgent.get(4) + ";" + hour2tollPerAgentPerKm.get(4) +";" + hour2tollPerAgentPerKmClassified.get(4) +";" + "" + ";"  + hour2toll.get(5) + ";" + hour2leavingAgents.get(5) + ";" + hour2tollPerAgent.get(5) + ";" + hour2tollPerAgentPerKm.get(5) +";" + hour2tollPerAgentPerKmClassified.get(5) +";" + "" + ";"  + hour2toll.get(6) + ";" + hour2leavingAgents.get(6) + ";" + hour2tollPerAgent.get(6) + ";" + hour2tollPerAgentPerKm.get(6) +";" + hour2tollPerAgentPerKmClassified.get(6) +";" + "" + ";"  + hour2toll.get(7) + ";" + hour2leavingAgents.get(7) + ";" + hour2tollPerAgent.get(7) + ";" + hour2tollPerAgentPerKm.get(7) +";" + hour2tollPerAgentPerKmClassified.get(7) +";" + "" + ";"  + hour2toll.get(8) + ";" + hour2leavingAgents.get(8) + ";" + hour2tollPerAgent.get(8) + ";" + hour2tollPerAgentPerKm.get(8) +";" + hour2tollPerAgentPerKmClassified.get(8) +";" + "" + ";"  + hour2toll.get(9) + ";" + hour2leavingAgents.get(9) + ";" + hour2tollPerAgent.get(9) + ";" + hour2tollPerAgentPerKm.get(9) +";" + hour2tollPerAgentPerKmClassified.get(9) +";" + "" + ";"  + hour2toll.get(10) + ";" + hour2leavingAgents.get(10) + ";" + hour2tollPerAgent.get(10) + ";" + hour2tollPerAgentPerKm.get(10) +";" + hour2tollPerAgentPerKmClassified.get(10) +";" + "" + ";"  + hour2toll.get(11) + ";" + hour2leavingAgents.get(11) + ";" + hour2tollPerAgent.get(11) + ";" + hour2tollPerAgentPerKm.get(11) +";" + hour2tollPerAgentPerKmClassified.get(11) +";" + "" + ";"  + hour2toll.get(12) + ";" + hour2leavingAgents.get(12) + ";" + hour2tollPerAgent.get(12) + ";" + hour2tollPerAgentPerKm.get(12) +";" + hour2tollPerAgentPerKmClassified.get(12) +";" + "" + ";"  + hour2toll.get(13) + ";" + hour2leavingAgents.get(13) + ";" + hour2tollPerAgent.get(13) + ";" + hour2tollPerAgentPerKm.get(13) +";" + hour2tollPerAgentPerKmClassified.get(13) +";" + "" + ";"  + hour2toll.get(14) + ";" + hour2leavingAgents.get(14) + ";" + hour2tollPerAgent.get(14) + ";" + hour2tollPerAgentPerKm.get(14) +";" + hour2tollPerAgentPerKmClassified.get(14) +";" + "" + ";"  + hour2toll.get(15) + ";" + hour2leavingAgents.get(15) + ";" + hour2tollPerAgent.get(15) + ";" + hour2tollPerAgentPerKm.get(15) +";" + hour2tollPerAgentPerKmClassified.get(15) +";" + "" + ";"  + hour2toll.get(16) + ";" + hour2leavingAgents.get(16) + ";" + hour2tollPerAgent.get(16) + ";" + hour2tollPerAgentPerKm.get(16) +";" + hour2tollPerAgentPerKmClassified.get(16) +";" + "" + ";"  + hour2toll.get(17) + ";" + hour2leavingAgents.get(17) + ";" + hour2tollPerAgent.get(17) + ";" + hour2tollPerAgentPerKm.get(17) +";" + hour2tollPerAgentPerKmClassified.get(17) +";" + "" + ";"  + hour2toll.get(18) + ";" + hour2leavingAgents.get(18) + ";" + hour2tollPerAgent.get(18) + ";" + hour2tollPerAgentPerKm.get(18) +";" + hour2tollPerAgentPerKmClassified.get(18) +";" + "" + ";"  + hour2toll.get(19) + ";" + hour2leavingAgents.get(19) + ";" + hour2tollPerAgent.get(19) + ";" + hour2tollPerAgentPerKm.get(19) +";" + hour2tollPerAgentPerKmClassified.get(19) +";" + "" + ";"  + hour2toll.get(20) + ";" + hour2leavingAgents.get(20) + ";" + hour2tollPerAgent.get(20) + ";" + hour2tollPerAgentPerKm.get(20) +";" + hour2tollPerAgentPerKmClassified.get(20) +";" + "" + ";"  + hour2toll.get(21) + ";" + hour2leavingAgents.get(21) + ";" + hour2tollPerAgent.get(21) + ";" + hour2tollPerAgentPerKm.get(21) +";" + hour2tollPerAgentPerKmClassified.get(21) +";"+ "" + ";"  + hour2toll.get(22) + ";" + hour2leavingAgents.get(22) + ";" + hour2tollPerAgent.get(22) + ";" + hour2tollPerAgentPerKm.get(22) +";" + hour2tollPerAgentPerKmClassified.get(22) +";"  + "" + ";"  + hour2toll.get(23) + ";" + hour2leavingAgents.get(23) + ";" + hour2tollPerAgent.get(23) + ";" + hour2tollPerAgentPerKm.get(23) + ";" + hour2tollPerAgentPerKmClassified.get(23) +";"  + "" + ";"  + hour2toll.get(24) + ";" + hour2leavingAgents.get(24) + ";" + hour2tollPerAgent.get(24) + ";" + hour2tollPerAgentPerKm.get(24) + ";" + hour2tollPerAgentPerKmClassified.get(24) +";"  + "" + ";"  + hour2toll.get(25) + ";" + hour2leavingAgents.get(25) + ";" + hour2tollPerAgent.get(25) + ";" + hour2tollPerAgentPerKm.get(25) + ";" + hour2tollPerAgentPerKmClassified.get(25));
				bw.write(linkId.toString()); 
				for(int i = 0 ; i < 26 ; i++) {
					bw.write(";"+""+";"+hour2toll.get(i) + ";" + hour2leavingAgents.get(i) + ";" + hour2tollPerAgent.get(i) + ";" + hour2tollPerAgentPerKm.get(i) +";" + hour2tollPerAgentPerKmClassified.get(i));	
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeTollStatsPerActivity(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("actType;;sumTollAffected;shareTollAffected;;sumActivityDuration;shareActivityDuration");
			bw.newLine();
			
			List<String> actTypesTollAffected = new ArrayList<String>();
			List<String> actTypesActivityDuration = new ArrayList<String>();
			Map<String,Double> actType2sumTollAffected = new HashMap<String, Double>();
			Map<String,Double> actType2sumActivityDuration = new HashMap<String, Double>();
			
			for(Id receiverPointId : noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().keySet()) {
				for(Id personId : noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(receiverPointId).keySet()) {
					for(int actNumber : noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(receiverPointId).get(personId).keySet()) {
						double actStart = noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(receiverPointId).get(personId).get(actNumber).getFirst();
						double actEnd = noiseHandler.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(receiverPointId).get(personId).get(actNumber).getSecond();
						
						if(actEnd>24*3600){
							actEnd = 24*3600;
						}
						if(actStart>24*3600){
							actStart = 24*3600;
						}
						double activityDuration = actEnd - actStart;
						
						String actType = noiseHandler.getPersonId2actNumber2actType().get(personId).get(actNumber);
						
						if(!(actTypesActivityDuration.contains(actType))) {
							actTypesActivityDuration.add(actType);
							actType2sumActivityDuration.put(actType, activityDuration);
						} else {
							double newSum = actType2sumActivityDuration.get(actType) + activityDuration;
							actType2sumActivityDuration.put(actType, newSum);
						}	
					}
				}
			}
			
			for(NoiseEventAffected event : noiseEventsAffected) {
				String actType = event.getActType();
				if(!(actTypesTollAffected.contains(actType))) {
					actTypesTollAffected.add(actType);
					actType2sumTollAffected.put(actType, event.getAmount());
				} else {
					double newSum = actType2sumTollAffected.get(actType) + event.getAmount();
					actType2sumTollAffected.put(actType, newSum);
				}	
			}
			
			double totalSumTollAffected = actType2sumTollAffected.get("home") + actType2sumTollAffected.get("work") + actType2sumTollAffected.get("secondary");
			double totalSumActivityDuration = actType2sumActivityDuration.get("home") + actType2sumActivityDuration.get("work") + actType2sumActivityDuration.get("secondary");
			bw.write("home;;"+actType2sumTollAffected.get("home")+";"+(actType2sumTollAffected.get("home")/totalSumTollAffected)+";;"+actType2sumActivityDuration.get("home")+";"+(actType2sumActivityDuration.get("home")/totalSumActivityDuration));
			bw.newLine();
			bw.write("work;;"+actType2sumTollAffected.get("work")+";"+(actType2sumTollAffected.get("work")/totalSumTollAffected)+";;"+actType2sumActivityDuration.get("work")+";"+(actType2sumActivityDuration.get("work")/totalSumActivityDuration));
			bw.newLine();	
			bw.write("secondary;;"+actType2sumTollAffected.get("secondary")+";"+(actType2sumTollAffected.get("secondary")/totalSumTollAffected)+";;"+actType2sumActivityDuration.get("secondary")+";"+(actType2sumActivityDuration.get("secondary")/totalSumActivityDuration));
			bw.newLine();	
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTollStatsCompareHomeVsActivityBased(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("personId;damage homeBased;damage activityBased");
			bw.newLine();
			
			for(Id personId : scenario.getPopulation().getPersons().keySet()) {
				
				double damageActivityBased = personId2damageSum.get(personId);
				double damageHomeBased = personId2homeBasedDamageSum.get(personId);
				
				bw.write(personId+";"+damageHomeBased+";"+damageActivityBased);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int classifyTollPerAgentPerKm(double tollPerAgentPerKm) {
		int classifiedValue = 0;
		
		if(tollPerAgentPerKm==0) {
			classifiedValue = 0;
		} else if(tollPerAgentPerKm<0.00005) {
			classifiedValue = 1;
		} else if(tollPerAgentPerKm<0.0001) {
			classifiedValue = 2;
		} else if(tollPerAgentPerKm<0.0002) {
			classifiedValue = 3;
		} else if(tollPerAgentPerKm<0.0004) {
			classifiedValue = 4;
		} else if(tollPerAgentPerKm<0.0008) {
			classifiedValue = 5;
		} else if(tollPerAgentPerKm<0.0016) {
			classifiedValue = 6;
		} else if(tollPerAgentPerKm<0.0032) {
			classifiedValue = 7;
		}  else if(tollPerAgentPerKm<0.0064) {
			classifiedValue = 8;
		}  else if(tollPerAgentPerKm<0.0128) {
			classifiedValue = 9;
		}  else if(tollPerAgentPerKm<0.0256) {
			classifiedValue = 10;
		}  else if(tollPerAgentPerKm<0.0512) {
			classifiedValue = 11;
		}  else if(tollPerAgentPerKm<0.1) {
			classifiedValue = 12;
		}  else if(tollPerAgentPerKm<0.2) {
			classifiedValue = 13;
		}  else if(tollPerAgentPerKm<0.4) {
			classifiedValue = 14;
		} else if((tollPerAgentPerKm>=0.4)&&(tollPerAgentPerKm<100000)) {
			classifiedValue = 15; 
		}
		
		return classifiedValue;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		linkLeaveEvents.add(event);
	}
}
