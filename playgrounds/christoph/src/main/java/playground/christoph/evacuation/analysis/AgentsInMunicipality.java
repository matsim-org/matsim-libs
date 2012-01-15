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

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import com.vividsolutions.jts.geom.Geometry;

public class AgentsInMunicipality {

	private final EventsManager eventsManager;
	private final List<AgentsInMunicipalityEventsHandler> handlers;
	
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
		
		AgentsInMunicipality aim = new AgentsInMunicipality(scenario, householdObjectAttributes, shpFile, outputPath, numThreads);
		aim.beforeEventsReading();
		aim.readEventsFile(eventsFile);
		aim.afterEventsReading();
	}
	
	public AgentsInMunicipality(Scenario scenario, ObjectAttributes householdObjectAttributes, String shpFile, String outputPath, int numThreads) throws Exception {	
		if (numThreads < 2) eventsManager = EventsUtils.createEventsManager();
		else eventsManager = new ParallelEventsManagerImpl(numThreads);
		handlers = new ArrayList<AgentsInMunicipalityEventsHandler>();
		
		if (eventsManager instanceof EventsManagerImpl) {
			((EventsManagerImpl) eventsManager).initProcessing();
		}
		
		List<Feature> municipalities = new ArrayList<Feature>();
		FeatureSource featureSource = ShapeFileReader.readDataFile(shpFile);
		for (Object o : featureSource.getFeatures()) {
			Feature feature = (Feature) o;
			municipalities.add(feature);
			
			Integer id = (Integer) feature.getAttribute(1);
			String name = (String) feature.getAttribute(4);
			name = name.replace('/', '_');
			name = name.replace('\\', '_');
			String fileName = name + "_" + id.toString();
			String outputFile = outputPath + "/" + fileName;
			
			Geometry area = feature.getDefaultGeometry();
			
			AgentsInMunicipalityEventsHandler aim = new AgentsInMunicipalityEventsHandler(scenario, householdObjectAttributes, outputFile, area);
			aim.printInitialStatistics();
			
			eventsManager.addHandler(aim);
			handlers.add(aim);
		}		
	}
	
	public void beforeEventsReading() {
		for (AgentsInMunicipalityEventsHandler aim : handlers) aim.beforeEventsReading();
	}
	
	public void readEventsFile(String eventsFile) {
		if (!eventsFile.toLowerCase().endsWith(".xml.gz") && !eventsFile.toLowerCase().endsWith(".xml")) {
			return;
		} else {
			new MatsimEventsReader(eventsManager).readFile(eventsFile);
			if (eventsManager instanceof EventsManagerImpl) {
				((EventsManagerImpl) eventsManager).finishProcessing();
			}
		}
	}
	
	public void afterEventsReading() {
		for (AgentsInMunicipalityEventsHandler aim : handlers) aim.afterEventsReading();
	}

}
