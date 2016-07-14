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
package org.matsim.contrib.noise.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.NoiseWriter;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.data.NoiseLink;
import org.matsim.contrib.noise.data.NoiseReceiverPoint;
import org.matsim.contrib.noise.data.PersonActivityInfo;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.contrib.noise.events.NoiseEventAffected;
import org.matsim.contrib.noise.events.NoiseEventCaused;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * A handler which computes noise emissions, immisions, affected agent units and damages for each receiver point and time interval.
 * Throws noise damage events for each affected and causing agent.
 * 
 * @author ikaddoura
 *
 */

public class NoiseTimeTracker implements LinkEnterEventHandler, TransitDriverStartsEventHandler {

	private static final Logger log = Logger.getLogger(NoiseTimeTracker.class);
	
	private final NoiseContext noiseContext;
	private final String outputDirectoryBasic;
	private final EventsManager events;

	private String outputDirectory;
	private int iteration;
	
	private boolean collectNoiseEvents = true;
	private List<NoiseEventCaused> noiseEventsCaused = new ArrayList<NoiseEventCaused>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	private double totalCausedNoiseCost = 0.;
	private double totalAffectedNoiseCost = 0.;
	
	private boolean useCompression = false ;
	
	public NoiseTimeTracker(NoiseContext noiseContext, EventsManager events, String outputDirectory) {
		this.noiseContext = noiseContext;
		this.outputDirectoryBasic = outputDirectory;
		this.outputDirectory = outputDirectory;
		this.events = events;	
	}
	
