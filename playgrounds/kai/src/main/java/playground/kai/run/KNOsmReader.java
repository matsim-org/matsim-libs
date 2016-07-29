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
package playground.kai.run;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.matsim4urbansim.utils.network.NetworkSimplifier;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author nagel
 *
 */
public class KNOsmReader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CoordinateTransformation ct = 
				 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:26918");

		Config config = ConfigUtils.createConfig() ;
		Scenario sc = ScenarioUtils.createScenario(config) ;
		Network network = sc.getNetwork() ;
		OsmNetworkReader reader = new OsmNetworkReader(network,ct) ;
		reader.setKeepPaths(false);
		reader.setHierarchyLayer(1);
		reader.parse("/Users/nagel/shared-svn/projects/tum-with-moeckel/data/other/osm/md_and_surroundings.osm");
		
		{		
			NetworkWriter writer = new NetworkWriter(network) ;
			writer.write("/Users/nagel/kw/net0.xml.gz");
		}
		NetworkSimplifier simplifier = new NetworkSimplifier() ;
		simplifier.setMergeLinkStats(true);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add( NetworkCalcTopoType.PASS1WAY );
		// (if you look at the code, this means a node where a 1-way street passes through)
		nodeTypesToMerge.add( NetworkCalcTopoType.PASS2WAY );
		// (if you look at the code, this means a node where a 2-way street passes through)
		simplifier.setNodesToMerge(nodeTypesToMerge);
		simplifier.run(network);
		{		
			NetworkWriter writer = new NetworkWriter(network) ;
			writer.write("/Users/nagel/kw/net.xml.gz");
		}
		
	}

}
