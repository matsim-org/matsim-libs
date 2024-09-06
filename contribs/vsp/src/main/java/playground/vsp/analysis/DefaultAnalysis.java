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

package playground.vsp.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * 
 * @author aneumann
 *
 */
public class DefaultAnalysis {
	
	private final static Logger log = LogManager.getLogger(DefaultAnalysis.class);
	
	private final String baseFolder;
	private final String iterationOutputDir;
	private final String eventsFile;
	private final Scenario scenario;
	private final Set<SimpleFeature> shapeFile;
	
	private final List<AbstractAnalysisModule> anaModules = new LinkedList<AbstractAnalysisModule>();

	public DefaultAnalysis(Scenario scenario, String baseFolder, String iterationOutputDir, String eventsFile, Set<SimpleFeature> shapeFile) {
		this.baseFolder = baseFolder;
		this.iterationOutputDir = iterationOutputDir;
		this.eventsFile = eventsFile;
		this.scenario = scenario;
		this.shapeFile = shapeFile;
	}

	public void init(String aasRunnerConfigFile){
		log.info("Configuration through config file is currently not implemented. Initializing all modules with defaults.");
		String ptDriverPrefix = "pt_";
		
		// END of configuration file
		
		LegModeDistanceDistribution distAna = new LegModeDistanceDistribution();
		distAna.init(this.scenario);
		this.anaModules.add(distAna);
		
		// END ugly code - Initialization needs to be configurable
	}

	public void preProcess(){
		for (AbstractAnalysisModule module : this.anaModules) {
			module.preProcessData();
		}
	}
	
	public void run(){
		EventsManager eventsManager = EventsUtils.createEventsManager();
		for (AbstractAnalysisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				eventsManager.addHandler(handler);
			}
		}
		
		if(this.eventsFile == null){
			log.warn("You did not provide any events file for analyis.");
			log.warn("Make sure if this is what you want. Analysis modules " +
					"that are based on events will not produce any results!");
		} else {
			EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
			reader.readFile(this.eventsFile);
		}
	}
	
	public void postProcess(){
		for (AbstractAnalysisModule module : this.anaModules) {
			module.postProcessData();
		}
	}
	
	public void writeResults(){
		String outputDir = this.iterationOutputDir + "defaultAnalysis" + "/";
		log.info("Generating output directory " + outputDir);
		new File(outputDir).mkdir();
		
		for (AbstractAnalysisModule module : this.anaModules) {
			String moduleOutputDir = outputDir + module.getName() + "/";
			log.info("Writing results of module " + module.getName() + " to " + moduleOutputDir + "...");
			new File(moduleOutputDir).mkdir();
			module.writeResults(moduleOutputDir);
			log.info("... finished");
		}
		
		log.info("All output written");
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}

	public List<AbstractAnalysisModule> getAnaModules() {
		return this.anaModules;
	}
}
