/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsInMunicipality.java
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

package playground.christoph.evacuation.analysis;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.ParallelEventsManagerImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author cdobler
 */
public class AgentsInMunicipality {

	final private static Logger log = Logger.getLogger(AgentsInMunicipality.class);
	
	private final int maxParallelMunicipalities = 750;
	private final HandlersCreator[] runnables;
	private final String eventsFile;
	private final int numThreads;
	
	/**
	 * Input arguments:
	 * <ul>
	 *	<li>path to network file</li>
	 *  <li>path to facilities file</li>
	 *  <li>path to population file</li>
	 *  <li>path to households file</li>
	 *  <li>path to households object attributes file</li>
	 *  <li>path to events file</li>
	 *  <li>path to SHP files containing swiss municipalities</li>
	 *  <li>path to the output directory</li>
	 * </ul>
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 8) return;
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(args[0]);
		config.facilities().setInputFile(args[1]);
		config.plans().setInputFile(args[2]);
		config.households().setInputFile(args[3]);
		config.scenario().setUseHouseholds(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String householdsObjectAttributesFile = args[4];
		String eventsFile = args[5];
		String shpFile = args[6];
		String outputPath = args[7];
		int numThreads = Integer.valueOf(args[8]); 
		
		ObjectAttributes householdObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdObjectAttributes).parse(householdsObjectAttributesFile);
		
		new AgentsInMunicipality(scenario, householdObjectAttributes, shpFile, eventsFile, outputPath, numThreads);

	}
	
	public AgentsInMunicipality(Scenario scenario, ObjectAttributes householdObjectAttributes, String shpFile, String eventsFile, String outputPath, int numThreads) throws Exception {	
		this.eventsFile = eventsFile;
		this.numThreads = numThreads;

		List<SimpleFeature> municipalities = new ArrayList<SimpleFeature>();
		municipalities.addAll(ShapeFileReader.getAllFeatures(shpFile));
		log.info("Found " + municipalities.size() + " municipalities.");

		runnables = new HandlersCreator[numThreads];
		for (int i = 0; i < numThreads; i++) {
			runnables[i] = new HandlersCreator(scenario, householdObjectAttributes, outputPath);
		}
		
		if (municipalities.size() <= maxParallelMunicipalities) {
			handleMunicipalities(municipalities);
		} else {
			int startIndex = 0;
			while (startIndex < municipalities.size()) {
				int endIndex = Math.min(startIndex + maxParallelMunicipalities, municipalities.size());
				List<SimpleFeature> subList = municipalities.subList(startIndex, endIndex);
				handleMunicipalities(subList);
				startIndex += this.maxParallelMunicipalities;
			}
		}
	}
	
	private void handleMunicipalities(List<SimpleFeature> municipalities) throws Exception {
		log.info("Handling " + municipalities.size() + " municipalities.");
				
		for (HandlersCreator handlersCreator : runnables) handlersCreator.reset();
		
		// assign municipalities
		log.info("\t\tAssigning municipalities to threads...");
		int roundRobin = 0;
		for (SimpleFeature municipality : municipalities) {
			runnables[roundRobin % numThreads].addMunicipality(municipality);
			roundRobin++;
		}
		log.info("\t\tdone.");
		
		Thread[] threads;
		
		// create threads to initialize AgentsInMunicipalityEventsHandler
		log.info("\t\tInitializing AgentsInMunicipalityEventsHandlers...");
		threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			threads[i] = new Thread(runnables[i]);
			threads[i].setName("HandlersCreator");
		}
		runThreads(threads);
		log.info("\t\tdone.");
		
		// collecting handlers
		List<AgentsInMunicipalityEventsHandler> handlers = new ArrayList<AgentsInMunicipalityEventsHandler>();
		for (HandlersCreator creator : runnables) {
			for (AgentsInMunicipalityEventsHandler aim : creator.getHandlers()) handlers.add(aim);
		}
		
		EventsManager eventsManager = null;
		if (numThreads < 2) eventsManager = EventsUtils.createEventsManager();
		else eventsManager = new ParallelEventsManagerImpl(numThreads);
		eventsManager.initProcessing();
	
		// adding Handlers to EventsManager
		log.info("\t\tAdding AgentsInMunicipalityEventsHandlers to EventsManager...");
		for (EventHandler handler : handlers) eventsManager.addHandler(handler);
		log.info("\t\tdone.");
		
		// before events reading
		log.info("\t\tBefore events reading...");
		threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			threads[i] = new Thread(new BeforeEventsReading(runnables[i].getHandlers()));
			threads[i].setName("BeforeEventsReading");
		}
		runThreads(threads);
		log.info("\t\tdone.");
		
		// read events file
		log.info("\t\tReading events...");
		readEventsFile(eventsManager);
		log.info("\t\tdone.");
		
		// after events reading
		log.info("\t\tAfter events reading...");
		threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			threads[i] = new Thread(new AfterEventsReading(runnables[i].getHandlers()));
			threads[i].setName("AfterEventsReading");
		}
		runThreads(threads);
		log.info("\t\tdone.");

		log.info("done.");
	}
	
	private void runThreads(Thread[] threads) throws Exception {
		// running threads
		for (Thread thread : threads) thread.start();
		
		// wait until all threads are finished
		for (Thread thread : threads) thread.join();
	}
	
	private void readEventsFile(EventsManager eventsManager) {
		if (!eventsFile.toLowerCase().endsWith(".xml.gz") && !eventsFile.toLowerCase().endsWith(".xml")) {
			return;
		} else {
			new MatsimEventsReader(eventsManager).readFile(eventsFile);
			if (eventsManager instanceof EventsManagerImpl) {
				((EventsManagerImpl) eventsManager).finishProcessing();
			}
		}
	}
	
	private class BeforeEventsReading implements Runnable {
		
		private final List<AgentsInMunicipalityEventsHandler> handlers;
		
		public BeforeEventsReading(List<AgentsInMunicipalityEventsHandler> handlers) {
			this.handlers = handlers;
		}
		
		public void run() {
			for (AgentsInMunicipalityEventsHandler handler : handlers) handler.beforeEventsReading();
		}
	}
	
	private class HandlersCreator implements Runnable {

		private final Scenario scenario;
		private final ObjectAttributes householdObjectAttributes;
		private final String outputPath;
		private final List<SimpleFeature> municipalities;
		private final List<AgentsInMunicipalityEventsHandler> handlers;
		
		public HandlersCreator(Scenario scenario, ObjectAttributes householdObjectAttributes, String outputPath) {
			this.scenario = scenario;
			this.householdObjectAttributes = householdObjectAttributes;
			this.outputPath = outputPath;
			
			this.municipalities = new ArrayList<SimpleFeature>();
			this.handlers = new ArrayList<AgentsInMunicipalityEventsHandler>();
		}
		
		public void reset() {
			this.municipalities.clear();
			this.handlers.clear();
		}
		
		public List<AgentsInMunicipalityEventsHandler> getHandlers() {
			return this.handlers;
		}
		
		public void addMunicipality(SimpleFeature municipality) {
			this.municipalities.add(municipality);
		}
		
		@Override
		public void run() {
			for (SimpleFeature feature : municipalities) {
				Integer id = (Integer) feature.getAttribute(1);
				String name = (String) feature.getAttribute(4);
				name = name.replace('/', '_');
				name = name.replace('\\', '_');
				String fileName = name + "_" + id.toString();
				String outputFile = outputPath + "/" + fileName;
				
				Geometry area = (Geometry) feature.getDefaultGeometry();
				
				AgentsInMunicipalityEventsHandler aim = new AgentsInMunicipalityEventsHandler(scenario, householdObjectAttributes, outputFile, area);
				aim.printInitialStatistics();
		
				handlers.add(aim);
			}
		}	
	}
	
	private class AfterEventsReading implements Runnable {
		
		private final List<AgentsInMunicipalityEventsHandler> handlers;
		
		public AfterEventsReading(List<AgentsInMunicipalityEventsHandler> handlers) {
			this.handlers = handlers;
		}
		
		public void run() {
			for (AgentsInMunicipalityEventsHandler handler : handlers) handler.afterEventsReading();
		}
	}

}