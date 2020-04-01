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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author aneumann, droeder
 *
 */
public class VspAnalyzer {

	private static final Logger log = Logger.getLogger(VspAnalyzer.class);
	private String outdir;
	private LinkedList<AbstractAnalysisModule> modules;
	private String eventsFile;

	/**
	 * Simple container-class to handle {@link AbstractAnalysisModule}s.
	 * The modules are processed in the order as they are added.
	 * The initialization of the single modules has to be done before.
	 * Make sure you add the correct data to the module, especially if you are
	 * handling events.Only one events-file will be handled!
	 * 
	 * @param outdir, the output-directory 
	 * @param eventsFile, the events
	 */
	public VspAnalyzer(String outdir, String eventsFile) {
		this.outdir = outdir;
		this.modules = new LinkedList<AbstractAnalysisModule>();
		this.eventsFile = eventsFile;
	}
	
	/**
	 * Simple container-class to handle {@link AbstractAnalysisModule}s.
	 * The modules are processed in the order as they are added.
	 * The initialization of the single modules has to be done before.
	 * Make sure you add the correct data to the module!
	 * 
	 * If you use this constructor, no events will be handled!!!
	 * 
	 * @param outdir, the output-directory 
	 */
	public VspAnalyzer(String outdir) {
		this.outdir = outdir;
		this.modules = new LinkedList<AbstractAnalysisModule>();
		this.eventsFile = null;
	}
	
	public void addAnalysisModule(AbstractAnalysisModule module){
		this.modules.add(module);
	}
	
	public void run(){
		log.info("running " + VspAnalyzer.class.getSimpleName());
		Gbl.startMeasurement();
		this.preProcess();
		if(!(this.eventsFile == null)){
			if(new File(this.eventsFile).exists()){
				this.handleEvents();
				Gbl.printElapsedTime(); Gbl.printMemoryUsage();
			}else{
				log.warn("can not handle events, because the specified file does not exist!");
			}
		}
		
		this.combinedPostProcessAndWriteResults();
		
//		this.postProcess();
//		this.writeResults();
		log.info("finished " + VspAnalyzer.class.getSimpleName());
	}

	/**
	 * 
	 */
	private void preProcess() {
		log.info("preprocessing all modules...");
		for(AbstractAnalysisModule module: this.modules){
			try {
				log.info("preprocessing " + module.getName());
				module.preProcessData();
			} catch (Exception e) {
				log.error("Preprocessing of module " + module.getName() + " failed.");
			}
			Gbl.printElapsedTime(); Gbl.printMemoryUsage();
		}
		log.info("preprocessing finished...");
	}

	/**
	 * 
	 */
	private void handleEvents() {
		log.info("handling events for all modules...");
		EventsManager manager = EventsUtils.createEventsManager();
		for(AbstractAnalysisModule module: this.modules){
			log.info("adding eventHandler from " + module.getName());
			for(EventHandler handler: module.getEventHandler()){
				manager.addHandler(handler);
			}
		}
		new MatsimEventsReader(manager).readFile(this.eventsFile);
		log.info("event-handling finished...");
	}

	/**
	 * 
	 */
	private void postProcess() {
		log.info("post-processing all modules...");
		for(AbstractAnalysisModule module: this.modules){
			log.info("postprocessing " + module.getName());
			module.postProcessData();
			Gbl.printElapsedTime(); Gbl.printMemoryUsage();
		}
		log.info("post-processing finished...");
		
	}

	/**
	 * 
	 */
	private void writeResults() {
		log.info("writing data for all modules...");
		for(AbstractAnalysisModule module: this.modules){
			String outputDir = this.outdir + "/" + module.getName() + "/";
			log.info("writing output for " + module.getName() + " to " + outputDir);
			if(!new File(outputDir).exists()){
				new File(outputDir).mkdirs();
			}
			module.writeResults(outputDir);
			Gbl.printElapsedTime(); Gbl.printMemoryUsage();
		}
		log.info("writing finished...");
	}

	private void combinedPostProcessAndWriteResults() {
		log.info("combinedPostProcessAndWriteResults for all modules...");
		
		while (!this.modules.isEmpty()) {
			AbstractAnalysisModule module = this.modules.removeFirst();
			
			try {
				log.info("postprocessing " + module.getName());
				module.postProcessData();
				
			} catch (Exception e) {
				log.error("Postprocessing of module " + module.getName() + " failed.");
				e.printStackTrace();
			}
			Gbl.printElapsedTime(); Gbl.printMemoryUsage();
//			log.info("post-processing finished...");
			
			try {	
				String outputDir = this.outdir + "/" + module.getName() + "/";
				log.info("writing output for " + module.getName() + " to " + outputDir);
				if(!new File(outputDir).exists()){
					new File(outputDir).mkdirs();
				}
				module.writeResults(outputDir);
			} catch (Exception e) {
				log.error("Writing the output of module " + module.getName() + " failed.");
				e.printStackTrace();
			}
			Gbl.printElapsedTime(); Gbl.printMemoryUsage();
		}
		log.info("combinedPostProcessAndWriteResults for all modules finished...");
	}
}