	int cWarn1 = 0;
	int cWarn2 = 0;

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		
		if (this.noiseContext.getNoiseParams().getBusIdIdentifierSet() == null || this.noiseContext.getNoiseParams().getBusIdIdentifierSet().size() == 0) {
			if (cWarn1 == 0) {
				log.warn("Simulated public transit detected. "
						+ "To calculate noise caused by road vehicles, e.g. buses, "
						+ "please provide a char sequence which marks a bus in the transit line Id. "
						+ "This message is only given once.");
				cWarn1++;
			}
			
		} else {
			boolean isBus = false;
			for (String busIdPrefix : this.noiseContext.getNoiseParams().getBusIdIdentifierSet()) {
				if (event.getTransitLineId().toString().contains(busIdPrefix)) {
					isBus = true;
					break;
				}
			}
			if (isBus) {
				if (!this.noiseContext.getBusVehicleIDs().contains(event.getVehicleId())) {
					this.noiseContext.getBusVehicleIDs().add(event.getVehicleId());
				}

			} else {
				if (cWarn2 == 0) {
					log.warn("This noise computation approach does not account for transit vehicles other than road vehicles. "
							+ "Vehicle " + event.getVehicleId() + " belonging to transit line " + event.getTransitLineId() + " will not be considered. "
							+ "This message is only given once");
					cWarn2++;
				}
			}
		}
	}

	@Override
	public void reset(int iteration) {
		
		this.outputDirectory = this.outputDirectoryBasic + "it." + iteration + "/";
		log.info("Setting the output directory to " + outputDirectory);
		
		this.iteration = iteration;
		
		this.totalCausedNoiseCost = 0.;
		this.totalAffectedNoiseCost = 0.;
		this.noiseEventsCaused.clear();
		this.noiseEventsAffected.clear();
		
		this.noiseContext.getNoiseLinks().clear();
		this.noiseContext.getTimeInterval2linkId2noiseLinks().clear();
		this.noiseContext.setCurrentTimeBinEndTime(this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
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
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
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
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
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
		if (writeOutput()) NoiseWriter.writeNoiseEmissionStatsPerHour(this.noiseContext, outputDirectory, useCompression);
		log.info("Calculating noise emissions... Done.");
		
		log.info("Calculating noise immissions...");
		calculateNoiseImmission();
		if (writeOutput()) NoiseWriter.writeNoiseImmissionStatsPerHour(noiseContext, outputDirectory);
		log.info("Calculating noise immissions... Done.");
	
		if (this.noiseContext.getNoiseParams().isComputePopulationUnits()) {
			log.info("Calculating the number of affected agent units...");
			calculateAffectedAgentUnits();
			if (writeOutput()) NoiseWriter.writePersonActivityInfoPerHour(noiseContext, outputDirectory);
			log.info("Calculating the number of affected agent units... Done.");
		}
	
		if (this.noiseContext.getNoiseParams().isComputeNoiseDamages()) {
			log.info("Calculating noise damage costs...");
			calculateNoiseDamageCosts();
			log.info("Calculating noise damage costs... Done.");
		}
			
	}
		
	private boolean writeOutput() {
		if (this.noiseContext.getNoiseParams().getWriteOutputIteration() == 0) {
			return false;
		} else if (this.iteration % this.noiseContext.getNoiseParams().getWriteOutputIteration() == 0) {
			return true;
		} else {
			return false;
		}
	}

	private void calculateAffectedAgentUnits() {
		
		for (NoiseReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
			
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

		if (this.noiseContext.getScenario().getPopulation().getPersons().containsKey(event.getVehicleId())
				|| this.noiseContext.getBusVehicleIDs().contains(event.getVehicleId())) {
			// car, lkw or bus
			
			if (this.noiseContext.getNoiseLinks().containsKey(event.getLinkId())) {
				this.noiseContext.getNoiseLinks().get(event.getLinkId()).getEnteringVehicleIds().add(event.getVehicleId());
			
			} else {
				
				NoiseLink noiseLink = new NoiseLink(event.getLinkId());
				List<Id<Vehicle>> enteringVehicleIds = new ArrayList<Id<Vehicle>>();
				enteringVehicleIds.add(event.getVehicleId());
				noiseLink.setEnteringVehicleIds(enteringVehicleIds);
				
				this.noiseContext.getNoiseLinks().put(event.getLinkId(), noiseLink);
			}
		
			boolean isHGV = false;
			for (String hgvPrefix : this.noiseContext.getNoiseParams().getHgvIdPrefixesArray()) {
				if (event.getVehicleId().toString().startsWith(hgvPrefix)) {
					isHGV = true;
					break;
				}
			}
			
			if (isHGV || this.noiseContext.getBusVehicleIDs().contains(event.getVehicleId())) {			
				// HGV or Bus
								
				int hgv = this.noiseContext.getNoiseLinks().get(event.getLinkId()).getHgvAgentsEntering();
				hgv++;
				this.noiseContext.getNoiseLinks().get(event.getLinkId()).setHgvAgentsEntering(hgv);
				
			} else {
				// Car
				
				int cars = this.noiseContext.getNoiseLinks().get(event.getLinkId()).getCarAgentsEntering();
				cars++;
				this.noiseContext.getNoiseLinks().get(event.getLinkId()).setCarAgentsEntering(cars);			
			}
			
		} else {
			// a transit vehicle which is not considered
		}
	}
	
	private void calculateNoiseDamageCosts() {
		
		log.info("Calculating noise damage costs for each receiver point...");
		calculateDamagePerReceiverPoint();
		if (writeOutput()) NoiseWriter.writeDamageInfoPerHour(noiseContext, outputDirectory);
		log.info("Calculating noise damage costs for each receiver point... Done.");

		if (this.noiseContext.getNoiseParams().isThrowNoiseEventsAffected()) {
			
			log.info("Throwing noise events for the affected agents...");
			throwNoiseEventsAffected();
			log.info("Throwing noise events for the affected agents... Done.");
		}
		
		if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
			
			computeAverageDamageCost();
			computeMarginalDamageCost();
			
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

	/*
	 * Damage cost for each receiver point
	 */
	private void calculateDamagePerReceiverPoint() {
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
				
			double noiseImmission = rp.getFinalImmission();
			double affectedAgentUnits = rp.getAffectedAgentUnits();
			
			double damageCost = NoiseEquations.calculateDamageCosts(noiseImmission, affectedAgentUnits, this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
			double damageCostPerAffectedAgentUnit = NoiseEquations.calculateDamageCosts(noiseImmission, 1., this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
				
			rp.setDamageCosts(damageCost);
			rp.setDamageCostsPerAffectedAgentUnit(damageCostPerAffectedAgentUnit);
		}
	}

	/*
	 * Noise allocation approach: AverageCost
	 */
	private void computeAverageDamageCost() {
		
		log.info("Allocating the total damage cost (per receiver point) to the relevant links...");
		calculateCostSharesPerLinkPerTimeInterval();
		if (writeOutput()) NoiseWriter.writeLinkDamageInfoPerHour(noiseContext, outputDirectory);
		log.info("Allocating the total damage cost (per receiver point) to the relevant links... Done.");
		log.info("Allocating the damage cost per link to the vehicle categories and vehicles...");
		calculateCostsPerVehiclePerLinkPerTimeInterval();
		if (writeOutput()) NoiseWriter.writeLinkAvgCarDamageInfoPerHour(noiseContext, outputDirectory);
		if (writeOutput()) NoiseWriter.writeLinkAvgHgvDamageInfoPerHour(noiseContext, outputDirectory);
		log.info("Allocating the damage cost per link to the vehicle categories and vehicles... Done.");
	}

	/*
	 * Noise allocation approach: AverageCost
	 */
	private void calculateCostSharesPerLinkPerTimeInterval() {
		
		Map<Id<ReceiverPoint>, Map<Id<Link>, Double>> rpId2linkId2costShare = new HashMap<Id<ReceiverPoint>, Map<Id<Link>,Double>>();

		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
										
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
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

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

	/*
	 * Noise allocation approach: AverageCost
	 */
	private void calculateCostsPerVehiclePerLinkPerTimeInterval() {
		
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {

			double damageCostPerCar = 0.;
			double damageCostPerHgv = 0.;
			
			double damageCostSum = 0.;
				
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {					
				damageCostSum = this.noiseContext.getNoiseLinks().get(linkId).getDamageCost();
			}
				
			int nCarAgents = 0;
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				nCarAgents = this.noiseContext.getNoiseLinks().get(linkId).getCarAgentsEntering();
			}
			
			int nHdvAgents = 0;
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				nHdvAgents = this.noiseContext.getNoiseLinks().get(linkId).getHgvAgentsEntering();
			}	
			
			Tuple<Double, Double> vCarVHdv = getV(linkId);
			double vCar = vCarVHdv.getFirst();
			double vHdv = vCarVHdv.getSecond();
				
			double lCar = NoiseEquations.calculateLCar(vCar);
			double lHdv = NoiseEquations.calculateLHdv(vHdv);
				
			double shareCar = 0.;
			double shareHdv = 0.;
				
			if ((nCarAgents > 0) || (nHdvAgents > 0)) {
				shareCar = NoiseEquations.calculateShare(nCarAgents, lCar, nHdvAgents, lHdv);
				shareHdv = NoiseEquations.calculateShare(nHdvAgents, lHdv, nCarAgents, lCar);
			}
			
			double damageCostSumCar = shareCar * damageCostSum;
			double damageCostSumHdv = shareHdv * damageCostSum;
				
			if (!(nCarAgents == 0)) {
				damageCostPerCar = damageCostSumCar / (nCarAgents * this.noiseContext.getNoiseParams().getScaleFactor());
			}
				
			if (!(nHdvAgents == 0)) {
				damageCostPerHgv = damageCostSumHdv / (nHdvAgents * this.noiseContext.getNoiseParams().getScaleFactor());
			}
			
			if (damageCostPerCar > 0.) {
				this.noiseContext.getNoiseLinks().get(linkId).setAverageDamageCostPerCar(damageCostPerCar);
			}
			if (damageCostPerHgv > 0.) {
				this.noiseContext.getNoiseLinks().get(linkId).setAverageDamageCostPerHgv(damageCostPerHgv);			
			}
		}
	}
	
	/*
	 * Noise allocation approach: MarginalCost
	 */
	private void computeMarginalDamageCost() {
		
		// For each receiver point we have something like:
		// Immission_linkA(n)
		// Immission_linkA(n-1)
		// Immission_linkB(n)
		// Immission_linkB(n-1)
		// Immission_linkC(n)
		// Immission_linkC(n-1)
		// ...
		
		// resultingImmission = computeResultingImmission(Immission_linkA(n), Immission_linkB(n), Immission_linkC(n), ...)
		
		// MarginalCostCar_linkA = damageCost(resultingImmission) - damageCost(X)
		// X = computeResultingImmission(Immission_linkA(n-1), Immission_linkB(n), Immission_linkC(n), ...)
		
		// MarginalCostCar_linkB = damageCost(resultingImmission) - damageCost(Y)
		// Y = computeResultingImmission(Immission_linkA(n), Immission_linkB(n-1), Immission_linkC(n), ...)		
		
		log.info("Computing the marginal damage cost for each link and receiver point...");
		calculateMarginalDamageCost();
		if (writeOutput()) NoiseWriter.writeLinkMarginalCarDamageInfoPerHour(noiseContext, outputDirectory);
		if (writeOutput()) NoiseWriter.writeLinkMarginalHgvDamageInfoPerHour(noiseContext, outputDirectory);
		log.info("Computing the marginal damage cost for each link and receiver point... Done.");
	}
	
	/*
	 * Noise allocation approach: MarginalCost
	 */
	private void calculateMarginalDamageCost() {
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

			if (rp.getAffectedAgentUnits() != 0.) {
				for (Id<Link> thisLink : rp.getLinkId2IsolatedImmission().keySet()) {
										
					double noiseImmissionPlusOneCarThisLink = NoiseEquations.calculateResultingNoiseImmissionPlusOneVehicle(rp.getFinalImmission(), rp.getLinkId2IsolatedImmission().get(thisLink), rp.getLinkId2IsolatedImmissionPlusOneCar().get(thisLink));
					double noiseImmissionPlusOneHGVThisLink = NoiseEquations.calculateResultingNoiseImmissionPlusOneVehicle(rp.getFinalImmission(), rp.getLinkId2IsolatedImmission().get(thisLink), rp.getLinkId2IsolatedImmissionPlusOneHGV().get(thisLink));
					
					double damageCostsPlusOneCarThisLink = NoiseEquations.calculateDamageCosts(noiseImmissionPlusOneCarThisLink, rp.getAffectedAgentUnits(), this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
					double marginalDamageCostCarThisLink = (damageCostsPlusOneCarThisLink - rp.getDamageCosts()) / this.noiseContext.getNoiseParams().getScaleFactor();
					
					if (marginalDamageCostCarThisLink < 0.0) {
						if (Math.abs(marginalDamageCostCarThisLink) < 0.0000000001) {
							marginalDamageCostCarThisLink = 0.;
						} else {
							log.warn("The marginal damage cost per car on link " + thisLink.toString() + " for receiver point " + rp.getId().toString() + " is " + marginalDamageCostCarThisLink + ".");
							log.warn("final immission: " + rp.getFinalImmission() + " - immission plus one car " + noiseImmissionPlusOneCarThisLink + " - marginal damage cost car: " + marginalDamageCostCarThisLink);
							log.warn("Setting the marginal damage cost per car to 0.");
							marginalDamageCostCarThisLink = 0.;
						}
					}
					
					double damageCostsPlusOneHGVThisLink = NoiseEquations.calculateDamageCosts(noiseImmissionPlusOneHGVThisLink, rp.getAffectedAgentUnits(), this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
					double marginalDamageCostHGVThisLink = (damageCostsPlusOneHGVThisLink - rp.getDamageCosts()) / this.noiseContext.getNoiseParams().getScaleFactor();
					
					if (marginalDamageCostHGVThisLink < 0.0) {
						if (Math.abs(marginalDamageCostHGVThisLink) < 0.0000000001) {
							marginalDamageCostHGVThisLink = 0.;
						} else {
							log.warn("The marginal damage cost per HGV on link " + thisLink.toString() + " for receiver point " + rp.getId().toString() + " is " + marginalDamageCostHGVThisLink + ".");
							log.warn("final immission: " + rp.getFinalImmission() + " - immission plus one car " + noiseImmissionPlusOneCarThisLink + " - marginal damage cost car: " + marginalDamageCostHGVThisLink);
							log.warn("Setting the marginal damage cost per HGV to 0.");
							marginalDamageCostHGVThisLink = 0.;
						}
					}
					
					double marginalDamageCostCarSum = this.noiseContext.getNoiseLinks().get(thisLink).getMarginalDamageCostPerCar() + marginalDamageCostCarThisLink;
					this.noiseContext.getNoiseLinks().get(thisLink).setMarginalDamageCostPerCar(marginalDamageCostCarSum);
					
					double marginalDamageCostHGVSum = this.noiseContext.getNoiseLinks().get(thisLink).getMarginalDamageCostPerHgv() + marginalDamageCostHGVThisLink;
					this.noiseContext.getNoiseLinks().get(thisLink).setMarginalDamageCostPerHgv(marginalDamageCostHGVSum);
				}			
			}	
		}
	}

	private void throwNoiseEventsCaused() {
		
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
											
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				
				double amountCar = 0.;
				double amountHdv = 0.;
				
				if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.AverageCost) {
					amountCar = this.noiseContext.getNoiseLinks().get(linkId).getAverageDamageCostPerCar();
					amountHdv = this.noiseContext.getNoiseLinks().get(linkId).getAverageDamageCostPerHgv();
				
				} else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.MarginalCost) {
					amountCar = this.noiseContext.getNoiseLinks().get(linkId).getMarginalDamageCostPerCar();
					amountHdv = this.noiseContext.getNoiseLinks().get(linkId).getMarginalDamageCostPerHgv();
					
				} else {
					throw new RuntimeException("Unknown noise allocation approach. Aborting...");
				}
				
				for(Id<Vehicle> vehicleId : this.noiseContext.getNoiseLinks().get(linkId).getEnteringVehicleIds()) {
					
					double amount = 0.;
					
					boolean isHGV = false;
					for (String hgvPrefix : this.noiseContext.getNoiseParams().getHgvIdPrefixesArray()) {
						if (vehicleId.toString().startsWith(hgvPrefix)) {
							isHGV = true;
							break;
						}
					}
										
					if(isHGV || this.noiseContext.getBusVehicleIDs().contains(vehicleId)) {
						amount = amountHdv;
					} else {
						amount = amountCar;
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
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			
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

	/*
	 * Immission
	 */
	private void calculateNoiseImmission() {
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
					
			Map<Id<Link>, Double> linkId2isolatedImmission = new HashMap<Id<Link>, Double>();
			Map<Id<Link>, Double> linkId2isolatedImmissionPlusOneCar = new HashMap<Id<Link>, Double>();
			Map<Id<Link>, Double> linkId2isolatedImmissionPlusOneHGV = new HashMap<Id<Link>, Double>();
			
			for(Id<Link> linkId : rp.getLinkId2distanceCorrection().keySet()) {
				if (this.noiseContext.getNoiseParams().getTunnelLinkIDsSet().contains(linkId)) {
					linkId2isolatedImmission.put(linkId, 0.);
					linkId2isolatedImmissionPlusOneCar.put(linkId, 0.);
					linkId2isolatedImmissionPlusOneHGV.put(linkId, 0.);
								 			
			 	} else {
				
			 		double noiseImmission = 0.;
			 		double noiseImmissionPlusOneCar = 0.;
			 		double noiseImmissionPlusOneHGV = 0.;
		 		
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
						
						if (!(this.noiseContext.getNoiseLinks().get(linkId).getEmissionPlusOneCar() == 0.)) {
							noiseImmissionPlusOneCar = this.noiseContext.getNoiseLinks().get(linkId).getEmissionPlusOneCar()
									+ this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2distanceCorrection().get(linkId)
									+ this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2angleCorrection().get(linkId)
									;
							
							if (noiseImmissionPlusOneCar < 0.) {
								noiseImmissionPlusOneCar = 0.;
							}
						}
						
						if (!(this.noiseContext.getNoiseLinks().get(linkId).getEmissionPlusOneHGV() == 0.)) {
							noiseImmissionPlusOneHGV = this.noiseContext.getNoiseLinks().get(linkId).getEmissionPlusOneHGV()
									+ this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2distanceCorrection().get(linkId)
									+ this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2angleCorrection().get(linkId)
									;
							
							if (noiseImmissionPlusOneHGV < 0.) {
								noiseImmissionPlusOneHGV = 0.;
							}
						}

					}
			 		
			 		if (noiseImmissionPlusOneCar < noiseImmission || noiseImmissionPlusOneHGV < noiseImmission) {
						throw new RuntimeException("noise immission: " + noiseImmission + " - noise immission plus one car: " + noiseImmissionPlusOneCar + " - noise immission plus one hgv: " + noiseImmissionPlusOneHGV + ". This should not happen. Aborting..."); 
					}
			 		
					linkId2isolatedImmission.put(linkId, noiseImmission);
					linkId2isolatedImmissionPlusOneCar.put(linkId, noiseImmissionPlusOneCar);
					linkId2isolatedImmissionPlusOneHGV.put(linkId, noiseImmissionPlusOneHGV);
			 	}
			}
			
			double finalNoiseImmission = 0.;
			if (!linkId2isolatedImmission.isEmpty()) {
				finalNoiseImmission = NoiseEquations.calculateResultingNoiseImmission(linkId2isolatedImmission.values());
			}
			
			rp.setFinalImmission(finalNoiseImmission);
			rp.setLinkId2IsolatedImmission(linkId2isolatedImmission);
			rp.setLinkId2IsolatedImmissionPlusOneCar(linkId2isolatedImmissionPlusOneCar);
			rp.setLinkId2IsolatedImmissionPlusOneHGV(linkId2isolatedImmissionPlusOneHGV);
		}
	}
	
	/*
	 * Emission
	 */
	private void calculateNoiseEmission() {
				
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
			
			Tuple<Double, Double> vCarVHdv = getV(linkId);
			double vCar = vCarVHdv.getFirst();
			double vHdv = vCarVHdv.getSecond();
							
			double noiseEmission = 0.;
			double noiseEmissionPlusOneCar = 0.;
			double noiseEmissionPlusOneHgv = 0.;
			
			int n_car = 0;
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				n_car = this.noiseContext.getNoiseLinks().get(linkId).getCarAgentsEntering();
			}
			
			int n_hgv = 0;
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				n_hgv = this.noiseContext.getNoiseLinks().get(linkId).getHgvAgentsEntering();
			}
			int n = n_car + n_hgv;
									
			double p = 0.;
			if(!(n == 0)) {
				p = n_hgv / ((double) n);
			}
			
			int nPlusOneCarOrHGV = n + 1;
	
			double pPlusOneHgv = (n_hgv + 1.) / ((double) nPlusOneCarOrHGV);
			double pPlusOneCar = n_hgv / ((double) nPlusOneCarOrHGV);
							
			// correction for a sample, multiplicate the scale factor
			n = (int) (n * (this.noiseContext.getNoiseParams().getScaleFactor()));
				
			// correction for intervals unequal to 3600 seconds (= one hour)
			n = (int) (n * (3600. / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()));
			
			// correction for a sample, multiplicate the scale factor
			nPlusOneCarOrHGV = (int) (nPlusOneCarOrHGV * (this.noiseContext.getNoiseParams().getScaleFactor()));
							
			// correction for intervals unequal to 3600 seconds (= one hour)
			nPlusOneCarOrHGV = (int) (nPlusOneCarOrHGV * (3600. / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()));
			
			if(!(n == 0)) {					
				double mittelungspegel = NoiseEquations.calculateMittelungspegelLm(n, p);
				double Dv = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHdv, p);
				noiseEmission = mittelungspegel + Dv;
			}
			
			double mittelungspegelPlusOneCar = NoiseEquations.calculateMittelungspegelLm(nPlusOneCarOrHGV, pPlusOneCar);
			double DvPlusOneCar = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHdv, pPlusOneCar);
			noiseEmissionPlusOneCar = mittelungspegelPlusOneCar + DvPlusOneCar;
			
			double mittelungspegelPlusOneHgv = NoiseEquations.calculateMittelungspegelLm(nPlusOneCarOrHGV, pPlusOneHgv);
			double DvPlusOneHgv = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHdv, pPlusOneHgv);
			noiseEmissionPlusOneHgv = mittelungspegelPlusOneHgv + DvPlusOneHgv;
			
			if (noiseEmissionPlusOneCar < noiseEmission || noiseEmissionPlusOneHgv < noiseEmission) {
				log.warn("vCar: " + vCar + " - vHGV: " + vHdv + " - p: " + p + " - n_car: " + n_car + " - n_hgv: " + n_hgv + " - n: " + n + " - pPlusOneCar: " + pPlusOneCar + " - pPlusOneHgv: " + pPlusOneHgv + " - noise emission: " + noiseEmission + " - noise emission plus one car: " + noiseEmissionPlusOneCar + " - noise emission plus one hgv: " + noiseEmissionPlusOneHgv + ". This should not happen. Aborting..."); 
			}
			
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				this.noiseContext.getNoiseLinks().get(linkId).setEmission(noiseEmission);
				this.noiseContext.getNoiseLinks().get(linkId).setEmissionPlusOneCar(noiseEmissionPlusOneCar);
				this.noiseContext.getNoiseLinks().get(linkId).setEmissionPlusOneHGV(noiseEmissionPlusOneHgv);

			} else {
				NoiseLink noiseLink = new NoiseLink(linkId);
				noiseLink.setEmission(noiseEmission);
				noiseLink.setEmissionPlusOneCar(noiseEmissionPlusOneCar);
				noiseLink.setEmissionPlusOneHGV(noiseEmissionPlusOneHgv);
				this.noiseContext.getNoiseLinks().put(linkId, noiseLink );
			}
		}
	}
	
	private Tuple<Double, Double> getV(Id<Link> linkId) {
		
		double vCar = (this.noiseContext.getScenario().getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
		double vHdv = vCar;
					
		double freespeedCar = vCar;

		if (this.noiseContext.getNoiseParams().isUseActualSpeedLevel()) {
			
			// use the actual speed level if possible
			if (this.noiseContext.getNoiseLinks().containsKey(linkId)) {
				
				// Car
				if (this.noiseContext.getNoiseLinks().get(linkId).getTravelTimeCar_sec() == 0. || this.noiseContext.getNoiseLinks().get(linkId).getCarAgentsLeaving() == 0) {
					// use the maximum speed level
					
				} else {
					double averageTravelTimeCar_sec = this.noiseContext.getNoiseLinks().get(linkId).getTravelTimeCar_sec() / this.noiseContext.getNoiseLinks().get(linkId).getCarAgentsLeaving();	
					vCar = 3.6 * (this.noiseContext.getScenario().getNetwork().getLinks().get(linkId).getLength() / averageTravelTimeCar_sec );
				}
				
				// HGV
				if (this.noiseContext.getNoiseLinks().get(linkId).getTravelTimeHGV_sec() == 0. || this.noiseContext.getNoiseLinks().get(linkId).getHgvAgentsLeaving() == 0) {
					// use the actual car speed level
					vHdv = vCar;
	
				} else {
					double averageTravelTimeHGV_sec = this.noiseContext.getNoiseLinks().get(linkId).getTravelTimeHGV_sec() / this.noiseContext.getNoiseLinks().get(linkId).getHgvAgentsLeaving();
					vHdv = 3.6 * (this.noiseContext.getScenario().getNetwork().getLinks().get(linkId).getLength() / averageTravelTimeHGV_sec );
				}		
			}
		}
				
		if (vCar > freespeedCar) {
			throw new RuntimeException(vCar + " > " + freespeedCar + ". This should not be possible. Aborting...");
		}
		
		if (this.noiseContext.getNoiseParams().isAllowForSpeedsOutsideTheValidRange() == false) {
			
			// shifting the speed into the allowed range defined by the RLS-90 computation approach
			
			if (vCar < 30.) {
				vCar = 30.;
			}
			
			if (vHdv < 30.) {
				vHdv = 30.;
			}
			
			if (vCar > 130.) {
				vCar = 130.;
			}
			
			if (vHdv > 80.) {
				vHdv = 80.;
			}
		}
		
		Tuple<Double, Double> vCarVHdv = new Tuple<>(vCar, vHdv);
		return vCarVHdv;
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

	public final boolean isUseCompression() {
		return this.useCompression;
	}

	public final void setUseCompression(boolean useCompression) {
		this.useCompression = useCompression;
	}
	
}
