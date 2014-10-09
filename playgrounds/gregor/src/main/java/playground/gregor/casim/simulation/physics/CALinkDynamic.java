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

	private final CASimpleDynamicAgent [] particles;
	private final double [] dsLastLeftTimes;
	private final double [] usLastLeftTimes;
	
	private final int size;

	private final CANodeDynamic ds;

	private final CANodeDynamic us;

	private final CANetworkDynamic net;


	private final double cellLength;

	private final double width;

	private final double tFree;

	private final double ratio;

	public CALinkDynamic(Link dsl, Link usl, CANodeDynamic ds, CANodeDynamic us, CANetworkDynamic net) {

		this.width = dsl.getCapacity();//TODO this is a misuse of the link's capacity attribute. Needs to be fixed!
		this.ratio = CANetworkDynamic.PED_WIDTH/this.width;
		this.cellLength = this.ratio/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.PED_WIDTH);
		this.dsl = dsl;
		this.usl = usl;
		this.size = (int) (0.5+dsl.getLength()/this.cellLength);
		this.particles = new CASimpleDynamicAgent[this.size];
		this.usLastLeftTimes = new double[this.size];
		this.dsLastLeftTimes = new double[this.size];
		this.ds = ds;
		this.us = us;
		this.tFree = this.cellLength/CANetworkDynamic.V_HAT;
		this.net = net;
