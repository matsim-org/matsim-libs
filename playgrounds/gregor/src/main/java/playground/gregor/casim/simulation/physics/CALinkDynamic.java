/* *********************************************************************** *
 * project: org.matsim.*
 * CALink.java
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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;

import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;


public class CALinkDynamic implements CANetworkEntity, CALink{

	private static final Logger log = Logger.getLogger(CALinkDynamic.class);


	private final Link dsl;
	private final Link usl;

	//Particles and dsLeftTimes are backed by arrays, thus for every cell on a link it consumes an additional 2*64bit. 
	//The number of cells are in the range of RHO_HAT*length*width. For a 100m Link with 10m with this makes roughly
	//7000 cells ~ 100KB for a network of 10k links this corresponds to about 1GB.
	//So, for large networks it would make sense to replace particles[] by a linked list and dsLeftTimes[] by something like a HashMap or 
	//we put lastLeftTimes directly in a queue exclusive for the succeeding agent (as long as one exist) ... need to think about this [GL Oct '14] 
	private final CAMoveableEntity [] particles;
	private final double [] lastLeftTimes;
	
	private final int size;

	private final CANodeDynamic ds;

	private final CANodeDynamic us;

	private final CANetworkDynamic net;


	private final double cellLength;

	private double width;

	private final double tFree;

	private double ratio;
	private final double epsilon;

	public CALinkDynamic(Link dsl, Link usl, CANodeDynamic ds, CANodeDynamic us, CANetworkDynamic net) {

		
		
		this.width = dsl.getCapacity();//TODO this is a misuse of the link's capacity attribute. Needs to be fixed!
		this.ratio = CANetworkDynamic.PED_WIDTH/this.width;
		this.cellLength = this.ratio/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.PED_WIDTH);
		this.dsl = dsl;
		this.usl = usl;
		this.size = (int) (0.5+dsl.getLength()/this.cellLength);
		this.particles = new CAMoveableEntity[this.size];
		this.lastLeftTimes = new double[this.size];
		this.ds = ds;
		this.us = us;
		this.tFree = this.cellLength/CANetworkDynamic.V_HAT;
		this.epsilon = tFree/1000;
		this.net = net;
	}

	//bottleneck experiment
	public void changeWidth(double width) {
		this.width = width; 
		this.ratio = CANetworkDynamic.PED_WIDTH/this.width;
	}

	/*package*/ double getD(CAMoveableEntity a) {
		final double rho = a.getRho();
		final double tmp = Math.pow(rho*CANetworkDynamic.PED_WIDTH, CANetworkDynamic.GAMMA);
		final double d = CANetworkDynamic.ALPHA + CANetworkDynamic.BETA*tmp;
		return d;
	}
	
	/*package*/ double getZ(CAMoveableEntity a) {
		double d = getD(a);
		double z = 1/(CANetworkDynamic.RHO_HAT+CANetworkDynamic.V_HAT)+d;
//		double z = this.tFree + d;
		return z;
	}

	@Override
	public void handleEvent(CAEvent e) {
		CAMoveableEntity a = e.getCAAgent();

		double time = e.getEventExcexutionTime();
		if (e.getCAEventType() == CAEventType.SWAP){
			handelSwap(a,time);
		} else if (e.getCAEventType() == CAEventType.TTA){
			handleTTA(a,time);
		} else if (e.getCAEventType() == CAEventType.TTE) {
			throw new RuntimeException("not implemented yet");
		}else {
			throw new RuntimeException("Unknown event type: " + e.getCAEventType());
		}
	}




	private void handleTTA(CAMoveableEntity a, double time) {
		

		int idx = a.getPos();
		int dir = a.getDir();
		int nextIdx = idx+dir;
		if (nextIdx < 0) {
			handleTTAUpStreamNode(a,time);
		} else if (nextIdx >= this.size) {
			handleTTADownStreamNode(a,time);
		} else {
			handleTTAOnLink(a,time,dir);
		}



	}

	private void handleTTAOnLink(CAMoveableEntity a, double time, int dir) {
		if (dir == -1) {
			handleTTAOnLinkUpStream(a,time);
		} else {
			handleTTAOnLinkDownStream(a,time);
		}
	}


	private void handleTTAOnLinkDownStream(CAMoveableEntity a, double time) {
		int idx = a.getPos();

		//check pre-condition
		if (this.particles[idx+1] == null) {
			double z = getZ(a);
			z *= this.ratio;
			if (this.lastLeftTimes[idx+1] <= (time-z+epsilon)){
				handleTTAOnLinkDownStreamOnPreCondition1(a,time,idx);
			} else {
				handleTTAOnLinkDownStreamOnPreCondition2(a,time,idx);
			}
		} else {
			handleTTAOnLinkDownStreamOnPreCondition3(a,time);
		}

	}




	private void handleTTAOnLinkDownStreamOnPreCondition1(
			CAMoveableEntity a, double time, int idx) {

		this.lastLeftTimes[idx] = time;
		this.particles[idx] = null;
		this.particles[idx+1] = a;
		a.proceed();

		//check post-condition and generate events
		//first for persons behind
		checkPostConditionForPersonBehindOnDownStreamAdvance(idx,a,time);

		//second for oneself
		checkPostConditionForAgentOnDownStreamAdvance(idx,a, time);

	}

	private void checkPostConditionForPersonBehindOnDownStreamAdvance(int idx,
			CAMoveableEntity a, double time) {
		if (idx-1 < 0) {
			CAMoveableEntity toBeTriggered = this.us.peekForAgent();
			if (toBeTriggered != null) {
				if (toBeTriggered.getNextLinkId().equals(this.dsl.getId())) {
					double z = getZ(toBeTriggered);
					z *= this.ratio;
					triggerTTA(toBeTriggered,this.us,time+z);
				}
			}
		} else {
			CAMoveableEntity toBeTriggered = this.particles[idx-1];
			if (toBeTriggered != null) {
				if (toBeTriggered.getDir() == 1) {
					double z = getZ(toBeTriggered);
					z *= this.ratio;
					triggerTTA(toBeTriggered,this,time+z);
				}
			}
		}
	}




	private void checkPostConditionForAgentOnDownStreamAdvance(int idx,
			CAMoveableEntity a, double time) {
		if (idx+2 >= this.size ) {
			CAMoveableEntity inFrontOfMe = this.ds.peekForAgent();
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getNextLinkId().equals(this.dsl.getId())) { //oncoming
					double d = getD(a);
					d *= this.ratio;
					triggerSWAP(a,this,time+d+this.tFree);
				} 
			} else {
				triggerTTA(a,this,time+this.tFree);
			}
		} else {
			CAMoveableEntity inFrontOfMe = this.particles[idx+2];
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getDir() == -1) { //oncoming
					double d = getD(a);
					d *= this.ratio;
					triggerSWAP(a,this,time+d+this.tFree);
				} 
			} else {
				triggerTTA(a,this,time+this.tFree);
			}
		}

	}




	private void handleTTAOnLinkDownStreamOnPreCondition2(
			CAMoveableEntity a, double time, int idx) {
		
		double z = getZ(a);
		z *= this.ratio;
		double zStar = z - (time - this.lastLeftTimes[idx+1]);//FIXME should not work!!!
		double nextTime = time + zStar;
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnLinkDownStreamOnPreCondition3(
			CAMoveableEntity a, double time) {
		// nothing to be done here.
	}

	private void handleTTAOnLinkUpStream(CAMoveableEntity a, double time) {
		int idx = a.getPos();

		//check pre-condition
		if (this.particles[idx-1] == null) {
			double z = getZ(a);
			z *= this.ratio;
			if (this.lastLeftTimes[idx-1] <= (time-z+epsilon)){
				handleTTAOnLinkUpStreamOnPreCondition1(a,time,idx);
			} else {
				handleTTAOnLinkUpStreamOnPreCondition2(a,time,idx);
			}
		} else {
			handleTTAOnLinkUpStreamOnPreCondition3(a,time);
		}

	}

	private void handleTTAOnLinkUpStreamOnPreCondition1(
			CAMoveableEntity a, double time, int idx) {

		
		this.lastLeftTimes[idx] = time;
		this.particles[idx] = null;
		this.particles[idx-1] = a;
		a.proceed();

		//check post-condition and generate events
		//first for persons behind
		checkPostConditionForPersonBehindOnUpStreamAdvance(idx,a,time);

		//second for oneself
		checkPostConditionForAgentOnUpStreamAdvance(idx,a, time);

	}

	private void checkPostConditionForPersonBehindOnUpStreamAdvance(int idx,
			CAMoveableEntity a, double time) {
		if (idx+1 >= this.size) {
			CAMoveableEntity toBeTriggered = this.ds.peekForAgent();
			if (toBeTriggered != null) {
				if (toBeTriggered.getNextLinkId().equals(this.dsl.getId())) {
					double z = getZ(toBeTriggered);
					z *= this.ratio;
					triggerTTA(toBeTriggered,this.ds,time+z);
				}
			}
		} else {
			CAMoveableEntity toBeTriggered = this.particles[idx+1];
			if (toBeTriggered != null) {
				if (toBeTriggered.getDir() == -1) {
					double z = getZ(toBeTriggered);
					z *= this.ratio;
					triggerTTA(toBeTriggered,this,time+z);
				}
			}
		}
	}

	private void checkPostConditionForAgentOnUpStreamAdvance(int idx,
			CAMoveableEntity a, double time) {
		if (idx-2 < 0) {
			CAMoveableEntity inFrontOfMe = this.us.peekForAgent();
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getNextLinkId().equals(this.dsl.getId())) { //oncoming
					
					double d = getD(a);
					d *= this.ratio;
					triggerSWAP(a,this,time+d+this.tFree);
				} 
			} else {
				triggerTTA(a,this,time+this.tFree);
			}
		} else {
			CAMoveableEntity inFrontOfMe = this.particles[idx-2];
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getDir() == 1) { //oncoming
					double d = getD(a);
					d *= this.ratio;
					triggerSWAP(a,this,time+d+this.tFree);
				} 
			} else {
				triggerTTA(a,this,time+this.tFree);
			}
		}

	}

	private void handleTTAOnLinkUpStreamOnPreCondition2(
			CAMoveableEntity a, double time, int idx) {
		double z = getZ(a);
		z *= this.ratio;
		double zStar = z - (time - this.lastLeftTimes[idx-1]);
		double nextTime = time + zStar;
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnLinkUpStreamOnPreCondition3(
			CAMoveableEntity a, double time) {
		// nothing to be done here.
	}

	private void triggerSWAP(CAMoveableEntity a, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, a, ne, CAEventType.SWAP);
		this.net.pushEvent(e);

	}

	private void triggerTTA(CAMoveableEntity toBeTriggered, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, toBeTriggered, ne, CAEventType.TTA);
		this.net.pushEvent(e);

	}


	private void handleTTADownStreamNode(CAMoveableEntity a, double time) {
//		//HACK break condition agent is at the end of its last link
//		if (a.getNextLinkId().equals(a.getCurrentLink().getId())) {
//			this.lastLeftTimes[this.size-1] = time;
//			this.particles[this.size-1] = null;
//			return;
//		}
		
		//check pre-condition
		if (this.ds.peekForAgent() == null) {
			double z = getZ(a);
			z *= this.ratio;
			if (this.ds.getLastNodeExitTimeForAgent(a) <= (time-z+epsilon)){
				handleTTADownStreamNodeOnPreCondition1(a,time);
			} else {
				handleTTANodeOnPreCondition2(a,time,this.ds);
			}
		} else {
			handleTTADownStreamNodeOnPreCondition3(a,time);
		}

	}

	private void handleTTADownStreamNodeOnPreCondition1(CAMoveableEntity a,
			double time) {

		this.lastLeftTimes[this.size-1] = time;
		this.particles[this.size-1] = null;
		this.ds.putAgent(a);

		fireDownstreamLeft(a, time);
		
		//check post-condition and generate events
		//first for persons behind
		checkPostConditionForPersonBehindOnDownStreamAdvance(this.size-1,a,time);

		//second for oneself
		checkPostConditionForOneSelfOnNodeAdvance(this.ds,a, time);

	}


	private void checkPostConditionForOneSelfOnNodeAdvance(CANodeDynamic n,
			CAMoveableEntity a, double time) {
		Id<Link> nextCALinkId = a.getNextLinkId();
		CALink nextCALink = this.net.getCALink(nextCALinkId);
		int nextNextA;
		int nextRevDir;
		if (nextCALink.getUpstreamCANode() == n) {
			nextNextA = 0;
			nextRevDir = -1;
		} else if (nextCALink.getDownstreamCANode() == n) {
			nextNextA = nextCALink.getNumOfCells()-1;
			nextRevDir = 1;
		} else {
			log.warn("inconsitent network, agent:" + a + " becomes stuck!");
			return;
		}

		CAMoveableEntity inFrontOfMe = nextCALink.getParticles()[nextNextA];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == nextRevDir) { //oncoming
				double d = getD(a);
				d *= this.ratio;
				triggerSWAP(a,n,time+d+this.tFree);
			} 
		} else {
			triggerTTA(a,n,time+this.tFree);
		}

	}


	private void handleTTANodeOnPreCondition2(CAMoveableEntity a,
			double time, CANodeDynamic n) {
		
		double z = getZ(a);
		z *= this.ratio;
		
		double zStar = z - (time - n.getLastNodeExitTimeForAgent(a));
		double nextTime = time + zStar;
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);

	}


	private void handleTTADownStreamNodeOnPreCondition3(CAMoveableEntity a,
			double time) {
		// nothing to be done here.
	}

	private void handleTTAUpStreamNode(CAMoveableEntity a, double time) {
		//TODO  HACK break condition agent is at the end of its last link
		if (a.getNextLinkId().equals(a.getCurrentLink().getId())) {
			this.lastLeftTimes[0] = time;
			this.particles[0] = null;
			return;
		}
		//check pre-condition
		if (this.us.peekForAgent() == null) {
			double z = getZ(a);
			z *= this.ratio;
			if (this.us.getLastNodeExitTimeForAgent(a) <= (time-z+epsilon)){
				handleTTAUpStreamNodeOnPreCondition1(a,time);
			} else {
				handleTTANodeOnPreCondition2(a,time,this.us);
			}
		} else {
			handleTTAUpStreamNodeOnPreCondition3(a,time);
		}

	}

	private void handleTTAUpStreamNodeOnPreCondition1(CAMoveableEntity a,
			double time) {
		
		this.lastLeftTimes[0] = time;
		this.particles[0] = null;
		this.us.putAgent(a);

		fireUpstreamLeft(a, time);
		
		//check post-condition and generate events
		//first for persons behind
		checkPostConditionForPersonBehindOnUpStreamAdvance(0,a,time);

		//second for oneself
		checkPostConditionForOneSelfOnNodeAdvance(this.us,a, time);

	}

	private void handleTTAUpStreamNodeOnPreCondition3(CAMoveableEntity a,
			double time) {
		// nothing to be done here.
	}

	//================================================================================== TODO

	private void handelSwap(CAMoveableEntity a, double time) {
		int idx = a.getPos();
		int dir = a.getDir();
		int nbIdx = idx + dir;
		if (nbIdx < 0) {
			swapWithUpStreamNode(a,time);
		} else if (nbIdx >= this.size) {
			swapWithDownStreamNode(a,time);
		} else {
			swapOnLink(a,idx,dir,time);
		}

	}

	private void swapWithDownStreamNode(CAMoveableEntity a, double time) {
		
		CAMoveableEntity swapA = this.ds.pollAgent(time);

		swapA.moveOverNode(this,time);
		swapA.materialize(this.size-1, -1);
		
				
		this.particles[this.size-1] = (CASimpleDynamicAgent) swapA;
		this.lastLeftTimes[this.size-1] = time;
		this.ds.putAgent(a);
		
		fireDownstreamLeft(a, time);
		fireDownstreamEntered(swapA, time);
		
		//check post-condition and generate events
		//first for swapA
		checkPostConditionForAgentOnUpStreamAdvance(this.size, ((CASimpleDynamicAgent)swapA), time);
		
		//second for oneself
		checkPostConditionForOneSelfOnNodeAdvance(this.ds,a, time);
	}
	
	private void swapWithUpStreamNode(CAMoveableEntity a, double time) {
		
		CAMoveableEntity swapA = this.us.pollAgent(time);
		swapA.moveOverNode(this,time);
		swapA.materialize(0, 1);
		
		
		this.particles[0] = (CASimpleDynamicAgent) swapA;
		this.lastLeftTimes[0] = time;
		this.us.putAgent(a);
		
		fireUpstreamLeft(a, time);
		fireUpstreamEntered(swapA, time);
		
		//check post-condition and generate events
		//first for swapA
		checkPostConditionForAgentOnDownStreamAdvance(-1, ((CASimpleDynamicAgent)swapA), time);
		
		//second for oneself
		checkPostConditionForOneSelfOnNodeAdvance(this.us,a, time);
	}


	@Override
	public void fireDownstreamEntered(CAMoveableEntity a, double time) {
		LinkEnterEvent e = new LinkEnterEvent(time, a.getId(), this.usl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
		//		System.out.println("down");

	}

	@Override
	public void fireUpstreamEntered(CAMoveableEntity a, double time) {
		LinkEnterEvent e = new LinkEnterEvent(time, a.getId(), this.dsl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
		//		System.out.println("up");
	}

	@Override
	public void fireDownstreamLeft(CAMoveableEntity a, double time) {
		LinkLeaveEvent e = new LinkLeaveEvent(time, a.getId(), this.dsl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);

	}

	@Override
	public void fireUpstreamLeft(CAMoveableEntity a, double time) {
		LinkLeaveEvent e = new LinkLeaveEvent(time, a.getId(), this.usl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
	}

	@Override
	public int getNumOfCells() {
		return this.size;
	}

	private void swapOnLink(CAMoveableEntity a, int idx, int dir, double time) {
		
		if (dir == 1) {
			swapOnLinkDownStream(a,idx,time);
		} else {
			swapOnLinkUpStream(a,idx,time);
		}
		


	}

	private void swapOnLinkDownStream(CAMoveableEntity a, int idx,
			double time) {
		
		int nbIdx = idx+1;
		CAMoveableEntity nb = this.particles[nbIdx];
		this.particles[nbIdx] = a;
		this.particles[idx] = nb;

		
		this.lastLeftTimes[nbIdx] = time;
		this.lastLeftTimes[idx] = time;
		
		nb.proceed();
		a.proceed();
		
		checkPostConditionForAgentOnDownStreamAdvance(idx, a, time);
		checkPostConditionForAgentOnUpStreamAdvance(nbIdx, nb, time);
		
	}

	private void swapOnLinkUpStream(CAMoveableEntity a, int idx, double time) {
		
		int nbIdx = idx-1;
		CAMoveableEntity nb = this.particles[nbIdx];
		this.particles[nbIdx] = a;
		this.particles[idx] = nb;
		
		this.lastLeftTimes[nbIdx] = time;
		this.lastLeftTimes[idx] = time;
		
		nb.proceed();
		a.proceed();
		
		checkPostConditionForAgentOnUpStreamAdvance(idx, a, time);
		checkPostConditionForAgentOnDownStreamAdvance(nbIdx, nb, time);
		
	}

	@Override
	public CANode getUpstreamCANode() {
		return this.us;
	}
	@Override
	public CANode getDownstreamCANode() {
		return this.ds;
	}

	@Override
	public CAMoveableEntity[] getParticles() {
		return this.particles;
	}
	
	public int getSize() {
		return this.size;
	}

	@Override
	public double[] getTimes() {
		throw new RuntimeException("deprecated");
	}
	
	public double [] getLastLeftTimes() {
		return this.lastLeftTimes;
	}

	@Override
	public Link getLink() {
		return this.dsl;
	}

	@Override
	public Link getUpstreamLink() {
		return this.usl;
	}
	
	public Link getDownstreamLink() {
		return this.dsl;
	}

	@Override
	public String toString() {
		return this.dsl.getId().toString();
	}

	public double getWidth() {
		return this.width;
	}


	public double getTFree() {
		return this.tFree;
	}


	public double getCellLength() {
		return this.cellLength;
	}


	@Override
	public void reset() {
		for (int i = 0; i < particles.length; i++) {
			lastLeftTimes[i] = 0;
			if (particles[i] != null) {
				CAMoveableEntity part = particles[i];
				this.net.unregisterAgent(part);
				particles[i] = null;
			}
		}
		
	}
}
