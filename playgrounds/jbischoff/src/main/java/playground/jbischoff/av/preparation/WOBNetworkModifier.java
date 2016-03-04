/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.preparation;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;

/**
 * @author  jbischoff
 *
 */
public class WOBNetworkModifier {
	public static void main(String[] args) {
		
		Network network = NetworkUtils.createNetwork();
		Network network2 = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/network_nopt.xml");
		for (Node node : network.getNodes().values()){
			node.getInLinks().clear();
			node.getOutLinks().clear();
		}
		for (Link link : network.getLinks().values()){
			if (!link.getAllowedModes().contains("vw")&&(link.getAllowedModes().size()==1)) {
				if (!network2.getNodes().containsKey(link.getFromNode().getId())){
					network2.addNode(link.getFromNode());
				}
				if (!network2.getNodes().containsKey(link.getToNode().getId())){
					network2.addNode(link.getToNode());
				}
				network2.addLink(link);
			}
		}
		new NetworkCleaner().run(network2);
		new NetworkWriter(network2).write("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/network_noptvw.xml");
	}
}
