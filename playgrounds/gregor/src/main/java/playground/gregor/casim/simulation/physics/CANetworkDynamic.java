/* *********************************************************************** *
 * project: org.matsim.*
 * CANetwork.java
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;

import playground.gregor.casim.monitoring.CALinkMonitorExact;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.debug.RectEvent;


public class CANetworkDynamic {


	public static double RHO = 1;


	//Floetteroed Laemmel parameters
	public static final double RHO_HAT = 6.69;
	public static final double V_HAT = 1.27;

	public static final double ALPHA = 0.;
	public static final double BETA = 0.39;
	public static final double GAMMA = 1.43;

	public static final double PED_WIDTH = .61;
	//Laemmel Floetteroed constants
	/*package*/ static int LOOK_AHEAD = 7;
	/*package*/ static final int MX_TRAVERSE = 20;

	public static int H = 13;
	private static final int CUTTOFF_DIST = 2*H;
	private static final boolean USE_SPH = false ;


	private static final Logger log = Logger.getLogger(CANetworkDynamic.class);

	private final PriorityQueue<CAEvent> events = new PriorityQueue<CAEvent>();
	private final Network net;

	private final Map<Id<Node>,CANode> caNodes = new HashMap<Id<Node>,CANode>();
	private final Map<Id<Link>,CALink> caLinks = new HashMap<Id<Link>,CALink>();
	private final EventsManager em;

	private double globalTime = 0;

	private CALinkMonitorExact monitor;

	private Set<CAAgent> agents = new HashSet<CAAgent>();

	private static int EXP_WARN_CNT;

	public CANetworkDynamic(Network net, EventsManager em) {
		this.net = net;
		this.em = em;
		init();
	}


	private void init() {
		for (Node n : this.net.getNodes().values()) {
			CANodeDynamic caNode = new CANodeDynamic(n, this);
			this.caNodes.put(n.getId(), caNode);
		}
		for (Link l : this.net.getLinks().values()) {
			CANodeDynamic us = (CANodeDynamic) this.caNodes.get(l.getFromNode().getId());
			CANodeDynamic ds = (CANodeDynamic) this.caNodes.get(l.getToNode().getId());
			Link rev = null;
			for (Link ll : l.getToNode().getOutLinks().values()) {
				if (ll.getToNode() == l.getFromNode()) {
					rev = ll;
				}
			}
			if (rev != null) {
				CALink revCA = this.caLinks.get(rev.getId());
				if (revCA != null){
					this.caLinks.put(l.getId(), revCA);
					continue;
				}
			}
			CALinkDynamic caL = new CALinkDynamic(l,rev, ds, us, this);
			us.addLink(caL);
			ds.addLink(caL);
			this.caLinks.put(l.getId(), caL);
		}

	}

	/*package*/ void registerAgent(CAAgent a) {
		if (!this.agents.add(a)) {
			throw new RuntimeException("Agent: " + a + " has already been registered!");
		}
	}

	/*package*/ void unregisterAgent(CAAgent a) {
		if (!this.agents.remove(a)){
			throw new RuntimeException("Could not unregister agent: " + a + "!");
		}
	}



	public double getRho(CAAgent a) {
		return a.getAgentInfo().getRho();
	}


	/*package*/ void updateRho() {

		for (CAAgent a : this.agents){
			double rho = (estRho(a)+a.getAgentInfo().getRho())/2;
			a.getAgentInfo().setRho(rho);
		}

	}

	public double estRho(CAAgent a) {
		CALink current = a.getCurrentLink();
		CAAgent[] currentParts = current.getParticles();
		int pos = a.getPos();
		int dir = a.getDir();
		int [] spacings;// = new int []{0,0};
		if (currentParts[pos] != a) {
			//agent on node
			//			log.warn("not yet implemented!!");
			return a.getAgentInfo().getRho();
		} else {

			double rho = bSplinesKernel(0);
			spacings = new int[]{0,0};
			rho+=traverseLink(currentParts, dir, pos+dir, spacings,rho);
			//check next node

			if (USE_SPH && spacings[1] < CUTTOFF_DIST || !USE_SPH && spacings[0] < LOOK_AHEAD && spacings[1] < MX_TRAVERSE){
				//			if (spacings[1] < MX_TRAVERSE){
				CANode n;
				if (dir == 1) {
					n = current.getDownstreamCANode();
				} else {
					n = current.getUpstreamCANode();
				}
				spacings[1]++;
				if (n.peekForAgent() != null) {
					spacings[0]++;
				}
				//check next link(s)? TODO it would make sense to check all outgoing links not only the next one ...
				if (USE_SPH && spacings[1] < CUTTOFF_DIST || !USE_SPH && spacings[0] < LOOK_AHEAD && spacings[1] < MX_TRAVERSE){
					//				if (spacings[1] < MX_TRAVERSE){
					CALink next = this.caLinks.get(a.getNextLinkId());
					CANode nn = next.getUpstreamCANode();
					int nextDir;
					int nextPos;
					CAAgent[] nextParts = next.getParticles();
					if (n == nn) {
						nextDir = 1;
						nextPos = 0;
					} else {
						nextDir = -1;
						nextPos = nextParts.length-1;
					}
					rho += traverseLink(nextParts,nextDir,nextPos,spacings,rho);
				}
			} 
			double coeff = (double)spacings[0]/(double)spacings[1];
			double cmp = RHO_HAT*coeff;
			//			rho *= RHO_HAT;
			if (USE_SPH) {
				return rho;
			} else {
				return cmp;
			}
		}
	}

	private double traverseLink(CAAgent[] parts, int dir, int idx,int[] spacings, double rho) {
		if (idx < 0 || idx >= parts.length) {
			return rho;
		}
		int toMx = dir == -1 ? 0 : parts.length-1;
		for (;idx != toMx; idx += dir) {
			spacings[1]++;
			if (parts[idx] != null) {
				spacings[0]++;
				rho += bSplinesKernel(spacings[1]);
				if (!USE_SPH && spacings[0] >= LOOK_AHEAD) {
					return rho;
				}
			}
			if (USE_SPH && spacings[1] >= CUTTOFF_DIST || !USE_SPH && spacings[1] >= MX_TRAVERSE) {
				return rho;
			}
		}
		return rho;
	}


	/*package*/ void run() {
		this.globalTime = this.events.peek().getEventExcexutionTime();
		double simTime = 0;
		updateRho();
		//		draw2();
		while (this.events.size() > 0) {
			CAEvent e = this.events.poll();
			if (this.monitor != null){
				this.monitor.trigger(e.getEventExcexutionTime());
			}

			if (e.getEventExcexutionTime() > simTime+1) {
				this.globalTime = e.getEventExcexutionTime();
				//				draw2();
				updateRho();
				simTime = e.getEventExcexutionTime();
			}

			//			log.info("==> " + e);

			if (e.isObsolete()){
				if (EXP_WARN_CNT++ < 10 ) {
					log.info("dropping obsolete event: " + e);
					if (EXP_WARN_CNT == 10) {
						log.info(Gbl.FUTURE_SUPPRESSED);
					}
				}
				continue;
			}


			if (e.isObsolete()){
				log.info("dropping obsolete event: " + e);
				continue;
			}


			e.getCANetworkEntity().handleEvent(e);

			if (CASimDynamicExperiment_ZhangJ2011.VIS && e.getEventExcexutionTime() > this.globalTime+0.04) {
				//				updateDensity();
				draw2();
				this.globalTime = e.getEventExcexutionTime();
			}
		}
	}

	private void draw2() {
		for (CALink l : this.caLinks.values()) {
			double dx = l.getLink().getToNode().getCoord().getX()-l.getLink().getFromNode().getCoord().getX();
			double dy = l.getLink().getToNode().getCoord().getY()-l.getLink().getFromNode().getCoord().getY();
			double length = Math.sqrt(dx*dx+dy*dy);
			dx /= length;
			dy /= length;
			double ldx = dx;
			double ldy = dy;
			double incr = l.getLink().getLength()/l.getNumOfCells();
			dx *= incr;
			dy *= incr;
			double width =l.getLink().getCapacity();
			double x = l.getLink().getFromNode().getCoord().getX();//+dx/2;
			double y = l.getLink().getFromNode().getCoord().getY();//+dy/2;
			for (int i = 0; i < l.getNumOfCells(); i++) {
				if (l.getParticles()[i]!= null) {
					if (l.getParticles()[i].getId().toString().equals("g272")){
						//						log.info(l.getParticles()[i]);
					}
					double ddx = 1;
					if (l.getParticles()[i].getDir() == -1) {
						ddx = -1;
					};
					XYVxVyEventImpl e = new XYVxVyEventImpl(l.getParticles()[i].getId(), x+dx/2, y+dy/2, ldx*ddx, ldy*ddx, this.globalTime);

					this.em.processEvent(e);
					//					System.out.println(l.getParticles()[i]);
				} else {
					RectEvent e = new RectEvent(this.globalTime, x, y+width/2, dx, width, false);
					this.em.processEvent(e);
				}
				x+=dx;
				y+=dy;
			}
		}
		for (CANode n : this.caNodes.values()) {
			if (n.peekForAgent() != null) {
				double x = n.getNode().getCoord().getX();
				double y = n.getNode().getCoord().getY();
				XYVxVyEventImpl e = new XYVxVyEventImpl(n.peekForAgent().getId(), x, y, 0, 0, this.globalTime);
				this.em.processEvent(e);
			}
		}
	}

	public void pushEvent(CAEvent event) {
		event.getCAAgent().setCurrentEvent(event);
		this.events.add(event);
	}

	public CAEvent pollEvent() {
		return this.events.poll();
	}

	public CAEvent peekEvent() {
		return this.events.peek();
	}

	public CALink getCALink(Id<Link> nextLinkId) {
		return this.caLinks.get(nextLinkId);
	}


	public EventsManager getEventsManager() {
		return this.em;
	}

	public void addMonitor(CALinkMonitorExact m) {
		this.monitor = m;
	}

	private double bSplinesKernel(double r) {
//		r = H-r;
		final double v = r/H;
		final double hCube = Math.pow(H, 3);
		if (v < 0) {
			return 0;
		}
		if (v <= 1) {
			final double term1 = 3./(4.*Math.PI*Math.pow(H, 3.));
			//			final double term1 = 1./6.;
			final double term2 = 10./3.-7*Math.pow(v, 2)+4*Math.pow(v, 3.);
			//			final double term2 = 3*Math.pow(v, 3) - 6*Math.pow(v, 2) + 4;
			return term1*term2*hCube;
		} else if (v <= 2) {
			final double term1 = 3./(4.*Math.PI*Math.pow(H, 3.));
			//			final double term1 = 1./6.;
			final double term2 = Math.pow(2-v,2)*((5.-4.*v)/3.);
			//			final double term2 = - Math.pow(v, 3) + 6.*Math.pow(v, 2) - 12*v +8;
			return term1*term2*hCube;
		}

		return 0.;
	}
}
