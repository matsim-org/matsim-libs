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
package playground.agarwalamit.siouxFalls.simulationInputs;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class LinkCapacityModifier {

	private static final Logger LOG = Logger.getLogger(LinkCapacityModifier.class);

	private final Network network;

	public LinkCapacityModifier(Network network) {
		this.network = network;
	}
	
	public static void main(String[] args) {
		String network = "/Users/amit/Documents/repos/runs-svn/siouxFalls/input/SiouxFalls_networkWithRoadType.xml.gz";
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(network);
		new LinkCapacityModifier(sc.getNetwork()).processNetwork(1.);
	}

	public void processNetwork(final double linkCapModificationFactor){
		LOG.info("Link capacity of each link in network will be modified by a factor of "+ linkCapModificationFactor);

		for (Link l : this.network.getLinks().values()){
			l.setCapacity(l.getCapacity()*linkCapModificationFactor);
			this.network.addLink(l);
		}
		LOG.info("Ignore the warning for duplicacy of link.");
	}

	public void writeNetwork(String outputFile){
		new NetworkWriter(network).write(outputFile);
	}
}