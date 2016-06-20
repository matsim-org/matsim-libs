/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.network;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.OsmNetworkReader;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
* @author amit
*/

public class PatnaOSM2MatsimNetwork {
	
	private static final String osmNetworkFile = PatnaUtils.INPUT_FILES_DIR + "/osmNetworkFile.osm";
	
	public static void main(String[] args) {
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), PatnaUtils.COORDINATE_TRANSFORMATION);
//		reader.setKeepPaths(true);
		reader.parse(osmNetworkFile);
		
		NetworkSimplifier simplifier = new NetworkSimplifier();
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();

		nodeTypesToMerge.add(Integer.valueOf(4));
		nodeTypesToMerge.add(Integer.valueOf(5));

		simplifier.setNodesToMerge(nodeTypesToMerge);
		simplifier.run(sc.getNetwork());
		
		
		new NetworkWriter(sc.getNetwork()).write(PatnaUtils.INPUT_FILES_DIR +"/networkFromOSM.xml.gz");
	}
	

}


	