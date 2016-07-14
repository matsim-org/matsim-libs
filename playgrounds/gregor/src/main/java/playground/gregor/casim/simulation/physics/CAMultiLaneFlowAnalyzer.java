/* *********************************************************************** *
 * project: org.matsim.*
 * CAMultiLaneFlowAnalyzer.java
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
import org.matsim.core.gbl.Gbl;

public class CAMultiLaneFlowAnalyzer {

	private static final Logger log = Logger.getLogger(CAMultiLaneFlowAnalyzer.class);
	
	public static int H = 24;
	
	private final AbstractCANetwork net;

	private static int logWarnCnt = 0;
	
	private final double [][] cnt = new double[2][8];
	private final double [][] spds = new double[2][8];

	public CAMultiLaneFlowAnalyzer(AbstractCANetwork net) {
		this.net = net;
	}
	
	public double updateAndGetLaneSpeed(CAMoveableEntity e,final int lane) {
		
		//Inefficient but fool proof version 
		CAMoveableEntity [] parts = new CAMoveableEntity[2*H+1];
		int dir = e.getDir();
		CANetworkEntity ett = e.getCurrentCANetworkEntity();
		if (ett instanceof CAMultiLaneNode) {
			collectNode(e,(CAMultiLaneNode)ett,parts,0,lane);
		} else {
			int pos = e.getPos();
			
			collectLink(e, (CAMultiLaneLink)ett, parts, dir, pos, lane, 0);
		}
		
//		if (parts[1] != null) {
////			throw new RuntimeException("fix me!");
//			System.err.println("fix me!");
//		}
		double b1 = bSplinesKernel(0);
		double b2 = 0;
//		if (parts[0] != null) {
//			if (parts[0].getDir() == dir ) {
//				b1 = bSplinesKernel(0);
//			} else {
//				b2 = bSplinesKernel(0);	
//			}
//		}
		for (int idx = 2; idx < parts.length-1; idx++) {
			if (parts[idx] != null) {
				if (parts[idx].getDir() == dir ) {
					b1 += 2*bSplinesKernel(idx-1);
				} else {
					b2 += 2*bSplinesKernel(idx-1);	
				}				
			}
		}
//		if (b1 == 0 && b2 == 0) {
//			System.out.println("Gotcha");
//		}
//		b1 += 2*bSplinesKernel(parts.length-1);
		
		double rho1 = b1 * AbstractCANetwork.RHO_HAT;
		double rho2 = b2 * AbstractCANetwork.RHO_HAT;
		double q = getMyFlow(rho1, rho2);
		double specFlow = q/AbstractCANetwork.PED_WIDTH;
		double spd =  specFlow/rho1;
		int dirIdx = (dir+1)/2;
		if (dirIdx < 0 || dirIdx > 1) {
			if (e.getId().toString().startsWith("g")) {
				dirIdx = 1;
			} else {
				dirIdx = 0;
			}
		}
		double msaSpd = this.spds[dirIdx][lane] * this.cnt[dirIdx][lane]/(1.+this.cnt[dirIdx][lane]) + spd*1/(1+this.cnt[dirIdx][lane]);
//		double msaSpd = this.spds[dirIdx][lane] *0.99 + spd*0.01;
		this.spds[dirIdx][lane] = msaSpd;
		this.cnt[dirIdx][lane]++;
		
		
		if (this.cnt[dirIdx][lane] % 10000 == 0) {
			System.out.println(this.cnt[dirIdx][lane]);
			for (int i = 0; i < 8; i++) {
				System.out.print(((int)(100*this.spds[0][i]+0.5))/100. + "\t");
			}
			if (dirIdx == 0) {
				System.out.print("*");
			}
			System.out.println();
			for (int i = 0; i < 8; i++) {
				System.out.print(((int)(100*this.spds[1][i]+0.5))/100. + "\t");
			}
			if (dirIdx == 1) {
				System.out.print("*");
			}
			System.out.println();
			System.out.println("--------------------------------------------");
		}
		if (lane == e.getLane()) {
			return msaSpd+0.1;
		}
		return msaSpd;
	}
	
	private void collectNode(CAMoveableEntity e, CAMultiLaneNode ett, CAMoveableEntity[] parts,
			int idx, int lane) {
		parts[idx++] =  ett.peekForAgentInSlot(lane);
		
		if (idx == parts.length) {
			return;
		}
		
		if (++logWarnCnt  < 100) {
			log.warn("current implementation works only for equal link width networks");
			if (logWarnCnt == 99) {
				log.warn(Gbl.FUTURE_SUPPRESSED);
			}
		}
		Id<Link> nextId = e.getNextLinkId();
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
		collectLink(e,next,parts,nextDir, nextIdx,lane,idx);
	}

	private void collectLink(CAMoveableEntity e, CAMultiLaneLink link,
			CAMoveableEntity[] parts, int dir, int linkIdx, int lane,int idx) {
		if (link.getSize() < parts.length) {
			throw new RuntimeException("length of link " + link + " is shorter than kernel band width!");
		}
		CAMoveableEntity[] linkParts = link.getParticles(lane);
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
			collectNode(e, n, parts, idx, lane);
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
	
	private double getMyFlow(double myRho, double theirRho) {
		final double tmp = Math.pow((myRho+theirRho) * AbstractCANetwork.PED_WIDTH,
				AbstractCANetwork.GAMMA);
		final double D = AbstractCANetwork.ALPHA + AbstractCANetwork.BETA * tmp;
		

		double rho_i, rho_j;
		boolean swap;
		if (myRho < theirRho) {
			rho_i = theirRho*AbstractCANetwork.PED_WIDTH;
			rho_j = myRho*AbstractCANetwork.PED_WIDTH;
			swap = true;
		} else {
			rho_i = myRho*AbstractCANetwork.PED_WIDTH;
			rho_j = theirRho*AbstractCANetwork.PED_WIDTH;
			swap = false;
		}
		
		double rhoHat = AbstractCANetwork.RHO_HAT*AbstractCANetwork.PED_WIDTH;
		double vHat = AbstractCANetwork.V_HAT;
		
		double q_i;
		double q_j;
		double tmp2 = rho_i *(2+D*vHat*rhoHat)/(D*vHat*rhoHat) - 1/(D*vHat);
		if (rho_j >= tmp2) { //SS regime
			double coeff = vHat /(1+D*vHat*(rho_i+rho_j));
			q_i = coeff * rho_i * (1+ D*vHat*(rho_i-rho_j));
			q_j = coeff * rho_j * (1+ D*vHat*(rho_j-rho_i));
		} else { //RS regime
			double coeff = vHat/(1+D*vHat*rhoHat);
			q_i = coeff * (rhoHat-rho_i);
			q_j = coeff * rho_j;
		}
		
		return swap ? q_j : q_i;
//		return q_j + q_i;
		
	}
	
}
