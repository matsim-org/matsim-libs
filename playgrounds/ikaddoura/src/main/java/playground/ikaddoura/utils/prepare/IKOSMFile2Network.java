/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.utils.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author dhosse, ikaddoura
 *
 */
public class IKOSMFile2Network {
	
	private String osmInputFile = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/incidents/network/germany-latest-highways-mainroads.osm";
	private String networkOutputFile = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/incidents/network/germany-network-mainroads-a.xml";
	
	private Scenario scenario;

	public static void main(String[] args) {
		
		IKOSMFile2Network main = new IKOSMFile2Network();
		main.loadScenario();
		
		main.generateAndWriteNetwork();
	}

	private void generateAndWriteNetwork(){
		
		Network network = scenario.getNetwork();
		OsmNetworkReader or = new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4));
		or.parse(this.osmInputFile);
		new NetworkCleaner().run(network);
		new NetworkWriter(network).writeV1(this.networkOutputFile);
	}
	
	private void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
	}
}
