/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStops2PlansConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dziemke.cemdapMatsimCadyts.cemdap2matsim;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;


/**
 * @author dziemke
 * based on "ucsb\demand\UCSBStops2PlansConverter.java"
 */
public class CemdapStops2MatsimPlansConverter {
	private static final Logger log = Logger.getLogger(CemdapStops2MatsimPlansConverter.class);
	
	// Parameters
	private static int numberOfFirstFileLocation = 59;
	private static int numberOfPlans = 3;
	private static boolean addStayHomePlan = true;
	
	// Input and output
	private static String outputDirectory = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap2matsim/31/";
	private static String tazShapeFile = "D:/Workspace/data/cemdapMatsimCadyts/input/shapefiles/gemeindenLOR_DHDN_GK4.shp";
	private static String networkFile = "D:/Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	
	public static void main(String[] args) throws IOException {
		// find respective stops file
		Map<Integer, String> cemdapStopsFilesMap = new HashMap<Integer, String>();
//		Map<Integer, String> cemdapToursFilesMap = new HashMap<Integer, String>();
//		Map<Integer, Map<String,String>> mapOfTourAttributesMaps = new HashMap<Integer, Map<String,String>>();
		for (int i=0; i<numberOfPlans; i++) {
			int numberOfCurrentInputFile = numberOfFirstFileLocation + i;
			String cemdapStopsFile = "D:/Workspace/runs-svn/cemdapMatsimCadyts/cemdapOutput/" + numberOfCurrentInputFile + "/stops.out1";
//			String cemdapToursFile = "D:/Workspace/runs-svn/cemdapMatsimCadyts/cemdapOutput/" + numberOfCurrentInputFile + "/tours.out1";
//			Map<String,String> tourAttributesMap = new HashMap<String,String>();
			cemdapStopsFilesMap.put(i, cemdapStopsFile);
//			cemdapToursFilesMap.put(i, cemdapToursFile);
//			mapOfTourAttributesMaps.put(i, tourAttributesMap);
		}
	
		// create ObjectAttrubutes for each agent
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Map<Integer, ObjectAttributes> personObjectAttributesMap = new HashMap<Integer, ObjectAttributes>();
		for (int i=0; i<numberOfPlans; i++) {
			ObjectAttributes personObjectAttributes = new ObjectAttributes();
			personObjectAttributesMap.put(i, personObjectAttributes);
		}
		
		// read in network
		new NetworkReaderMatsimV1(scenario).parse(networkFile);
		
		// write all (geographic) features of planning area to a map
		Map<String,SimpleFeature> combinedFeatures = new HashMap<String, SimpleFeature>();
		for (SimpleFeature feature: ShapeFileReader.getAllFeatures(tazShapeFile)) {
			Integer schluessel = Integer.parseInt((String) feature.getAttribute("NR"));
			String id = schluessel.toString();
			combinedFeatures.put(id,feature);
		}

//		// parse cemdap tours file
//		for (int i=0; i<numberOfPlans; i++) {
//			new CemdapToursParser().parse(cemdapToursFilesMap.get(i), mapOfTourAttributesMaps.get(i));
//		}
		
		// parse cemdap stops file
		for (int i=0; i<numberOfPlans; i++) {
			new CemdapStopsParser().parse(cemdapStopsFilesMap.get(i), i, //mapOfTourAttributesMaps.get(i), 
					scenario,
					personObjectAttributesMap.get(i), false);
			new Feature2Coord().assignCoords(scenario, i, personObjectAttributesMap.get(i), combinedFeatures);
		}
				
		// if applicable, add a stay-home plan
		if (addStayHomePlan == true) {
			int planNumber = numberOfPlans; // Thus, number of stay-home plan is one more than number of last plan.
			new CemdapStopsParser().parse(cemdapStopsFilesMap.get(0), planNumber, //mapOfTourAttributesMaps.get(0), 
					scenario,
					personObjectAttributesMap.get(0), true);
			new Feature2Coord().assignCoords(scenario, planNumber, personObjectAttributesMap.get(0), combinedFeatures);
		}
			
		// check if number of plans that each agent has is correct
		int counter = 0;
		int expectedNumberOfPlans;
		if (addStayHomePlan == true) {
			expectedNumberOfPlans = numberOfPlans + 1;
		} else {
			expectedNumberOfPlans = numberOfPlans;
		}
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getPlans().size() < expectedNumberOfPlans) {
				log.warn("Person with ID=" + person.getId() + " has less than " + expectedNumberOfPlans + " plans");
			}
			if (person.getPlans().size() > expectedNumberOfPlans) {
				log.warn("Person with ID=" + person.getId() + " has more than " + expectedNumberOfPlans + " plans");
				}
			if (person.getPlans().size() == expectedNumberOfPlans) {
				counter++;
			}
		}
		log.info(counter + " persons have " + expectedNumberOfPlans + " plans.");
		
		// assign activities to links
		new XY2Links((MutableScenario)scenario).run(scenario.getPopulation());
		
		// write population file
		new File(outputDirectory).mkdir();
		new PopulationWriter(scenario.getPopulation(), null).write(outputDirectory + "plans.xml.gz");
		//new ObjectAttributesXmlWriter(personObjectAttributesMap.get(0)).writeFile(outputBase+"personObjectAttributes0.xml.gz");
	}

}
