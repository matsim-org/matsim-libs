/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTimeAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package osm;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.run.NetworkCleaner;
import org.xml.sax.SAXException;



public class OsmToMatsim {

	/**
	 * @param args
	 * This class creates a Matsim network from an OSM network file.
	 */
	public static void main(String[] args) {

		NetworkImpl network = NetworkImpl.createNetwork();
		OsmNetworkReader osmReader = new OsmNetworkReader(network,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.CH1903_LV03), false);
		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);
		String input = args[0];				// OSM Input File
		String output = args[1];			// MATSim Output File

//		 set osmReader useHighwayDefaults false
//		 Autobahn
		osmReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		osmReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
//		 Bundesstrasse?
		osmReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
		osmReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
//		 Durchgangsstrassen
		osmReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
//		 Hauptstrassen
		osmReader.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000);
//		 mehr Hauptstrassen
		osmReader.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
//		 Nebenstrassen
		osmReader.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600);
//		 diverse
		osmReader.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600);
//		 Wohngebiete
		osmReader.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600);
//		 Spielstrassen irrelevant, since only tiny percentile
//		 osmReader.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);

		try {
			osmReader.parse(input);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Write network to file
		new NetworkWriter(network).write(output + ".xml.gz");
		System.out.println("Done! Unprocessed MATSim Network saved as " + output + ".xml.gz");
		// Clean network
		new NetworkCleaner().run(new String[] {output + ".xml.gz", output + "_cl.xml.gz"});
		System.out.println("NetworkCleaner done! Cleaned Network saved as " + output + "_cl.xml.gz");

		// Simplyfy Network
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(output + "_cl.xml.gz");

		Simplifier simply = new Simplifier();
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(3));
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		simply.setNodesToMerge(nodeTypesToMerge);
		simply.run(network);
		new NetworkWriter(network).write(output + "_cl_simple.xml.gz");
		System.out.println("Simplifier done! Network saved as " + output + "_cl_simple.xml.gz");
		System.out.println("Conversion completed!");

//		// Double Links
//		NetworkSegmentDoubleLinks networkDoubleLinks = new NetworkSegmentDoubleLinks();
//		networkDoubleLinks.run(network);
//		new NetworkWriter(network).write(output + "_cl_simple_dl.xml.gz");
//		System.out.println("\nConversion completed! Saved as " + output + "_cl_simple_dl.xml.gz");

	}

}
