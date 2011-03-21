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

package playground.duncan;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.OsmNetworkReader;


public class OSM2MATSim {
	private static final Logger log = Logger.getLogger(OSM2MATSim.class);

	public static void main(final String[] args) {

		log.error( "This still doesn't have the coordinate transform problem fixed.  kai, jan09" ) ;

		Gbl.startMeasurement();
		NetworkImpl network = NetworkImpl.createNetwork();
		OsmNetworkReader osmReader = new OsmNetworkReader(network, new WGS84toCH1903LV03()); // wrong coordinate system! that's for Switzerland
		//			osmReader.parse("../mystudies/zueri.osm");
		osmReader.parse("../shared-svn/studies/north-america/ca/vancouver/network/osm/map.osm");
//		new NetworkWriter(network, "../mystudies/zueri-net.xml").write();
		new NetworkWriter(network).write("../shared-svn/studies/north-america/ca/vancouver/network/net.xml");
		Gbl.printElapsedTime();
	}

}
