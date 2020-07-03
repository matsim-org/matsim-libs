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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import static org.matsim.contrib.noise.RLS90VehicleType.car;
import static org.matsim.contrib.noise.RLS90VehicleType.hgv;

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

	private String outputDirectory;
	private int iteration;
	

	private boolean useCompression = false ;
	
	private int cWarn1 = 0;
	private int cWarn2 = 0;


	@Inject
	private NoiseEmission emission;
	@Inject
	private NoiseImmission immissionModule;
	@Inject
	private NoiseDamageCalculation damageCalculation;

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
		if (!outputDir.endsWith("/")) {
			outputDir = outputDir + "/";
		}
		
		this.outputDirectory = outputDir + "ITERS/" + "it." + iteration + "/";
		log.info("Setting the output directory to " + outputDirectory);
		
		this.iteration = iteration;
		this.damageCalculation.reset(iteration);

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
		
		if (printLog) {
			log.info("##############################################");
		}
		if (printLog) {
			log.info("# Computing noise for time interval " + Time.writeTime(this.noiseContext.getCurrentTimeBinEndTime(), Time.TIMEFORMAT_HHMMSS) + " #");
		}
		if (printLog) {
			log.info("##############################################");
		}

		// Remove activities that were completed in the previous time interval.
		updateActivityInformation();
		// Compute noise emissions, immissions, affected agent units and damages for the current time interval.
		computeNoiseForCurrentTimeInterval();
		// Set the current time bin to the next one ( current time bin = current time bin + time bin size ).
		updateCurrentTimeInterval();
		// Reset all time-specific information from the previous time interval.
		resetCurrentTimeIntervalInfo();
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
		
		if (printLog) {
			log.info("Calculating noise emissions...");
		}
		calculateNoiseEmission();
		if (writeOutput()) {
			NoiseWriter.writeNoiseEmissionStatsPerHour(this.noiseContext, outputDirectory, useCompression);
		}
		if (printLog) {
			log.info("Calculating noise emissions... Done.");
		}
		
		/*
		 * The basic idea is to calculate the immisions, damages etc per receiver-point.
		 * Doing it that way we we can save a lot of memory since we do not store informations 
		 * for every RP for the complete timestep. //DR20180216 
		 */
		
		if (printLog) {
			log.info("Calculating noise immissions...");
		}
		if (printLog) {
			log.info("Calculating the number of affected agent units...");
		}
		if (printLog) {
			log.info("Calculating noise damage costs...");
		}

		for(NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			NoiseReceiverPointImmision immisions = calculateNoiseImmission(rp);
			damageCalculation.calculateDamages(rp, immisions);
		}
		damageCalculation.finishNoiseDamageCosts();


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
								
				int hgvs = this.noiseContext.getNoiseLinks().get(event.getLinkId()).getAgentsEntering(hgv.getId());
				hgvs++;
				this.noiseContext.getNoiseLinks().get(event.getLinkId()).setAgentsEntering(hgv.getId(), hgvs);
				
			} else {
				// Car
				
				int cars = this.noiseContext.getNoiseLinks().get(event.getLinkId()).getAgentsEntering(car.getId());
				cars++;
				this.noiseContext.getNoiseLinks().get(event.getLinkId()).setAgentsEntering(car.getId(), cars);
			}		
		}
	}
	
	/*
	 * Immission
	 */
	private NoiseReceiverPointImmision calculateNoiseImmission(NoiseReceiverPoint rp) {
		NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();
		NoiseReceiverPointImmision immision = new NoiseReceiverPointImmision();

			Map<Id<NoiseVehicleType>, Map<Id<Link>, Double>> linkId2isolatedImmissionPlusOne = new HashMap<>();

			for(Id<Link> linkId : rp.getRelevantLinks()) {
				double noiseImmission = 0;
				if (noiseParams.getTunnelLinkIDsSet().contains(linkId)) {
					immision.setLinkId2IsolatedImmission(linkId, 0.);
					linkId2isolatedImmissionPlusOne.computeIfAbsent(car.getId(), type -> new HashMap<>()).put(linkId, 0.);
					linkId2isolatedImmissionPlusOne.computeIfAbsent(hgv.getId(), type -> new HashMap<>()).put(linkId, 0.);

			 	} else {
			 		NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
			 		if (noiseLink != null) {
			 			noiseImmission = immissionModule.calculateIsolatedLinkImmission(rp, noiseLink);
			 			for(NoiseVehicleType vehicleType: noiseParams.getNoiseComputationMethod().noiseVehiclesTypes) {
			 				double immissionPlusOne = immissionModule.calculateIsolatedLinkImmissionPlusOneVehicle(rp, noiseLink, vehicleType);
							if (immissionPlusOne < 0.) {
								immissionPlusOne = 0.;
							}
							if (immissionPlusOne < noiseImmission) {
								throw new RuntimeException("noise immission: " + noiseImmission + " - noise immission plus one "
										+ vehicleType.getId() + immissionPlusOne + ". This should not happen. Aborting...");
							}
							linkId2isolatedImmissionPlusOne.computeIfAbsent(vehicleType.getId(), type -> new HashMap<>()).put(linkId, immissionPlusOne);

						}
					}
			 		immision.setLinkId2IsolatedImmission(linkId, noiseImmission);
			 	}
			}

			double finalNoiseImmission;
			Map<Id<Link>, Double> linkId2isolatedImmission = immision.getLinkId2IsolatedImmission();
			finalNoiseImmission = immissionModule.calculateResultingNoiseImmission(linkId2isolatedImmission.values());

			rp.setCurrentImmission(finalNoiseImmission, this.noiseContext.getCurrentTimeBinEndTime());
			immision.setLinkId2IsolatedImmissionPlusOneVehicle(car.getId(), linkId2isolatedImmissionPlusOne.getOrDefault(car.getId(), new HashMap<>()));
			immision.setLinkId2IsolatedImmissionPlusOneVehicle(hgv.getId(), linkId2isolatedImmissionPlusOne.getOrDefault(hgv.getId(), new HashMap<>()));
			return immision;
	}
	
	/*
	 * Emission
	 */
	private void calculateNoiseEmission() {
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
			NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
            if(noiseLink == null) {
                noiseLink = new NoiseLink(linkId);
                this.noiseContext.getNoiseLinks().put(linkId, noiseLink );
            }
			emission.calculateEmission(noiseLink);
		}
	}

	void computeFinalTimeIntervals() {
		while (this.noiseContext.getCurrentTimeBinEndTime() <= Math.max(24. * 3600., this.noiseContext.getScenario().getConfig().qsim().getEndTime().orElse(0))) {
			processTimeBin();			
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.noiseContext.getVehicleId2PersonId().put(event.getVehicleId(), event.getPersonId());
	}

	public void setNoiseContext(NoiseContext noiseContext) {
		this.noiseContext = noiseContext;
	}

	public void setOutputFilePath(String outputFilePath) {
		this.outputDirectory = outputFilePath;
		damageCalculation.setOutputFilePath(outputFilePath);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.noiseContext.getNoiseParams().getNetworkModesToIgnore();
		if (this.noiseContext.getNoiseParams().getNetworkModesToIgnore().contains(event.getNetworkMode())) {
			this.noiseContext.getIgnoredNetworkModeVehicleIDs().add(event.getVehicleId());
		}
	}

	NoiseDamageCalculation getDamageCalculation() {
		return damageCalculation;
	}
}
