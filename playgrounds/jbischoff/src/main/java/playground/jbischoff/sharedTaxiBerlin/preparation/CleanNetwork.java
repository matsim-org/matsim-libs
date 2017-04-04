/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package playground.jbischoff.sharedTaxiBerlin.preparation;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CleanNetwork {
public static void main(String[] args) {
	Network net = NetworkUtils.createNetwork();
	new MatsimNetworkReader(net).readFile("C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/input/network-bvg_25833_cut.xml.gz");
	new NetworkCleaner().run(net);
	new NetworkWriter(net).write("C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/input/network-bvg_25833_cut_cleaned.xml.gz");
}
}
