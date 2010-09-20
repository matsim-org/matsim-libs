/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;

import playground.mrieser.core.mobsim.api.TimestepSimEngine;
import playground.mrieser.core.mobsim.network.api.MobSimNetwork;

/**
 * @author mrieser
 */
/*package*/ abstract class QueueNetworkCreator {

	public static MobSimNetwork createQueueNetwork(final Network network, final TimestepSimEngine simEngine, final Operator operator) {
		QueueNetwork qnet = new QueueNetwork(simEngine, operator);

		for (Link link : network.getLinks().values()) {
			qnet.addLink(new QueueLink(link, qnet, operator));
		}
		for (Node node : network.getNodes().values()) {
			qnet.addNode(new QueueNode(node, qnet, operator, MatsimRandom.getLocalInstance()));
		}
		for (QueueLink ql : qnet.getLinks().values()) {
			ql.buffer.init();
		}

		return qnet;
	}

}
