/* *********************************************************************** *
 * project: org.matsim.*
 * CALinkMonitorAttachedToSim.java
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

package playground.gregor.casim.monitoring;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import playground.gregor.casim.simulation.physics.CAAgent;
import playground.gregor.casim.simulation.physics.CALinkDynamic;
import playground.gregor.casim.simulation.physics.CANetworkDynamic;
import playground.gregor.casim.simulation.physics.CASimDynamicExperiment_ZhangJ2011.Setting;
import playground.gregor.casim.simulation.physics.CASimpleDynamicAgent;

public class CALinkMonitorAttachedToSim {

	private final CALinkDynamic l;
	private final int from;
	private final int to;
	private final int range;

	private double lastTime = -1;


	private CAAgent currentFromLR;
	private CAAgent currentToLR;
	
	
	private final Map<CAAgent,Obs> obs = new HashMap<CAAgent,Obs>();
	
	
	private final List<Measurement> ms = new ArrayList<CALinkMonitorAttachedToSim.Measurement>();
	private final double cl;
	private final BufferedWriter bw;
	private final Setting s;

	public CALinkMonitorAttachedToSim(CALinkDynamic l, BufferedWriter bw, Setting s) {
		this.bw = bw;
		this.s = s;
		if (l.getLink().getLength() < 8) {
			throw new RuntimeException("Link to short");
		}
		this.l = l;
		this.cl = l.getCellLength();
		double mRange = (2/this.cl); //2 meter
		int nr = l.getNumOfCells();
		this.from = (int)(0.5+(nr-mRange)/2);
		this.to = (int)(0.5+(nr+mRange)/2);
		this.range = this.to-this.from;
//		System.out.println(this.range*this.cl);
		//		throw new RuntimeException("stop!!");
	}

	public void report() {
		Measurement last = this.ms.get(this.ms.size()-1);
		double fromTime = last.time * .5;
		
		int cnt = 0;
		Set<Double> rho = new HashSet<Double>();
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (Measurement m : this.ms) {
//			if (cnt++ < 400) {
//				continue;
//			}
			if (m.time < fromTime || m.time > (fromTime+fromTime/2)) {
				continue;
			}
			if (min > m.rhoLR) {
				min = m.rhoLR;
			}
			if (max < m.rhoLR) {
				max = m.rhoLR;
			}
		}
		
		double range = max/min;
		
//		if (range > 1.5) {
//			return;
//		}
		cnt = 0;
		for (Measurement m : this.ms) {
//			if (cnt++ < 20000 || cnt > 22000) {
//				continue;
//			if (m.time < fromTime || m.time > (fromTime+fromTime/2)) {
//				continue;
//			}
//			}
			m.calcSpd();
			if (rho.contains(m.rhoLR)) {
				continue;
			}
			rho.add(m.rhoLR);
			if (m.rhoLR > 0 || m.rhoRL > 0) {
//				System.out.println(m);
				try {
					this.bw.append(m.toString());
					this.bw.append(" " + this.s.bL + " " + this.s.bCor + " " + this.s.bEx);
					this.bw.append('\n');
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}


	public void trigger(double newTime) {

		if (newTime <= this.lastTime) {
			return;
		}
		int cntLR = 0;
		int cntRL = 0;

		int firstLR = -1;
		int lastLR = -1;
		int firstRL = -1;
		int lastRL = -1;
		
		
		List<CAAgent> lrAgs = new ArrayList<CAAgent>();
		List<Double> lrRho = new ArrayList<Double>();
		List<Double> lrZ = new ArrayList<Double>();
		List<CAAgent> rlAgs = new ArrayList<CAAgent>();
		
		//LR enter-exits
		CAAgent frLR = this.l.getParticles()[this.from];
		if (frLR != null && frLR != this.currentFromLR) {
			Obs ob = new Obs();
			ob.enterTime=this.lastTime;
			this.obs.put(frLR, ob);
		}
		this.currentFromLR = frLR;
		CAAgent toLR = this.l.getParticles()[this.to];
		if (toLR != this.currentToLR && this.currentToLR != null){
			Obs ob = this.obs.get(this.currentToLR);
			ob.exitTime = this.lastTime;
		}
		this.currentToLR = toLR;

		//agents inside measurement area
		for (int i = this.from; i < this.to; i++) {
			CAAgent part = this.l.getParticles()[i];
			if (part != null) {
				if (part.getDir() == 1) {
					lrAgs.add(part);
					lrRho.add(((CASimpleDynamicAgent)part).getMyDirectionRho());
					lrZ.add(((CASimpleDynamicAgent)part).getZ());
					cntLR++;
					if (firstLR == -1) {
						firstLR = i;
					}
					lastLR = i;
				} else {
					rlAgs.add(part);
					cntRL++;
					if (firstRL == -1) {
						firstRL = i;
					}
					lastRL = i;
				}
			}
		}

		//LR boundaries
		double rhoLR = 0;
		double rangeLR = 0;
		if (cntLR > 0 ) {
			rangeLR = getRange(firstLR,lastLR,1);
			if (rangeLR <= 0) {
//				System.err.println("err range");
				this.lastTime = newTime;
				return;
			}
			rhoLR = CANetworkDynamic.RHO_HAT*cntLR/this.range;
		}
		//RL boundaries
		

		double simRho = 0;
		double simZ = 0;
		for (double z : lrZ) {
			simZ += z/lrZ.size();
		}
		for (double r : lrRho) {
			simRho += r/lrRho.size();
			
		}
		
		Measurement m = new Measurement(this.lastTime,rhoLR, 0,rangeLR, cntLR,lrAgs,rlAgs,simRho,simZ);
		this.ms.add(m);
		this.lastTime = newTime;

	}

	private double getRange(int first, int last, int dir) {
		double rangeFrom = 0;
		double rangeTo = 0;
		int idx = this.from-1;
		boolean found = false;
		while(!found) {
			if (idx < 0) {
				return -1;
			}
			CAAgent part = this.l.getParticles()[idx];
			if (part != null) {
				if (part.getDir() == dir) {
					rangeFrom = (idx+first)/2.;
					found = true;
				}
			}
			idx--;
		}
		idx = this.to+1;
		found = false;
		while(!found) {
			if (idx >= this.l.getParticles().length) {
				return -1;
			}
			CAAgent part = this.l.getParticles()[idx];
			if (part != null) {
				if (part.getDir() == dir) {
					rangeTo = (idx+last)/2.;
					found = true;
				}
			}
			idx--;
		}

		return rangeTo-rangeFrom;
	}


	private final class Measurement {

		private final double time;
		private final double rhoLR;
		private final double rhoRL;
		private final double rangeLR;
		private final double cntLR;
		private final List<CAAgent> lrAgs;
		private final List<CAAgent> rlAgs;
		private double spdRL;
		private double spdLR;
		private final double simRho;
		private final double simZ;

		public Measurement(double time, double rhoLR, double rhoRL,double rangeLR, double cntLR, List<CAAgent> lrAgs, List<CAAgent> rlAgs, double simRho,double simZ) {
			this.time = time;
			this.rhoLR = rhoLR;
			this.rhoRL = rhoRL;
			this.rangeLR = rangeLR;
			this.cntLR = cntLR;
			this.lrAgs = lrAgs;
			this.rlAgs = rlAgs;
			this.simRho = simRho;
			this.simZ = simZ;
		}

		public void calcSpd() {
			//TODO chk for bias!
			double ttSumLR = 0;
			for (CAAgent a : this.lrAgs) {
				Obs ob = CALinkMonitorAttachedToSim.this.obs.get(a);
				ttSumLR += ob.exitTime-ob.enterTime;
			}
//			ttSumLR /= this.lrAgs.size();
			this.spdLR = this.lrAgs.size() * CALinkMonitorAttachedToSim.this.range * CALinkMonitorAttachedToSim.this.cl / ttSumLR;
			
		}

		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append(this.time);
			buf.append(' ');
			buf.append(this.rhoLR);
			buf.append(' ');
			buf.append(this.rhoRL);
			buf.append(' ');
			buf.append(this.rangeLR);
			buf.append(' ');
			buf.append(this.cntLR);
			buf.append(' ');
			buf.append(this.spdLR);
			buf.append(' ');
			buf.append(this.spdLR*this.rhoLR);
			buf.append(' ');
			buf.append(this.simRho);
			buf.append(' ');
			buf.append(this.simZ);
			return buf.toString();
		}
	}

	private static final class Obs {
		double enterTime;
		double exitTime = -1;
	}
}
