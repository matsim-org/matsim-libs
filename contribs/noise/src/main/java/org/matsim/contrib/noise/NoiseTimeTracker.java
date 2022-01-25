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

import com.google.inject.Inject;
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
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.MemoryObserver;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A handler which computes noise emissions, immisions, affected agent units and damages for each receiver point and time interval.
 * Throws noise damage events for each affected and causing agent.
 * 
 * @author ikaddoura
 *
 */
class NoiseTimeTracker implements VehicleEntersTrafficEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler, TransitDriverStartsEventHandler {

	private static final Logger log = Logger.getLogger(NoiseTimeTracker.class);
	private static final boolean printLog = true;
	
	private NoiseContext noiseContext;

	private String outputDirectory;
	private int iteration;
	
	private boolean useCompression = false ;

	private int cWarn1 = 0;
	private int cWarn2 = 0;

	private final NoiseEmission emission;
	private final NoiseImmission immissionModule;
	private final NoiseDamageCalculation damageCalculation;
    private final NoiseVehicleIdentifier vehicleIdentifier;
	private final Set<NoiseVehicleType> vehicleTypes;
	private String networkModesToIgnore;

	@Inject
	NoiseTimeTracker(NoiseContext context, NoiseEmission emission, NoiseImmission immissionModule,
					 NoiseDamageCalculation damageCalculation, NoiseVehicleIdentifier vehicleIdentifier,
					 Set<NoiseVehicleType> vehicleTypes) {
		this.noiseContext = context;
		this.emission = emission;
		this.immissionModule = immissionModule;
		this.damageCalculation = damageCalculation;
        this.vehicleIdentifier = vehicleIdentifier;
		this.vehicleTypes = vehicleTypes;
		networkModesToIgnore = this.noiseContext.getNoiseParams().getNetworkModesToIgnore();
		setRelevantLinkInfo();
	}

	private void setRelevantLinkInfo() {
		MemoryObserver.start(60);
		Counter cnt = new Counter("set relevant link-info # ");
		final NoiseConfigGroup noiseParams = noiseContext.getNoiseParams();
		for(NoiseReceiverPoint nrp: noiseContext.getGrid().getReceiverPoints().values()) {
			if(!nrp.isInitialized()) {
				// get the zone grid cell around the receiver point
				Set<Id<Link>> potentialLinks = noiseContext.getPotentialLinks(nrp);
				immissionModule.setCurrentRp(nrp);

					// go through these potential relevant link Ids
				Set<Id<Link>> relevantLinkIds = ConcurrentHashMap.newKeySet();
				potentialLinks.parallelStream().forEach(linkId -> {
					if (!(relevantLinkIds.contains(linkId))) {
						Link candidateLink = noiseContext.getScenario().getNetwork().getLinks().get(linkId);
						double projectedDistance = CoordUtils.distancePointLinesegment(candidateLink.getFromNode().getCoord(), candidateLink.getToNode().getCoord(), nrp.getCoord());
						if (projectedDistance < noiseParams.getRelevantRadius()) {
							relevantLinkIds.add(linkId);
							double correction = immissionModule.calculateCorrection(projectedDistance, nrp, candidateLink);
							nrp.setLinkId2Correction(linkId, correction);
						}
					}
				});
				nrp.setInitialized();
			}

			noiseContext.getReceiverPoints().put(nrp.getId(), nrp);
			cnt.incCounter();
		}
		cnt.printCounter();
		MemoryObserver.stop();
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
		this.noiseContext.reset();

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
		MemoryObserver.printMemory();

		// Remove activities that were completed in the previous time interval.
		updateActivityInformation();
		// Compute noise emissions, immissions, affected agent units and damages for the current time interval.
		computeNoiseForCurrentTimeInterval();

		log.info("# Finished time interval " + Time.writeTime(this.noiseContext.getCurrentTimeBinEndTime(), Time.TIMEFORMAT_HHMMSS) + " #");

		// Set the current time bin to the next one ( current time bin = current time bin + time bin size ).
		updateCurrentTimeInterval();
		// Reset all time-specific information from the previous time interval.
		resetCurrentTimeIntervalInfo();

	}

