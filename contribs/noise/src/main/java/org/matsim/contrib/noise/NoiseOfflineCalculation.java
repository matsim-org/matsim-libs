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
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile.OutputFormat;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
* @author ikaddoura
*/

public class NoiseOfflineCalculation {
	private static final Logger log = Logger.getLogger(NoiseOfflineCalculation.class);

	private String outputDirectory;
	private Scenario scenario;
	private NoiseConfigGroup noiseParameters;

	public NoiseOfflineCalculation(Scenario scenario, String analysisOutputDirectory) {
		this.scenario = scenario;
		this.outputDirectory = analysisOutputDirectory;
		
		if ((NoiseConfigGroup) this.scenario.getConfig().getModule("noise") == null) {
			throw new RuntimeException("Could not find a noise config group. "
					+ "Check if the custom module is loaded, e.g. 'ConfigUtils.loadConfig(configFile, new NoiseConfigGroup())'"
					+ " Aborting...");
		}
		this.noiseParameters = (NoiseConfigGroup) this.scenario.getConfig().getModule("noise");
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
					
		NoiseContext noiseContext = new NoiseContext(scenario);
		NoiseWriter.writeReceiverPoints(noiseContext, outputFilePath + "/receiverPoints/", false);
				
		EventsManager events = EventsUtils.createEventsManager();

		NoiseTimeTracker timeTracker = new NoiseTimeTracker(noiseContext, events, outputFilePath);
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
		
		log.info("Processing the noise immissions...");
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", this.noiseParameters.getReceiverPointGap());
		process.run();
		
		log.info("Merging other information to one file...");
		
		final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
		final String[] workingDirectories = { outputFilePath + "/immissions/" , outputFilePath + "/consideredAgentUnits/" , outputFilePath + "/damages_receiverPoint/" };

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
		merger.setTimeBinSize(this.noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.setOutputFormat(OutputFormat.xyt);
		merger.setThreshold(-1.);
		merger.run();
	}
}

