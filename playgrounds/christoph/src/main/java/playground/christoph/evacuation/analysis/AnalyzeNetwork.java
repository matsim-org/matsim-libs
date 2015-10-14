/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.controler.PrepareEvacuationScenario;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import com.vividsolutions.jts.geom.Geometry;

public class AnalyzeNetwork implements PersonDepartureEventHandler, PersonArrivalEventHandler, 
	LinkLeaveEventHandler, PersonStuckEventHandler {

	private static final Logger log = Logger.getLogger(AnalyzeNetwork.class);
	
	private final String outputFile = "evacuationNetworkAnalysis.txt";
	
	private final Config config;
	private final Scenario scenario;
	private final EventsManager eventsManager;
	
	private final CoordAnalyzer coordAnalyzer;	
	private final Set<Id> affectedLinkIds = new HashSet<Id>();
	private final Set<Id> enRouteCarAgents = new HashSet<Id>();
	private double affectedCarTripLength = 0.0;
	
	public static void main(String[] args) {
		String configFile;
		String evacuationConfigFile;
		String outputPath;

//		configFile = "../../matsim/mysimulations/census2000V2/output_10pct_evac/evac.1.output_config.xml";
//		evacuationConfigFile = "../../matsim/mysimulations/census2000V2/config_evacuation.xml";
//		outputPath = "../../matsim/mysimulations/census2000V2/output_10pct_evac/";

		if (args.length != 3) return;
		else {
			configFile = args[0];
			evacuationConfigFile = args[1];
			outputPath = args[2];
		}
		
		new AnalyzeNetwork(configFile, evacuationConfigFile, outputPath);
	}
	
	public AnalyzeNetwork(String configFile, String evacuationConfigFile, String outputPath) {
		config = ConfigUtils.loadConfig(configFile);
				
		scenario = ScenarioUtils.loadScenario(config);
		eventsManager = EventsUtils.createEventsManager();
				
		new EvacuationConfigReader().readFile(evacuationConfigFile);
		EvacuationConfig.printConfig();
		
		/*
		 * Prepare the scenario:
		 * 	- connect facilities to network
		 * 	- add exit links to network
		 * 	- add pickup facilities
		 *  - add z Coordinates to network
		 */
		new PrepareEvacuationScenario().prepareScenario(scenario);
		
		/*
		 * Create two OutputDirectoryHierarchies that point to the analyzed run's output directory.
		 * Since we do not want to overwrite existing results we add an additional prefix
		 * to the re-created outputs.
		 */
		OutputDirectoryHierarchy dummyInputDirectoryHierarchy;
		if (outputPath == null) outputPath = scenario.getConfig().controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		if (scenario.getConfig().controler().getRunId() != null) {
			dummyInputDirectoryHierarchy = new OutputDirectoryHierarchy(
					outputPath,
					scenario.getConfig().controler().getRunId(),
							true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		} else {
			dummyInputDirectoryHierarchy = new OutputDirectoryHierarchy(
					outputPath,
					null,
							true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		}
		
		// add another string to the runId to not overwrite old files
		String runId = scenario.getConfig().controler().getRunId();
		scenario.getConfig().controler().setRunId(runId + ".postprocessed");
		OutputDirectoryHierarchy dummyOutputDirectoryHierarchy;
		if (outputPath == null) outputPath = scenario.getConfig().controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		if (scenario.getConfig().controler().getRunId() != null) {
			dummyOutputDirectoryHierarchy = new OutputDirectoryHierarchy(
					outputPath,
					scenario.getConfig().controler().getRunId(),
							true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		} else {
			dummyOutputDirectoryHierarchy = new OutputDirectoryHierarchy(
					outputPath,
					null,
							true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		}
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(ShapeFileReader.getAllFeatures(file));		
		}
		Geometry affectedArea = util.mergeGeometries(features);
		
		coordAnalyzer = new CoordAnalyzer(affectedArea);

		double affectedLength = 0.0;
		double affectedLaneLength = 0.0;
		double affectedCapacity = 0.0;
		// analyze affected links
		for (Link link : scenario.getNetwork().getLinks().values()) {
			boolean isAffected = coordAnalyzer.isLinkAffected(link);
			
			if (isAffected) {
				affectedLength += link.getLength();
				affectedLaneLength += link.getLength() * link.getNumberOfLanes();
				affectedCapacity += link.getCapacity();
				affectedLinkIds.add(link.getId());
			}
		}
		
		eventsManager.addHandler(this);
		String eventsFile = dummyInputDirectoryHierarchy.getIterationFilename(0, Controler.FILENAME_EVENTS_XML);
		new EventsReaderXMLv1(eventsManager).parse(eventsFile);
		
		log.info("affected links: \t" + affectedLinkIds.size());
		log.info("affected length: \t" + String.format("%.1f", affectedLength));
		log.info("affected lane length: \t" + String.format("%.1f", affectedLaneLength));
		log.info("affected capacity: \t" + String.format("%.1f", affectedCapacity));
		log.info("affected car trip length: \t" + String.format("%.1f", affectedCarTripLength));
			
		/*
		 * Write results to files.
		 */
		try {
			String fileName = dummyOutputDirectoryHierarchy.getIterationFilename(0, outputFile);
			BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(fileName);

			bufferedWriter.write("affected links: \t");
			bufferedWriter.write(String.valueOf(affectedLinkIds.size()));
			bufferedWriter.newLine();
			
			bufferedWriter.write("affected length: \t");
			bufferedWriter.write(String.format("%.1f", affectedLength));
			bufferedWriter.newLine();
			
			bufferedWriter.write("affected lane length: \t");
			bufferedWriter.write(String.format("%.1f", affectedLaneLength));
			bufferedWriter.newLine();
			
			bufferedWriter.write("affected capacity: \t");
			bufferedWriter.write(String.format("%.1f", affectedCapacity));
			bufferedWriter.newLine();

			bufferedWriter.write("affected car trip length: \t");
			bufferedWriter.write(String.format("%.1f", affectedCarTripLength));
			bufferedWriter.newLine();
			
			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset(int iteration) {
		enRouteCarAgents.clear();
		affectedCarTripLength = 0.0;
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.enRouteCarAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (event.getTime() < EvacuationConfig.evacuationTime) return;
		
		if (affectedLinkIds.contains(event.getLinkId())) {
			if (this.enRouteCarAgents.contains(event.getDriverId())) {
				affectedCarTripLength += this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			}			
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.enRouteCarAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			this.enRouteCarAgents.add(event.getPersonId());
		}
	}
}
