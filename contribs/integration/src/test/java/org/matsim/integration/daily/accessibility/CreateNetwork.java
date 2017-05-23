/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.integration.daily.accessibility;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

//import playground.dziemke.utils.LogToOutputSaver;

/**
 * @author dziemke
 */
public class CreateNetwork {
	final private static Logger LOG = Logger.getLogger(CreateNetwork.class);

	public static void main(String[] args) {
		// Input and output
//		String osmFile = "/Users/dominik/Accessibility/Data/OSM/2015-10-15_nairobi.osm.xml";
//		String osmFile = "/Users/dominik/Accessibility/Data/OSM/2015-11-05_kibera.osm.xml";
//		String osmFile = "../../../shared-svn/projects/maxess/data/kibera/osm/2016-12-18_kibera.osm";
		String osmFile = "../../../shared-svn/projects/maxess/data/nairobi/osm/2017-04-25_nairobi_central_and_kibera";
//		String osmFile = "../../../../Workspace/data/accessibility/osm/2015-10-15_capetown_central.osm.xml";
//		String osmFile = "../../../shared-svn/projects/accessibility_berlin/osm/berlin/2015-05-26_berlin.osm";
		
//		String osmFile = "../../../../SVN/shared-svn/projects/tum-with-moeckel/data/mstm/siloMatsim/network/md_dc.osm";
//		String osmFile = "../../../../LandUseTransport/Data/OSM/md_and_surroundings.osm";
//		String osmFile = "../../../shared-svn/projects/maxess/data/kenya/osm/kenya-latest.osm";
		
//		String networkFile = "/Users/dominik/Accessibility/Data/Networks/Kenya/2015-10-15_nairobi_paths.xml";
//		String networkFile = "/Users/dominik/Accessibility/Data/Networks/Kenya/2015-11-05_kibera_paths_detailed.xml";
//		String outputBase = "../../../../Workspace/data/accessibility/capetown/network/2015-10-15/";
		
//		String outputBase = "../../../shared-svn/projects/accessibility_berlin/network/2015-05-26/";
		
//		String outputBase = "../../../../SVN/shared-svn/projects/tum-with-moeckel/data/mstm/siloMatsim/network/";
//		String outputBase = "../../../../LandUseTransport/Data/OSM/network_04";
//		String outputBase = "../../../shared-svn/projects/maxess/data/kenya/network";
//		String outputBase = "../../../shared-svn/projects/maxess/data/kibera/network/2016-12-18/";
		String outputBase = "../../../shared-svn/projects/maxess/data/nairobi/network/";
		
//		String networkFile = outputBase + "/2016-10-19_network_detailed.xml";
//		String networkFile = outputBase + "2016-12-18_kibera_paths_detailed.xml";
		String networkFile = outputBase + "2017-04-25_nairobi_central_and_kibera_no_paths.xml";
//		String networkFile = outputBase + "network.xml";

		
		// Parameters
		String inputCRS = "EPSG:4326"; // EPSG:4326 = WGS84
		// EPSG:31468 = DHDN GK4, for Berlin; DE
		// EPSG:26918 = NAD83 / UTM zone 18N, for Maryland, US
		String outputCRS = "EPSG:21037"; // EPSG:21037 = Arc 1960 / UTM zone 37S, for Nairobi, KE
//		String outputCRS = TransformationFactory.WGS84_SA_Albers;
		
		createNetwork(osmFile, outputBase, networkFile, inputCRS, outputCRS);
	}
		
		
	public static void createNetwork(String osmFile, String outputBase, String networkFile, String inputCRS, String outputCRS) {
//		LogToOutputSaver.setOutputDirectory(outputBase);
		LOG.info("Input CRS is " + inputCRS + "; output CRS is " + outputCRS);
		
		boolean keepPaths = false;
		boolean includeLowHierarchyWays = true;
		boolean onlyBiggerRoads = false; // "thinner" network; do not use this together with "includeLowHierarchyWays"
		LOG.info("Settings: includeLowHierarchyWays = " + includeLowHierarchyWays + "; keepPaths = " + keepPaths);
		
		// Infrastructure
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		OsmNetworkReader osmNetworkReader = null;
		if (onlyBiggerRoads == true) {
			osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, false);
		} else {
			osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, true);
		}
		NetworkWriter networkWriter = new NetworkWriter(network);
		
		// Keeping the path means that links are not straightened between intersection nodes, but that also pure geometry-describing
		// nodes are kept. This makes the file (for the Nairobi case) three times as big (22.4MB vs. 8.7MB)
		if (keepPaths == true) {
			LOG.info("Detailed geometry of paths is kept.");
			osmNetworkReader.setKeepPaths(true);
		}
				
		// This block is for the low hierarchy roads
		if (includeLowHierarchyWays == true) {
			LOG.info("Low hierarchy ways are included.");
			// defaults already set for motorway, motorway_link, trunk, trunk_link, primary,
			// primary_link, secondary, tertiary, minor, unclassified, residential, living_street
			// minor does not seem to exist on the website anymore
			//
			// other types in osm, see: http://wiki.openstreetmap.org/wiki/Key:highway
			// secondary_link, tertiary_link, pedestrian, track, bus_guideway, raceway, road,
			// footway, bridleway, steps, path
			//
			// onr.setHighwayDefaults(hierarchy, highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour);
			//
			// lowest hierarchy contained in defaults: 6, "living_street", 1,  15.0/3.6, 1.0,  300);
			//
			osmNetworkReader.setHighwayDefaults(7, "pedestrian", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "track", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "road", 1, 15/3.6, 1.0, 300); // like "living_street"
			osmNetworkReader.setHighwayDefaults(7, "footway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "bridleway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "steps", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "path", 1, 15/3.6, 1.0, 0);
		}		
				
		// This block is to use only bigger roads
		// This makes the file (for the Maryland case) only a 14th as big (77.8MB vs. 1.04GB)
		if (onlyBiggerRoads == true) {
			LOG.info("Only bigger roads are included.");
			if (includeLowHierarchyWays == true) {
				throw new RuntimeException("It does not make sense to set both \"includeLowHierarchyWays\""
						+ " and \"onlyBiggerRoads\" to true");
			}
			osmNetworkReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
			osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
			osmNetworkReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
			osmNetworkReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(4, "secondary",     1,  30.0/3.6, 1.0, 1000);
			osmNetworkReader.setHighwayDefaults(4, "secondary_link",     1,  30.0/3.6, 1.0, 1000);
			osmNetworkReader.setHighwayDefaults(5, "tertiary",      1,  25.0/3.6, 1.0,  600);
			osmNetworkReader.setHighwayDefaults(5, "tertiary_link",      1,  25.0/3.6, 1.0,  600);
		}

		osmNetworkReader.parse(osmFile); 
		new NetworkCleaner().run(network);
		networkWriter.write(networkFile);
		LOG.info("Network file written to " + networkFile);
	}
	
	
	public static Network createNetwork(InputStream osmInputStream, String outputCRS) {		
		boolean keepPaths = true;
		boolean includeLowHierarchyWays = true;
		boolean onlyBiggerRoads = false; // "thinner" network; do not use this together with "includeLowHierarchyWays"
		LOG.info("Settings: includeLowHierarchyWays = " + includeLowHierarchyWays + "; keepPaths = " + keepPaths);
		
		// Infrastructure
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", outputCRS);
		OsmNetworkReader osmNetworkReader = null;
		if (onlyBiggerRoads == true) {
			osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, false);
		} else {
			osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, true);
		}
		
		// Keeping the path means that links are not straightened between intersection nodes, but that also pure geometry-describing
		// nodes are kept. This makes the file (for the Nairobi case) three times as big (22.4MB vs. 8.7MB)
		if (keepPaths == true) {
			LOG.info("Detailed geometry of paths is kept.");
			osmNetworkReader.setKeepPaths(true);
		}
				
		// This block is for the low hierarchy roads
		if (includeLowHierarchyWays == true) {
			LOG.info("Low hierarchy ways are included.");
			// defaults already set for motorway, motorway_link, trunk, trunk_link, primary,
			// primary_link, secondary, tertiary, minor, unclassified, residential, living_street
			// minor does not seem to exist on the website anymore
			//
			// other types in osm, see: http://wiki.openstreetmap.org/wiki/Key:highway
			// secondary_link, tertiary_link, pedestrian, track, bus_guideway, raceway, road,
			// footway, bridleway, steps, path
			//
			// onr.setHighwayDefaults(hierarchy, highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour);
			//
			// lowest hierarchy contained in defaults: 6, "living_street", 1,  15.0/3.6, 1.0,  300);
			//
			osmNetworkReader.setHighwayDefaults(7, "pedestrian", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "track", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "road", 1, 15/3.6, 1.0, 300); // like "living_street"
			osmNetworkReader.setHighwayDefaults(7, "footway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "bridleway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "steps", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "path", 1, 15/3.6, 1.0, 0);
		}		
				
		// This block is to use only bigger roads
		// This makes the file (for the Maryland case) only a 14th as big (77.8MB vs. 1.04GB)
		if (onlyBiggerRoads == true) {
			LOG.info("Only bigger roads are included.");
			if (includeLowHierarchyWays == true) {
				throw new RuntimeException("It does not make sense to set both \"includeLowHierarchyWays\""
						+ " and \"onlyBiggerRoads\" to true");
			}
			osmNetworkReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
			osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
			osmNetworkReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
			osmNetworkReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(4, "secondary",     1,  30.0/3.6, 1.0, 1000);
			osmNetworkReader.setHighwayDefaults(4, "secondary_link",     1,  30.0/3.6, 1.0, 1000);
			osmNetworkReader.setHighwayDefaults(5, "tertiary",      1,  25.0/3.6, 1.0,  600);
			osmNetworkReader.setHighwayDefaults(5, "tertiary_link",      1,  25.0/3.6, 1.0,  600);
		}

		osmNetworkReader.parse(osmInputStream); 
		new NetworkCleaner().run(network);
		return network;
	}
}