/* *********************************************************************** *
 * project: org.matsim.*
 * Ks2010Utils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.network;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import playground.dgrether.utils.DgNet2Shape;


/**
 * @author dgrether
 *
 */
public class DgNetworkUtils {
	
	public static void writeNetwork(Network net, String outputFile){
		NetworkWriter netWriter = new NetworkWriter(net);
		netWriter.write(outputFile);
	}
	
	public static void writeNetwork2Shape(Network net, CoordinateReferenceSystem crs, String outputFilenamePrefix){
		new DgNet2Shape().write(net, outputFilenamePrefix + "_links.shp", crs);
		new Nodes2ESRIShape(net, outputFilenamePrefix + "_nodes.shp", crs).write();
	}
}
