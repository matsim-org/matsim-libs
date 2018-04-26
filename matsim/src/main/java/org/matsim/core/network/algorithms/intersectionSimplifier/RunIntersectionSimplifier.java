/* *********************************************************************** *
 * project: org.matsim.*
 * RunNetworkSimplifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.network.algorithms.intersectionSimplifier;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

/**
 * Example to illustrate how the density-based algorithm is used to simplify a
 * network's intersections.
 * 
 * @author jwjoubert
 */
public class RunIntersectionSimplifier {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		run(args);
	}
	
	public static void run(String[] args) {
		String input = args[0];
		String output = args[1];
		String cluster = args[2];
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(input);
		
		IntersectionSimplifier ns = new IntersectionSimplifier(30.0, 2);
		Network newNetwork = ns.simplify(network, cluster);
		new NetworkWriter(newNetwork).write(output);
	}

}
