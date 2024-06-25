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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.router.util.TravelDisutility;
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
public final class NoiseOfflineCalculation{
	private static final Logger log = LogManager.getLogger( NoiseOfflineCalculation.class );

	private String outputDirectory;
	private Scenario scenario;

	private NoiseContext noiseContext = null;
	private NoiseTimeTracker timeTracker = null;

	public NoiseOfflineCalculation( Scenario scenario, String analysisOutputDirectory ) {
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

		EventsManager events = injector.getInstance( EventsManager.class ) ;

		timeTracker = injector.getInstance( NoiseTimeTracker.class ) ;
		timeTracker.setOutputFilePath(outputFilePath);

		//LinkSpeedCalculation is already injected with the NoiseModule! nk jul '20
//		if (noiseContext.getNoiseParams().isUseActualSpeedLevel()) {
//			LinkSpeedCalculation linkSpeedCalculator = new LinkSpeedCalculation();
//			linkSpeedCalculator.setNoiseContext(noiseContext);
//			events.addHandler(linkSpeedCalculator);
//		}

		final NoiseConfigGroup noiseParams = noiseContext.getNoiseParams();
		noiseParams.checkConsistency(noiseContext.getScenario().getConfig());

		EventWriterXML eventWriter = null;
		if (noiseContext.getNoiseParams().isThrowNoiseEventsAffected() || noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
			String eventsFile;
			if (this.scenario.getConfig().controller().getRunId() == null || this.scenario.getConfig().controller().getRunId().equals("")) {
				eventsFile = outputFilePath + "noise.output_events.offline.xml.gz";
			} else {
				eventsFile = outputFilePath + this.scenario.getConfig().controller().getRunId() + ".noise.output_events.offline.xml.gz";
			}
			eventWriter = new EventWriterXML(eventsFile);
			events.addHandler(eventWriter);
		}


		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		String eventsFile;
		if (this.scenario.getConfig().controller().getRunId() == null || this.scenario.getConfig().controller().getRunId().equals("")) {
			eventsFile = this.scenario.getConfig().controller().getOutputDirectory() + "output_events.xml.gz";
		} else {
			eventsFile = this.scenario.getConfig().controller().getOutputDirectory() + "/" + this.scenario.getConfig().controller().getRunId() + ".output_events.xml.gz";
		}
		reader.readFile(eventsFile);
		log.info("Reading events file... Done.");

		timeTracker.computeFinalTimeIntervals();

		if (noiseContext.getNoiseParams().isThrowNoiseEventsAffected() || noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
			eventWriter.closeFile();
		}
		log.info("Noise calculation completed.");

	}

	NoiseTimeTracker getTimeTracker() {
		return timeTracker;
	}

	NoiseContext getNoiseContext() {
		return noiseContext;
	}

	public final TravelDisutility getTollDisutility() {
		return new NoiseTollCalculator( noiseContext );
	}

}

