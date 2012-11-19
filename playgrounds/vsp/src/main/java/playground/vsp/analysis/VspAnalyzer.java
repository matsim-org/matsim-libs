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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author droeder
 *
 */
public class VspAnalyzer {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(VspAnalyzer.class);
	private String outdir;
	private List<AbstractAnalyisModule> modules;
	private String eventsFile;

	/**
	 * Simple container-class to handle {@link AbstractAnalysisModule}s.
	 * The modules are processed in the order as they are added.
	 * The initialization of the single modules has to be done before.
	 * Make sure you to ad the correct data to the module, especially if you are
	 * handling events, the events-file has to be the right one!
	 * 
	 * @param outdir, the output-directory where you want your modules create their output-directories.
	 * @param eventsFile, the events
	 */
	public VspAnalyzer(String outdir, String eventsFile) {
		this.outdir = outdir;
		this.modules = new ArrayList<AbstractAnalyisModule>();
		this.eventsFile = eventsFile;
	}
	
	public void addAnalysisModule(AbstractAnalyisModule module){
		this.modules.add(module);
	}
	
	public void run(){
		this.preProcess();
		this.handleEvents();
		this.postProcess();
		this.writeResults();
	}

	/**
	 * 
	 */
	private void preProcess() {
		log.info("preprocessing all modules...");
		for(AbstractAnalyisModule module: this.modules){
			module.preProcessData();
		}
		log.info("finished...");
	}

	/**
	 * 
	 */
	private void handleEvents() {
		log.info("handling events for all modules...");
		EventsManager manager = EventsUtils.createEventsManager();
		for(AbstractAnalyisModule module: this.modules){
			for(EventHandler handler: module.getEventHandler()){
				manager.addHandler(handler);
			}
		}
		new MatsimEventsReader(manager).readFile(this.eventsFile);
		log.info("finished...");
	}

	/**
	 * 
	 */
	private void postProcess() {
		log.info("postprocessing all modules...");
		for(AbstractAnalyisModule module: this.modules){
			module.postProcessData();
		}
		log.info("finished...");
		
	}

	/**
	 * 
	 */
	private void writeResults() {
		log.info("writing data for all modules...");
		String outdir;
		for(AbstractAnalyisModule module: this.modules){
			outdir = this.outdir + "/" + module.getName() + "/";
			if(!new File(outdir).exists()){
				new File(outdir).mkdirs();
			}
			module.writeResults(outdir);
		}
		log.info("finished...");
	}
}

