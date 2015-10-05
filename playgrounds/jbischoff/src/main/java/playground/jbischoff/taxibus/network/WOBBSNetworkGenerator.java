/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
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
package playground.jbischoff.taxibus.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author jbischoff
 *
 */
public class WOBBSNetworkGenerator {

	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		String dir = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/generation/";
		String epsg = "EPSG:25832";
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, epsg);
		OsmNetworkReader onr = new OsmNetworkReader(scenario.getNetwork(), ct, true);
		
		// all big roads
		onr.setHierarchyLayer(53.0098, 9.6158, 52.0542, 11.9064, 4);
		
		//Wolfsburg-BS-Helmstedt-Gifhorn, incl. secondary roads
		onr.setHierarchyLayer(52.5313,10.3848,52.1436,11.1346, 5);
		
		//everything:
		//Braunschweig
		onr.setHierarchyLayer(52.3664,10.4315,52.2139,10.6100,6);
	
		//Wolfsburg+Gifhorn
		onr.setHierarchyLayer(52.5346, 10.4610, 52.3664, 10.8984, 6);
		
		onr.parse(dir+"complete_area.osm");
		
		new NetworkCleaner().run(scenario.getNetwork());
		new NetworkWriter(scenario.getNetwork()).write(dir+"network.xml");

		
		
	}
}
