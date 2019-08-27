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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.LinkSpeedCalculation;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.contrib.noise.handler.PersonActivityTracker;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioByInstanceModule;

import java.io.File;
import java.io.IOException;

/**
 * (1) Computes noise emissions, immissions, person activities and damages based on a standard events file.
 * (2) Optionally throws noise immission damage events for the causing agent and the affected agent.
 *
 * @author ikaddoura
 *
 */
public class NoiseOfflineCalculationWithInjection{
	private static final Logger log = Logger.getLogger( NoiseOfflineCalculationWithInjection.class );

	private String outputDirectory;
	private Scenario scenario;

	private NoiseContext noiseContext = null;
	private NoiseTimeTracker timeTracker = null;

	public NoiseOfflineCalculationWithInjection( Scenario scenario, String analysisOutputDirectory ) {
		this.scenario = scenario;
		this.outputDirectory = analysisOutputDirectory;

		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}

		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(scenario.getConfig(), NoiseConfigGroup.class) ;
		if (noiseParameters.isInternalizeNoiseDamages()) {
			log.warn("If you intend to internalize noise damages, please run the online noise computation."
					+ " This is an offline noise calculation which can only be used for analysis purposes.");
			noiseParameters.setInternalizeNoiseDamages(false);
		}
	}

	public void run() {

		String outputFilePath = outputDirectory + "noise-analysis/";
		File file = new File(outputFilePath);
		file.mkdirs();

		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputFilePath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		com.google.inject.Injector injector = Injector.createInjector( scenario.getConfig() , new AbstractModule(){
				  @Override public void install(){
					  install( new ScenarioByInstanceModule( scenario ) );
					  install( new NoiseModule() ) ;
					  install( new EventsManagerModule() ) ;
				  }
			  }) ;

		noiseContext = injector.getInstance( NoiseContext.class ) ;
		NoiseWriter.writeReceiverPoints(noiseContext, outputFilePath + "/receiverPoints/", false);

//		EventsManager events = EventsUtils.createEventsManager();
		EventsManager events = injector.getInstance( EventsManager.class ) ;

		timeTracker = new NoiseTimeTracker();
		timeTracker.setNoiseContext(noiseContext);
		timeTracker.setEvents(events);
		timeTracker.setOutputFilePath(outputFilePath);

		events.addHandler(timeTracker);

		if (noiseContext.getNoiseParams().isUseActualSpeedLevel()) {
			LinkSpeedCalculation linkSpeedCalculator = new LinkSpeedCalculation();
			linkSpeedCalculator.setNoiseContext(noiseContext);
			events.addHandler(linkSpeedCalculator);
		}

		EventWriterXML eventWriter = null;
		if (noiseContext.getNoiseParams().isThrowNoiseEventsAffected() || noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
			String eventsFile;
			if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
				eventsFile = outputFilePath + "noise.output_events.offline.xml.gz";
			} else {
				eventsFile = outputFilePath + this.scenario.getConfig().controler().getRunId() + ".noise.output_events.offline.xml.gz";
			}
			eventWriter = new EventWriterXML(eventsFile);
			events.addHandler(eventWriter);
		}

		if (noiseContext.getNoiseParams().isComputePopulationUnits()) {
			PersonActivityTracker actTracker = new PersonActivityTracker(noiseContext);
			events.addHandler(actTracker);
		}

		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		String eventsFile;
		if (this.scenario.getConfig().controler().getRunId() == null || this.scenario.getConfig().controler().getRunId().equals("")) {
			eventsFile = this.scenario.getConfig().controler().getOutputDirectory() + "output_events.xml.gz";
		} else {
			eventsFile = this.scenario.getConfig().controler().getOutputDirectory() + this.scenario.getConfig().controler().getRunId() + ".output_events.xml.gz";
		}
		reader.readFile(eventsFile);
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

