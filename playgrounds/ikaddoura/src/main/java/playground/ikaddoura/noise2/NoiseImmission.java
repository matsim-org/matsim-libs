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
package playground.ikaddoura.noise2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;

/**
 * 
 * Collects the relevant information in order to compute the noise immission for each receiver point.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseImmission {

	private static final Logger log = Logger.getLogger(NoiseImmission.class);
		
	private Scenario scenario;
	private EventsManager events;
	
	private NoiseSpatialInfo spatialInfo;
	private NoiseImmissionEquations noiseImmissionCalculator;
	private double annualCostRate;
		
	// from emission handler
	private Map<Id,Map<Double,Double>> linkId2timeInterval2noiseEmission;
	private List<Id> hdvVehicles;
	private Map<Id, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkEnterEvents;
	private Map<Id, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkEnterEventsCar;
	private Map<Id, Map<Double,List<LinkEnterEvent>>> linkId2timeInterval2linkEnterEventsHdv;
	
	// from person activity tracker
	private Map<Id,Map<Id,Map<Integer,Tuple<Double,Double>>>> receiverPointId2personId2actNumber2activityStartAndActivityEnd;
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits;
	private Map<Id,Map<Double,Map<Id,Map<Integer,Tuple<Double,String>>>>> receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType;
	private Map<Id,List<Id>> receiverPointId2ListOfHomeAgents;
	
	// optional information for a more detailed calculation of noise immission
	private final List<Id> tunnelLinks = new ArrayList<Id>();
	private final List<Id> noiseBarrierLinks = new ArrayList<Id>();
	
	// noise damage cost
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCost = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCostPerCar = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> linkId2timeInterval2damageCostPerHdvVehicle = new HashMap<Id, Map<Double,Double>>();
		
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCostPerAffectedAgentUnit = new HashMap<Id, Map<Double,Double>>();
	
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission = new HashMap<Id, Map<Double,Map<Id,Double>>>();

	// some additional analysis
	private Map<Id,Double> personId2tollSum = new HashMap<Id, Double>();
	private Map<Id,Double> personId2damageSum = new HashMap<Id, Double>();
	private Map<Id,Double> personId2homeBasedDamageSum = new HashMap<Id, Double>();
	private double totalToll = 0.;
	private double totalTollAffected = 0.;
	
	// to be filled during the computation of noise events
	private List<NoiseEvent> noiseEvents = new ArrayList<NoiseEvent>();
	private List<NoiseEvent> noiseEventsCar = new ArrayList<NoiseEvent>();
	private List<NoiseEvent> noiseEventsHdv = new ArrayList<NoiseEvent>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	
	public NoiseImmission (Scenario scenario , EventsManager events, NoiseSpatialInfo spatialInfo, double annualCostRate, NoiseEmissionHandler noiseEmissionHandler, PersonActivityHandler activityTracker) {
		this.scenario = scenario;
		this.events = events;
		this.spatialInfo = spatialInfo;
		this.noiseImmissionCalculator = new NoiseImmissionEquations();
		
		this.linkId2timeInterval2noiseEmission = noiseEmissionHandler.getLinkId2timeInterval2noiseEmission();
		this.hdvVehicles = noiseEmissionHandler.getHdvVehicles();
		this.linkId2timeInterval2linkEnterEvents = noiseEmissionHandler.getLinkId2timeInterval2linkEnterEvents();
		this.linkId2timeInterval2linkEnterEventsCar = noiseEmissionHandler.getLinkId2timeInterval2linkEnterEventsCar();
		this.linkId2timeInterval2linkEnterEventsHdv = noiseEmissionHandler.getLinkId2timeInterval2linkEnterEventsHdv();
		
		this.receiverPointId2personId2actNumber2activityStartAndActivityEnd = activityTracker.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd();
		this.receiverPointId2timeInterval2affectedAgentUnits = activityTracker.getReceiverPointId2timeInterval2affectedAgentUnits();
		this.receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType = activityTracker.getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType();
		this.receiverPointId2ListOfHomeAgents = activityTracker.getReceiverPointId2ListOfHomeAgents();
				
		if (annualCostRate == 0.) {
			throw new RuntimeException("Annual cost rate is zero. Aborting...");

		} else {
			log.info("Setting annual noise cost rate to " + annualCostRate);
			this.annualCostRate = annualCostRate;
		}
	}
	
	public void setTunnelLinks(ArrayList<Id> tunnelLinks) {
		if (tunnelLinks == null) {
			
		} else {
			this.tunnelLinks.addAll(tunnelLinks);
			// TODO
		}
	}
	
	public void setNoiseBarrierLinks(ArrayList<Id> noiseBarrierLinks) {
		if (noiseBarrierLinks == null) {
			
		} else {
			this.noiseBarrierLinks.addAll(noiseBarrierLinks);
			// TODO
		}
	}
	
	public void calculateNoiseImmission() {
		
		calculateImmissionSharesPerReceiverPointPerTimeInterval();
		calculateFinalNoiseImmissions();
		
		// calculate the duration of stay for each agent at each receiver point and sum up for each time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits)
		// calculate noise exposure (damage) for each receiver point (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost)

		log.info("calculateDamagePerReceiverPoint...");
		calculateDamagePerReceiverPoint();
		
		calculateCostSharesPerLinkPerTimeInterval();
						
		log.info("calculateCostsPerVehiclePerLinkPerTimeInterval...");
		calculateCostsPerVehiclePerLinkPerTimeInterval();
		
		throwNoiseEvents();
		throwNoiseEventsAffected();
	}

	private void calculateImmissionSharesPerReceiverPointPerTimeInterval() {
		
		for (Id coordId : spatialInfo.getReceiverPointId2Coord().keySet()) {
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2isolatedImmission = new HashMap<Double, Map<Id,Double>>();
		
			for (double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
			 	Map<Id,Double> noiseLinks2isolatedImmission = new HashMap<Id, Double>();
			
			 	for(Id linkId : spatialInfo.getReceiverPointId2relevantLinkIds().get(coordId)) {
					double noiseEmission = linkId2timeInterval2noiseEmission.get(linkId).get(timeInterval);
					double noiseImmission = 0.;
					Coord coord = spatialInfo.getReceiverPointId2Coord().get(coordId);
					if (!(noiseEmission == 0.)) {
						noiseImmission = emission2immission(this.spatialInfo , linkId , noiseEmission , coord);						
					} else {
//						log.info("emission is 0");
					}
					noiseLinks2isolatedImmission.put(linkId,noiseImmission);
				}
				timeIntervals2noiseLinks2isolatedImmission.put(timeInterval, noiseLinks2isolatedImmission);
			}
			receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.put(coordId, timeIntervals2noiseLinks2isolatedImmission);
		}
	}
	
	
	private double emission2immission(NoiseSpatialInfo spatialInfo, Id linkId, double noiseEmission, Coord coord) {
		double noiseImmission = 0.;
		
		Id receiverPointId = spatialInfo.getCoord2receiverPointId().get(coord);
	
		noiseImmission = noiseEmission
				+ spatialInfo.getReceiverPointId2relevantLinkId2correctionTermDs().get(receiverPointId).get(linkId)
				+ spatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(receiverPointId).get(linkId);
		
		if(noiseImmission < 0.) {
			noiseImmission = 0.;
		}
		return noiseImmission;
	}

	private void calculateFinalNoiseImmissions() {
		for(Id coordId : spatialInfo.getReceiverPointId2Coord().keySet()) {
			Map<Double,Double> timeInterval2noiseImmission = new HashMap<Double, Double>();
			for(double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
				List<Double> noiseImmissions = new ArrayList<Double>();
				if(!(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval)==null)) {
					for(Id linkId : receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).keySet()) {
						if(!(linkId2timeInterval2linkEnterEvents.get(linkId).get(timeInterval).size()==0.)) {
							noiseImmissions.add(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval).get(linkId));
						}
					}	
					double resultingNoiseImmission = noiseImmissionCalculator.calculateResultingNoiseImmission(noiseImmissions);
					timeInterval2noiseImmission.put(timeInterval, resultingNoiseImmission);
				} else {
					// if no link has to to be considered for the calculation due to too long distances
					timeInterval2noiseImmission.put(timeInterval, 0.);
				}
			}
			receiverPointId2timeInterval2noiseImmission.put(coordId, timeInterval2noiseImmission);
		}
	}
		
	private void calculateDamagePerReceiverPoint() {
		for(Id receiverPointId : receiverPointId2timeInterval2noiseImmission.keySet()) {
			for(double timeInterval : receiverPointId2timeInterval2noiseImmission.get(receiverPointId).keySet()) {
				double noiseImmission = receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get(timeInterval);
				double affectedAgentUnits = 0.;
				if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
					if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(timeInterval)) {
						affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).get(timeInterval);
					} 	
				}
				double damageCost = calculateDamageCosts(noiseImmission,affectedAgentUnits,timeInterval);
				double damageCostPerAffectedAgentUnit = calculateDamageCosts(noiseImmission,1.,timeInterval);
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
	}
	
	private double calculateDamageCosts(double noiseImmission, double affectedAgentUnits , double timeInterval) {
		String dayOrNight = "NIGHT";
		if (timeInterval>6*3600 && timeInterval<=22*3600) {
			dayOrNight = "DAY";
		} else if(timeInterval>18*3600 && timeInterval<=22*3600) {
			dayOrNight = "EVENING";
		}
		
		double lautheitsgewicht = calculateLautheitsgewicht(noiseImmission, dayOrNight);  
		
		double laermEinwohnerGleichwert = lautheitsgewicht*affectedAgentUnits;
		
		double damageCosts = 0.;
		if(dayOrNight == "DAY"){
			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/24.0);
		} else if(dayOrNight == "EVENING"){
			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/24.0);
		} else if(dayOrNight == "NIGHT"){
			damageCosts = (annualCostRate*laermEinwohnerGleichwert/(365))*(1.0/24.0);
		} else{
			throw new RuntimeException("Neither day nor night!");
		}
		return damageCosts;	
	}

	private double calculateLautheitsgewicht (double noiseImmission , String dayOrNight){
		double lautheitsgewicht = 0;
		
		if(dayOrNight == "DAY"){
			if (noiseImmission<50){
			} else{
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 50));
			}
		} else if(dayOrNight == "EVENING"){
			if(noiseImmission<45){
			}else{
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 45));
			}
		} else if(dayOrNight == "NIGHT"){
			if(noiseImmission<40){
			}else{
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 40));
			}
		} else{
			throw new RuntimeException("Neither day nor night!");
		}
		
		return lautheitsgewicht;
		
	}

	private void calculateCostSharesPerLinkPerTimeInterval() {
		
		Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2costShare = new HashMap<Id, Map<Double,Map<Id,Double>>>();

		// preparation
		for(Id coordId : receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.keySet()) {
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2isolatedImmission = receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId);
			Map<Double,Map<Id,Double>> timeIntervals2noiseLinks2costShare = new HashMap<Double, Map<Id,Double>>();
			for(double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
				Map<Id,Double> noiseLinks2isolatedImmission = timeIntervals2noiseLinks2isolatedImmission.get(timeInterval);
				Map<Id,Double> noiseLinks2costShare = new HashMap<Id, Double>();
				double resultingNoiseImmission = receiverPointId2timeInterval2noiseImmission.get(coordId).get(timeInterval);
//				System.out.println(coordId);
//				System.out.println(timeInterval);
//				System.out.println(receiverPointId2timeInterval2damageCost.toString());
				if(!((receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval)) == 0.)) {
					for(Id linkId : noiseLinks2isolatedImmission.keySet()) {
						double noiseImmission = noiseLinks2isolatedImmission.get(linkId);
						double costs = 0.;
						if(!(noiseImmission==0.)) {
							double costShare = noiseImmissionCalculator.calculateShareOfResultingNoiseImmission(noiseImmission, resultingNoiseImmission);
							costs = costShare * receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval);
							
						}
						noiseLinks2costShare.put(linkId, costs);
					}
				}
				timeIntervals2noiseLinks2costShare.put(timeInterval, noiseLinks2costShare);
			}
			receiverPointIds2timeIntervals2noiseLinks2costShare.put(coordId, timeIntervals2noiseLinks2costShare);
		}
		
		//summing up the link-based-costs
		//initializing the map:
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double,Double> timeInterval2damageCost = new HashMap<Double, Double>();
			for(double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
				timeInterval2damageCost.put(timeInterval, 0.);
			}
			linkId2timeInterval2damageCost.put(linkId,timeInterval2damageCost);
		}

		for(Id coordId : spatialInfo.getReceiverPointId2Coord().keySet()) {
			for(Id linkId : spatialInfo.getReceiverPointId2relevantLinkIds().get(coordId)) {
				for(double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
					if(!((receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval))==0.)) {
						double costs = receiverPointIds2timeIntervals2noiseLinks2costShare.get(coordId).get(timeInterval).get(linkId);
						double sumBefore = linkId2timeInterval2damageCost.get(linkId).get(timeInterval);
						double sumNew = sumBefore + costs;
						linkId2timeInterval2damageCost.get(linkId).put(timeInterval, sumNew);
					}
				}
			}
		}
	}

	private void calculateCostsPerVehiclePerLinkPerTimeInterval() {
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
//		for(Id linkId : linkId2timeInterval2damageCost.keySet()) {
			Map<Double,Double> timeInterval2damageCostPerCar = new HashMap<Double, Double>();
			Map<Double,Double> timeInterval2damageCostPerHdvVehicle = new HashMap<Double, Double>();
//			for(double timeInterval : linkId2timeInterval2damageCost.get(linkId).keySet()) {
			for(double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval<=30*3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
				double damageCostSum = 0.;
				if(linkId2timeInterval2damageCost.containsKey(linkId)) {
					if(linkId2timeInterval2damageCost.get(linkId).containsKey(timeInterval)) {
						damageCostSum = linkId2timeInterval2damageCost.get(linkId).get(timeInterval);
					}
				}
				
				int nCar = linkId2timeInterval2linkEnterEventsCar.get(linkId).get(timeInterval).size();
				int nHdv = linkId2timeInterval2linkEnterEventsHdv.get(linkId).get(timeInterval).size();
			
				double vCar = (scenario.getNetwork().getLinks().get(linkId).getFreespeed())*3.6;
				double vHdv = vCar;
				// TODO: If different speeds for different vehicle types have to be considered, adapt the calculation here.
				// For example, a maximum speed for hdv-vehicles could be set here (for instance for German highways) 
				
				double lCar = 27.7 + (10 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
				double lHdv = 23.1 + (12.5 * Math.log10(vHdv));
				
				double shareCar = 0.;
				double shareHdv = 0.;
				
				if((nCar>0)||(nHdv>0)) {
					shareCar = ((nCar * Math.pow(10, 0.1*lCar)) / ((nCar * Math.pow(10, 0.1*lCar))+(nHdv * Math.pow(10, 0.1*lHdv))));
					shareHdv = ((nHdv * Math.pow(10, 0.1*lHdv)) / ((nCar * Math.pow(10, 0.1*lCar))+(nHdv * Math.pow(10, 0.1*lHdv))));
					if((!(((shareCar+shareHdv)>0.999)&&((shareCar+shareHdv)<1.001)))) {
						
						log.warn("The sum of shareCar and shareHdv is not equal to 1.0! The value is "+(shareCar+shareHdv));
					}
				}
				double damageCostSumCar = shareCar * damageCostSum;
				double damageCostSumHdv = shareHdv * damageCostSum;
				
				double damageCostPerCar = 0.;
				if(!(nCar == 0)) {
					damageCostPerCar = damageCostSumCar/nCar;
				}
				timeInterval2damageCostPerCar.put(timeInterval,damageCostPerCar);
				
				double damageCostPerHdvVehicle = 0.;
				if(!(nHdv == 0)) {
					damageCostPerHdvVehicle = damageCostSumHdv/nHdv;
				}
				timeInterval2damageCostPerHdvVehicle.put(timeInterval,damageCostPerHdvVehicle);
			}
			linkId2timeInterval2damageCostPerCar.put(linkId, timeInterval2damageCostPerCar);
			linkId2timeInterval2damageCostPerHdvVehicle.put(linkId, timeInterval2damageCostPerHdvVehicle);
		}
	}

	private void throwNoiseEvents() {
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			for(double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
				double amountCar = (linkId2timeInterval2damageCostPerCar.get(linkId).get(timeInterval))/(NoiseConfigParameters.getScaleFactor());
				double amountHdv = (linkId2timeInterval2damageCostPerHdvVehicle.get(linkId).get(timeInterval))/(NoiseConfigParameters.getScaleFactor());
				
				List<LinkEnterEvent> listLinkLeaveEventsTmp = linkId2timeInterval2linkEnterEvents.get(linkId).get(timeInterval);
				List<Id>  listIdsTmp = new ArrayList<Id>();
				
				// calculate shares for the affected Agents
				for(LinkEnterEvent event : listLinkLeaveEventsTmp) {
					listIdsTmp.add(event.getVehicleId());
				}
				for(Id vehicleId : listIdsTmp) {
					double amount = 0.;
					boolean isHdv = false;
					
					if(!(hdvVehicles.contains(vehicleId))) {
						amount = amountCar;
					} else {
						amount = amountHdv;
						isHdv = true;
					}
					double time = timeInterval-1;
					Id agentId = vehicleId;
					NoiseVehicleType carOrHdv = NoiseVehicleType.car;
					if(isHdv == true) {
						carOrHdv = NoiseVehicleType.hdv;
					}

					NoiseEvent noiseEvent = new NoiseEvent(time,agentId,vehicleId,amount,linkId,carOrHdv);
					events.processEvent(noiseEvent);
					this.noiseEvents.add(noiseEvent);
					if(isHdv == true) {
						this.noiseEventsHdv.add(noiseEvent);
					} else {
						this.noiseEventsCar.add(noiseEvent);
					}
//					
					totalToll = totalToll+amount;
					
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
	
	private void throwNoiseEventsAffected() {
		for (Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for (Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for (int actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
					
					for (double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
						double factor = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId).get(timeInterval).get(personId).get(actNumber).getFirst();
						
						if (!(factor==0.)) {
							double time = timeInterval;
							Id agentId = personId;
							String actType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId).get(timeInterval).get(personId).get(actNumber).getSecond();
							
							double costPerUnit = receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId).get(timeInterval);
							double amount = factor * costPerUnit;
							
							NoiseEventAffected noiseEventAffected = new NoiseEventAffected(time,agentId,amount,receiverPointId,actType);
							events.processEvent(noiseEventAffected);
							this.noiseEventsAffected.add(noiseEventAffected);
							totalTollAffected = totalTollAffected + amount;
						
							if (personId2damageSum.containsKey(personId)) {
								double newTollSum = personId2damageSum.get(personId) + amount;
								personId2damageSum.put(personId,newTollSum);
							} else {
								personId2damageSum.put(personId,amount);
							}
						}
					}
				}
			}
		}
		
		//for comparison the home-based-oriented calculation
		Map<Id,Id> personId2homeReceiverPointId = new HashMap<Id, Id>();
		
		for (Id receiverPointId : receiverPointId2ListOfHomeAgents.keySet()) {
			for (Id personId : receiverPointId2ListOfHomeAgents.get(receiverPointId)) {
				personId2homeReceiverPointId.put(personId,receiverPointId);
			}
		}
		
		for (Id personId : scenario.getPopulation().getPersons().keySet()) {
			Id receiverPointId = personId2homeReceiverPointId.get(personId);
			double homeBasedDamageSum = 0.;
			for(double timeInterval = NoiseConfigParameters.getIntervalLength() ; timeInterval <= 30*3600 ; timeInterval = timeInterval + NoiseConfigParameters.getIntervalLength()) {
				double cost = 0.;
				if (receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId).containsKey(timeInterval)) {
					cost = receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId).get(timeInterval);
				} else {
					cost = 0.;
				}
				homeBasedDamageSum = homeBasedDamageSum + cost;
			}
			personId2homeBasedDamageSum.put(personId, homeBasedDamageSum);
		}
	}
	
	// for testing purposes
	public Map<Id, Map<Double, Map<Id, Map<Integer, Tuple<Double, String>>>>> getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType() {
		return receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType;
	}

	public Map<Id, Map<Double, Double>> getReceiverPointId2timeInterval2noiseImmission() {
		return receiverPointId2timeInterval2noiseImmission;
	}

	public Map<Id, Map<Double, Map<Id, Double>>> getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission() {
		return receiverPointIds2timeIntervals2noiseLinks2isolatedImmission;
	}

	public List<NoiseEvent> getNoiseEvents() {
		return noiseEvents;
	}

	public List<NoiseEventAffected> getNoiseEventsAffected() {
		return noiseEventsAffected;
	}
	
}
