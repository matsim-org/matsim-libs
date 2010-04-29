/* *********************************************************************** *
 * project: org.matsim.*
 * OSM2MATSim.java
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

package playground.mrieser.osm2matsim;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.xml.sax.SAXException;

public class OSM2MATSim {

	public static void main(final String[] args) {

		NetworkLayer network = new NetworkLayer();
		OsmNetworkReader osmReader = new OsmNetworkReader(network, new WGS84toCH1903LV03());
		osmReader.setKeepPaths(false);
		try {
			osmReader.parse("../mystudies/osmnet/switzerland-20090313.osm");
//			osmReader.parse("../mystudies/osmnet/zueri-20080410.osm");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new NetworkWriter(network).write("../mystudies/osmnet/switzerland-20090313.xml");
//		new NetworkWriter(network, "../mystudies/osmnet/zueri-20080410.xml").write();
	}

}
