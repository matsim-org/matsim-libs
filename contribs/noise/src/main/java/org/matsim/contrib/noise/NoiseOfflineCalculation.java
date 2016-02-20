/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.noise;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.LinkSpeedCalculation;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.contrib.noise.handler.PersonActivityTracker;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * (1) Computes noise emissions, immissions, person activities and damages based on a standard events file.
 * (2) Optionally throws noise immission damage events for the causing agent and the affected agent.
 *
 * @author ikaddoura
 *
 */
public class NoiseOfflineCalculation {
	private static final Logger log = Logger.getLogger(NoiseOfflineCalculation.class);

	private String outputDirectory;
	private Scenario scenario;
	
	private NoiseContext noiseContext = null;
	private NoiseTimeTracker timeTracker = null;

	public NoiseOfflineCalculation(Scenario scenario, String analysisOutputDirectory) {
		this.scenario = scenario;
		this.outputDirectory = analysisOutputDirectory;
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) scenario.getConfig().getModule("noise");
		if (noiseParameters.isInternalizeNoiseDamages()) {
			log.warn("If you intend to internalize noise damages, please run the online noise computation."
					+ " This is an offline noise calculation which can only be used for analysis purposes.");
			noiseParameters.setInternalizeNoiseDamages(false);
		}
	}

	public void run() {
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
			
		String outputFilePath = outputDirectory + "analysis_it." + this.scenario.getConfig().controler().getLastIteration() + "/";
		File file = new File(outputFilePath);
		file.mkdirs();
					
		noiseContext = new NoiseContext(scenario);
		NoiseWriter.writeReceiverPoints(noiseContext, outputFilePath + "/receiverPoints/", false);
				
		EventsManager events = EventsUtils.createEventsManager();

		timeTracker = new NoiseTimeTracker(noiseContext, events, outputFilePath);
		events.addHandler(timeTracker);
		
		if (noiseContext.getNoiseParams().isUseActualSpeedLevel()) {
			LinkSpeedCalculation linkSpeedCalculator = new LinkSpeedCalculation(noiseContext);
			events.addHandler(linkSpeedCalculator);	
		}
		
		EventWriterXML eventWriter = null;
		if (noiseContext.getNoiseParams().isThrowNoiseEventsAffected() || noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
			eventWriter = new EventWriterXML(outputFilePath + this.scenario.getConfig().controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
			events.addHandler(eventWriter);	
		}
		
		if (noiseContext.getNoiseParams().isComputePopulationUnits()) {
			PersonActivityTracker actTracker = new PersonActivityTracker(noiseContext);
			events.addHandler(actTracker);
		}
		
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(this.scenario.getConfig().controler().getOutputDirectory() + "ITERS/it." + this.scenario.getConfig().controler().getLastIteration() + "/" + this.scenario.getConfig().controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");
		
		timeTracker.computeFinalTimeIntervals();

		if (noiseContext.getNoiseParams().isThrowNoiseEventsAffected() || noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
			eventWriter.closeFile();
		}
		log.info("Noise calculation completed.");
		
	}

	public NoiseTimeTracker getTimeTracker() {
		return timeTracker;
	}

	public NoiseContext getNoiseContext() {
		return noiseContext;
	}
	
}

