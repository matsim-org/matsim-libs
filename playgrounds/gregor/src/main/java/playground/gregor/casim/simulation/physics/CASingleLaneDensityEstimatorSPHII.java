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

public class CASingleLaneDensityEstimatorSPHII implements
		CADensityEstimatorKernel {

	private static final Logger log = Logger
			.getLogger(CASingleLaneDensityEstimatorSPHII.class);

	public static int H = 6;// CASingleLaneDensityEstimatorSPH.H;
	private AbstractCANetwork net;
	private int misses = 0;

	public CASingleLaneDensityEstimatorSPHII(AbstractCANetwork net) {
		this.net = net;
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

		int start = 0;
		int stop = 0;
		// if (dir == 1) {
		// start = pos - (2 * dir * H);
		// stop = pos + (2 * dir * H);
		// } else {
		// start = pos + (2 * dir * H);
		// stop = pos - (2 * dir * H);
		// }
		if (dir == 1) {
			start = pos;
			stop = pos + (2 * dir * H);
		} else {
			start = pos + (2 * dir * H);
			stop = pos;
		}
		double load = traverse(start, pos, stop, l, e);
		if (dir == 1) {
			load += traverse(start + 1, pos, stop, l, e);
		} else {
			load += traverse(start, pos, stop - 1, l, e);
		}

		return load * AbstractCANetwork.RHO_HAT;
	}

	private double traverse(int current, int pos, int stop, CASingleLaneLink l,
			CAMoveableEntity e) {

		double load = 0;
		CASingleLaneLink last = null;
		CASingleLaneLink next = null;
		boolean dsNext = false;
		boolean dsLast = false;
		while (current <= stop) {
			int d = Math.abs(current - pos);
			if (current == -1) {
				CASingleLaneNode n = (CASingleLaneNode) l.getUpstreamCANode();
				if (n.peekForAgent() != null) {
					load += bSplinesKernel(d);
				}
			} else if (current == l.getNumOfCells()) {
				CASingleLaneNode n = (CASingleLaneNode) l.getDownstreamCANode();
				if (n.peekForAgent() != null) {
					load += bSplinesKernel(d);
				}
			} else if (current < -1) {
				if (last == null) {
					last = (CASingleLaneLink) e.getLastCANetworkEntity();
					if (last == null) { // agent is on first link of leg
						return e.getRho();
					}
					if (last.getDownstreamCANode() == l.getUpstreamCANode()) {
						dsLast = true;
					}
				}
				int tmp = dsLast ? last.getNumOfCells() + current + 1
						: -(current + 2);
				if (last.getParticles()[tmp] != null) {
					load += bSplinesKernel(d);
				}

			} else if (current > l.getNumOfCells()) {
				if (next == null) {
					Id<Link> nextId;
					if ((nextId = e.getNextLinkId()) == null) { // agent is on
																// last link of
																// leg
						return e.getRho();

					}
					next = (CASingleLaneLink) this.net.getCALink(nextId);
					if (next.getUpstreamCANode() == l.getDownstreamCANode()) {
						dsNext = true;
					}
				}
				int ttmp = current - l.getNumOfCells();
				int tmp = dsNext ? ttmp - 1 : next.getNumOfCells() - ttmp;
				if (next.getParticles()[tmp] != null) {
					load += bSplinesKernel(d);
				}
			} else {
				if (l.getParticles()[current] != null) {
					load += bSplinesKernel(d);
				}
			}
			current++;
		}
		return load;
	}

	@Override
	public void report() {
		log.info("misses in this iteration: " + this.misses);

	}

	private double bSplinesKernel(final double r) {
		final double sigma = 2d / 3d; // 1d normalization
		final double v = 1d; // 1d
		final double term1 = sigma / Math.pow(H, v);
		double q = r / H;
		if (q <= 1d) {
			final double term2 = 1d - 3d / 2d * Math.pow(q, 2d) + 3d / 4d
					* Math.pow(q, 3d);
			return term1 * term2;
		} else if (q <= 2d) {
			final double term2 = 1d / 4d * Math.pow(2d - q, 3);
			return term1 * term2;
		}
		return 0;

	}

}
