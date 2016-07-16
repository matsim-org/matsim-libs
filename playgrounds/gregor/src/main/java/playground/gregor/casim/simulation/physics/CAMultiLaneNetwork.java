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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.casim.monitoring.CAMultiLaneTrajectoryWriter;
import playground.gregor.casim.proto.CALinkInfos.CALinInfos;
import playground.gregor.casim.proto.CALinkInfos.CALinInfos.CALinkInfo;
import playground.gregor.casim.simulation.CANetsimEngine;
import playground.gregor.vis.CASimVisRequestHandler;

public class CAMultiLaneNetwork extends AbstractCANetwork {

	private static final Logger log = Logger
			.getLogger(CAMultiLaneNetwork.class);
	private CADensityEstimatorKernel k;

	public CAMultiLaneNetwork(Network net, EventsManager em,
			CANetsimEngine engine, CASimDensityEstimatorFactory fac) {
		super(net, em, engine, fac);
		this.k = fac.createCASimDensityEstimator(this);
		init();
		if (STATIC_VIS_HANDLER != null) {
			((CASimVisRequestHandler) STATIC_VIS_HANDLER).intitialize(this);
		}
	}

	private void init() {



		super.tFreeMin = Double.POSITIVE_INFINITY;
		for (Node n : this.net.getNodes().values()) {

			CAMultiLaneNode caNode = new CAMultiLaneNode(n, this);
			this.caNodes.put(n.getId(), caNode);
			if (caNode.getTFree() < this.tFreeMin) {
				this.tFreeMin = caNode.getTFree();
			}
		}

		for (Link l : this.net.getLinks().values()) {
			CAMultiLaneNode us = (CAMultiLaneNode) this.caNodes.get(l
					.getFromNode().getId());
			CAMultiLaneNode ds = (CAMultiLaneNode) this.caNodes.get(l
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
			CAMultiLaneLink caL = new CAMultiLaneLink(l, rev, ds, us, this,
					(MultiLaneDensityEstimator) this.k);

			if (caL.getTFree() < tFreeMin) {
				tFreeMin = caL.getTFree();
			}

			us.addLink(caL);
			ds.addLink(caL);
			this.caLinks.put(l.getId(), caL);
		}

		log.info("Minimal free speed cell travel time is: " + this.tFreeMin);


		Object o = this.getEngine().getMobsim().getScenario().getScenarioElement("CALinkInfos");
		CALinInfos infos = null;
		if (o != null) {

			infos = (CALinInfos) o;
			log.info("creating signal plans");
			for (CALinkInfo inf : infos.getCaLinkInfoList()) {
				if (inf.getCycle() > 0) {
					Id<Link> id = Id.createLinkId(inf.getId());
					CAMultiLaneLink caLink = (CAMultiLaneLink) this.caLinks.get(id);
					caLink.setSignalPlan(inf.getOffset(),inf.getWk(), inf.getCycle());
				}
			}
			if (EMIT_VIS_EVENTS) {
				log.info("creating monitors");
				for (CALinkInfo inf : infos.getCaLinkInfoList()) {
					if (inf.getMonitor()) {
						Id<Link> id = Id.createLinkId(inf.getId());
						CAMultiLaneLink caLink = (CAMultiLaneLink) this.caLinks.get(id);
						String outDir = this.getEngine().getMobsim().getScenario().getConfig().controler().getOutputDirectory();
						CAMultiLaneTrajectoryWriter m = new CAMultiLaneTrajectoryWriter(caLink, outDir);
						this.addMonitor(m);
					}
				}
			}
		}


	}
}
