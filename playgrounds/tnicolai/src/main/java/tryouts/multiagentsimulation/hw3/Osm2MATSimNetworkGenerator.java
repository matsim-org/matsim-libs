/* *********************************************************************** *
 * project: org.matsim.*
 * Main.java
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

/**
 * 
 */
package tryouts.multiagentsimulation.hw3;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

/**
 * @author thomas
 *
 */
public class Osm2MATSimNetworkGenerator {
	
	private static boolean useHighwayDefaults = true;
	
	public static void main(String args[]){
		String osm = "./tnicolai/configs/osm/brandenburg.osm"; // Potsdam
		// String osm = "./tnicolai/configs/osm/map.osm"; // Eiche

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
		Network net = sc.getNetwork();

		CoordinateTransformation coordinateTransormation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S); //the coordinate transformation is needed to get a projected  coordinate system
		// for this basic example UTM zone 33 North is the right coordinate system. This may differ depending on your scenario. See also http://en.wikipedia.org/wiki/Universal_Transverse_Mercator


		// OsmNetworkReader onr = new OsmNetworkReader(net,coordinateTransormation); //constructs a new openstreetmap reader
		OsmNetworkReader osmNetworkReader = new OsmNetworkReader(net, coordinateTransormation, useHighwayDefaults);
		osmNetworkReader.setHierarchyLayer(52.774, 12.398, 52.051, 13.774, 3); // Brandenburg (mittlere bis große Straßen)
		osmNetworkReader.setHierarchyLayer(52.5152, 12.8838, 52.3402, 13.1709, 6);	// Potsdam (kleine bis grope Straßen)
		// osmNetworkReader.setHierarchyLayer(52.4147, 12.9719, 52.4023, 13.0006, 6); // Eiche (kleine bis grope Straßen)
		try {
			// onr.parse(osm); //starts the conversion from osm to matsim
			osmNetworkReader.parse(osm);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//at this point we already have a matsim network...
		new NetworkCleaner().run(net); //but may be there are isolated not connected links. The network cleaner removes those links

		new NetworkWriter(net).write("./tnicolai/configs/osm/network.xml");//here we write the network to a xml file
	}

}