//		this.ratio /= CANetworkDynamic.PED_WIDTH;
//		this.ratio = this.cellLength
//		this.ratio = 1;
	}

	/*package*/ double getD() {
		final double rho = this.net.getRho(this);
		final double tmp = Math.pow(rho*CANetworkDynamic.PED_WIDTH, CANetworkDynamic.GAMMA);
		final double d = CANetworkDynamic.ALPHA + CANetworkDynamic.BETA*tmp;
		if (Double.isNaN(d)) {
			throw new RuntimeException("NaN");
		}
		return d;
	}
	
	/*package*/ double getZ() {
		double d = getD();
		double z = 1/(CANetworkDynamic.RHO_HAT+CANetworkDynamic.V_HAT)+d;
		if (Double.isNaN(z)) {
			throw new RuntimeException("NaN");
		}
		return z;
	}

	@Override
	public void handleEvent(CAEvent e) {
		CAAgent a = e.getCAAgent();
		if (!(a instanceof CASimpleDynamicAgent)) {
			throw new RuntimeException("Can not handle agent+" + a+ " only instances of" + CASimpleDynamicAgent.class.getName() + " are allowd");
		}

		CASimpleDynamicAgent dyna = (CASimpleDynamicAgent)a;

		double time = e.getEventExcexutionTime();
		if (e.getCAEventType() == CAEventType.SWAP){
			handelSwap(dyna,time);
		} else if (e.getCAEventType() == CAEventType.TTA){
			handleTTA(dyna,time);
		} else if (e.getCAEventType() == CAEventType.TTE) {
			throw new RuntimeException("not implemented yet");
		}else {
			throw new RuntimeException("Unknown event type: " + e.getCAEventType());
		}
	}




	private void handleTTA(CASimpleDynamicAgent a, double time) {
		

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

	private void handleTTAOnLink(CASimpleDynamicAgent a, double time, int dir) {
		if (dir == -1) {
			handleTTAOnLinkUpStream(a,time);
		} else {
			handleTTAOnLinkDownStream(a,time);
		}
	}


	private void handleTTAOnLinkDownStream(CASimpleDynamicAgent a, double time) {
		int idx = a.getPos();

		//check pre-condition
		if (this.particles[idx+1] == null) {
			double z = getZ();
			z *= this.ratio;
			if (this.dsLastLeftTimes[idx+1] <= (time-z)){
				handleTTAOnLinkDownStreamOnPreCondition1(a,time,idx);
			} else {
				handleTTAOnLinkDownStreamOnPreCondition2(a,time,idx);
			}
		} else {
			handleTTAOnLinkDownStreamOnPreCondition3(a,time);
		}

	}




	private void handleTTAOnLinkDownStreamOnPreCondition1(
			CASimpleDynamicAgent a, double time, int idx) {

		this.dsLastLeftTimes[idx] = time;
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
			CASimpleDynamicAgent a, double time) {
		if (idx-1 < 0) {
			CAAgent toBeTriggered = this.us.peekForAgent();
			if (toBeTriggered != null) {
				if (toBeTriggered.getNextLinkId().equals(this.dsl.getId())) {
					double z = getZ();
					z *= this.ratio;
					triggerTTA(toBeTriggered,this.us,time+z);
				}
			}
		} else {
			CAAgent toBeTriggered = this.particles[idx-1];
			if (toBeTriggered != null) {
				if (toBeTriggered.getDir() == 1) {
					double z = getZ();
					z *= this.ratio;
					triggerTTA(toBeTriggered,this,time+z);
				}
			}
		}
	}




	private void checkPostConditionForAgentOnDownStreamAdvance(int idx,
			CASimpleDynamicAgent a, double time) {
		if (idx+2 >= this.size ) {
			CAAgent inFrontOfMe = this.ds.peekForAgent();
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getNextLinkId().equals(this.dsl.getId())) { //oncoming
					double d = getD();
					d *= this.ratio;
					triggerSWAP(a,this,time+d+this.tFree);
				} else {
					triggerTTA(a,this,time+this.tFree);
				}
			} else {
				triggerTTA(a,this,time+this.tFree);
			}
		} else {
			CAAgent inFrontOfMe = this.particles[idx+2];
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getDir() == -1) { //oncoming
					double d = getD();
					d *= this.ratio;
					triggerSWAP(a,this,time+d+this.tFree);
				} else {
					triggerTTA(a,this,time+this.tFree);
				}
			} else {
				triggerTTA(a,this,time+this.tFree);
			}
		}

	}




	private void handleTTAOnLinkDownStreamOnPreCondition2(
			CASimpleDynamicAgent a, double time, int idx) {
		
		double z = getZ();
		z *= this.ratio;
		double zStar = z - (time - this.dsLastLeftTimes[idx+1]);//FIXME should not work!!!
		double nextTime = time + zStar;
		if (!(nextTime > time)) {
			nextTime += 0.0001;
		}
		
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnLinkDownStreamOnPreCondition3(
			CASimpleDynamicAgent a, double time) {
		// nothing to be done here.
	}

	private void handleTTAOnLinkUpStream(CASimpleDynamicAgent a, double time) {
		int idx = a.getPos();

		//check pre-condition
		if (this.particles[idx-1] == null) {
			double z = getZ();
			z *= this.ratio;
			if (this.usLastLeftTimes[idx-1] <= (time-z)){
				handleTTAOnLinkUpStreamOnPreCondition1(a,time,idx);
			} else {
				handleTTAOnLinkUpStreamOnPreCondition2(a,time,idx);
			}
		} else {
			handleTTAOnLinkUpStreamOnPreCondition3(a,time);
		}

	}

	private void handleTTAOnLinkUpStreamOnPreCondition1(
			CASimpleDynamicAgent a, double time, int idx) {

		
		throw new RuntimeException("not implemented!");
		
//		double timeGap = time-this.usLastLeftTimes[idx];
//		a.updateMyDynamicQuantitiesOnAdvance(timeGap,time,this.cellLength,this.width);
//
//		this.usLastLeftTimes[idx] = time;
//		this.particles[idx] = null;
//		this.particles[idx-1] = a;
//		a.proceed();
//
//		//check post-condition and generate events
//		//first for persons behind
//		checkPostConditionForPersonBehindOnUpStreamAdvance(idx,a,time);
//
//		//second for oneself
//		checkPostConditionForAgentOnUpStreamAdvance(idx,a, time);

	}

	private void checkPostConditionForPersonBehindOnUpStreamAdvance(int idx,
			CASimpleDynamicAgent a, double time) {
		if (idx+1 >= this.size) {
			CAAgent toBeTriggered = this.ds.peekForAgent();
			if (toBeTriggered != null) {
				if (toBeTriggered.getNextLinkId().equals(this.dsl.getId())) {
					double z = getZ();
					z *= this.ratio;
					triggerTTA(toBeTriggered,this.ds,time+z);
				}
			}
		} else {
			CAAgent toBeTriggered = this.particles[idx+1];
			if (toBeTriggered != null) {
				if (toBeTriggered.getDir() == -1) {
					double z = getZ();
					z *= this.ratio;
					triggerTTA(toBeTriggered,this,time+z);
				}
			}
		}
	}

	private void checkPostConditionForAgentOnUpStreamAdvance(int idx,
			CASimpleDynamicAgent a, double time) {
		if (idx-2 < 0) {
			CAAgent inFrontOfMe = this.us.peekForAgent();
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getNextLinkId().equals(this.dsl.getId())) { //oncoming
					
					double d = getD();
					d *= this.ratio;
					triggerSWAP(a,this,time+d+this.tFree);
				} else {
					triggerTTA(a,this,time+this.tFree);
				}
			} else {
				triggerTTA(a,this,time+this.tFree);
			}
		} else {
			CAAgent inFrontOfMe = this.particles[idx-2];
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getDir() == 1) { //oncoming
					double d = getD();
					d *= this.ratio;
					triggerSWAP(a,this,time+d+this.tFree);
				} else {
					triggerTTA(a,this,time+this.tFree);
				}
			} else {
				triggerTTA(a,this,time+this.tFree);
			}
		}

	}

	private void handleTTAOnLinkUpStreamOnPreCondition2(
			CASimpleDynamicAgent a, double time, int idx) {
		double z = getZ();
		z *= this.ratio;
		double zStar = z - (time - this.usLastLeftTimes[idx-1]);//FIXME should not work!!!
		double nextTime = time + zStar;
		if (!(nextTime > time)) {
			nextTime += 0.0001;
		}
		
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnLinkUpStreamOnPreCondition3(
			CASimpleDynamicAgent a, double time) {
		// nothing to be done here.
	}

	private void triggerSWAP(CAAgent a, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, a, ne, CAEventType.SWAP);
		this.net.pushEvent(e);

	}

	private void triggerTTA(CAAgent toBeTriggered, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, toBeTriggered, ne, CAEventType.TTA);
		this.net.pushEvent(e);

	}


	private void handleTTADownStreamNode(CASimpleDynamicAgent a, double time) {
		//HACK break condition agent is at the end of its last link
		if (a.getNextLinkId() == a.getCurrentLink().getLink().getId()) {
			this.usLastLeftTimes[this.size-1] = time;
			this.particles[this.size-1] = null;
			return;
		}
		
		//check pre-condition
		if (this.ds.peekForAgent() == null) {
			double z = getZ();
			z *= this.ratio;
			if (this.ds.getLastNodeExitTimeForAgent(a) <= (time-z)){
				handleTTADownStreamNodeOnPreCondition1(a,time);
			} else {
				handleTTANodeOnPreCondition2(a,time,this.ds);
			}
		} else {
			handleTTADownStreamNodeOnPreCondition3(a,time);
		}

	}

	private void handleTTADownStreamNodeOnPreCondition1(CASimpleDynamicAgent a,
			double time) {

		this.dsLastLeftTimes[this.size-1] = time;
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
			CASimpleDynamicAgent a, double time) {
		Id nextCALinkId = a.getNextLinkId();
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

		CAAgent inFrontOfMe = nextCALink.getParticles()[nextNextA];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == nextRevDir) { //oncoming
				double d = getD();
				d *= this.ratio;
				triggerSWAP(a,n,time+d+this.tFree);
			} else {
				triggerTTA(a,n,time+this.tFree);
			}
		} else {
			triggerTTA(a,n,time+this.tFree);
		}

	}


	private void handleTTANodeOnPreCondition2(CASimpleDynamicAgent a,
			double time, CANodeDynamic n) {
		
		double z = getZ();
		z *= this.ratio;
		
		double zStar = z - (time - n.getLastNodeExitTimeForAgent(a));
		double nextTime = time + zStar;
		if (!(nextTime > time)) {
			nextTime += 0.0001;
		}
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);

	}


	private void handleTTADownStreamNodeOnPreCondition3(CASimpleDynamicAgent a,
			double time) {
		// nothing to be done here.
	}

	private void handleTTAUpStreamNode(CASimpleDynamicAgent a, double time) {
		//HACK break condition agent is at the end of its last link
		if (a.getNextLinkId() == a.getCurrentLink().getLink().getId()) {
			this.usLastLeftTimes[0] = time;
			this.particles[0] = null;
			return;
		}
		//check pre-condition
		if (this.us.peekForAgent() == null) {
			double z = getZ();
			z *= this.ratio;
			if (this.us.getLastNodeExitTimeForAgent(a) <= (time-z)){
				handleTTAUpStreamNodeOnPreCondition1(a,time);
			} else {
				handleTTANodeOnPreCondition2(a,time,this.us);
			}
		} else {
			handleTTAUpStreamNodeOnPreCondition3(a,time);
		}

	}

	private void handleTTAUpStreamNodeOnPreCondition1(CASimpleDynamicAgent a,
			double time) {
		
		
		throw new RuntimeException("not implemented!");
		
//		double timeGap = time-this.usLastLeftTimes[0];
//		a.updateMyDynamicQuantitiesOnAdvance(timeGap,time,this.cellLength,this.width);
//
//		this.usLastLeftTimes[0] = time;
//		this.particles[0] = null;
//		this.us.putAgent(a);
//
//		fireUpstreamLeft(a, time);
//		
//		//check post-condition and generate events
//		//first for persons behind
//		checkPostConditionForPersonBehindOnUpStreamAdvance(0,a,time);
//
//		//second for oneself
//		checkPostConditionForOneSelfOnNodeAdvance(this.us,a, time);

	}

	private void handleTTAUpStreamNodeOnPreCondition3(CASimpleDynamicAgent a,
			double time) {
		// nothing to be done here.
	}

	//================================================================================== TODO

	private void handelSwap(CASimpleDynamicAgent a, double time) {
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

	private void swapWithDownStreamNode(CASimpleDynamicAgent a, double time) {
		
		throw new RuntimeException("not implemented!");
		
//		CAAgent swapA = this.ds.pollAgent(time);
//		if (!(swapA instanceof CASimpleDynamicAgent)){
//			throw new RuntimeException("only agents of type " + CASimpleDynamicAgent.class.getName() + " are allowed here.");
//		}
//		double theirV = ((CASimpleDynamicAgent)swapA).getV();
//		double theirRho = ((CASimpleDynamicAgent)swapA).getMyDirectionRho();
//		double timeGap = time - this.dsLastLeftTimes[this.size-1];
//		a.updateMyDynamicQuantitiesOnAdvance(timeGap, time, this.cellLength, this.width);
//		a.updateMyDynamicQuantitiesOnSwap(theirV,time,this.width,theirRho);
//		swapA.moveOverNode(this,time);
//		swapA.materialize(this.size-1, -1);
//		
//		double myRho = a.getMyDirectionRho();
//		((CASimpleDynamicAgent)swapA).updateMyDynamicQuantitiesOnSwap(a.getV(),time,this.width,myRho);
//				
//		this.particles[this.size-1] = (CASimpleDynamicAgent) swapA;
//		this.dsLastLeftTimes[this.size-1] = time;
//		this.dsLastLeftAgent[this.size-1] = a;
//		this.ds.putAgent(a);
//		
//		fireDownstreamLeft(a, time);
//		fireDownstreamEntered(swapA, time);
//		
//		
//		//check post-condition and generate events
//		//first for swapA
//		checkPostConditionForAgentOnUpStreamAdvance(this.size, ((CASimpleDynamicAgent)swapA), time);
//		
//		//second for oneself
//		checkPostConditionForOneSelfOnNodeAdvance(this.ds,a, time);
	}
	
	private void swapWithUpStreamNode(CASimpleDynamicAgent a, double time) {
		
		throw new RuntimeException("not implemented!");
		
//		CAAgent swapA = this.us.pollAgent(time);
//		if (!(swapA instanceof CASimpleDynamicAgent)){
//			throw new RuntimeException("only agents of type " + CASimpleDynamicAgent.class.getName() + " are allowed here.");
//		}
//		double theirV = ((CASimpleDynamicAgent)swapA).getV();
//		double theirRho = ((CASimpleDynamicAgent)swapA).getMyDirectionRho();
//		double timeGap = time - this.usLastLeftTimes[0];
//		a.updateMyDynamicQuantitiesOnAdvance(timeGap, time, this.cellLength, this.width);
//		a.updateMyDynamicQuantitiesOnSwap(theirV,time,this.width,theirRho);
//		swapA.moveOverNode(this,time);
//		swapA.materialize(0, 1);
//		
//		double myRho = a.getMyDirectionRho();
//		
//		((CASimpleDynamicAgent)swapA).updateMyDynamicQuantitiesOnSwap(a.getV(),time,this.width,myRho);
//		
//		this.particles[0] = (CASimpleDynamicAgent) swapA;
//		this.usLastLeftTimes[0] = time;
//		this.us.putAgent(a);
//		
//		fireUpstreamLeft(a, time);
//		fireUpstreamEntered(swapA, time);
//		
//		//check post-condition and generate events
//		//first for swapA
//		checkPostConditionForAgentOnDownStreamAdvance(-1, ((CASimpleDynamicAgent)swapA), time);
//		
//		//second for oneself
//		checkPostConditionForOneSelfOnNodeAdvance(this.us,a, time);
	}


	@Override
	public void fireDownstreamEntered(CAAgent a, double time) {
		LinkEnterEvent e = new LinkEnterEvent(time, a.getId(), this.usl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
		//		System.out.println("down");

	}

	@Override
	public void fireUpstreamEntered(CAAgent a, double time) {
		LinkEnterEvent e = new LinkEnterEvent(time, a.getId(), this.dsl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
		//		System.out.println("up");
	}

	@Override
	public void fireDownstreamLeft(CAAgent a, double time) {
		LinkLeaveEvent e = new LinkLeaveEvent(time, a.getId(), this.dsl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);

	}

	@Override
	public void fireUpstreamLeft(CAAgent a, double time) {
		LinkLeaveEvent e = new LinkLeaveEvent(time, a.getId(), this.usl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
	}

	@Override
	public int getNumOfCells() {
		return this.size;
	}

	private void swapOnLink(CASimpleDynamicAgent a, int idx, int dir, double time) {
		
		if (dir == 1) {
			swapOnLinkDownStream(a,idx,time);
		} else {
			swapOnLinkUpStream(a,idx,time);
		}
		


	}

	private void swapOnLinkDownStream(CASimpleDynamicAgent a, int idx,
			double time) {
		
		throw new RuntimeException("not implemented!");
		
//		int nbIdx = idx+1;
//		CASimpleDynamicAgent nb = this.particles[nbIdx];
//		this.particles[nbIdx] = a;
//		this.particles[idx] = nb;
//
//		double theirRho = nb.getMyDirectionRho();
//		double myRho = a.getMyDirectionRho();
//		
//		double onCommingTimeGap = time-this.usLastLeftTimes[nbIdx];
//		this.usLastLeftTimes[nbIdx] = time;
//		nb.updateMyDynamicQuantitiesOnAdvance(onCommingTimeGap,time,this.cellLength,this.width);
//		nb.updateMyDynamicQuantitiesOnSwap(a.getV(),time,this.width,myRho);
//
//		double timeGap = time-this.dsLastLeftTimes[idx];
//		this.dsLastLeftTimes[idx] = time;
//		this.dsLastLeftAgent[idx] = a;
//		a.updateMyDynamicQuantitiesOnAdvance(timeGap,time,this.cellLength,this.width);
//		a.updateMyDynamicQuantitiesOnSwap(nb.getV(),time,this.width,theirRho);
//		
//		nb.proceed();
//		a.proceed();
//		
//		checkPostConditionForAgentOnDownStreamAdvance(idx, a, time);
//		checkPostConditionForAgentOnUpStreamAdvance(nbIdx, nb, time);
		
	}

	private void swapOnLinkUpStream(CASimpleDynamicAgent a, int idx, double time) {
		
		throw new RuntimeException("not implemented!");
		
//		int nbIdx = idx-1;
//		CASimpleDynamicAgent nb = this.particles[nbIdx];
//		this.particles[nbIdx] = a;
//		this.particles[idx] = nb;
//
//		double theirRho = nb.getMyDirectionRho();
//		double myRho = a.getMyDirectionRho();
//		
//		double onCommingTimeGap = time-this.dsLastLeftTimes[nbIdx];
//		this.dsLastLeftTimes[nbIdx] = time;
//		this.dsLastLeftAgent[nbIdx] = nb;
//		nb.updateMyDynamicQuantitiesOnAdvance(onCommingTimeGap,time,this.cellLength,this.width);
//		nb.updateMyDynamicQuantitiesOnSwap(a.getV(),time,this.width,myRho);
//
//		double timeGap = time-this.usLastLeftTimes[idx];
//		this.usLastLeftTimes[idx] = time;
//		a.updateMyDynamicQuantitiesOnAdvance(timeGap,time,this.cellLength,this.width);
//		a.updateMyDynamicQuantitiesOnSwap(nb.getV(),time,this.width,theirRho);
//		
//		nb.proceed();
//		a.proceed();
//		
//		checkPostConditionForAgentOnUpStreamAdvance(idx, a, time);
//		checkPostConditionForAgentOnDownStreamAdvance(nbIdx, nb, time);
		
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
	public CAAgent[] getParticles() {
		return this.particles;
	}
	
	public int getSize() {
		return this.size;
	}

	@Override
	public double[] getTimes() {
		throw new RuntimeException("deprecated");
	}
	
	public double [] getDsLastLeftTimes() {
		return this.dsLastLeftTimes;
	}
	
	public double [] getUsLastLeftTimes() {
		return this.usLastLeftTimes;
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


	//MATSim integration
	@Override
	public void letAgentDepart(CAVehicle veh) {
		throw new RuntimeException("not implemented yet!");
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
}
