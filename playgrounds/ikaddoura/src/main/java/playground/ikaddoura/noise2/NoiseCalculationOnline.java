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
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.ikaddoura.noise2.data.NoiseContext;
import playground.ikaddoura.noise2.data.NoiseReceiverPoint;
import playground.ikaddoura.noise2.handler.LinkSpeedCalculation;
import playground.ikaddoura.noise2.handler.NoisePricingHandler;
import playground.ikaddoura.noise2.handler.NoiseTimeTracker;
import playground.ikaddoura.noise2.handler.PersonActivityTracker;


/**
 * @author ikaddoura
 *
 */

public class NoiseCalculationOnline implements BeforeMobsimListener, AfterMobsimListener , StartupListener {
	private static final Logger log = Logger.getLogger(NoiseCalculationOnline.class);
	
	private NoiseContext noiseContext;
	private NoiseTimeTracker timeTracker;
	private PersonActivityTracker actTracker;
	private NoisePricingHandler pricing;
			
	public NoiseCalculationOnline(NoiseContext noiseContext) {
		this.noiseContext = noiseContext;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		log.info("Initialization...");
		
		this.noiseContext.initialize();
		NoiseWriter.writeReceiverPoints(noiseContext, event.getServices().getConfig().controler().getOutputDirectory() + "/receiverPoints/", false);
		
		log.info("Initialization... Done.");
	
		this.timeTracker = new NoiseTimeTracker(noiseContext, event.getServices().getEvents(), event.getServices().getConfig().controler().getOutputDirectory() + "/ITERS/");
		event.getServices().getEvents().addHandler(this.timeTracker);
	
		if (this.noiseContext.getNoiseParams().isUseActualSpeedLevel()) {
			LinkSpeedCalculation linkSpeedCalculator = new LinkSpeedCalculation(noiseContext);
			event.getServices().getEvents().addHandler(linkSpeedCalculator);
		}
		
		if (this.noiseContext.getNoiseParams().isComputePopulationUnits()) {
			this.actTracker = new PersonActivityTracker(noiseContext);
			event.getServices().getEvents().addHandler(this.actTracker);
		}
			
		if (this.noiseContext.getNoiseParams().isInternalizeNoiseDamages()) {
			this.pricing = new NoisePricingHandler(event.getServices().getEvents());
			event.getServices().getEvents().addHandler(this.pricing);
		}		
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		log.info("Resetting noise immissions, activity information and damages...");

		this.noiseContext.getNoiseLinks().clear();
		this.noiseContext.getTimeInterval2linkId2noiseLinks().clear();
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			rp.getLinkId2IsolatedImmission().clear();
			rp.setFinalImmission(0.);
			rp.setDamageCosts(0.);
			rp.setDamageCostsPerAffectedAgentUnit(0.);
		}
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
				
		timeTracker.computeFinalTimeIntervals();
		log.info("Noise calculation completed.");
	}

	NoiseContext getNoiseContext() {
		return noiseContext;
	}
	
}
