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
import org.matsim.core.gbl.Gbl;

public class CAMultiLaneDensityEstimatorSPHII implements
		CADensityEstimatorKernel, MultiLaneDensityEstimator {
	private static Logger log = Logger
			.getLogger(CAMultiLaneDensityEstimatorSPHII.class);
	private final int misses = 0;
	private final AbstractCANetwork net;
	public static final int H = 6;
	
	public static boolean ACTIVATED = false;
	
	private static int logWarnCnt = 0;

	public CAMultiLaneDensityEstimatorSPHII(AbstractCANetwork net) {
		this.net = net;
	}
	
	@Override
	public double estRho(CAMoveableEntity e) {
		
		
		//Inefficient but fool proof version 
		CAMoveableEntity [] parts = new CAMoveableEntity[2*H];
		
		CANetworkEntity ett = e.getCurrentCANetworkEntity();
		int lane = e.getLane();
		if (ett instanceof CAMultiLaneNode) {
			collectNode(e,(CAMultiLaneNode)ett,parts,0,lane,ett.getNrLanes());
		} else {
			int pos = e.getPos();
			int dir = e.getDir();
			collectLink(e, (CAMultiLaneLink)ett, parts, dir, pos, 0,lane, ett.getNrLanes());
		}
		double b = 0;		
		if (parts[0] != null) {
			b = bSplinesKernel(0);
		}
		for (int idx = 2; idx < parts.length; idx++) {
			if (parts[idx] != null) {
				b += 2*bSplinesKernel(idx-1);
			}
		}
		
		double rho = b * AbstractCANetwork.RHO_HAT;
		return rho;
	}

	private void collectNode(CAMoveableEntity e, CAMultiLaneNode ett, CAMoveableEntity[] parts,
			int idx, int intendedLane, int lastNrLanes) {
		
		intendedLane = CAMultiLaneLink.getIntendedLane(intendedLane, lastNrLanes, ett.getNrLanes());
		parts[idx++] =  ett.peekForAgentInSlot(intendedLane);
		
		if (idx == parts.length) {
			return;
		}
		
//		if (++logWarnCnt < 100) {
//			log.warn("current implementation works only for equal link width networks");
//			if (logWarnCnt == 99) {
//				log.warn(Gbl.FUTURE_SUPPRESSED);
//			}
//		}
		Id<Link> nextId = e.getNextLinkId();
		if (nextId == null) {
			return;
		}
		CAMultiLaneLink next = (CAMultiLaneLink) this.net
				.getCALink(nextId);
		CANode nn = next.getUpstreamCANode();
		int nextDir;
		int nextIdx;
		if (ett == nn) {
			nextDir = 1;
			nextIdx = 0;
		} else {
			nextDir = -1;
			nextIdx = next.getSize()-1;
		}
		collectLink(e,next,parts,nextDir, nextIdx,idx, intendedLane,ett.getNrLanes());
	}

	private void collectLink(CAMoveableEntity e, CAMultiLaneLink link,
			CAMoveableEntity[] parts, int dir, int linkIdx, int idx, int intendedLane, int lastNrLanes) {
		if (link.getSize() < parts.length && logWarnCnt++ < 10) {
			log.warn("length of link " + link + " is shorter than kernel band width!");
			if (logWarnCnt == 9) {
				log.warn(Gbl.FUTURE_SUPPRESSED);
			}
//			return;
		}
		intendedLane = CAMultiLaneLink.getIntendedLane(intendedLane, lastNrLanes, link.getNrLanes());
		CAMoveableEntity[] linkParts = link.getParticles(intendedLane);
		int stopIdx = dir == 1 ? linkParts.length : -1;
		while (linkIdx != stopIdx && idx < parts.length) {
			parts[idx++] = linkParts[linkIdx];
			linkIdx += dir;
		}
		if (idx < parts.length) {
			CAMultiLaneNode n;
			if (dir == 1) {
				n = (CAMultiLaneNode) link.getDownstreamCANode();
			} else {
				n = (CAMultiLaneNode) link.getUpstreamCANode();
			}
			collectNode(e, n, parts, idx, intendedLane,link.getNrLanes());
		}
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

	@Override
	public void report() {
		log.info("misses in this iteration: " + this.misses);
	}

}
