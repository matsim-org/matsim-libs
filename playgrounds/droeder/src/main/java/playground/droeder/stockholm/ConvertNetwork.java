/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */
package playground.droeder.stockholm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author droeder / Senozon Deutschland GmbH
 *
 */
class ConvertNetwork {

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(ConvertNetwork.class);

    private ConvertNetwork() {
	
    }

    public static void main(String[] args) {
	String osm = "C:\\Users\\Daniel\\Desktop\\_Ablage\\Stockholm\\osm\\sweden-latest.osm";
	String result = "C:\\Users\\Daniel\\Desktop\\_Ablage\\Stockholm\\osm\\network.xml.gz";
	
	Network net = NetworkUtils.createNetwork();
	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:3006");
	
	OsmNetworkReader reader = new OsmNetworkReader(net, ct, true);
	reader.setKeepPaths(false);
	reader.setMemoryOptimization(true);
	
	reader.parse(osm);
	
	new NetworkWriter(net).write(result);
    }

}