	private void updateActivityInformation() {
		double timeBinEnd = this.noiseContext.getCurrentTimeBinEndTime() - this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation() ;
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			for (Id<Person> personId : rp.getPersonId2actInfos().keySet()) {
				rp.getPersonId2actInfos().get(personId).removeIf(personActivityInfo -> personActivityInfo.getEndTime() < (timeBinEnd));
			}				
		}
	}

	private void computeNoiseForCurrentTimeInterval() {
		
		if (printLog) {
			log.info("Calculating noise emissions...");
		}
		calculateNoiseEmission();

		if (printLog) {
			log.info("Calculating noise immissions, the number of affected agent units and noise damage costs.....");
		}
		calculateNoiseImmissionsAndDamages();

		if (writeOutput()) {
			log.info("Writing to files...");
			NoiseWriter.writeNoiseEmissionStatsPerHour(this.noiseContext, outputDirectory, useCompression, vehicleTypes);
			NoiseWriter.writeNoiseImmissionStatsPerHour(noiseContext, outputDirectory);
			if (this.noiseContext.getNoiseParams().isComputePopulationUnits()) {
				NoiseWriter.writePersonActivityInfoPerHour(noiseContext, outputDirectory);
			}
			if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
				for(NoiseVehicleType type: vehicleTypes) {
					NoiseWriter.writeLinkMarginalVehicleDamageInfoPerHour(noiseContext, outputDirectory, type);
				}
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
		double newTimeInterval = this.noiseContext.getCurrentTimeBinEndTime()
				+ this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
		this.noiseContext.setCurrentTimeBinEndTime(newTimeInterval);
	}
	
	/*
	 * Emission
	 */
	private void calculateNoiseEmission() {
		Counter cnt = new Counter("calculate link noise emission # ");
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
			NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
            if(noiseLink == null) {
                noiseLink = new NoiseLink(linkId);
                this.noiseContext.getNoiseLinks().put(linkId, noiseLink );
            }
			emission.calculateEmission(noiseLink);
			cnt.incCounter();
		}
		cnt.printCounter();
	}

	/**
	 * Immissions and damages
	 */
	private void calculateNoiseImmissionsAndDamages() {
		Counter cnt = new Counter("process noise receiver point # ");
		this.noiseContext.getReceiverPoints().values().parallelStream().forEach( rp -> {
			immissionModule.calculateImmission(rp, this.noiseContext.getCurrentTimeBinEndTime());
			damageCalculation.calculateDamages(rp);
            cnt.incCounter();

            //free up memory
            rp.setLinkId2IsolatedImmission(null);
            rp.setLinkId2IsolatedImmissionPlusOneVehicle(null);
		});
		cnt.printCounter();
		log.info("Done processing receiver points.");
		damageCalculation.finishNoiseDamageCosts();
	}

	void computeFinalTimeIntervals() {
		while (this.noiseContext.getCurrentTimeBinEndTime() <= Math.max(24. * 3600., this.noiseContext.getScenario().getConfig().qsim().getEndTime().orElse(0))) {
			processTimeBin();			
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (networkModesToIgnore.contains(event.getNetworkMode())) {
			this.noiseContext.getIgnoredNetworkModeVehicleIDs().add(event.getVehicleId());
		}
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

			NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(event.getLinkId());
			if (noiseLink == null) {
				noiseLink = new NoiseLink(event.getLinkId());
				this.noiseContext.getNoiseLinks().put(event.getLinkId(), noiseLink);
			}

			if(noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
				noiseLink.addEnteringVehicleId(event.getVehicleId());
			}

            NoiseVehicleType noiseVehicleType = vehicleIdentifier.identifyVehicle(event.getVehicleId());
			this.noiseContext.getNoiseLinks().get(event.getLinkId()).addEnteringAgent(noiseVehicleType);
		}
	}

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
                this.noiseContext.getBusVehicleIDs().add(event.getVehicleId());

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
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.noiseContext.getVehicleId2PersonId().put(event.getVehicleId(), event.getPersonId());
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

	public void setNoiseContext(NoiseContext noiseContext) {
		this.noiseContext = noiseContext;
	}

	void setOutputFilePath(String outputFilePath) {
		this.outputDirectory = outputFilePath;
		damageCalculation.setOutputFilePath(outputFilePath);
	}

	NoiseDamageCalculation getDamageCalculation() {
		return damageCalculation;
	}
}
