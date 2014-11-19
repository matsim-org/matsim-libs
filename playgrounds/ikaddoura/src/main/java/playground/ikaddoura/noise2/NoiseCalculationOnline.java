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

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;


/**
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseCalculationOnline implements AfterMobsimListener , IterationEndsListener , StartupListener {
	private static final Logger log = Logger.getLogger(NoiseCalculationOnline.class);
	
	private NoiseParameters noiseParameters;
	private NoiseInitialization initialization;
	private NoiseEmissionHandler noiseEmissionHandler;
	private NoiseImmissionCalculation noiseImmission;
	private PersonActivityHandler personActivityTracker;
	private NoiseDamageCalculation noiseDamageCosts;
	
	public NoiseCalculationOnline(NoiseParameters noiseParameters) {
		this.noiseParameters = noiseParameters;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		log.info("Initialization...");
		
		this.initialization = new NoiseInitialization(event.getControler().getScenario(), noiseParameters);
		this.initialization.setActivityCoords();
		this.initialization.setReceiverPoints();
		this.initialization.setActivityCoord2NearestReceiverPointId();
		this.initialization.setRelevantLinkInfo();
		this.initialization.writeReceiverPoints(event.getControler().getConfig().controler().getOutputDirectory() + "/receiverPoints/");
		
		this.noiseEmissionHandler = new NoiseEmissionHandler(event.getControler().getScenario(), noiseParameters);
		
		this.personActivityTracker = new PersonActivityHandler(event.getControler().getScenario(), this.initialization, noiseParameters);

		log.info("Initialization... Done.");
		
		event.getControler().getEvents().addHandler(noiseEmissionHandler);
		event.getControler().getEvents().addHandler(personActivityTracker);
		
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		// calculate noise emission for each link and time interval
		log.info("Calculating noise emission...");
		this.noiseEmissionHandler.calculateNoiseEmission();
		this.noiseEmissionHandler.writeNoiseEmissionStats(event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/emissionStats.csv");
		this.noiseEmissionHandler.writeNoiseEmissionStatsPerHour(event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/emissionStatsPerHour.csv");
		log.info("Calculating noise emission... Done.");
		
		// calculate the noise immission for each receiver point and time interval
		log.info("Calculating noise immission...");
		this.noiseImmission = new NoiseImmissionCalculation(this.initialization, this.noiseEmissionHandler, noiseParameters);
		noiseImmission.setTunnelLinks(null);
		noiseImmission.setNoiseBarrierLinks(null);
		noiseImmission.calculateNoiseImmission();
		this.noiseImmission.writeNoiseImmissionStats(event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/immissionStats.csv");
		this.noiseImmission.writeNoiseImmissionStatsPerHour(event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/immissionStatsPerHour.csv");
		log.info("Calculating noise immission... Done.");
		
		// calculate activity durations for each agent
		log.info("Calculating each agent's activity durations...");
		this.personActivityTracker.calculateDurationOfStay();
		log.info("Calculating each agent's activity durations... Done.");
				
		log.info("Calculating noise damage costs and throwing noise events...");
		this.noiseDamageCosts = new NoiseDamageCalculation(event.getControler().getScenario(), event.getControler().getEvents(), initialization, noiseParameters, noiseEmissionHandler, personActivityTracker, noiseImmission);
		this.noiseDamageCosts.calculateNoiseDamageCosts();
		log.info("Calculating noise damage costs and throwing noise events... Done.");
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Total Caused Noise Cost: " + noiseDamageCosts.getTotalCausedNoiseCost());
		log.info("Total Affected Noise Cost: " + noiseDamageCosts.getTotalAffectedNoiseCost());
	}
	
	// for testing purposes
	
	NoiseEmissionHandler getNoiseEmissionHandler() {
		return noiseEmissionHandler;
	}

	PersonActivityHandler getPersonActivityTracker() {
		return personActivityTracker;
	}

	NoiseImmissionCalculation getNoiseImmission() {
		return noiseImmission;
	}

	NoiseDamageCalculation getNoiseDamageCosts() {
		return noiseDamageCosts;
	}

	NoiseInitialization getSpatialInfo() {
		return initialization;
	}
		
}
