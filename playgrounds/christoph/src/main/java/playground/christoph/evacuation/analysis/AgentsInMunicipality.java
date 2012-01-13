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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class AgentsInMunicipality {

	private final EventsManager eventsManager;
	
	/**
	 * Input arguments:
	 * <ul>
	 *	<li>path to network file</li>
	 *  <li>path to facilities file</li>
	 *  <li>path to population file</li>
	 *  <li>path to households file</li>
	 *  <li>path to households object attributes file</li>
	 *  <li>path to events file</li>
	 *  <li>path to the output file (without file type extension)</li>
	 *  <li>path to SHP files containing swiss municipalities</li>
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
		String outputFile = args[6];
		String shpFile = args[7];
		
		ObjectAttributes householdObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdObjectAttributes).parse(householdsObjectAttributesFile);
		
		AgentsInMunicipality aim = new AgentsInMunicipality(scenario, householdObjectAttributes, shpFile);
		aim.readEventsFile(eventsFile);
	}
	
	public AgentsInMunicipality(Scenario scenario, ObjectAttributes householdObjectAttributes, String shpFile) throws Exception {	
		eventsManager = EventsUtils.createEventsManager();
		
		List<Feature> municipalities = new ArrayList<Feature>();
		FeatureSource featureSource = ShapeFileReader.readDataFile(shpFile);
		for (Object o : featureSource.getFeatures()) {
			municipalities.add((Feature) o);

			// TODO: create an AgentsInMunicipalityEventsHandler for each Municipality here
//		AgentsInMunicipalityEventsHandler aim = new AgentsInMunicipalityEventsHandler(scenario, householdObjectAttributes, outputFile, area);
//		aim.printInitialStatistics();
		}
		
//		Set<Feature> features = new HashSet<Feature>();
//		SHPFileUtil util = new SHPFileUtil();
//		for (String file : municipalitySHPFiles) {
//			features.addAll(util.readFile(file));		
//		}
//		Geometry area = util.mergeGeomgetries(features);
		
	}
	
	public void readEventsFile(String eventsFile) {
		if (!eventsFile.toLowerCase().endsWith(".xml.gz") && !eventsFile.toLowerCase().endsWith(".xml")) {
			return;
		}else {
			new MatsimEventsReader(eventsManager).readFile(eventsFile);
		}
	}
}
