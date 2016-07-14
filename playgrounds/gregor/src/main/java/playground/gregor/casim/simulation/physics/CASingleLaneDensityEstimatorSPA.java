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

public class CASingleLaneDensityEstimatorSPA implements
		CADensityEstimatorKernel {
	private static Logger log = Logger
			.getLogger(CASingleLaneDensityEstimatorSPA.class);
	public static int RANGE = 3;
	private int cutoff;
	private int misses = 0;
	private AbstractCANetwork net;

	public CASingleLaneDensityEstimatorSPA(AbstractCANetwork net) {
		this.net = net;
		this.cutoff = (int) Math.ceil(AbstractCANetwork.RHO_HAT * RANGE);
	}

	@Override
	public double estRho(CAMoveableEntity e) {
		CANetworkEntity ett = e.getCurrentCANetworkEntity();
		if (ett instanceof CASingleLaneNode) {
			return e.getRho();
		}
		CASingleLaneLink l = (CASingleLaneLink) ett;
		CAMoveableEntity[] parts = l.getParticles();

		int pos = e.getPos();
		int dir = e.getDir();
		if (parts[pos] != e) {
			// agent on link?
			misses++;
			return e.getRho();
		}

		double[] spacings = { 0., 0. };
		traverseLink(parts, dir, pos + dir, spacings);
		if (spacings[0] < RANGE && spacings[1] < cutoff) {
			CASingleLaneNode n;
			if (dir == 1) {
				n = (CASingleLaneNode) l.getDownstreamCANode();
			} else {
				n = (CASingleLaneNode) l.getUpstreamCANode();
			}
			spacings[1]++;
			if (n.peekForAgent() != null) {
				spacings[0]++;
			}
			Id<Link> nextId = e.getNextLinkId();
			if (spacings[0] < RANGE && spacings[1] < cutoff
					&& (nextId = e.getNextLinkId()) != null) {
				CASingleLaneLink next = (CASingleLaneLink) this.net
						.getCALink(nextId);
				CAMoveableEntity[] nextParts = next.getParticles();
				CANode nn = next.getUpstreamCANode();
				int nextDir;
				int nextPos;
				if (n == nn) {
					nextDir = 1;
					nextPos = 0;
				} else {
					nextDir = -1;
					nextPos = nextParts.length - 1;
				}
				traverseLink(nextParts, nextDir, nextPos, spacings);
			}
		}

		return AbstractCANetwork.RHO_HAT * spacings[0] / spacings[1];
	}

	private void traverseLink(CAMoveableEntity[] parts, int dir, int idx,
			double[] spacings) {
		int toMx = dir == -1 ? 0 : parts.length - 1;
		if (idx - dir == toMx) {
			return;
		}
		for (; idx != toMx; idx += dir) {
			spacings[1]++;
			if (parts[idx] != null) {
				spacings[0]++;
				if (spacings[0] >= RANGE) {
					return;
				}
			}
			if (spacings[1] >= cutoff) {
				return;
			}
		}

	}

	@Override
	public void report() {
		log.info("misses in this iteration: " + this.misses);
	}
}
