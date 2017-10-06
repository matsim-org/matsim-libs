/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accessibility.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author dziemke
 */
public class AccessibilityOsmNetworkReader {
	final private static Logger LOG = Logger.getLogger(AccessibilityOsmNetworkReader.class);
	
	private String osmFileName;
	private InputStream osmInputStream;
	private final String outputCRS;
	private String inputCRS = "EPSG:4326"; // EPSG:4326 = WGS84, OSM input is always in this CRS
	private boolean keepPaths = false; 
	private boolean includeLowHierarchyWays = false;
	private boolean onlyMotorwayToTertiary = false; // "Thinner" network down to tertiary; do not use this together with "includeLowHierarchyWays"
	private boolean onlyMotorwayToSecondary = false; // "Thinner" network down to tertiary; do not use this together with "includeLowHierarchyWays"
	
	private Network network;

	public static void main(String[] args) {
		String osmFileName = "../../upretoria/data/capetown/osm/2017-10-03";
		String outputRoot = "../../upretoria/data/capetown/network/";
		String networkFileName = outputRoot + "2017-10-03_network.xml.gz";

		// EPSG:4326 = WGS84
		// EPSG:31468 = DHDN GK4, for Berlin; DE
		// EPSG:26918 = NAD83 / UTM zone 18N, for Maryland, US
		// EPSG:25832 = ETRS89 / UTM zone 32N, for Nordrhein-Westfalen
		// EPSG:22235 = Cape / UTM zone 35S, for South Africa
		String outputCRS = "EPSG:22235";
		
		AccessibilityOsmNetworkReader osmNetworkCreatorDZ = new AccessibilityOsmNetworkReader(osmFileName, outputCRS);
		osmNetworkCreatorDZ.createNetwork();
		osmNetworkCreatorDZ.writeNetwork(outputRoot, networkFileName);
	}

	public AccessibilityOsmNetworkReader(String osmFileName, String outputCRS) {
		this.osmFileName = osmFileName;
		this.outputCRS = outputCRS;
	}
	
	public AccessibilityOsmNetworkReader(InputStream osmInputStream, String outputCRS) {
		this.osmInputStream = osmInputStream;
		this.outputCRS = outputCRS;
	}

	public void createNetwork() {
		LOG.info("Input CRS is " + inputCRS + "; output CRS is " + outputCRS);
		LOG.info("Setting \"keepPaths\" = " + keepPaths);
		LOG.info("Setting \"includeLowHierarchyWays\" = " + includeLowHierarchyWays);
		LOG.info("Setting \"onlyMotorwayToTertiary\" = " + onlyMotorwayToTertiary);
		LOG.info("Setting \"onlyMotorwayToSecondary\" = " + onlyMotorwayToSecondary);

		// Infrastructure
		network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		OsmNetworkReader osmNetworkReader;
		if (onlyMotorwayToTertiary == true || onlyMotorwayToSecondary == true) {
			osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, false);
		} else {
			osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, true);
		}
		
		// Setting this to true means that pure geometry-describing nodes (i.e. nodes which are not intersections etc.) are kept.
		// This makes the file (for the Nairobi case) three times as big (22.4MB vs. 8.7MB)
		if (keepPaths == true) {
			LOG.info("Detailed geometry of paths is kept.");
			osmNetworkReader.setKeepPaths(true);
		}
				
		// This block is for the low hierarchy roads
		if (includeLowHierarchyWays == true) {
			LOG.info("Low hierarchy ways are included.");
			// By defaults set for motorway, motorway_link, trunk, trunk_link, primary, primary_link, secondary, secondary_link,
			// tertiary, tertiary_link, minor, unclassified, residential, living_street; minor does not exist on the website anymore
			// Parameters for living_street: (6, "living_street", 1,  15.0/3.6, 1.0,  300);
			// (hierarchy, highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour)
			//
			// Other types on OSM, see: http://wiki.openstreetmap.org/wiki/Key:highway
			osmNetworkReader.setHighwayDefaults(7, "cycleway", 1, 30/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "pedestrian", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "road", 1, 15/3.6, 1.0, 300); // like "living_street"
			osmNetworkReader.setHighwayDefaults(8, "track", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(8, "footway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(8, "steps", 1, 5/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(8, "path", 1, 15/3.6, 1.0, 0);
		}		
				
		// For a coarser network ("onlyMotorwayToTertiaryMotorways" leads to a network only a 14th as big (77.8MB vs. 1.04GB) for Maryland)
		if (onlyMotorwayToSecondary == true || onlyMotorwayToTertiary == true) {
			LOG.info("Only bigger roads are included.");
			if (includeLowHierarchyWays == true) {
				throw new RuntimeException("It does not make sense to set both \"includeLowHierarchyWays\""
						+ " and \"onlyMotorwayToSecondary\" or \"onlyMotorwayToTertiary\" to true");
			}
			osmNetworkReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
			osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
			osmNetworkReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
			osmNetworkReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(4, "secondary",     1,  30.0/3.6, 1.0, 1000);
			osmNetworkReader.setHighwayDefaults(4, "secondary_link",     1,  30.0/3.6, 1.0, 1000);
			if (onlyMotorwayToTertiary == true) {
				osmNetworkReader.setHighwayDefaults(5, "tertiary",      1,  25.0/3.6, 1.0,  600);
				osmNetworkReader.setHighwayDefaults(5, "tertiary_link",      1,  25.0/3.6, 1.0,  600);
			}
		}

		if (osmFileName != null && osmInputStream == null) {
			osmNetworkReader.parse(osmFileName);
		} else if (osmFileName == null && osmInputStream != null) {
			osmNetworkReader.parse(osmInputStream); 
		} else {
			throw new IllegalArgumentException("Either the input stream OR the input file should be defined, but not both.");
		}
		
		new NetworkCleaner().run(network);
	}
	
	public void writeNetwork(String outputRoot, String networkFileName) {
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputRoot);
		} catch (IOException e) {
			e.printStackTrace();
		}
		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(networkFileName);
		LOG.info("Network file written to " + networkFileName);
	}
	
	/**
	 * The input CRS for data from OSM should in almost all cases be EPSG:4326 = WGS84, which is used here as default.
	 */
	public void setInputCRS(String inputCRS) {
		this.inputCRS = inputCRS;
	}

	public void setKeepPaths(boolean keepPaths) {
		this.keepPaths = keepPaths;
	}
	
	public void setIincludeLowHierarchyWays(boolean includeLowHierarchyWays) {
		this.includeLowHierarchyWays = includeLowHierarchyWays;
	}

	public void setOnlyMotorwayToTertiary(boolean onlyMotorwayToTertiary) {
		this.onlyMotorwayToTertiary = onlyMotorwayToTertiary;
	}

	public void setOnlyMotorwayToSecondary(boolean onlyMotorwayToSecondary) {
		this.onlyMotorwayToSecondary = onlyMotorwayToSecondary;
	}
	
	public Network getNetwork() {
		return this.network;
	}
}