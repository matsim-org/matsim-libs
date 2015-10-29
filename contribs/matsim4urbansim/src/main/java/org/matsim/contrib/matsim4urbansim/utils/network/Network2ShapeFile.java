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
package org.matsim.contrib.matsim4urbansim.utils.network;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class helps to convert a network file into a shape file in order to 
 * determine the link ids.
 * 
 * @author thomas
 *
 */
public class Network2ShapeFile {

	public static void main(String[] args) {
		String netFile = "/Users/thomas/Downloads/belgiumReduced.xml.gz";//"/Users/thomas/Desktop/zurich_ivtch-osm_network/1000it/schwamendingertunnel/matsim/output_network.xml.gz";
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);

		// WGS84 isn't correct and may affects the transformation, but this isn't important to see the link id's
		// use CH1903_LV03_GT for switzerland or "EPSG:31300" for belgium
		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:31300");
		String transformation = crs.getName().toString();
		// new Links2ESRIShape(net, "/Users/thomas/Downloads/belgiumReduced.shp", TransformationFactory.WGS84).write();
		new Links2ESRIShape(network, "/Users/thomas/Downloads/belgiumReduced.shp", transformation).write();
	}

}
