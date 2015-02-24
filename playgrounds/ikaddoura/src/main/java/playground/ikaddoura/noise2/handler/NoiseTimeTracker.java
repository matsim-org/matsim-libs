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
package playground.ikaddoura.noise2.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.noise2.NoiseWriter;
import playground.ikaddoura.noise2.data.NoiseContext;
import playground.ikaddoura.noise2.data.NoiseLink;
import playground.ikaddoura.noise2.data.PersonActivityInfo;
import playground.ikaddoura.noise2.data.ReceiverPoint;
import playground.ikaddoura.noise2.events.NoiseEventAffected;
import playground.ikaddoura.noise2.events.NoiseEventCaused;

/**
 * A handler which computes noise emissions, immisions, affected agent units and damages for each receiver point and time interval.
 * Throws noise damage events for each affected and causing agent.
 * 
 * @author ikaddoura
 *
 */

public class NoiseTimeTracker implements LinkEnterEventHandler {

	private static final Logger log = Logger.getLogger(NoiseTimeTracker.class);
	
	private final NoiseContext noiseContext;
	private final String outputDirectoryGeneral;
	private final EventsManager events;

	private String outputDirectory;
	
	private boolean collectNoiseEvents = true;
	private List<NoiseEventCaused> noiseEventsCaused = new ArrayList<NoiseEventCaused>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	private double totalCausedNoiseCost = 0.;
	private double totalAffectedNoiseCost = 0.;
	
	public NoiseTimeTracker(NoiseContext noiseContext, EventsManager events, String outputDirectory) {
		this.noiseContext = noiseContext;
		this.outputDirectoryGeneral = outputDirectory;
		this.outputDirectory = outputDirectory;
		this.events = events;	
	}

	@Override
	public void reset(int iteration) {
		
		this.outputDirectory = this.outputDirectoryGeneral + "it." + iteration + "/";
		log.info("Setting the output directory to " + outputDirectory);
		
		this.totalCausedNoiseCost = 0.;
		this.totalAffectedNoiseCost = 0.;
		this.noiseEventsCaused.clear();
		this.noiseEventsAffected.clear();
		
		this.noiseContext.getNoiseLinks().clear();
		this.noiseContext.getTimeInterval2linkId2noiseLinks().clear();
		this.noiseContext.setCurrentTimeBinEndTime(this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
		
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			rp.getLinkId2IsolatedImmission().clear();
			rp.setFinalImmission(0.);
			rp.setAffectedAgentUnits(0.);
			rp.getPersonId2actInfos().clear();
			rp.setDamageCosts(0.);
			rp.setDamageCostsPerAffectedAgentUnit(0.);
		}
		
	}
	
