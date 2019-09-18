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
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;

import com.google.inject.Inject;


/**
 * 
 * @author ikaddoura
 *
 */
final class NoiseCalculationOnline implements BeforeMobsimListener, AfterMobsimListener, StartupListener {
	private static final Logger log = Logger.getLogger(NoiseCalculationOnline.class);
	
	@Inject
	private NoiseContext noiseContext;
	
	@Inject
	private NoiseTimeTracker timeTracker;
			
	@Override
	public void notifyStartup(StartupEvent event) {
		NoiseWriter.writeReceiverPoints(noiseContext, event.getServices().getConfig().controler().getOutputDirectory() + "/receiverPoints/", false);	
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		log.info("Resetting noise immissions, activity information and damages...");

		this.noiseContext.getNoiseLinks().clear();
		this.noiseContext.getTimeInterval2linkId2noiseLinks().clear();
		
		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			rp.reset();
		}
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
				
		timeTracker.computeFinalTimeIntervals();
		timeTracker.processImmissions();
		log.info("Noise calculation completed.");
	}

	NoiseContext getNoiseContext() {
		return noiseContext;
	}
	
}
