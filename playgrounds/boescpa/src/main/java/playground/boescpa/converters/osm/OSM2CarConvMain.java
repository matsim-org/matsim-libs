/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import playground.boescpa.converters.osm.procedures.OsmNetworkReaderWithPT;

/**
 * Runs OSM-Converters...
 *
 * @author boescpa
 */
public class OSM2CarConvMain {

	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();

		//OsmNetworkReader osmReader =
		//		new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"), false);
		OsmNetworkReaderWithPT osmReader =
				new OsmNetworkReaderWithPT(network, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"), false);
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

		osmReader.parse(args[0]);

		new NetworkWriter(network).write(args[1]);
		new NetworkCleaner().run(network);
		new NetworkWriter(network).write(args[2]);
	}

}
