/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

public class CASingleLaneDensityEstimatorSPAII implements
		CADensityEstimatorKernel {

	private static final Logger log = Logger
			.getLogger(CASingleLaneDensityEstimatorSPAII.class);

	public static int RANGE = 3;
	private final int behind;
	private final int infront;

	private AbstractCANetwork net;
	private int misses = 0;

	private final int cutoffBehind;

	private final int cutoffInfront;

	public CASingleLaneDensityEstimatorSPAII(AbstractCANetwork net) {
		this.net = net;
		// this.behind = RANGE / 2;
		this.behind = 0;
		this.infront = RANGE - behind;

		this.cutoffBehind = (int) Math.ceil(AbstractCANetwork.RHO_HAT * behind);
		this.cutoffInfront = (int) Math.ceil(AbstractCANetwork.RHO_HAT
				* infront);
	}

	@Override
	public double estRho(CAMoveableEntity e) {
		CANetworkEntity ett = e.getCurrentCANetworkEntity();
		if (ett instanceof CASingleLaneNode) {
			return e.getRho();
		}
		CASingleLaneLink l = (CASingleLaneLink) ett;

		int pos = e.getPos();
		int dir = e.getDir();

		if (l.getParticles()[pos] != e) {
			// agent on link?
			misses++;
			return e.getRho();
		}
		int stepsBehind = traverse(e, pos, -dir, cutoffBehind, behind);
		if (stepsBehind < 0) {
			return e.getRho();
		}
		int stepsInfront = traverse(e, pos, dir, cutoffInfront, infront);
		if (stepsInfront < 0) {
			return e.getRho();
		}
		int steps = stepsBehind + stepsInfront;

		return (AbstractCANetwork.RHO_HAT * RANGE) / (steps - 1);
	}

	private int traverse(CAMoveableEntity e, int pos, int dir, int cutoff,
			int range) {
		int found = 0;
		int traversed = 0;
		CASingleLaneLink l = (CASingleLaneLink) e.getCurrentCANetworkEntity();

		CASingleLaneLink last = null;
		CASingleLaneLink next = null;
		boolean dsNext = false;
		boolean dsLast = false;
		while (found < range && traversed++ < cutoff) {
			pos += dir;
			if (pos == -1) {
				CASingleLaneNode n = (CASingleLaneNode) l.getUpstreamCANode();
				if (n.peekForAgent() != null) {
					found++;
				}
			} else if (pos < -1) {
				if (last == null) {
					last = (CASingleLaneLink) e.getLastCANetworkEntity();
					if (last == null) { // agent is on first link of leg
						return -1;
					}
					if (last.getDownstreamCANode() == l.getUpstreamCANode()) {
						dsLast = true;
					}
				}
				int tmp = dsLast ? last.getNumOfCells() + pos + 1 : -(pos + 2);
				if (last.getParticles()[tmp] != null) {
					found++;
				}
			} else if (pos == l.getNumOfCells()) {
				CASingleLaneNode n = (CASingleLaneNode) l.getDownstreamCANode();
				if (n.peekForAgent() != null) {
					found++;
				}
			} else if (pos > l.getNumOfCells()) {
				if (next == null) {
					Id<Link> nextId;
					if ((nextId = e.getNextLinkId()) == null) { // agent is on
																// last link of
																// leg
						return -1;

					}
					next = (CASingleLaneLink) this.net.getCALink(nextId);
					if (next.getUpstreamCANode() == l.getDownstreamCANode()) {
						dsNext = true;
					}
				}
				int ttmp = pos - l.getNumOfCells();
				int tmp = dsNext ? ttmp - 1 : next.getNumOfCells() - ttmp;
				if (next.getParticles()[tmp] != null) {
					found++;
				}
			} else {
				if (l.getParticles()[pos] != null) {
					found++;
				}
			}

		}
		// TODO Auto-generated method stub
		return traversed;
	}

	@Override
	public void report() {
		log.info("misses in this iteration: " + this.misses);

	}

}
