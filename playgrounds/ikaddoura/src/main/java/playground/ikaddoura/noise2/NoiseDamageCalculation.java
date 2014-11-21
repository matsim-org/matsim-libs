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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
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
	
	private NoiseParameters noiseParams;
	private Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints;
	private NoiseEmissionHandler noiseEmissionHandler;
			
	// link based noise damage cost
	private Map<Id<Link>,Map<Double,Double>> linkId2timeInterval2damageCost = new HashMap<Id<Link>, Map<Double,Double>>();
	private Map<Id<Link>,Map<Double,Double>> linkId2timeInterval2damageCostPerCar = new HashMap<Id<Link>, Map<Double,Double>>();
	private Map<Id<Link>,Map<Double,Double>> linkId2timeInterval2damageCostPerHdvVehicle = new HashMap<Id<Link>, Map<Double,Double>>();
		
	// some additional analysis
	private Map<Id<Person>,Double> personId2causedNoiseCosts = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>,Double> personId2affectedNoiseCosts = new HashMap<Id<Person>, Double>();
	private double totalCausedNoiseCost = 0.;
	private double totalAffectedNoiseCost = 0.;
	
//	 to be filled during the computation of noise events
	private boolean collectNoiseEvents;
	private List<NoiseEventCaused> noiseEventsCaused = new ArrayList<NoiseEventCaused>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	
	public NoiseDamageCalculation (Scenario scenario , EventsManager events, NoiseParameters noiseParams, NoiseEmissionHandler noiseEmissionHandler, NoiseImmissionCalculation noiseImmission, Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints) {
		this.scenario = scenario;
		this.events = events;
		this.noiseParams = noiseParams;
		this.receiverPoints = receiverPoints;
		this.noiseEmissionHandler = noiseEmissionHandler;
												
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
		log.info("Calculating noise exposure costs for a total of " + this.receiverPoints.size() + " receiver points.");
		
		for(ReceiverPoint rp : this.receiverPoints.values()) {
			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}
			
			for (double timeInterval : rp.getTimeInterval2immission().keySet()) {
				
				double noiseImmission = rp.getTimeInterval2immission().get(timeInterval);
				
				double affectedAgentUnits = 0.;	
				if (rp.getTimeInterval2affectedAgentUnits().containsKey(timeInterval)) {
					affectedAgentUnits = rp.getTimeInterval2affectedAgentUnits().get(timeInterval);
				} 	
				
				double damageCost = calculateDamageCosts(noiseImmission, affectedAgentUnits, timeInterval);
				double damageCostPerAffectedAgentUnit = calculateDamageCosts(noiseImmission, 1., timeInterval);
				
				rp.getTimeInterval2damageCosts().put(timeInterval, damageCost);
				rp.getTimeInterval2damageCostPerAffectedAgentUnit().put(timeInterval, damageCostPerAffectedAgentUnit);				
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
		for (ReceiverPoint rp : this.receiverPoints.values()) {
			Id<ReceiverPoint> coordId = rp.getId();
			
			if (prepCounter % 10000 == 0) {
				log.info("receiver point # " + prepCounter);
			}
			
			Map<Double,Map<Id<Link>,Double>> timeIntervals2noiseLinks2costShare = new HashMap<Double, Map<Id<Link>,Double>>();
			
			for (double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {
				
				Map<Id<Link>,Double> noiseLinks2isolatedImmission = rp.getTimeInterval2LinkId2IsolatedImmission().get(timeInterval);
				Map<Id<Link>,Double> noiseLinks2costShare = new HashMap<Id<Link>, Double>();
				double resultingNoiseImmission = rp.getTimeInterval2immission().get(timeInterval);
				
				if (!((rp.getTimeInterval2damageCosts().get(timeInterval)) == 0.)) {
					for (Id<Link> linkId : noiseLinks2isolatedImmission.keySet()) {
						
						double noiseImmission = noiseLinks2isolatedImmission.get(linkId);
						double costs = 0.;
						
						if (!(noiseImmission == 0.)) {
						
							double costShare = NoiseEquations.calculateShareOfResultingNoiseImmission(noiseImmission, resultingNoiseImmission);
							costs = costShare * rp.getTimeInterval2damageCosts().get(timeInterval);	
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

		log.info("Going through all receiver points... Total number: " + this.receiverPoints.keySet().size());
		int counter = 0;
		for(ReceiverPoint rp : this.receiverPoints.values()) {
			Id<ReceiverPoint> id = rp.getId();
			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}
			for(Id<Link> linkId : this.receiverPoints.get(id).getLinkId2distanceCorrection().keySet()) {
				for(double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {
					if(!((rp.getTimeInterval2damageCosts().get(timeInterval)) == 0.)) {
						double sumNew = linkId2timeInterval2damageCost.get(linkId).get(timeInterval) + receiverPointIds2timeIntervals2noiseLinks2costShare.get(id).get(timeInterval).get(linkId);
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
				
				int nCar = noiseEmissionHandler.getLinkId2timeInterval2numberOfLinkEnterCars().get(linkId).get(timeInterval);
				int nHdv = noiseEmissionHandler.getLinkId2timeInterval2numberOfLinkEnterHgv().get(linkId).get(timeInterval);
			
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
				for(Id<Vehicle> id : noiseEmissionHandler.getLinkId2timeInterval2linkEnterVehicleIDs().get(linkId).get(timeInterval)) {
					
					double amount = 0.;
					boolean isHdv = false;
					
					if(!(id.toString().startsWith(this.noiseParams.getHgvIdPrefix()))) {
						amount = amountCar;
					} else {
						amount = amountHdv;
						isHdv = true;
					}
					
					// The person Id is assumed to be equal to the vehicle Id.
					Id<Person> agentId = Id.create(id, Person.class); 
				
					NoiseVehicleType carOrHdv = NoiseVehicleType.car;
					if (isHdv == true) {
						carOrHdv = NoiseVehicleType.hdv;
					}

					NoiseEventCaused noiseEvent = new NoiseEventCaused(timeInterval, agentId, id, amount, linkId, carOrHdv);
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
		
		for (ReceiverPoint rp : this.receiverPoints.values()) {
			Id<ReceiverPoint> receiverPointId = rp.getId();
			
			for (double timeInterval = this.noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600. ; timeInterval = timeInterval + this.noiseParams.getTimeBinSizeNoiseComputation()) {

				if (rp.getTimeInterval2actInfos().containsKey(timeInterval)) {
					for (PersonActivityInfo actInfo : rp.getTimeInterval2actInfos().get(timeInterval)) {
						double factor = actInfo.getDurationWithinInterval() / this.noiseParams.getTimeBinSizeNoiseComputation();

						if (!(factor == 0.)) {
							
							Id<Person> personId = actInfo.getPersonId();
							String actType = actInfo.getActivityType();
							
							double costPerUnit = rp.getTimeInterval2damageCostPerAffectedAgentUnit().get(timeInterval);
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
	
	Map<Id<Link>, Map<Double, Double>> getLinkId2timeInterval2damageCost() {
		return linkId2timeInterval2damageCost;
	}

	Map<Id<Link>, Map<Double, Double>> getLinkId2timeInterval2damageCostPerCar() {
		return linkId2timeInterval2damageCostPerCar;
	}

	Map<Id<Link>, Map<Double, Double>> getLinkId2timeInterval2damageCostPerHdvVehicle() {
		return linkId2timeInterval2damageCostPerHdvVehicle;
	}

	List<NoiseEventCaused> getNoiseEventsCaused() {
		return noiseEventsCaused;
	}

	List<NoiseEventAffected> getNoiseEventsAffected() {
		return noiseEventsAffected;
	}
	
}
