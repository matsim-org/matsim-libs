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

import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class OSM2MATSim {

	public static void main(final String[] args) {

		NetworkImpl network = NetworkImpl.createNetwork();
//		OsmNetworkReader osmReader = new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation("WGS84", "DHDN_GK4"), false);
//		OsmNetworkReader osmReader = new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"), false);
		OsmNetworkReader osmReader = new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation("WGS84", "WGS84"), false);
		osmReader.setKeepPaths(false);
		osmReader.setMemoryOptimization(true);

		osmReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		osmReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		osmReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
		osmReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000);
		osmReader.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
		osmReader.setHighwayDefaults(5, "minor",         1,  45.0/3.6, 1.0,  600);
		osmReader.setHighwayDefaults(5, "unclassified",  1,  45.0/3.6, 1.0,  600);
		osmReader.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600);
		osmReader.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);

//		freespeeds
//		AT:urban=50
//		AT:rural=100
//		CH:urban=50
//		CH:rural=80
//		DE:urban=50
//		DE:rural=100

osmReader.parse("/Volumes/Data/Downloads/osm/australia-oceania.osm");
//			osmReader.parse("/data/projects/bvg2010/Daten/20100902_brandenburg.osm");
//			osmReader.parse("../mystudies/osmnet/switzerland-20090313.osm");
//			osmReader.parse("../mystudies/osmnet/zueri-20080410.osm");
		new NetworkWriter(network).write("/Volumes/Data/Downloads/osm/australia-network.xml.gz");
//		new NetworkWriter(network).write("/data/projects/bvg2010/Daten/20100902_brandenburg.net.xml");
//		new NetworkWriter(network).write("../mystudies/osmnet/switzerland-20090313.xml");
//		new NetworkWriter(network, "../mystudies/osmnet/zueri-20080410.xml").write();
		new NetworkCleaner().run(network);
//		new NetworkWriter(network).write("/data/projects/bvg2010/Daten/20100902_brandenburg.clean-net.xml");
		new NetworkWriter(network).write("/Volumes/Data/Downloads/osm/australia-network-clean1.xml.gz");
	}

}
