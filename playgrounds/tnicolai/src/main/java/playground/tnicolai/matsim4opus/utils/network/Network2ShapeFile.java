/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusNet2Shape
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
package playground.tnicolai.matsim4opus.utils.network;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

/**
 * This class helps to convert a network file into a shape file in order to 
 * determine the link ids.
 * 
 * @author thomas
 *
 */
public class Network2ShapeFile {

	public static void main(String[] args) {
		String netFile = "/Users/thomas/Development/opus_home/data/brussels_zone/matsim/network/belgium_simple_clean.xml.gz";//"/Users/thomas/Desktop/zurich_ivtch-osm_network/1000it/schwamendingertunnel/matsim/output_network.xml.gz";
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);

		// WGS84 isn't correct and may affects the transformation, but this isn't important to see the link id's
		// use CH1903_LV03_GT for switzerland
		new Links2ESRIShape(net, "/Users/thomas/Development/opus_home/data/brussels_zone/shapefiles/belgium_simple_clean.shp", TransformationFactory.WGS84).write();
	}

}
