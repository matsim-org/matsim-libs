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
package org.matsim.contrib.noise;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.analysis.XYTRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * A handler which computes noise emissions, immisions, affected agent units and damages for each receiver point and time interval.
 * Throws noise damage events for each affected and causing agent.
 * 
 * @author ikaddoura
 *
 */

class NoiseTimeTracker implements VehicleEntersTrafficEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler, TransitDriverStartsEventHandler {

	private static final Logger log = Logger.getLogger(NoiseTimeTracker.class);
	private static final boolean printLog = false;
	
	@Inject private NoiseContext noiseContext;
		
	@Inject private EventsManager events;

	@Inject(optional = true) Set<NoiseModule.NoiseListener> listeners ;

	private String outputDirectory;
	private int iteration;
	
	private boolean collectNoiseEvents = true;
	private List<NoiseEventCaused> noiseEventsCaused = new ArrayList<NoiseEventCaused>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	private double totalCausedNoiseCost = 0.;
	private double totalAffectedNoiseCost = 0.;
	
	private boolean useCompression = false ;
	
	private int cWarn1 = 0;
	private int cWarn2 = 0;
	private int cWarn3 = 0;
	private int cWarn4 = 0;

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
			this.noiseContext.getNotConsideredTransitVehicleIDs().add(event.getVehicleId());

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
				this.noiseContext.getNotConsideredTransitVehicleIDs().add(event.getVehicleId());
			}
		}
	}

	@Override
	public void reset(int iteration) {
		
		String outputDir = noiseContext.getScenario().getConfig().controler().getOutputDirectory();
		if (!outputDir.endsWith("/")) outputDir = outputDir + "/";
		
		this.outputDirectory = outputDir + "ITERS/" + "it." + iteration + "/";
		log.info("Setting the output directory to " + outputDirectory);
		
		this.iteration = iteration;
		
		this.totalCausedNoiseCost = 0.;
		this.totalAffectedNoiseCost = 0.;
		this.noiseEventsCaused.clear();
		this.noiseEventsAffected.clear();
		
		this.noiseContext.getNoiseLinks().clear();
		this.noiseContext.getTimeInterval2linkId2noiseLinks().clear();
		this.noiseContext.getLinkId2vehicleId2lastEnterTime().clear();
		this.noiseContext.setCurrentTimeBinEndTime(this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
		this.noiseContext.getVehicleId2PersonId().clear();
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			rp.reset();
		}
		
	}
	
	private void resetCurrentTimeIntervalInfo() {
		
		this.noiseContext.getNoiseLinks().clear();
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			rp.resetTimeInterval();
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
		
		if (printLog) log.info("##############################################");
		if (printLog) log.info("# Computing noise for time interval " + Time.writeTime(this.noiseContext.getCurrentTimeBinEndTime(), Time.TIMEFORMAT_HHMMSS) + " #");
		if (printLog) log.info("##############################################");

		updateActivityInformation(); // Remove activities that were completed in the previous time interval.
		computeNoiseForCurrentTimeInterval(); // Compute noise emissions, immissions, affected agent units and damages for the current time interval.			
		updateCurrentTimeInterval(); // Set the current time bin to the next one ( current time bin = current time bin + time bin size ).
		resetCurrentTimeIntervalInfo(); // Reset all time-specific information from the previous time interval.
	}

	private void updateActivityInformation() {
		double timeBinEnd = this.noiseContext.getCurrentTimeBinEndTime() - this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation() ;
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			for (Id<Person> personId : rp.getPersonId2actInfos().keySet()) {
				Iterator<PersonActivityInfo> it = rp.getPersonId2actInfos().get(personId).iterator();
				while (it.hasNext()) {
				    if ( it.next().getEndTime() < ( timeBinEnd) ) {
				        it.remove();
				    }
				}
			}				
		}
	}

	private void computeNoiseForCurrentTimeInterval() {
		
		if (printLog) log.info("Calculating noise emissions...");
		calculateNoiseEmission();
		if (writeOutput()) NoiseWriter.writeNoiseEmissionStatsPerHour(this.noiseContext, outputDirectory, useCompression);
		if (printLog) log.info("Calculating noise emissions... Done.");
		
		/*
		 * The basic idea is to calculate the immisions, damages etc per receiver-point.
		 * Doing it that way we we can save a lot of memory since we do not store informations 
		 * for every RP for the complete timestep. //DR20180216 
		 */
		
		if (printLog) log.info("Calculating noise immissions...");
		if (printLog) log.info("Calculating the number of affected agent units...");
		if (printLog) log.info("Calculating noise damage costs...");

		for(NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			NoiseReceiverPointImmision immisions = calculateNoiseImmission(rp);
			if (this.noiseContext.getNoiseParams().isComputePopulationUnits()) {
				calculateAffectedAgentUnits(rp);
				if (this.noiseContext.getNoiseParams().isComputeNoiseDamages()) {
					calculateDamagePerReceiverPoint(rp);
				}
				if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
					computeAverageDamageCost(rp, immisions);
					calculateMarginalDamageCost(rp, immisions);
				}
			}
			if ( listeners!=null ) {
				XYTRecord record = new XYTRecord.Builder()
										   .setStartTime( 0. )
										   .setEndTime( 0. )
										   .setCoord( rp.getCoord() )
										   .setFacilityId( null )
										   .put( "immissions", rp.getFinalImmission() )
										   .build() ;
				for( NoiseModule.NoiseListener listener : listeners ){
					listener.newRecord( record );
				}
			} else {
				log.warn("listeners=null") ;
			}
		}
		calculateCostsPerVehiclePerLinkPerTimeInterval();
		
		finishNoiseDamageCosts();


		if (writeOutput()) {
			NoiseWriter.writeNoiseImmissionStatsPerHour(noiseContext, outputDirectory);
			if (this.noiseContext.getNoiseParams().isComputePopulationUnits()) {
				NoiseWriter.writePersonActivityInfoPerHour(noiseContext, outputDirectory);
			}
			if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
				NoiseWriter.writeLinkMarginalCarDamageInfoPerHour(noiseContext, outputDirectory);
				NoiseWriter.writeLinkMarginalHgvDamageInfoPerHour(noiseContext, outputDirectory);
			}
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

	private void calculateAffectedAgentUnits(NoiseReceiverPoint rp) {
		
//		for (NoiseReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
			
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
//		}
	}

	private void updateCurrentTimeInterval() {
		double newTimeInterval = this.noiseContext.getCurrentTimeBinEndTime() + this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
		this.noiseContext.setCurrentTimeBinEndTime(newTimeInterval);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		checkTime(event.getTime());

		if (this.noiseContext.getIgnoredNetworkModeVehicleIDs().contains(event.getVehicleId()) || this.noiseContext.getNotConsideredTransitVehicleIDs().contains(event.getVehicleId())) {
			// a not considered mode (e.g. 'bike') or a not considered transit vehicle (e.g. train)
			
		} else {
			// car, HGV or a considered transit vehicle (e.g. a bus)
					
			if (this.noiseContext.getLinkId2vehicleId2lastEnterTime().get(event.getLinkId()) != null) {
				this.noiseContext.getLinkId2vehicleId2lastEnterTime().get(event.getLinkId()).put(event.getVehicleId(), event.getTime());
				
			} else {
				Map<Id<Vehicle>, Double> vehicleId2enterTime = new HashMap<>();
				vehicleId2enterTime.put(event.getVehicleId(), event.getTime());
				this.noiseContext.getLinkId2vehicleId2lastEnterTime().put(event.getLinkId(), vehicleId2enterTime);
			}
			
			if (this.noiseContext.getNoiseLinks().get(event.getLinkId()) != null) {
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
		}
	}
	
	private void finishNoiseDamageCosts() {
		
		if (writeOutput()) NoiseWriter.writeDamageInfoPerHour(noiseContext, outputDirectory);

		if (this.noiseContext.getNoiseParams().isThrowNoiseEventsAffected()) {
			
			if (printLog) log.info("Throwing noise events for the affected agents...");
			throwNoiseEventsAffected();
			if (printLog) log.info("Throwing noise events for the affected agents... Done.");
		}
		
		if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
			calculateCostsPerVehiclePerLinkPerTimeInterval();
			if (writeOutput()) NoiseWriter.writeLinkDamageInfoPerHour(noiseContext, outputDirectory);
			if (writeOutput()) NoiseWriter.writeLinkAvgCarDamageInfoPerHour(noiseContext, outputDirectory);
			if (writeOutput()) NoiseWriter.writeLinkAvgHgvDamageInfoPerHour(noiseContext, outputDirectory);
			
			if (this.noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
				if (printLog) log.info("Throwing noise events for the causing agents...");
				throwNoiseEventsCaused();
				if (printLog) log.info("Throwing noise events for the causing agents... Done.");
				
				if (this.noiseContext.getNoiseParams().isComputeAvgNoiseCostPerLinkAndTime()) {
					this.noiseContext.storeTimeInterval();
				}
			}	
		}	
	}

	/*
	 * Damage cost for each receiver point
	 */
	private void calculateDamagePerReceiverPoint(NoiseReceiverPoint rp) {
		double currentTimeBinEndTime = this.noiseContext.getCurrentTimeBinEndTime();
		double annualCostRate = this.noiseContext.getNoiseParams().getAnnualCostRate();
		double timeBinsSize = this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
		
//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
				
			double noiseImmission = rp.getFinalImmission();
			double affectedAgentUnits = rp.getAffectedAgentUnits();
			
			double damageCost = NoiseEquations.calculateDamageCosts(noiseImmission, affectedAgentUnits, currentTimeBinEndTime, annualCostRate, timeBinsSize);
			double damageCostPerAffectedAgentUnit = NoiseEquations.calculateDamageCosts(noiseImmission, 1., currentTimeBinEndTime, annualCostRate, timeBinsSize);
				
			rp.setDamageCosts(damageCost);
			rp.setDamageCostsPerAffectedAgentUnit(damageCostPerAffectedAgentUnit);
//		}
	}

	private void throwNoiseEventsAffected() {
		double currentTimeBinEndTime = this.noiseContext.getCurrentTimeBinEndTime();
		double eventTime = this.noiseContext.getEventTime();
		double timeBinSizeNoiseComputation = this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			
			if (!(rp.getPersonId2actInfos().isEmpty())) {
				
				for (Id<Person> personId : rp.getPersonId2actInfos().keySet()) {
					
					for (PersonActivityInfo actInfo : rp.getPersonId2actInfos().get(personId)) {
						
						double factor = actInfo.getDurationWithinInterval(currentTimeBinEndTime, timeBinSizeNoiseComputation) /  timeBinSizeNoiseComputation;
						double amount = factor * rp.getDamageCostsPerAffectedAgentUnit();
						
						if (amount != 0.) {
							NoiseEventAffected noiseEventAffected = new NoiseEventAffected(eventTime, currentTimeBinEndTime, personId, amount, rp.getId(), actInfo.getActivityType());
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
	 * Noise allocation approach: AverageCost
	 */
	private void computeAverageDamageCost(NoiseReceiverPoint rp, NoiseReceiverPointImmision immisions) {
		calculateCostSharesPerLinkPerTimeInterval(rp, immisions);
//		calculateCostsPerVehiclePerLinkPerTimeInterval();
	}

	/*
	 * Noise allocation approach: AverageCost
	 */
	private void calculateCostSharesPerLinkPerTimeInterval(NoiseReceiverPoint rp, NoiseReceiverPointImmision immisions) {
		
		Map<Id<Link>, Double> linkId2costShare = new HashMap<Id<Link>, Double>();

//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
										
							
			if (rp.getDamageCosts() != 0.) {
				for (Id<Link> linkId : immisions.getLinkId2IsolatedImmission().keySet()) {
										
					double noiseImmission = immisions.getLinkId2IsolatedImmission().get(linkId);
					double costs = 0.;
						
					if (!(noiseImmission == 0.)) {
						double costShare = NoiseEquations.calculateShareOfResultingNoiseImmission(noiseImmission, rp.getFinalImmission());
						costs = costShare * rp.getDamageCosts();	
					}
					linkId2costShare.put(linkId, costs);
				}
			}
			
//		}
		
		// summing up the link-based costs
//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

			if (rp.getDamageCosts() != 0.) {
				
				for (Id<Link> linkId : rp.getLinkId2distanceCorrection().keySet()) {
					NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId); 
					if ( noiseLink != null) {
						double sum = noiseLink.getDamageCost() + linkId2costShare.get(linkId);
						noiseLink.setDamageCost(sum);
					}		
				}
			}
//		}
	}

	/*
	 * Noise allocation approach: AverageCost
	 */
	private void calculateCostsPerVehiclePerLinkPerTimeInterval() {
		
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {

			double damageCostPerCar = 0.;
			double damageCostPerHgv = 0.;
			
			NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);

			double damageCostSum = 0.;
			int nCarAgents = 0;
			int nHdvAgents = 0;
			
			if (noiseLink != null) {					
				damageCostSum = noiseLink.getDamageCost();
				nCarAgents = noiseLink.getCarAgentsEntering();
				nHdvAgents = noiseLink.getHgvAgentsEntering();
			}
				
			Tuple<Double, Double> vCarVHdv = getV(linkId, noiseLink);
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
			
			NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();
			if (!(nCarAgents == 0)) {
				damageCostPerCar = damageCostSumCar / (nCarAgents * noiseParams.getScaleFactor());
			}
				
			if (!(nHdvAgents == 0)) {
				damageCostPerHgv = damageCostSumHdv / (nHdvAgents * noiseParams.getScaleFactor());
			}
			
			if (damageCostPerCar > 0.) {
				noiseLink.setAverageDamageCostPerCar(damageCostPerCar);
			}
			if (damageCostPerHgv > 0.) {
				noiseLink.setAverageDamageCostPerHgv(damageCostPerHgv);			
			}
		}
	}
	
//	/*
//	 * Noise allocation approach: MarginalCost
//	 */
//	private void computeMarginalDamageCost() {
//		
//		// For each receiver point we have something like:
//		// Immission_linkA(n)
//		// Immission_linkA(n-1)
//		// Immission_linkB(n)
//		// Immission_linkB(n-1)
//		// Immission_linkC(n)
//		// Immission_linkC(n-1)
//		// ...
//		
//		// resultingImmission = computeResultingImmission(Immission_linkA(n), Immission_linkB(n), Immission_linkC(n), ...)
//		
//		// MarginalCostCar_linkA = damageCost(resultingImmission) - damageCost(X)
//		// X = computeResultingImmission(Immission_linkA(n-1), Immission_linkB(n), Immission_linkC(n), ...)
//		
//		// MarginalCostCar_linkB = damageCost(resultingImmission) - damageCost(Y)
//		// Y = computeResultingImmission(Immission_linkA(n), Immission_linkB(n-1), Immission_linkC(n), ...)		
//		
//		if (printLog) log.info("Computing the marginal damage cost for each link and receiver point...");
//		calculateMarginalDamageCost();
//		if (writeOutput()) NoiseWriter.writeLinkMarginalCarDamageInfoPerHour(noiseContext, outputDirectory);
//		if (writeOutput()) NoiseWriter.writeLinkMarginalHgvDamageInfoPerHour(noiseContext, outputDirectory);
//		if (printLog) log.info("Computing the marginal damage cost for each link and receiver point... Done.");
//	}
	
	/*
	 * Noise allocation approach: MarginalCost
	 */
	private void calculateMarginalDamageCost(NoiseReceiverPoint rp, NoiseReceiverPointImmision immision) {
//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

			if (rp.getAffectedAgentUnits() != 0.) {
				for (Id<Link> thisLink : immision.getLinkId2IsolatedImmission().keySet()) {
										
					double noiseImmissionPlusOneCarThisLink = NoiseEquations.calculateResultingNoiseImmissionPlusOneVehicle(rp.getFinalImmission(), immision.getLinkId2IsolatedImmission().get(thisLink), immision.getLinkId2IsolatedImmissionPlusOneCar().get(thisLink));
					double noiseImmissionPlusOneHGVThisLink = NoiseEquations.calculateResultingNoiseImmissionPlusOneVehicle(rp.getFinalImmission(), immision.getLinkId2IsolatedImmission().get(thisLink), immision.getLinkId2IsolatedImmissionPlusOneHGV().get(thisLink));
					
					double damageCostsPlusOneCarThisLink = NoiseEquations.calculateDamageCosts(noiseImmissionPlusOneCarThisLink, rp.getAffectedAgentUnits(), this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
					double marginalDamageCostCarThisLink = (damageCostsPlusOneCarThisLink - rp.getDamageCosts()) / this.noiseContext.getNoiseParams().getScaleFactor();
					
					if (marginalDamageCostCarThisLink < 0.0) {
						if (Math.abs(marginalDamageCostCarThisLink) < 0.0000000001) {
							marginalDamageCostCarThisLink = 0.;
						} else {
							if (cWarn3 == 0) {
								log.warn("The marginal damage cost per car on link " + thisLink.toString() + " for receiver point " + rp.getId().toString() + " is " + marginalDamageCostCarThisLink + ".");
								log.warn("final immission: " + rp.getFinalImmission() + " - immission plus one car " + noiseImmissionPlusOneCarThisLink + " - marginal damage cost car: " + marginalDamageCostCarThisLink);
								log.warn("Setting the marginal damage cost per car to 0.");
								log.warn("This message is only given once.");
								cWarn3++;
							}
							
							marginalDamageCostCarThisLink = 0.;
						}
					}
					
					double damageCostsPlusOneHGVThisLink = NoiseEquations.calculateDamageCosts(noiseImmissionPlusOneHGVThisLink, rp.getAffectedAgentUnits(), this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
					double marginalDamageCostHGVThisLink = (damageCostsPlusOneHGVThisLink - rp.getDamageCosts()) / this.noiseContext.getNoiseParams().getScaleFactor();
					
					if (marginalDamageCostHGVThisLink < 0.0) {
						if (Math.abs(marginalDamageCostHGVThisLink) < 0.0000000001) {
							marginalDamageCostHGVThisLink = 0.;
						} else {
							if (cWarn4 == 0) {
								log.warn("The marginal damage cost per HGV on link " + thisLink.toString() + " for receiver point " + rp.getId().toString() + " is " + marginalDamageCostHGVThisLink + ".");
								log.warn("final immission: " + rp.getFinalImmission() + " - immission plus one car " + noiseImmissionPlusOneCarThisLink + " - marginal damage cost car: " + marginalDamageCostHGVThisLink);
								log.warn("Setting the marginal damage cost per HGV to 0.");
								log.warn("This message is only given once.");
								cWarn4++;
							}
							
							marginalDamageCostHGVThisLink = 0.;
						}
					}
					NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(thisLink);
					double marginalDamageCostCarSum = noiseLink.getMarginalDamageCostPerCar() + marginalDamageCostCarThisLink;
					noiseLink.setMarginalDamageCostPerCar(marginalDamageCostCarSum);
					
					double marginalDamageCostHGVSum = noiseLink.getMarginalDamageCostPerHgv() + marginalDamageCostHGVThisLink;
					noiseLink.setMarginalDamageCostPerHgv(marginalDamageCostHGVSum);
				}			
			}	
//		}
	}

	private void throwNoiseEventsCaused() {
		String[] hgvPrefixes = this.noiseContext.getNoiseParams().getHgvIdPrefixesArray();
		Set<Id<Vehicle>> busVehicleIds = this.noiseContext.getBusVehicleIDs();
		double eventTime = this.noiseContext.getEventTime();
		double currentTimeBinEndTime = this.noiseContext.getCurrentTimeBinEndTime();
		NoiseAllocationApproach noiseAllocationApproach = this.noiseContext.getNoiseParams().getNoiseAllocationApproach();
		
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
			NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
			if (noiseLink != null) {
				
				double amountCar = 0.;
				double amountHdv = 0.;
				
				if (noiseAllocationApproach == NoiseAllocationApproach.AverageCost) {
					amountCar = noiseLink.getAverageDamageCostPerCar();
					amountHdv = noiseLink.getAverageDamageCostPerHgv();
				
				} else if (noiseAllocationApproach == NoiseAllocationApproach.MarginalCost) {
					amountCar = noiseLink.getMarginalDamageCostPerCar();
					amountHdv = noiseLink.getMarginalDamageCostPerHgv();
					
				} else {
					throw new RuntimeException("Unknown noise allocation approach. Aborting...");
				}
				
				for(Id<Vehicle> vehicleId : noiseLink.getEnteringVehicleIds()) {
					
					double amount = 0.;
					
					boolean isHGV = false;
					for (String hgvPrefix : hgvPrefixes) {
						if (vehicleId.toString().startsWith(hgvPrefix)) {
							isHGV = true;
							break;
						}
					}
										
					if(isHGV || busVehicleIds.contains(vehicleId)) {
						amount = amountHdv;
					} else {
						amount = amountCar;
					}
					
					if (amount != 0.) {
						
						if (this.noiseContext.getNotConsideredTransitVehicleIDs().contains(vehicleId)) {
							// skip
						} else {
							NoiseEventCaused noiseEvent = new NoiseEventCaused(
									eventTime, 
									currentTimeBinEndTime, 
									this.noiseContext.getLinkId2vehicleId2lastEnterTime().get(linkId).get(vehicleId), 
									this.noiseContext.getVehicleId2PersonId().get(vehicleId), 
									vehicleId, amount, linkId);
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
	}
	
	/*
	 * Immission
	 */
	private NoiseReceiverPointImmision calculateNoiseImmission(NoiseReceiverPoint rp) {
		NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();
		NoiseReceiverPointImmision immision = new NoiseReceiverPointImmision();
		
//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
					
//			Map<Id<Link>, Double> linkId2isolatedImmission = new HashMap<Id<Link>, Double>();
			Map<Id<Link>, Double> linkId2isolatedImmissionPlusOneCar = new HashMap<Id<Link>, Double>();
			Map<Id<Link>, Double> linkId2isolatedImmissionPlusOneHGV = new HashMap<Id<Link>, Double>();
			
			for(Id<Link> linkId : rp.getLinkId2distanceCorrection().keySet()) {
				double distanceCorrection = rp.getLinkId2distanceCorrection().get(linkId);
				double angleCorrection = rp.getLinkId2angleCorrection().get(linkId);
				double shieldingCorrection = 0.;
				if(noiseParams.isConsiderNoiseBarriers()) {
					shieldingCorrection = rp.getLinkId2ShieldingCorrection().get(linkId);
				}

				if (noiseParams.getTunnelLinkIDsSet().contains(linkId)) {
					immision.setLinkId2IsolatedImmission(linkId, 0.);
					linkId2isolatedImmissionPlusOneCar.put(linkId, 0.);
					linkId2isolatedImmissionPlusOneHGV.put(linkId, 0.);
								 			
			 	} else {
				
			 		double noiseImmission = 0.;
			 		double noiseImmissionPlusOneCar = 0.;
			 		double noiseImmissionPlusOneHGV = 0.;
			 		NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
			 		if (noiseLink != null) {
						if (!(noiseLink.getEmission() == 0.)) {
							noiseImmission = noiseLink.getEmission()
									+ distanceCorrection + angleCorrection - shieldingCorrection;
							
							if (noiseImmission < 0.) {
								noiseImmission = 0.;
							}
						}
						
						if (!(noiseLink.getEmissionPlusOneCar() == 0.)) {
							noiseImmissionPlusOneCar = noiseLink.getEmissionPlusOneCar()
									+ distanceCorrection + angleCorrection - shieldingCorrection;
							
							if (noiseImmissionPlusOneCar < 0.) {
								noiseImmissionPlusOneCar = 0.;
							}
						}
						
						if (!(noiseLink.getEmissionPlusOneHGV() == 0.)) {
							noiseImmissionPlusOneHGV = noiseLink.getEmissionPlusOneHGV()
									+ distanceCorrection + angleCorrection - shieldingCorrection;
							
							if (noiseImmissionPlusOneHGV < 0.) {
								noiseImmissionPlusOneHGV = 0.;
							}
						}

					}
			 		
			 		if (noiseImmissionPlusOneCar < noiseImmission || noiseImmissionPlusOneHGV < noiseImmission) {
						throw new RuntimeException("noise immission: " + noiseImmission + " - noise immission plus one car: " + noiseImmissionPlusOneCar + " - noise immission plus one hgv: " + noiseImmissionPlusOneHGV + ". This should not happen. Aborting..."); 
					}
			 		
			 		immision.setLinkId2IsolatedImmission(linkId, noiseImmission);
					linkId2isolatedImmissionPlusOneCar.put(linkId, noiseImmissionPlusOneCar);
					linkId2isolatedImmissionPlusOneHGV.put(linkId, noiseImmissionPlusOneHGV);
			 	}
			}
			
			double finalNoiseImmission = 0.;
			Map<Id<Link>, Double> linkId2isolatedImmission = immision.getLinkId2IsolatedImmission();
			if (linkId2isolatedImmission != null && !linkId2isolatedImmission.isEmpty()) {
				finalNoiseImmission = NoiseEquations.calculateResultingNoiseImmission(linkId2isolatedImmission.values());
			}
			
			rp.setFinalImmission(finalNoiseImmission);
			immision.setLinkId2IsolatedImmissionPlusOneCar(linkId2isolatedImmissionPlusOneCar);
			immision.setLinkId2IsolatedImmissionPlusOneHGV(linkId2isolatedImmissionPlusOneHGV);
			return immision;
//		}
	}
	
	/*
	 * Emission
	 */
	private void calculateNoiseEmission() {
		NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();
				
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
			NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
			
			Tuple<Double, Double> vCarVHdv = getV(linkId, noiseLink);
			double vCar = vCarVHdv.getFirst();
			double vHdv = vCarVHdv.getSecond();
							
			double noiseEmission = 0.;
			double noiseEmissionPlusOneCar = 0.;
			double noiseEmissionPlusOneHgv = 0.;
			
			int n_car = 0;
			if (noiseLink != null) {
				n_car = noiseLink.getCarAgentsEntering();
			}
			
			int n_hgv = 0;
			if (noiseLink != null) {
				n_hgv = noiseLink.getHgvAgentsEntering();
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
			n = (int) (n * (noiseParams.getScaleFactor()));
				
			// correction for intervals unequal to 3600 seconds (= one hour)
			n = (int) (n * (3600. / noiseParams.getTimeBinSizeNoiseComputation()));
			
			// correction for a sample, multiplicate the scale factor
			nPlusOneCarOrHGV = (int) (nPlusOneCarOrHGV * (noiseParams.getScaleFactor()));
							
			// correction for intervals unequal to 3600 seconds (= one hour)
			nPlusOneCarOrHGV = (int) (nPlusOneCarOrHGV * (3600. / noiseParams.getTimeBinSizeNoiseComputation()));
			
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
			
			if(noiseLink == null) {
				noiseLink = new NoiseLink(linkId);
				this.noiseContext.getNoiseLinks().put(linkId, noiseLink );
			}
			
			noiseLink.setEmission(noiseEmission);
			noiseLink.setEmissionPlusOneCar(noiseEmissionPlusOneCar);
			noiseLink.setEmissionPlusOneHGV(noiseEmissionPlusOneHgv);
		}
	}
	
	private Tuple<Double, Double> getV(Id<Link> linkId, NoiseLink noiseLink) {
		Link link = this.noiseContext.getScenario().getNetwork().getLinks().get(linkId);
		NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();
		
		double vCar = (link.getFreespeed()) * 3.6;
		double vHdv = vCar;
					
		double freespeedCar = vCar;

		if (noiseParams.isUseActualSpeedLevel()) {
			
			// use the actual speed level if possible
			if (noiseLink != null) {
				
				// Car
				if (noiseLink.getTravelTimeCar_sec() == 0. || noiseLink.getCarAgentsLeaving() == 0) {
					// use the maximum speed level
					
				} else {
					double averageTravelTimeCar_sec = noiseLink.getTravelTimeCar_sec() / noiseLink.getCarAgentsLeaving();	
					vCar = 3.6 * (link.getLength() / averageTravelTimeCar_sec );
				}
				
				// HGV
				if (noiseLink.getTravelTimeHGV_sec() == 0. || noiseLink.getHgvAgentsLeaving() == 0) {
					// use the actual car speed level
					vHdv = vCar;
	
				} else {
					double averageTravelTimeHGV_sec = noiseLink.getTravelTimeHGV_sec() / noiseLink.getHgvAgentsLeaving();
					vHdv = 3.6 * (link.getLength() / averageTravelTimeHGV_sec );
				}		
			}
		}
				
		if (vCar > freespeedCar) {
			throw new RuntimeException(vCar + " > " + freespeedCar + ". This should not be possible. Aborting...");
		}
		
		if (noiseParams.isAllowForSpeedsOutsideTheValidRange() == false) {
			
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

		while (this.noiseContext.getCurrentTimeBinEndTime() <= Math.max(24. * 3600., this.noiseContext.getScenario().getConfig().qsim().getEndTime())) {
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

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.noiseContext.getVehicleId2PersonId().put(event.getVehicleId(), event.getPersonId());
	}

	public void setNoiseContext(NoiseContext noiseContext) {
		this.noiseContext = noiseContext;
	}

	public void setEvents(EventsManager events) {
		this.events = events;
	}

	public void setOutputFilePath(String outputFilePath) {
		this.outputDirectory = outputFilePath;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (this.noiseContext.getNoiseParams().getNetworkModesToIgnore() != null) {
			if (this.noiseContext.getNoiseParams().getNetworkModesToIgnore().contains(event.getNetworkMode())) {
				this.noiseContext.getIgnoredNetworkModeVehicleIDs().add(event.getVehicleId());
			}
		}
	}
	
	
	
}
