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

package playground.gregor.casim.simulation.physics;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.casim.simulation.CANetsimEngine;

public class CASingleLaneNetwork extends AbstractCANetwork {

	public CASingleLaneNetwork(Network net, EventsManager em,
			CANetsimEngine engine, CASimDensityEstimatorFactory fac) {
		super(net, em, engine, fac);
		init();
	}

	private void init() {
		this.tFreeMin = Double.POSITIVE_INFINITY;
		for (Node n : this.net.getNodes().values()) {
			CASingleLaneNode caNode = new CASingleLaneNode(n, this);
			this.caNodes.put(n.getId(), caNode);
		}
		for (Link l : this.net.getLinks().values()) {
			CASingleLaneNode us = (CASingleLaneNode) this.caNodes.get(l
					.getFromNode().getId());
			CASingleLaneNode ds = (CASingleLaneNode) this.caNodes.get(l
					.getToNode().getId());
			Link rev = null;
			for (Link ll : l.getToNode().getOutLinks().values()) {
				if (ll.getToNode() == l.getFromNode()) {
					rev = ll;
				}
			}
			if (rev != null) {
				CALink revCA = this.caLinks.get(rev.getId());
				if (revCA != null) {
					this.caLinks.put(l.getId(), revCA);
					continue;
				}
			}
			CASingleLaneLink caL = new CASingleLaneLink(l, rev, ds, us, this);
			if (caL.getTFree() < this.tFreeMin) {
				this.tFreeMin = caL.getTFree();
			}
			us.addLink(caL);
			ds.addLink(caL);
			this.caLinks.put(l.getId(), caL);
		}

	}
}
