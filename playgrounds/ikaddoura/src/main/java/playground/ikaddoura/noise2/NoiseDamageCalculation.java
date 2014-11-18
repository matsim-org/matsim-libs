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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * Collects the relevant information in order to compute the noise immission for each receiver point.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseDamageCalculation {

	private static final Logger log = Logger.getLogger(NoiseDamageCalculation.class);
		
	private Scenario scenario;
	private EventsManager events;
	
	private NoiseSpatialInfo spatialInfo;
	private NoiseEquations noiseImmissionCalculator;
	private NoiseParameters noiseParams;
		
	// from emission handler
	private List<Id<Vehicle>> hdvVehicles;
	private Map<Id<Link>, Map<Double,List<Id<Vehicle>>>> linkId2timeInterval2linkEnterVehicleIDs;
	private Map<Id<Link>, Map<Double,Integer>> linkId2timeInterval2linkEnterVehicleIDsCar;
	private Map<Id<Link>, Map<Double,Integer>> linkId2timeInterval2linkEnterVehicleIDsHdv;
	
	// from person activity tracker
	private Map<Id<ReceiverPoint>,Map<Id<Person>,Map<Integer,Tuple<Double,Double>>>> receiverPointId2personId2actNumber2activityStartAndActivityEnd;
	private Map<Id<ReceiverPoint>,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits;
	private Map<Id<ReceiverPoint>,Map<Double,Map<Id<Person>,Map<Integer,Tuple<Double,String>>>>> receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType;
		
	// noise damage cost
	private Map<Id<Link>,Map<Double,Double>> linkId2timeInterval2damageCost = new HashMap<Id<Link>, Map<Double,Double>>();
	private Map<Id<Link>,Map<Double,Double>> linkId2timeInterval2damageCostPerCar = new HashMap<Id<Link>, Map<Double,Double>>();
	private Map<Id<Link>,Map<Double,Double>> linkId2timeInterval2damageCostPerHdvVehicle = new HashMap<Id<Link>, Map<Double,Double>>();
		
	private Map<Id<ReceiverPoint>,Map<Double,Double>> receiverPointId2timeInterval2damageCost = new HashMap<Id<ReceiverPoint>, Map<Double,Double>>();
	private Map<Id<ReceiverPoint>,Map<Double,Double>> receiverPointId2timeInterval2damageCostPerAffectedAgentUnit = new HashMap<Id<ReceiverPoint>, Map<Double,Double>>();
	
	private Map<Id<ReceiverPoint>,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission;
	private Map<Id<ReceiverPoint>,Map<Double,Map<Id<Link>,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission;

	// some additional analysis
	private Map<Id<Person>,Double> personId2causedNoiseCosts = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>,Double> personId2affectedNoiseCosts = new HashMap<Id<Person>, Double>();
	private double totalCausedNoiseCost = 0.;
	private double totalAffectedNoiseCost = 0.;
	
//	 to be filled during the computation of noise events
	private boolean collectNoiseEvents;
	private List<NoiseEventCaused> noiseEventsCaused = new ArrayList<NoiseEventCaused>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	
	public NoiseDamageCalculation (Scenario scenario , EventsManager events, NoiseSpatialInfo spatialInfo, NoiseParameters noiseParams, NoiseEmissionHandler noiseEmissionHandler, PersonActivityHandler activityTracker, NoiseImmissionCalculation noiseImmission) {
		this.scenario = scenario;
		this.events = events;
		this.spatialInfo = spatialInfo;
		this.noiseParams = noiseParams;
		this.noiseImmissionCalculator = new NoiseEquations();
		
		this.hdvVehicles = noiseEmissionHandler.getHdvVehicles();
		this.linkId2timeInterval2linkEnterVehicleIDs = noiseEmissionHandler.getLinkId2timeInterval2linkEnterVehicleIDs();
		this.linkId2timeInterval2linkEnterVehicleIDsCar = noiseEmissionHandler.getLinkId2timeInterval2numberOfLinkEnterCars();
		this.linkId2timeInterval2linkEnterVehicleIDsHdv = noiseEmissionHandler.getLinkId2timeInterval2numberOfLinkEnterHdv();
		
		this.receiverPointId2personId2actNumber2activityStartAndActivityEnd = activityTracker.getReceiverPointId2personId2actNumber2activityStartAndActivityEnd();
		this.receiverPointId2timeInterval2affectedAgentUnits = activityTracker.getReceiverPointId2timeInterval2affectedAgentUnits();
		this.receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType = activityTracker.getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType();
		
		this.receiverPointId2timeInterval2noiseImmission = noiseImmission.getReceiverPointId2timeInterval2noiseImmission();
		this.receiverPointIds2timeIntervals2noiseLinks2isolatedImmission = noiseImmission.getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission();
						
		this.collectNoiseEvents = true;
	}
	
	public void setCollectNoiseEvents(boolean collectNoiseEvents) {
		this.collectNoiseEvents = collectNoiseEvents;
		log.info("Collecting Noise Events is set to " + collectNoiseEvents);
	}

	public void calculateNoiseDamageCosts() {
		
		log.info("Calculating noise exposure costs for each receiver point...");
		calculateDamagePerReceiverPoint();
		log.info("Calculating noise exposure costs for each receiver point... Done.");

		log.info("Allocating the total exposure cost (per receiver point) to the relevant links...");
		calculateCostSharesPerLinkPerTimeInterval();
		log.info("Allocating the total exposure cost (per receiver point) to the relevant links... Done.");
		
		log.info("Allocating the exposure cost per link to the vehicle categories and vehicles...");
		calculateCostsPerVehiclePerLinkPerTimeInterval();
		log.info("Allocating the exposure cost per link to the vehicle categories and vehicles... Done.");
		
		log.info("Throwing noise events (caused)...");
		throwNoiseEventsCaused();
		log.info("Throwing noise events (caused)... Done.");

		log.info("Throwing noise events (affected)...");
		throwNoiseEventsAffected();
		log.info("Throwing noise events (affected)... Done.");
	}
		
	private void calculateDamagePerReceiverPoint() {
		int counter = 0;
		log.info("Calculating noise exposure costs for a total of " + receiverPointId2timeInterval2noiseImmission.size() + " receiver points.");
		
		for(Id<ReceiverPoint> receiverPointId : receiverPointId2timeInterval2noiseImmission.keySet()) {
			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}
			
			for(double timeInterval : receiverPointId2timeInterval2noiseImmission.get(receiverPointId).keySet()) {
				double noiseImmission = receiverPointId2timeInterval2noiseImmission.get(receiverPointId).get(timeInterval);
				double affectedAgentUnits = 0.;
				
				if (receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
					
					if (receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(timeInterval)) {
						
						affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).get(timeInterval);
					} 	
				}
				
				double damageCost = calculateDamageCosts(noiseImmission, affectedAgentUnits, timeInterval);
				double damageCostPerAffectedAgentUnit = calculateDamageCosts(noiseImmission, 1., timeInterval);
				
				if (receiverPointId2timeInterval2damageCost.containsKey(receiverPointId)) {
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
			counter++;
		}
	}
	
	private double calculateDamageCosts(double noiseImmission, double affectedAgentUnits , double timeInterval) {
		
		String dayOrNight = "NIGHT";
		
		if (timeInterval > 6 * 3600 && timeInterval <= 18 * 3600) {
			dayOrNight = "DAY";
		} else if (timeInterval > 18 * 3600 && timeInterval <= 22 * 3600) {
			dayOrNight = "EVENING";
		}
		
		double lautheitsgewicht = calculateLautheitsgewicht(noiseImmission, dayOrNight);  
		
		double laermEinwohnerGleichwert = lautheitsgewicht * affectedAgentUnits;
		
		double damageCosts = 0.;
		if (dayOrNight == "DAY"){
			damageCosts = (this.noiseParams.getAnnualCostRate() * laermEinwohnerGleichwert/(365))*(this.noiseParams.getTimeBinSizeNoiseComputation()/(24.0 * 3600));
		} else if (dayOrNight == "EVENING"){
			damageCosts = (this.noiseParams.getAnnualCostRate() * laermEinwohnerGleichwert/(365))*(this.noiseParams.getTimeBinSizeNoiseComputation()/(24.0 * 3600));
		} else if (dayOrNight == "NIGHT"){
			damageCosts = (this.noiseParams.getAnnualCostRate() * laermEinwohnerGleichwert/(365))*(this.noiseParams.getTimeBinSizeNoiseComputation()/(24.0 * 3600));
		} else {
			throw new RuntimeException("Neither day nor night. Aborting...");
		}
		return damageCosts;	
	}

	private double calculateLautheitsgewicht (double noiseImmission , String dayOrNight){
		double lautheitsgewicht = 0;
		
		if (dayOrNight == "DAY"){
			if (noiseImmission < 50){
			} else {
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 50));
			}
		} else if(dayOrNight == "EVENING"){
			if (noiseImmission < 45){
			} else {
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 45));
			}
		} else if(dayOrNight == "NIGHT"){
			if (noiseImmission < 40){
			} else {
				lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 40));
			}
		} else{
			throw new RuntimeException("Neither day nor night!");
		}
		
		return lautheitsgewicht;
	}

	private void calculateCostSharesPerLinkPerTimeInterval() {
		
		Map<Id<ReceiverPoint>, Map<Double, Map<Id<Link>, Double>>> receiverPointIds2timeIntervals2noiseLinks2costShare = new HashMap<Id<ReceiverPoint>, Map<Double,Map<Id<Link>,Double>>>();

		log.info("Initialization...");
		int prepCounter = 0;
		for (Id<ReceiverPoint> coordId : receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.keySet()) {
			
			if (prepCounter % 10000 == 0) {
				log.info("receiver point # " + prepCounter);
			}
			
			Map<Double,Map<Id<Link>,Double>> timeIntervals2noiseLinks2costShare = new HashMap<Double, Map<Id<Link>,Double>>();
			
			for (double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {
				
				Map<Id<Link>,Double> noiseLinks2isolatedImmission = receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(coordId).get(timeInterval);
				Map<Id<Link>,Double> noiseLinks2costShare = new HashMap<Id<Link>, Double>();
				double resultingNoiseImmission = receiverPointId2timeInterval2noiseImmission.get(coordId).get(timeInterval);
				
				if (!((receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval)) == 0.)) {
					for (Id<Link> linkId : noiseLinks2isolatedImmission.keySet()) {
						
						double noiseImmission = noiseLinks2isolatedImmission.get(linkId);
						double costs = 0.;
						
						if (!(noiseImmission == 0.)) {
						
							double costShare = noiseImmissionCalculator.calculateShareOfResultingNoiseImmission(noiseImmission, resultingNoiseImmission);
							costs = costShare * receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval);	
						}
						noiseLinks2costShare.put(linkId, costs);
					}
				}
				timeIntervals2noiseLinks2costShare.put(timeInterval, noiseLinks2costShare);
			}
			receiverPointIds2timeIntervals2noiseLinks2costShare.put(coordId, timeIntervals2noiseLinks2costShare);
			prepCounter++;
		}
		log.info("Initialization... Done.");
		
		//summing up the link-based-costs
		for(Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
			Map<Double,Double> timeInterval2damageCost = new HashMap<Double, Double>();
			for(double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {
				timeInterval2damageCost.put(timeInterval, 0.);
			}
			linkId2timeInterval2damageCost.put(linkId, timeInterval2damageCost);
		}

		log.info("Going through all receiver points... Total number: " + spatialInfo.getReceiverPointId2Coord().keySet().size());
		int counter = 0;
		for(Id<ReceiverPoint> coordId : spatialInfo.getReceiverPointId2Coord().keySet()) {
			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}
			for(Id<Link> linkId : spatialInfo.getReceiverPointId2relevantLinkIds().get(coordId)) {
				for(double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {
					if(!((receiverPointId2timeInterval2damageCost.get(coordId).get(timeInterval)) == 0.)) {
						double sumNew = linkId2timeInterval2damageCost.get(linkId).get(timeInterval) + receiverPointIds2timeIntervals2noiseLinks2costShare.get(coordId).get(timeInterval).get(linkId);
						linkId2timeInterval2damageCost.get(linkId).put(timeInterval, sumNew);
					}
				}
			}
			counter++;
		}
		log.info("Going through all receiver points... Done.");
	}

	private void calculateCostsPerVehiclePerLinkPerTimeInterval() {
		
		log.info("Going through all links... Total number: " + scenario.getNetwork().getLinks().keySet().size());
		int counter = 0;
		for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
			
			if (counter % 10000 == 0) {
				log.info("link # " + counter);
			}
			
			Map<Double,Double> timeInterval2damageCostPerCar = new HashMap<Double, Double>();
			Map<Double,Double> timeInterval2damageCostPerHdvVehicle = new HashMap<Double, Double>();
			
			for (double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval<=30*3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {
				
				double damageCostSum = 0.;
				
				if (linkId2timeInterval2damageCost.containsKey(linkId)) {
					
					if (linkId2timeInterval2damageCost.get(linkId).containsKey(timeInterval)) {
						
						damageCostSum = linkId2timeInterval2damageCost.get(linkId).get(timeInterval);
					}
				}
				
				int nCar = linkId2timeInterval2linkEnterVehicleIDsCar.get(linkId).get(timeInterval);
				int nHdv = linkId2timeInterval2linkEnterVehicleIDsHdv.get(linkId).get(timeInterval);
			
				double vCar = (scenario.getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
				double vHdv = vCar;
				
				// If different speeds for different vehicle types have to be considered, adapt the calculation here.
				// For example, a maximum speed for hdv-vehicles could be set here (for instance for German highways) 
				
				double lCar = NoiseEquations.calculateLCar(vCar);
				double lHdv = NoiseEquations.calculateLHdv(vHdv);
				
				double shareCar = 0.;
				double shareHdv = 0.;
				
				if ((nCar > 0) || (nHdv > 0)) {
					shareCar = NoiseEquations.calculateShare(nCar, lCar, nHdv, lHdv);
					shareHdv = NoiseEquations.calculateShare(nHdv, lHdv, nCar, lCar);
					
					if ((!(((shareCar + shareHdv) > 0.999) && ((shareCar + shareHdv) < 1.001)))) {
						log.warn("The sum of the car share and hdv share is not equal to 1.0! The value is " + (shareCar + shareHdv));
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
			
			counter++;
		}
	}

	private void throwNoiseEventsCaused() {
		
		for(Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
			for(double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {
				double amountCar = (linkId2timeInterval2damageCostPerCar.get(linkId).get(timeInterval))/(this.noiseParams.getScaleFactor());
				double amountHdv = (linkId2timeInterval2damageCostPerHdvVehicle.get(linkId).get(timeInterval))/(this.noiseParams.getScaleFactor());
								
				// calculate shares for the affected Agents
				for(Id<Vehicle> id : linkId2timeInterval2linkEnterVehicleIDs.get(linkId).get(timeInterval)) {
					
					double amount = 0.;
					boolean isHdv = false;
					
					if(!(hdvVehicles.contains(id))) {
						amount = amountCar;
					} else {
						amount = amountHdv;
						isHdv = true;
					}
					double time = timeInterval - 1;
					
					// The person Id is assumed to be equal to the vehicle Id.
					Id<Person> agentId = Id.create(id, Person.class); 
				
					NoiseVehicleType carOrHdv = NoiseVehicleType.car;
					if (isHdv == true) {
						carOrHdv = NoiseVehicleType.hdv;
					}

					NoiseEventCaused noiseEvent = new NoiseEventCaused(time, agentId, id, amount, linkId, carOrHdv);
					events.processEvent(noiseEvent);
					
					if (this.collectNoiseEvents) {
						this.noiseEventsCaused.add(noiseEvent);
					}
					
					totalCausedNoiseCost = totalCausedNoiseCost + amount;
					
					if(personId2causedNoiseCosts.containsKey(agentId)) {
						double newTollSum = personId2causedNoiseCosts.get(agentId) + amount;
						personId2causedNoiseCosts.put(agentId,newTollSum);
					} else {
						personId2causedNoiseCosts.put(agentId,amount);
					}
				}
			}
		}
	}
	
	private void throwNoiseEventsAffected() {
		for (Id<ReceiverPoint> receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for (Id<Person> personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for (int actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
					
					for (double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600. ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {
						double factor = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId).get(timeInterval).get(personId).get(actNumber).getFirst();
						if (!(factor == 0.)) {
							
							String actType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId).get(timeInterval).get(personId).get(actNumber).getSecond();
							
							double costPerUnit = receiverPointId2timeInterval2damageCostPerAffectedAgentUnit.get(receiverPointId).get(timeInterval);
							double amount = factor * costPerUnit;
							
							NoiseEventAffected noiseEventAffected = new NoiseEventAffected(timeInterval, personId, amount, receiverPointId, actType);
							events.processEvent(noiseEventAffected);
							
							if (this.collectNoiseEvents) {
								this.noiseEventsAffected.add(noiseEventAffected);
							}
							
							totalAffectedNoiseCost = totalAffectedNoiseCost + amount;
						
							if (personId2affectedNoiseCosts.containsKey(personId)) {
								double newTollSum = personId2affectedNoiseCosts.get(personId) + amount;
								personId2affectedNoiseCosts.put(personId,newTollSum);
							} else {
								personId2affectedNoiseCosts.put(personId,amount);
							}
						}
					}
				}
			}
		}
	}
	
	// write immission infos
	
	public void writeNoiseImmissionStats(String fileName) {
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("receiver point Id;avg noise immission;avg noise immission (day);avg noise immission (night);avg noise immission (peak);avg noise immission (off-peak)");
			bw.newLine();
			
			List<Double> day = new ArrayList<Double>();
			for(double timeInterval = 6 * 3600 + this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 22 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()){
				day.add(timeInterval);
			}
			List<Double> night = new ArrayList<Double>();
			for(double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 24 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()){
				if(!(day.contains(timeInterval))) {
					night.add(timeInterval);
				}
			}
			
			List<Double> peak = new ArrayList<Double>();
			for(double timeInterval = 7 * 3600 + this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 9 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()){
				peak.add(timeInterval);
			}
			for(double timeInterval = 15 * 3600 + this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 18 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()){
				peak.add(timeInterval);
			}
			List<Double> offPeak = new ArrayList<Double>();
			for(double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 24 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()){
				if(!(peak.contains(timeInterval))) {
					offPeak.add(timeInterval);
				}
			}
			
			for (Id<ReceiverPoint> rpId : this.receiverPointId2timeInterval2noiseImmission.keySet()){
				double avgNoise = 0.;
				double avgNoiseDay = 0.;
				double avgNoiseNight = 0.;
				double avgNoisePeak = 0.;
				double avgNoiseOffPeak = 0.;
				
				double sumAvgNoise = 0.;
				int counterAvgNoise = 0;
				double sumAvgNoiseDay = 0.;
				int counterAvgNoiseDay = 0;
				double sumAvgNoiseNight = 0.;
				int counterAvgNoiseNight = 0;
				double sumAvgNoisePeak = 0.;
				int counterAvgNoisePeak = 0;
				double sumAvgNoiseOffPeak = 0.;
				int counterAvgNoiseOffPeak = 0;
				
				for(double timeInterval : receiverPointId2timeInterval2noiseImmission.get(rpId).keySet()) {
					double noiseValue = receiverPointId2timeInterval2noiseImmission.get(rpId).get(timeInterval);
					double termToAdd = Math.pow(10., noiseValue/10.);
					
					if(timeInterval < 30 * 3600) {
						sumAvgNoise = sumAvgNoise + termToAdd;
						counterAvgNoise++;
					}
					
					if(day.contains(timeInterval)) {
						sumAvgNoiseDay = sumAvgNoiseDay + termToAdd;
						counterAvgNoiseDay++;
					}
					
					if(night.contains(timeInterval)) {
						sumAvgNoiseNight = sumAvgNoiseNight + termToAdd;
						counterAvgNoiseNight++;
					}
				
					if(peak.contains(timeInterval)) {
						sumAvgNoisePeak = sumAvgNoisePeak + termToAdd;
						counterAvgNoisePeak++;
					}
					
					if(offPeak.contains(timeInterval)) {
						sumAvgNoiseOffPeak = sumAvgNoiseOffPeak + termToAdd;
						counterAvgNoiseOffPeak++;
					}	
				}
				
				avgNoise = 10 * Math.log10(sumAvgNoise / (counterAvgNoise));
				avgNoiseDay = 10 * Math.log10(sumAvgNoiseDay / counterAvgNoiseDay);
				avgNoiseNight = 10 * Math.log10(sumAvgNoiseNight / counterAvgNoiseNight);
				avgNoisePeak = 10 * Math.log10(sumAvgNoisePeak / counterAvgNoisePeak);
				avgNoiseOffPeak = 10 * Math.log10(sumAvgNoiseOffPeak / counterAvgNoiseOffPeak);
								
				bw.write(rpId + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\",\"Real\",\"Real\",\"Real\",\"Real\"");
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
				
	}
	
	public void writeNoiseImmissionStatsPerHour(String fileName) {
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// column headers
			bw.write("receiver point");
			for(int i = 0; i < 30 ; i++) {
				String time = Time.writeTime( (i+1) * this.noiseParams.getTimeBinSizeNoiseComputation(), Time.TIMEFORMAT_HHMMSS );
				bw.write(";agent units " + time + ";noise immission " + time);
			}
			bw.newLine();

			
			for (Id<ReceiverPoint> rpId : this.receiverPointId2timeInterval2noiseImmission.keySet()){
				bw.write(rpId.toString());
				for(int i = 0 ; i < 30 ; i++) {
					double timeInterval = (i+1) * this.noiseParams.getTimeBinSizeNoiseComputation();
					double affectedAgentUnits = 0.;
					double noiseImmission = 0.;
					
					if (receiverPointId2timeInterval2affectedAgentUnits.get(rpId) != null) {
						affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(rpId).get(timeInterval);
					}
					
					if (receiverPointId2timeInterval2noiseImmission.get(rpId).get(timeInterval) != null) {
						noiseImmission = receiverPointId2timeInterval2noiseImmission.get(rpId).get(timeInterval);
					}
					
					bw.write(";"+ affectedAgentUnits + ";" + noiseImmission);	
				}
				
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\"");
			
			for(int i = 0; i < 30 ; i++) {
				bw.write(",\"Real\",\"Real\"");
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	// analysis
	
	public Map<Id<Person>, Double> getPersonId2causedNoiseCosts() {
		return personId2causedNoiseCosts;
	}

	public Map<Id<Person>, Double> getPersonId2affectedNoiseCosts() {
		return personId2affectedNoiseCosts;
	}

	public double getTotalCausedNoiseCost() {
		return totalCausedNoiseCost;
	}

	public double getTotalAffectedNoiseCost() {
		return totalAffectedNoiseCost;
	}
	
	// for testing purposes
	
	public Map<Id<ReceiverPoint>, Map<Double, Double>> getReceiverPointId2timeInterval2damageCost() {
		return receiverPointId2timeInterval2damageCost;
	}

	public Map<Id<ReceiverPoint>, Map<Double, Double>> getReceiverPointId2timeInterval2damageCostPerAffectedAgentUnit() {
		return receiverPointId2timeInterval2damageCostPerAffectedAgentUnit;
	}
	
	public Map<Id<Link>, Map<Double, Double>> getLinkId2timeInterval2damageCost() {
		return linkId2timeInterval2damageCost;
	}

	public Map<Id<Link>, Map<Double, Double>> getLinkId2timeInterval2damageCostPerCar() {
		return linkId2timeInterval2damageCostPerCar;
	}

	public Map<Id<Link>, Map<Double, Double>> getLinkId2timeInterval2damageCostPerHdvVehicle() {
		return linkId2timeInterval2damageCostPerHdvVehicle;
	}

	public List<NoiseEventCaused> getNoiseEventsCaused() {
		return noiseEventsCaused;
	}

	public List<NoiseEventAffected> getNoiseEventsAffected() {
		return noiseEventsAffected;
	}
	
}
