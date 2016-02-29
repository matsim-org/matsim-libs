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

import org.apache.log4j.Logger;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.data.NoiseReceiverPoint;
import org.matsim.contrib.noise.handler.LinkSpeedCalculation;
import org.matsim.contrib.noise.handler.NoisePricingHandler;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.contrib.noise.handler.PersonActivityTracker;
import org.matsim.contrib.noise.routing.NoiseTollDisutilityCalculatorFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;


/**
 * Starts the noise computation as specified in the {@link NoiseConfigGroup}.
 * 
 * @author ikaddoura
 *
 */
public class NoiseCalculationOnline implements BeforeMobsimListener, AfterMobsimListener, StartupListener {
	private static final Logger log = Logger.getLogger(NoiseCalculationOnline.class);
	
	private NoiseContext noiseContext;
	private NoiseTimeTracker timeTracker;
	private PersonActivityTracker actTracker;
	private NoisePricingHandler pricing;
		
	/**
	 * Use this constructor if the default travel disutility was previously replaced in your own controler. 
	 *
	 */
	public NoiseCalculationOnline(NoiseContext noiseContext) {
		this.noiseContext = noiseContext;
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) noiseContext.getScenario().getConfig().getModule("noise");
		
		if (noiseParameters.isInternalizeNoiseDamages()) {
			log.warn("Internalizing noise damages. This requires that the default travel disutility is replaced by a travel distuility which accounts for noise tolls.");
		}
	}

	/**
	 * In case noise damages are internalized, this constructor replaces the default travel disutility by a travel disutility which accounts for the noise tolls. 
	 *
	 */
	public NoiseCalculationOnline(Controler controler) {
		
		NoiseContext noiseContext = new NoiseContext(controler.getScenario());
		this.noiseContext = noiseContext;
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) controler.getConfig().getModule("noise");
		
		if (noiseParameters.isInternalizeNoiseDamages()) {
			
			log.info("Internalizing noise damages. The default travel disutility will be replaced by a travel distuility which accounts for noise tolls...");
			
			final NoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new NoiseTollDisutilityCalculatorFactory(this.noiseContext, controler.getConfig().planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
				
		NoiseWriter.writeReceiverPoints(noiseContext, event.getServices().getConfig().controler().getOutputDirectory() + "/receiverPoints/", false);
			
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
