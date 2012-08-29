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

package playground.andreas.aas;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.andreas.aas.modules.AbstractAnalyisModule;
import playground.andreas.aas.modules.ptTripAnalysis.BvgTripAnalysisRunnerV4;

/**
 * 
 * @author aneumann
 *
 */
public class AasRunner {
	
	private final static Logger log = Logger.getLogger(AasRunner.class);
	
	private final Config config;
	private final String baseFolder;
	private final String iterationOutputDir;
	private final String eventsFile;
	private final EventsManager eventsManager;
	private ScenarioImpl scenario;
	private Set<Feature> shapeFile;
	
	private List<AbstractAnalyisModule> anaModules = new LinkedList<AbstractAnalyisModule>();

	

	public AasRunner(Config config, String baseFolder, String iterationOutputDir, String eventsFile, ScenarioImpl scenario, Set<Feature> shapeFile) {
		this.config = config;
		this.baseFolder = baseFolder;
		this.iterationOutputDir = iterationOutputDir;
		this.eventsFile = eventsFile;
		this.eventsManager = EventsUtils.createEventsManager();
		this.scenario = scenario;
		this.shapeFile = shapeFile;
	}

	public void init(String aasRunnerConfigFile){
		log.info("This is currently not implemented. Initializing all modules with defaults.");
		String ptDriverPrefix = "pt_";
		
		
		// END of configuration file
		
		
		BvgTripAnalysisRunnerV4 ptAna = new BvgTripAnalysisRunnerV4(ptDriverPrefix);
		ptAna.init(this.config, this.scenario, this.shapeFile);
		anaModules.add(ptAna);
		this.eventsManager.addHandler(ptAna);
	}

	public void preProcess(){
		for (AbstractAnalyisModule module : this.anaModules) {
			module.preProcessData();
		}
	}
	
	public void run(){
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(this.eventsManager);
		reader.parse(this.eventsFile);
	}
	
	public void postProcess(){
		for (AbstractAnalyisModule module : this.anaModules) {
			module.postProcessData();
		}
	}
	
	public void writeResults(){
		String outputDir = this.iterationOutputDir + "/" + "aasRunner" + "/";
		log.info("Generating output directory " + outputDir);
		new File(outputDir).mkdir();
		
		for (AbstractAnalyisModule module : this.anaModules) {
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
}
