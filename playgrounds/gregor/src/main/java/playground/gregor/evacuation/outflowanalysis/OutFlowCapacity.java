/* *********************************************************************** *
 * project: org.matsim.*
 * OutFlowCapacity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.evacuation.outflowanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class OutFlowCapacity {

	private static Logger log = Logger.getLogger(OutFlowCapacity.class);
	private final NetworkImpl network;
	private double outFlowCapcity = 0;
	private final Set<Link> outFlowLinks = new HashSet<Link>();

	public OutFlowCapacity(NetworkImpl network) {
		this.network = network;
	}

	public void run() {
		parseOutLinks();
		parseOutNodes();
	}

	private void parseOutNodes() {
		Node node = this.network.getNodes().get(new IdImpl("en1"));
		double cap = 0;
		ArrayList<Node> outNode = new ArrayList<Node>();
		for (Link l : node.getInLinks().values()) {
			Node out = l.getFromNode();
			outNode.add(out);
			for (Link ll : out.getInLinks().values()) {
				cap += ll.getCapacity();
			}
		}
		System.err.println("cap:" + cap);

	}

	private void parseOutLinks() {
		outFlowCapcity = 0;
		for (Link link : this.network.getLinks().values()) {
			if (this.outFlowLinks.contains(link)) {
				continue;
			}
			if (link.getId().toString().contains("el")) {
				continue;
			}

			for (Link l2 : link.getToNode().getOutLinks().values()) {
				if (l2.getId().toString().contains("el")) {
					this.outFlowLinks.add(link);
					this.outFlowCapcity += link.getCapacity();
					break;
				}

			}


		}
		System.out.println("OutFlowCap:" + this.outFlowCapcity);
	}

	public static void main(String [] args) {
		 String netfile = "./networks/padang_net_evac_v20080618.xml";

			log.info("loading network from " + netfile);
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			NetworkImpl network = scenario.getNetwork();
			new MatsimNetworkReader(scenario).readFile(netfile);
//			world.setNetworkLayer(network);
//			world.complete();
			log.info("done.");

			new OutFlowCapacity(network).run();
	}

}
