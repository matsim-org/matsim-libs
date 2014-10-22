/* *********************************************************************** *
 * project: org.matsim.*
 * CANode.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNode;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;

public class CANodeStatic implements NetsimNode, CANode{
	
	private static final Logger log = Logger.getLogger(CANodeStatic.class);
	
	
	//Floetteroed Laemmel parameters
	private static final double alpha = 0;
	private static final double beta = 0.12;
	private static final double gamma = 1.38;

	private double vHat = 1.29; //cells per unit time
	private final double rhoHat = 6.661; //particles per meter
//	private double D = 0.72;
	private double D = 0.27;
	private final double z;

	private CAAgent agent;
	private final Node node;
	private final CANetwork net;

	private final List<CALink> links = new ArrayList<CALink>();
	private double agentLeft;

	public CANodeStatic(Node node, CANetwork net){
		double cap = 36;//dsl.getCapacity();
		double tmp = Math.pow(CANetwork.RHO, gamma);
		this.D = alpha+beta*tmp;
//		for (net.get)
		
//		if (node.getId().toString().equals("3")) {
//			cap=2.5;
//		}
		this.D /= cap;
		this.vHat *= cap;
//		this.rhoHat *= cap;
//		this.D /= cap;
		this.node = node;
		this.net = net;
		this.z = this.D+1/(this.vHat*this.rhoHat);
	}

	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CANode#addLink(playground.gregor.casim.simulation.physics.CALink)
	 */
	@Override
	public void addLink(CALink link) {
		this.links.add(link);
		
		//		//DEBUG
		//		System.out.println("DEBUG");
		//		if (this.node.getId().toString().equals("3")) {
		//			System.out.println("DEBUG");	
		//		}
	}


	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CANode#putAgent(playground.gregor.casim.simulation.physics.CAAgent)
	 */
	@Override
	public void putAgent(CAAgent a) {
		if (this.agent != null) {
			throw new RuntimeException("There is already an agent on node:" + this.node.getId());

		}
		this.agent = a;
	}
	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CANode#peekForAgent()
	 */
	@Override
	public CAAgent peekForAgent() {
		return this.agent;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CANode#pollAgent(double)
	 */
	@Override
	public CAAgent pollAgent(double time) {
		CAAgent a = this.agent;
		this.agent = null;
		this.agentLeft = time;
		return a;
	}
	
	public double getTime() {
//		return this.agentLeft;
		throw new RuntimeException("this method must return last enter time and not exit time!");
	
	}
	
	@Override
	public void handleEvent(CAEvent e) {
		CAAgent a = e.getCAAgent();
		double time = e.getEventExcexutionTime();
		if (e.getCAEventType() == CAEventType.SWAP){
			handelSwap(a,time);
		} else if (e.getCAEventType() == CAEventType.TTA){
			handleTTA(a,time);
		} else {
			throw new RuntimeException("Unknown event type: " + e.getCAEventType());
		}
	}

	private void handleTTA(CAAgent a, double time) {

		Id nextLinkId = a.getNextLinkId();
		CALink nextLink = this.net.getCALink(nextLinkId);
		int nextIdx;
		int dir;
		if (nextLink.getDownstreamCANode() == this) {
			nextIdx = nextLink.getNumOfCells()-1;
			dir = -1;
		} else if (nextLink.getUpstreamCANode() == this) {
			nextIdx = 0;
			dir = 1;
		} else {
			throw new RuntimeException("Inconsitent network or agent plan!");
		}

		if (nextLink.getParticles()[nextIdx] == null) {
			if (nextLink.getTimes()[nextIdx] > time) {
				CAEvent e = new CAEvent(nextLink.getTimes()[nextIdx], a, this, CAEventType.TTA);
				this.net.pushEvent(e);
			}else {
//				this.pollAgent(time+ this.z + 1/(this.vHat*this.rhoHat));
				this.pollAgent(time+ this.z);
				a.materialize(nextIdx, dir);
				if (dir == -1) {
					nextLink.fireDownstreamEntered(a, time);
				} else {
					nextLink.fireUpstreamEntered(a, time);
				}
				a.moveOverNode(nextLink,time);

				nextLink.getParticles()[nextIdx] = a;
				int nextNextIdx = nextIdx+dir;
				if (nextLink.getParticles()[nextNextIdx] == null) {
					CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, nextLink, CAEventType.TTA);
					this.net.pushEvent(e);				
				} else if (nextLink.getParticles()[nextNextIdx].getDir() != dir){
					CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, nextLink, CAEventType.SWAP);
					this.net.pushEvent(e);				
				}
				triggerPrevAgent(time);
			}

		} 



	}

	private void triggerPrevAgent(double time) {

		double cap = 0;
		List<Tuple<CALink,CAAgent>> cands = new ArrayList<Tuple<CALink,CAAgent>>();
		for (CALink l : this.links) {
			if (l.getDownstreamCANode() == this) {
				if (l.getParticles()[l.getNumOfCells()-1] != null && l.getParticles()[l.getNumOfCells()-1].getDir() == 1) {
					cands.add(new Tuple<CALink,CAAgent>(l,l.getParticles()[l.getNumOfCells()-1]));
					cap += l.getLink().getCapacity();
				}
			} else if (l.getUpstreamCANode() == this) {
				if (l.getParticles()[0] != null && l.getParticles()[0].getDir() == -1) {
					cands.add(new Tuple<CALink,CAAgent>(l,l.getParticles()[0]));
					cap += l.getLink().getCapacity();
				}
			}
		}
		if (cands.size() == 0) {
			return;
		}

		double rnd = cap*MatsimRandom.getRandom().nextDouble();
		double incr = cap/cands.size();
		double comp = incr;
		for (Tuple<CALink, CAAgent> t : cands) {
			if (rnd <= comp) {
//				CAEvent e = new CAEvent(time + this.z +  1/(this.vHat*this.rhoHat), t.getSecond(), t.getFirst(), CAEventType.TTA);
				CAEvent e = new CAEvent(time + this.z, t.getSecond(), t.getFirst(), CAEventType.TTA);
				this.net.pushEvent(e);
				return;
			}
			comp += incr;
		}

		throw new RuntimeException("should be unreachable!");
	}





	private void handelSwap(CAAgent a, double time) {
		Id nextLinkId = a.getNextLinkId();
		CALink nextLink = this.net.getCALink(nextLinkId);
		int nextIdx;
		int dir;
		if (nextLink.getDownstreamCANode() == this) {
			nextIdx = nextLink.getNumOfCells()-1;
			dir = -1;
		} else if (nextLink.getUpstreamCANode() == this) {
			nextIdx = 0;
			dir = 1;
		} else {
			throw new RuntimeException("Inconsitent network or agent plan!");
		}

		//enter link
		//leave link
		CAAgent swapA = nextLink.getParticles()[nextIdx];
		this.pollAgent(0);
		this.putAgent(swapA);
		nextLink.getParticles()[nextIdx] = a;
		a.materialize(nextIdx, dir);
		if (dir == -1) {
			nextLink.fireDownstreamLeft(swapA, time);
			nextLink.fireDownstreamEntered(a, time);
		} else {
			nextLink.fireUpstreamLeft(swapA, time);
			nextLink.fireUpstreamEntered(a, time);
		}

		a.moveOverNode(nextLink,time);

		int nextNextA = nextIdx+dir;
		if (nextLink.getParticles()[nextNextA] == null) {
			CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, nextLink, CAEventType.TTA);
			this.net.pushEvent(e);			
		} else if (nextLink.getParticles()[nextNextA].getDir() != dir) {
			CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, nextLink, CAEventType.SWAP);
			this.net.pushEvent(e);			
		}

		int nextNextSwapA;
		Id nextSwapALinkId = swapA.getNextLinkId();
		CALink nextSwapALink = this.net.getCALink(nextSwapALinkId);
		int nextSwapADir;
		if (nextSwapALink.getDownstreamCANode() == this) {
			nextNextSwapA = nextSwapALink.getNumOfCells() -1;
			nextSwapADir = -1;
		} else if  (nextSwapALink.getUpstreamCANode() == this) {
			nextNextSwapA = 0;
			nextSwapADir = 1;
		} else {
			log.warn("inconsitent network, agent:" + a + " becomes stuck!");
			return;
//			throw new RuntimeException("inconsisten network!");
		}
		if (nextSwapALink.getParticles()[nextNextSwapA] == null) {
			CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), swapA,this, CAEventType.TTA);
			this.net.pushEvent(e);
		} else if (nextSwapALink.getParticles()[nextNextSwapA].getDir() != nextSwapADir) {
			CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), swapA,this, CAEventType.SWAP);
			this.net.pushEvent(e);		
		}



	}

	@Override
	public Node getNode() {
		return this.node;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<CALinkDynamic> getLinks() {
		throw new RuntimeException("won't be implemented!");
	}

}
