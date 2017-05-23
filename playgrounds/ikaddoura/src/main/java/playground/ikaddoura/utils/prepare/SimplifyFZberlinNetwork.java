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

package playground.ikaddoura.utils.prepare;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
* @author ikaddoura
*/

public class SimplifyFZberlinNetwork {

	public static void main(String[] args) {
		
		Network network = LoadMyScenarios.loadScenarioFromNetwork("/Users/ihab/Documents/workspace/shared-svn/studies/fzwick/BerlinNetworkV0_GK4.xml").getNetwork();
		
		for (Link link : network.getLinks().values()) {
			link.getAttributes().clear();
		}
		
//		new NetworkWriter(network).write("/Users/ihab/Documents/workspace/shared-svn/studies/fzwick/BerlinNetworkV0_GK4_noLinkAttributes.xml.gz");

		NetworkSimplifier nn = new NetworkSimplifier();
		nn.run(network);
		
		new NetworkWriter(network).write("/Users/ihab/Documents/workspace/shared-svn/studies/fzwick/BerlinNetworkV0_GK4_simplified-0.xml.gz");
	}

}

