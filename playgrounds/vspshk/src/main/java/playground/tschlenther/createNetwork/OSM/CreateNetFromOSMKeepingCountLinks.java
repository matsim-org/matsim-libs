/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.tschlenther.createNetwork.OSM;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author Work
 *
 */
public class CreateNetFromOSMKeepingCountLinks {

	private static final String INPUT_OSMFILE = "C:/Users/Work/VSP/OSM/Reinickendorf_Flottenstr.osm";
	private static final String OUTPUT_NETWORK = "C:/Users/Work/VSP/OSM/test_DontKeepPaths_fast_firstKeep1NodeThanSimplify.xml";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		readAndWrite();
	}

	
	private static void readAndWrite(){
	
		CoordinateTransformation ct = 
			 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		Network network = scenario.getNetwork();
		
		OsmNetworkReader onr = new OsmNetworkReader(network,ct);
		onr.setKeepPaths(false);
		
		Set<Long> nodeIDsToKeep = new HashSet<Long>();
		nodeIDsToKeep.add(1184135011l);
		onr.setNodeIDsToKeep(nodeIDsToKeep);
		
		onr.parse(INPUT_OSMFILE);
		
		/*
		 * Clean the Network. Cleaning means removing disconnected components, so that afterwards there is a route from every link
		 * to every other link. This may not be the case in the initial network converted from OpenStreetMap.
		 */
		new NetworkSimplifier().run(network);
		
		new NetworkWriter(network).write(OUTPUT_NETWORK);
		
	}
	
	
}
