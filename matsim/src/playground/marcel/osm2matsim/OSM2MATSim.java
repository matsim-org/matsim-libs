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

package playground.marcel.osm2matsim;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.xml.sax.SAXException;

public class OSM2MATSim {

	public static void main(final String[] args) {

		Gbl.startMeasurement();
		OSMReader osmReader = new OSMReader();
		try {
//			osmReader.parse("../mystudies/zueri.osm");
			osmReader.parse("../mystudies/switzerland.osm");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		NetworkLayer network = osmReader.convert();
//		new NetworkWriter(network, "../mystudies/zueri-net.xml").write();
		new NetworkWriter(network, "../mystudies/swiss-net.xml").write();
		Gbl.printElapsedTime();
	}

}
