/* *********************************************************************** *
 * project: org.matsim.*
 * NodeLinksCounter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import org.matsim.core.network.NetworkImpl;

public class NodeLinksCounter {

	private void run(NetworkImpl net){
		System.out.println("links: " + net.getLinks().size());
		System.out.println("nodes: " + net.getNodes().size());
	}
	
	public static void main(String[] args) {
		String netFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_network.xml.gz";
		NetworkImpl net = new DataLoader().readNetwork(netFile);
		new NodeLinksCounter().run(net);
	}

}
