/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.ikaddoura.aas;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.vsp.aas.modules.AbstractAnalyisModule;
import playground.vsp.aas.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * 
 * @author aneumann, ikaddoura
 *
 */
public class AasRunner_ik {
	
	private final static Logger log = Logger.getLogger(AasRunner_ik.class);
	
	private final String iterationOutputDir;
	private final String eventsFile;
	private final ScenarioImpl scenario;
	
	private final List<AbstractAnalyisModule> anaModules = new LinkedList<AbstractAnalyisModule>();

	public AasRunner_ik(ScenarioImpl scenario, String iterationOutputDir, String eventsFile) {
		this.iterationOutputDir = iterationOutputDir;
		this.eventsFile = eventsFile;
		this.scenario = scenario;
	}

	public void init(String aasRunnerConfigFile){
		log.info("This is currently not implemented. Initializing all modules with defaults...");
		String ptDriverPrefix = "pt_";
		
		// END of configuration file
		
		LegModeDistanceDistribution distAna = new LegModeDistanceDistribution(ptDriverPrefix);
		distAna.init(this.scenario);
		this.anaModules.add(distAna);
		
		// END ugly code - Initialization needs to be configurable
		log.info("Initializing all modules with defaults... done.");
	}

	public void preProcess(){
		log.info("Preprocessing all modules...");
		for (AbstractAnalyisModule module : this.anaModules) {
			module.preProcessData();
		}
		log.info("Preprocessing all modules... done.");
	}
	
	public void run(){
		EventsManager eventsManager = EventsUtils.createEventsManager();
		for (AbstractAnalyisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				eventsManager.addHandler(handler);
			}
		}
		try {
			log.info("Trying to parse eventsFile " + this.eventsFile);
			EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
			reader.parse(this.eventsFile);
		} catch (UncheckedIOException e) {
			log.warn("Failed parsing " + this.eventsFile + ". Skipping events handling...");
		}
	}
	
	public void postProcess(){
		log.info("Postprocessing all modules...");
		for (AbstractAnalyisModule module : this.anaModules) {
			module.postProcessData();
		}
		log.info("Postprocessing all modules... done.");
	}
	
	public void writeResults(){
		log.info("Writing results for all modules...");
		String outputDir = this.iterationOutputDir + "analysis" + "/";
		log.info("Generating output directory " + outputDir);
		new File(outputDir).mkdir();
		
		for (AbstractAnalyisModule module : this.anaModules) {
			String moduleOutputDir = outputDir + module.getName() + "/";
			log.info("Writing results of module " + module.getName() + " to " + moduleOutputDir + "...");
			new File(moduleOutputDir).mkdir();
			module.writeResults(moduleOutputDir);
			log.info("Writing results of module " + module.getName() + " to " + moduleOutputDir + "... done.");
		}
		log.info("Writing results for all modules... done.");
		
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}
}
