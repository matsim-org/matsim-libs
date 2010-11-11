/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkGen.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.tutorial;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.xml.sax.SAXException;

/**
 * @author laemmel
 * 
 */
public class NetworkGen {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String osm = "/home/laemmel/arbeit/evacuationTutorial/map.osm";
		Scenario sc = new ScenarioImpl();
		Network net = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
		OsmNetworkReader onr = new OsmNetworkReader(net, ct);
		onr.parse(osm);
		new NetworkWriter(net).write("/home/laemmel/arbeit/evacuationTutorial/network.xml");
	}

}