	private void resetCurrentTimeIntervalInfo() {
		
		this.noiseContext.getNoiseLinks().clear();
		
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			rp.getLinkId2IsolatedImmission().clear();
			rp.setFinalImmission(0.);
			rp.setAffectedAgentUnits(0.);
			rp.setDamageCosts(0.);
			rp.setDamageCostsPerAffectedAgentUnit(0.);
		}
	}
	
	private void checkTime(double time) {
		// Check for every event that is thrown if the current interval has changed.
		
		if (time > this.noiseContext.getCurrentTimeBinEndTime()) {
			// All events of the current time bin are processed.

			while (time > this.noiseContext.getCurrentTimeBinEndTime()) {
				this.noiseContext.setEventTime(time);
				processTimeBin();
			}
		}
	}
	
	private void processTimeBin() {
		
		log.info("##############################################");
		log.info("# Computing noise for time interval " + Time.writeTime(this.noiseContext.getCurrentTimeBinEndTime(), Time.TIMEFORMAT_HHMMSS) + " #");
		log.info("##############################################");

		updateActivityInformation(); // Remove activities that were completed in the previous time interval.
		computeNoiseForCurrentTimeInterval(); // Compute noise emissions, immissions, affected agent units and damages for the current time interval.			
		updateCurrentTimeInterval(); // Set the current time bin to the next one ( current time bin = current time bin + time bin size ).
		resetCurrentTimeIntervalInfo(); // Reset all time-specific information from the previous time interval.
	}

	private void updateActivityInformation() {
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
						
			for (Id<Person> personId : rp.getPersonId2actInfos().keySet()) {
				Iterator<PersonActivityInfo> it = rp.getPersonId2actInfos().get(personId).iterator();
				while (it.hasNext()) {
				    if ( it.next().getEndTime() < ( this.noiseContext.getCurrentTimeBinEndTime() - this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation() ) ) {
				        it.remove();
				    }
				}
			}				
		}
	}

	private void computeNoiseForCurrentTimeInterval() {
		
		log.info("Calculating noise emissions...");
		calculateNoiseEmission();
		NoiseWriter.writeNoiseEmissionStatsPerHour(this.noiseContext, outputDirectory);
		log.info("Calculating noise emissions... Done.");
		
		log.info("Calculating noise immissions...");
		calculateNoiseImmission();
		NoiseWriter.writeNoiseImmissionStatsPerHour(noiseContext, outputDirectory);
		log.info("Calculating noise immissions... Done.");
		
		if (this.noiseContext.getNoiseParams().isComputeNoiseDamages()) {
			
			log.info("Calculating the number of affected agent units...");
			calculateAffectedAgentUnits();
			NoiseWriter.writePersonActivityInfoPerHour(noiseContext, outputDirectory);
			log.info("Calculating the number of affected agent units... Done.");
		
			log.info("Calculating noise damage costs...");
			calculateNoiseDamageCosts();
			log.info("Calculating noise damage costs... Done.");
		}
			
	}
		
	private void calculateAffectedAgentUnits() {
		
		for (ReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
			
			double affectedAgentUnits = 0.;
			if (!(rp.getPersonId2actInfos().isEmpty())) {
				
				for (Id<Person> personId : rp.getPersonId2actInfos().keySet()) {
					
					for (PersonActivityInfo actInfo : rp.getPersonId2actInfos().get(personId)) {
						double unitsThisPersonActivityInfo = actInfo.getDurationWithinInterval(noiseContext.getCurrentTimeBinEndTime(), noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) / noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
						affectedAgentUnits = affectedAgentUnits + ( unitsThisPersonActivityInfo * noiseContext.getNoiseParams().getScaleFactor() );
					}
				}
			}
			rp.setAffectedAgentUnits(affectedAgentUnits);
		}
	}

	private void updateCurrentTimeInterval() {
		double newTimeInterval = this.noiseContext.getCurrentTimeBinEndTime() + this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
		this.noiseContext.setCurrentTimeBinEndTime(newTimeInterval);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
				
		checkTime(event.getTime());
		
		if (!(noiseContext.getScenario().getPopulation().getPersons().containsKey(event.getVehicleId()))) {
			// probably public transit
			
		} else {
			
			// for all vehicle types
			if (this.noiseContext.getNoiseLinks().containsKey(event.getLinkId())) {
				this.noiseContext.getNoiseLinks().get(event.getLinkId()).getEnteringVehicleIds().add(event.getVehicleId());
			
			} else {
				
				NoiseLink noiseLink = new NoiseLink(event.getLinkId());
				List<Id<Vehicle>> enteringVehicleIds = new ArrayList<Id<Vehicle>>();
				enteringVehicleIds.add(event.getVehicleId());
				noiseLink.setEnteringVehicleIds(enteringVehicleIds);
				
				this.noiseContext.getNoiseLinks().put(event.getLinkId(), noiseLink);
			}
		
			if (event.getVehicleId().toString().startsWith(this.noiseContext.getNoiseParams().getHgvIdPrefix())) {
				// HGV
				
				int hgv = this.noiseContext.getNoiseLinks().get(event.getLinkId()).getHgv();
				hgv++;
				this.noiseContext.getNoiseLinks().get(event.getLinkId()).setHgv(hgv);
				
			} else {
				// Car
				
				int cars = this.noiseContext.getNoiseLinks().get(event.getLinkId()).getCars();
				cars++;
				this.noiseContext.getNoiseLinks().get(event.getLinkId()).setCars(cars);			
			}
		}
	}

	private void calculateNoiseDamageCosts() {
		
		log.info("Calculating noise damage costs for each receiver point...");
		calculateDamagePerReceiverPoint();
		NoiseWriter.writeDamageInfoPerHour(noiseContext, outputDirectory);
		log.info("Calculating noise damage costs for each receiver point... Done.");

		if (this.noiseContext.getNoiseParams().isThrowNoiseEventsAffected()) {
			
			log.info("Throwing noise events for the affected agents...");
			throwNoiseEventsAffected();
			log.info("Throwing noise events for the affected agents... Done.");
		}
		
		if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
			
			log.info("Allocating the total damage cost (per receiver point) to the relevant links...");
			calculateCostSharesPerLinkPerTimeInterval();
			NoiseWriter.writeLinkDamageInfoPerHour(noiseContext, outputDirectory);
			log.info("Allocating the total damage cost (per receiver point) to the relevant links... Done.");
			
			log.info("Allocating the damage cost per link to the vehicle categories and vehicles...");
			calculateCostsPerVehiclePerLinkPerTimeInterval();
			NoiseWriter.writeLinkAvgCarDamageInfoPerHour(noiseContext, outputDirectory);
			NoiseWriter.writeLinkAvgHgvDamageInfoPerHour(noiseContext, outputDirectory);
			log.info("Allocating the damage cost per link to the vehicle categories and vehicles... Done.");
						
			if (this.noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
				log.info("Throwing noise events for the causing agents...");
				throwNoiseEventsCaused();
				log.info("Throwing noise events for the causing agents... Done.");
			
				if (this.noiseContext.getNoiseParams().isInternalizeNoiseDamages()) {
					this.noiseContext.storeTimeInterval();
				}
			}
		}
		
	}
	
	private void calculateDamagePerReceiverPoint() {
		
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
				
			double noiseImmission = rp.getFinalImmission();
			double affectedAgentUnits = rp.getAffectedAgentUnits();
			
			double damageCost = NoiseEquations.calculateDamageCosts(noiseImmission, affectedAgentUnits, this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
			double damageCostPerAffectedAgentUnit = NoiseEquations.calculateDamageCosts(noiseImmission, 1., this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
				
			rp.setDamageCosts(damageCost);
			rp.setDamageCostsPerAffectedAgentUnit(damageCostPerAffectedAgentUnit);
		}
	}

	private void calculateCostSharesPerLinkPerTimeInterval() {
		
		Map<Id<ReceiverPoint>, Map<Id<Link>, Double>> rpId2linkId2costShare = new HashMap<Id<ReceiverPoint>, Map<Id<Link>,Double>>();

		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
										
			Map<Id<Link>,Double> linkId2costShare = new HashMap<Id<Link>, Double>();
							
			if (rp.getDamageCosts() != 0.) {
				for (Id<Link> linkId : rp.getLinkId2IsolatedImmission().keySet()) {
										
						double noiseImmission = rp.getLinkId2IsolatedImmission().get(linkId);
						double costs = 0.;
						
						if (!(noiseImmission == 0.)) {
							double costShare = NoiseEquations.calculateShareOfResultingNoiseImmission(noiseImmission, rp.getFinalImmission());
							costs = costShare * rp.getDamageCosts();	
						}
						linkId2costShare.put(linkId, costs);
					}
				}
			
			rpId2linkId2costShare.put(rp.getId(), linkId2costShare);
		}
		
		// summing up the link-based costs
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

			if (rp.getDamageCosts() != 0.) {
				
				for (Id<Link> linkId : this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2distanceCorrection().keySet()) {
					if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
						double sum = this.noiseContext.getNoiseLinks().get(linkId).getDamageCost() + rpId2linkId2costShare.get(rp.getId()).get(linkId);
						this.noiseContext.getNoiseLinks().get(linkId).setDamageCost(sum);
					}		
				}
			}
		}
	}

	private void calculateCostsPerVehiclePerLinkPerTimeInterval() {
		
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {

			double damageCostPerCar = 0.;
			double damageCostPerHgv = 0.;
			
			double damageCostSum = 0.;
				
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {					
				damageCostSum = this.noiseContext.getNoiseLinks().get(linkId).getDamageCost();
			}
				
			int nCar = 0;
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				nCar = this.noiseContext.getNoiseLinks().get(linkId).getCars();
			}
			
			int nHdv = 0;
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				nHdv = this.noiseContext.getNoiseLinks().get(linkId).getHgv();
			}
			
			double vCar = (this.noiseContext.getScenario().getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
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
			}
			
			double damageCostSumCar = shareCar * damageCostSum;
			double damageCostSumHdv = shareHdv * damageCostSum;
				
			if (!(nCar == 0)) {
				damageCostPerCar = damageCostSumCar/nCar;
			}
				
			if (!(nHdv == 0)) {
				damageCostPerHgv = damageCostSumHdv/nHdv;
			}
			
			if (damageCostPerCar > 0.) {
				this.noiseContext.getNoiseLinks().get(linkId).setDamageCostPerCar(damageCostPerCar);
			}
			if (damageCostPerHgv > 0.) {
				this.noiseContext.getNoiseLinks().get(linkId).setDamageCostPerHgv(damageCostPerHgv);			
			}
		}
	}

	private void throwNoiseEventsCaused() {
		
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
											
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)){
				double amountCar = (this.noiseContext.getNoiseLinks().get(linkId).getDamageCostPerCar()) / (this.noiseContext.getNoiseParams().getScaleFactor());
				double amountHdv = (this.noiseContext.getNoiseLinks().get(linkId).getDamageCostPerHgv()) / (this.noiseContext.getNoiseParams().getScaleFactor());
				
				for(Id<Vehicle> vehicleId : this.noiseContext.getNoiseLinks().get(linkId).getEnteringVehicleIds()) {
					
					double amount = 0.;
					
					if(!(vehicleId.toString().startsWith(this.noiseContext.getNoiseParams().getHgvIdPrefix()))) {
						amount = amountCar;
					} else {
						amount = amountHdv;
					}
					
					if (amount != 0.) {
						
						// The person Id is assumed to be equal to the vehicle Id.
						NoiseEventCaused noiseEvent = new NoiseEventCaused(this.noiseContext.getEventTime(), this.noiseContext.getCurrentTimeBinEndTime(), Id.create(vehicleId, Person.class), vehicleId, amount, linkId);
						events.processEvent(noiseEvent);
						
						if (this.collectNoiseEvents) {
							this.noiseEventsCaused.add(noiseEvent);
						}
						
						totalCausedNoiseCost = totalCausedNoiseCost + amount;
					}
				}
			}
		}
	}
	
	private void throwNoiseEventsAffected() {
		
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			
			if (!(rp.getPersonId2actInfos().isEmpty())) {
				
				for (Id<Person> personId : rp.getPersonId2actInfos().keySet()) {
					
					for (PersonActivityInfo actInfo : rp.getPersonId2actInfos().get(personId)) {
						
						double factor = actInfo.getDurationWithinInterval(this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
						double amount = factor * rp.getDamageCostsPerAffectedAgentUnit();
						
						if (amount != 0.) {
							NoiseEventAffected noiseEventAffected = new NoiseEventAffected(this.noiseContext.getEventTime(), this.noiseContext.getCurrentTimeBinEndTime(), personId, amount, rp.getId(), actInfo.getActivityType());
							events.processEvent(noiseEventAffected);
							
							if (this.collectNoiseEvents) {
								this.noiseEventsAffected.add(noiseEventAffected);
							}
							
							totalAffectedNoiseCost = totalAffectedNoiseCost + amount;
						}				
					}
				}
			}	
		}
	}

	private void calculateNoiseImmission() {
		
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
					
			Map<Id<Link>, Double> linkId2isolatedImmission = new HashMap<Id<Link>, Double>();
			
			for(Id<Link> linkId : rp.getLinkId2distanceCorrection().keySet()) {
				if (this.noiseContext.getNoiseParams().getTunnelLinkIDs().contains(linkId)) {
					linkId2isolatedImmission.put(linkId, 0.);
			 			
			 	} else {
				
			 		double noiseImmission = 0.;
					
			 		if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
						if (!(this.noiseContext.getNoiseLinks().get(linkId).getEmission() == 0.)) {
							noiseImmission = this.noiseContext.getNoiseLinks().get(linkId).getEmission()
									+ this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2distanceCorrection().get(linkId)
									+ this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2angleCorrection().get(linkId)
									;
							
							if (noiseImmission < 0.) {
								noiseImmission = 0.;
							}
						}
					}
			 		
					linkId2isolatedImmission.put(linkId, noiseImmission);
			 	}
			}
			
			double finalNoiseImmission = 0.;
			if (!linkId2isolatedImmission.isEmpty()) {
				finalNoiseImmission = NoiseEquations.calculateResultingNoiseImmission(linkId2isolatedImmission.values());
			}
			
			rp.setFinalImmission(finalNoiseImmission);
			rp.setLinkId2IsolatedImmission(linkId2isolatedImmission);
		}
	}
	
	private void calculateNoiseEmission() {
				
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()){
			
			double vCar = (this.noiseContext.getScenario().getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
			double vHdv = vCar;
			
			double noiseEmission = 0.;

			int n_car = 0;
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				n_car = this.noiseContext.getNoiseLinks().get(linkId).getCars();
			}
			
			int n_hgv = 0;
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				n_hgv = this.noiseContext.getNoiseLinks().get(linkId).getHgv();
			}
			int n = n_car + n_hgv;
			double p = 0.;
				
			if(!(n == 0)) {
				p = n_hgv / ((double) n);
			}
				
			if(!(n == 0)) {
					
				// correction for a sample, multiplicate the scale factor
				n = (int) (n * (this.noiseContext.getNoiseParams().getScaleFactor()));
					
				// correction for intervals unequal to 3600 seconds (= one hour)
				n = (int) (n * (3600. / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()));
					
				double mittelungspegel = NoiseEquations.calculateMittelungspegelLm(n, p);
				double Dv = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHdv, p);
				noiseEmission = mittelungspegel + Dv;	
			}
			
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				this.noiseContext.getNoiseLinks().get(linkId).setEmission(noiseEmission);		
			} else {
				// also saving zero emissions
				NoiseLink noiseLink = new NoiseLink(linkId);
				noiseLink.setEmission(noiseEmission);
				this.noiseContext.getNoiseLinks().put(linkId, noiseLink );
			}
		}
	}
	
	public void computeFinalTimeIntervals() {
		
		while (this.noiseContext.getCurrentTimeBinEndTime() <= 30 * 3600.) {
			processTimeBin();			
		}
	}

	public List<NoiseEventCaused> getNoiseEventsCaused() {
		return noiseEventsCaused;
	}

	public List<NoiseEventAffected> getNoiseEventsAffected() {
		return noiseEventsAffected;
	}

	public double getTotalCausedNoiseCost() {
		return totalCausedNoiseCost;
	}

	public double getTotalAffectedNoiseCost() {
		return totalAffectedNoiseCost;
	}
	
}
