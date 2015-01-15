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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.casim.simulation.CANetsimEngine;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;

/**
 * CANode models the dynamics for pedestrian streams (uni- and bidirectional)
 * 
 * @author laemmel
 *
 */
public class CASingleLaneNode implements CANode {

	private static final Logger log = Logger.getLogger(CASingleLaneNode.class);

	private CAMoveableEntity agent;
	private final Node node;
	private final AbstractCANetwork net;

	private final List<CASingleLaneLink> links = new ArrayList<CASingleLaneLink>();

	private final Map<CALink, Double> towardsLinkLastExitTimes = new HashMap<CALink, Double>();
	private final Map<CALink, CAMoveableEntity> towardsLinkLastExitAgents = new HashMap<CALink, CAMoveableEntity>();

	private final double width;

	private double agentLeft;

	private final double tFree;

	private final double cellLength;

	private final double ratio;

	private final double epsilon;

	final ReentrantLock lock = new ReentrantLock();

	private final int threadNR;

	private final double x;

	private final double y;

	public CASingleLaneNode(Node node, AbstractCANetwork net) {

		this.threadNR = MatsimRandom.getRandom().nextInt(
				AbstractCANetwork.NR_THREADS);

		double width = 0;
		for (Link l : node.getInLinks().values()) {
			if (l.getCapacity() > width) {
				width = l.getCapacity();
			}
		}
		if (node.getId().toString().equals("2")
				|| node.getId().toString().equals("3")) {
			width = 10;
		}
		this.width = width;
		this.node = node;
		this.net = net;
		this.ratio = AbstractCANetwork.PED_WIDTH / this.width;
		this.cellLength = this.ratio
				/ (AbstractCANetwork.RHO_HAT * AbstractCANetwork.PED_WIDTH);
		this.tFree = this.cellLength / AbstractCANetwork.V_HAT;
		this.epsilon = tFree / 1000;

		x = node.getCoord().getX();
		y = node.getCoord().getY();
	}

	@Override
	public void addLink(CALink link) {
		if (!(link instanceof CASingleLaneLink)) {
			throw new RuntimeException("Incompatible link type:"
					+ link.getClass().toString());
		}
		this.links.add((CASingleLaneLink) link);
		this.towardsLinkLastExitTimes.put(link, -1.);
	}

	public void putAgent(CAMoveableEntity a) {
		if (this.agent != null) {
			throw new RuntimeException("There is already an agent on node:"
					+ this.node.getId());

		}
		this.agent = a;
		a.moveToNode(this);
	}

	public CAMoveableEntity peekForAgent() {
		return this.agent;
	}

	public CAMoveableEntity pollAgent(double time) {
		CAMoveableEntity a = this.agent;
		this.agent = null;
		this.agentLeft = time;
		return a;
	}

	@Override
	public void handleEvent(CAEvent e) {
		CAMoveableEntity a = e.getCAAgent();

		double time = e.getEventExcexutionTime();
		if (e.getCAEventType() == CAEventType.SWAP) {
			handelSwap(a, time);
		} else if (e.getCAEventType() == CAEventType.TTA) {
			handleTTA(a, time);
		} else {
			throw new RuntimeException("Unknown event type: "
					+ e.getCAEventType());
		}
	}

	private void handleTTA(CAMoveableEntity a, double time) {

		Id<Link> nextLinkId = a.getNextLinkId();
		CASingleLaneLink nextLink = (CASingleLaneLink) this.net
				.getCALink(nextLinkId);

		if (nextLink.getDownstreamCANode() == this) {
			handleTTAEnterNextLinkFromDownstreamEnd(nextLink, a, time);
		} else if (nextLink.getUpstreamCANode() == this) {
			handleTTAEnterNextLinkFromUpstreamEnd(nextLink, a, time);
		} else {
			throw new RuntimeException("Inconsitent network or agent plan!");
		}

	}

	private void handleTTAEnterNextLinkFromUpstreamEnd(
			CASingleLaneLink nextLink, CAMoveableEntity a, double time) {

		// check pre-condition
		if (nextLink.getParticles()[0] == null) {
			double z = nextLink.getZ(a);
			z *= this.ratio;
			if (nextLink.getLastLeftTimes()[0] <= (time - z + epsilon)) {
				handleTTAEnterNextLinkFromUpstreamEndOnPrecondition1(nextLink,
						a, time);
			} else {
				handleTTAEnterNextLinkFromUpstreamEndOnPrecondition2(nextLink,
						a, time);
			}
		} else {
			handleTTAOnPrecondition3(a, time);
		}

	}

	private void handleTTAEnterNextLinkFromUpstreamEndOnPrecondition1(
			CASingleLaneLink nextLink, CAMoveableEntity a, double time) {

		this.towardsLinkLastExitTimes.put(nextLink, time);
		this.towardsLinkLastExitAgents.put(nextLink, a);

		this.pollAgent(Double.NaN);

		nextLink.getParticles()[0] = a;
		a.materialize(0, 1);
		// this.towardsLinkLastExitTimes.put(nextLink, time);
		a.moveOverNode(nextLink, time);
		nextLink.fireUpstreamEntered(a, time);

		// check post-condition and generate events
		// first for persons behind
		triggerPrevAgent(time);

		// second for oneself
		checkPostConditionForAgentEnteredLinkFromUpstreamEnd(nextLink, a, time);

	}

	private void checkPostConditionForAgentEnteredLinkFromUpstreamEnd(
			CASingleLaneLink nextLink, CAMoveableEntity a, double time) {
		CAMoveableEntity inFrontOfMe = nextLink.getParticles()[1];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == -1) { // oncoming
				// double d = Math.min(nextLink.getD(a),
				// nextLink.getD(inFrontOfMe));
				double d = nextLink.getD(a);
				d *= this.ratio;
				triggerSWAP(a, nextLink, time + d + nextLink.getTFree());
			}
		} else {
			triggerTTA(a, nextLink, time + nextLink.getTFree());
		}

	}

	private void handleTTAEnterNextLinkFromUpstreamEndOnPrecondition2(
			CASingleLaneLink nextLink, CAMoveableEntity a, double time) {
		double z = nextLink.getZ(a);
		z *= this.ratio;
		double zStar = z - (time - nextLink.getLastLeftTimes()[0]);
		double nextTime = time + zStar;

		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnPrecondition3(CAMoveableEntity a, double time) {
		// nothing to be done here

	}

	// ///////////////////////

	private void handleTTAEnterNextLinkFromDownstreamEnd(
			CASingleLaneLink nextLink, CAMoveableEntity a, double time) {

		// check pre-condition
		if (nextLink.getParticles()[nextLink.getNumOfCells() - 1] == null) {
			double z = nextLink.getZ(a);
			z *= this.ratio;
			if (nextLink.getLastLeftTimes()[nextLink.getNumOfCells() - 1] <= (time
					- z + epsilon)) {
				handleTTAEnterNextLinkFromDownstreamEndOnPrecondition1(
						nextLink, a, time);
			} else {
				handleTTAEnterNextLinkFromDownstreamEndOnPrecondition2(
						nextLink, a, time);
			}
		} else {
			handleTTAOnPrecondition3(a, time);
		}
	}

	private void handleTTAEnterNextLinkFromDownstreamEndOnPrecondition1(
			CASingleLaneLink nextLink, CAMoveableEntity a, double time) {

		this.towardsLinkLastExitTimes.put(nextLink, time);
		this.towardsLinkLastExitAgents.put(nextLink, a);

		this.pollAgent(Double.NaN);

		nextLink.getParticles()[nextLink.getNumOfCells() - 1] = a;
		a.materialize(nextLink.getNumOfCells() - 1, -1);
		a.moveOverNode(nextLink, time);
		nextLink.fireDownstreamEntered(a, time);

		// check post-condition and generate events
		// first for persons behind
		triggerPrevAgent(time);

		// second for oneself
		checkPostConditionForAgentEnteredLinkFromDownstreamEnd(nextLink, a,
				time);

	}

	private void checkPostConditionForAgentEnteredLinkFromDownstreamEnd(
			CASingleLaneLink nextLink, CAMoveableEntity a, double time) {
		CAMoveableEntity inFrontOfMe = nextLink.getParticles()[nextLink
				.getNumOfCells() - 2];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == 1) { // oncoming
				// double d = Math.min(nextLink.getD(a),
				// nextLink.getD(inFrontOfMe));
				double d = nextLink.getD(a);
				d *= this.ratio;
				triggerSWAP(a, nextLink, time + d + nextLink.getTFree());
			}
		} else {
			triggerTTA(a, nextLink, time + nextLink.getTFree());
		}

	}

	private void handleTTAEnterNextLinkFromDownstreamEndOnPrecondition2(
			CASingleLaneLink nextLink, CAMoveableEntity a, double time) {
		double z = nextLink.getZ(a);
		z *= this.ratio;
		double zStar = z
				- (time - nextLink.getLastLeftTimes()[nextLink.getNumOfCells() - 1]);
		double nextTime = time + zStar;
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void triggerSWAP(CAMoveableEntity a, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, a, ne, CAEventType.SWAP);
		this.net.pushEvent(e);

	}

	private void triggerTTA(CAMoveableEntity toBeTriggered, CANetworkEntity ne,
			double time) {
		CAEvent e = new CAEvent(time, toBeTriggered, ne, CAEventType.TTA);
		this.net.pushEvent(e);

	}

	// ======================================================

	private void triggerPrevAgent(double time) {

		double cap = 0;
		List<Tuple<CASingleLaneLink, CAMoveableEntity>> cands = new ArrayList<Tuple<CASingleLaneLink, CAMoveableEntity>>();
		for (CASingleLaneLink l : this.links) {
			if (l.getDownstreamCANode() == this) {
				if (l.getParticles()[l.getNumOfCells() - 1] != null
						&& l.getParticles()[l.getNumOfCells() - 1].getDir() == 1) {
					cands.add(new Tuple<CASingleLaneLink, CAMoveableEntity>(l,
							l.getParticles()[l.getNumOfCells() - 1]));
					cap += l.getLink().getCapacity();
				}
			} else if (l.getUpstreamCANode() == this) {
				if (l.getParticles()[0] != null
						&& l.getParticles()[0].getDir() == -1) {
					cands.add(new Tuple<CASingleLaneLink, CAMoveableEntity>(l,
							l.getParticles()[0]));
					cap += l.getLink().getCapacity();
				}
			}
		}
		if (cands.size() == 0) {
			return;
		}

		double rnd = cap * MatsimRandom.getRandom().nextDouble();
		double incr = cap / cands.size();
		double comp = incr;
		for (Tuple<CASingleLaneLink, CAMoveableEntity> t : cands) {
			if (rnd <= comp) {
				double z = t.getFirst().getZ(t.getSecond());
				z *= this.ratio;
				CAEvent e = new CAEvent(time + z, t.getSecond(), t.getFirst(),
						CAEventType.TTA);
				this.net.pushEvent(e);
				return;
			}
			comp += incr;
		}

		throw new RuntimeException("should be unreachable!");
	}

	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	private void handelSwap(CAMoveableEntity a, double time) {
		Id<Link> nextLinkId = a.getNextLinkId();
		CASingleLaneLink nextLink = (CASingleLaneLink) this.net
				.getCALink(nextLinkId);

		if (nextLink.getDownstreamCANode() == this) {
			handleSwapWithDownStreamEnd(a, time, nextLink);
		} else if (nextLink.getUpstreamCANode() == this) {
			handleSwapWithUpStreamEnd(a, time, nextLink);
		} else {
			throw new RuntimeException("Inconsitent network or agent plan!");
		}

	}

	private void handleSwapWithUpStreamEnd(CAMoveableEntity a, double time,
			CASingleLaneLink nextLink) {

		CAMoveableEntity swapA = nextLink.getParticles()[0];
		this.pollAgent(0);
		this.putAgent(swapA);
		nextLink.getParticles()[0] = a;
		a.materialize(0, 1);
		nextLink.fireUpstreamLeft(swapA, time);
		nextLink.fireUpstreamEntered(a, time);
		a.moveOverNode(nextLink, time);

		nextLink.getLastLeftTimes()[0] = time;
		this.towardsLinkLastExitTimes.put(nextLink, time);

		checkPostConditionForAgentEnteredLinkFromUpstreamEnd(nextLink, a, time);
		checkPostConditionForAgentSwapedToNode(swapA, time);

	}

	private void checkPostConditionForAgentSwapedToNode(CAMoveableEntity swapA,
			double time) {
		Id<Link> nextLinkId = swapA.getNextLinkId();
		if (nextLinkId == null) {
			letAgentArrive(swapA, time);
			return;
		}
		CASingleLaneLink nextLink = (CASingleLaneLink) this.net
				.getCALink(nextLinkId);
		if (nextLink.getDownstreamCANode() == this) {
			checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(
					swapA, nextLink, time);
		} else {
			checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(
					swapA, nextLink, time);
		}

	}

	void letAgentArrive(CAMoveableEntity a, double time) {
		this.pollAgent(time);
		CANetsimEngine engine = this.net.getEngine();
		if (engine != null) {
			engine.letVehicleArrive((CAVehicle) a);
		}
		this.net.unregisterAgent(a);
		triggerPrevAgent(time);
	}

	private void checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(
			CAMoveableEntity swapA, CASingleLaneLink nextLink, double time) {

		CAMoveableEntity inFrontOfMe = nextLink.getParticles()[0];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == -1) {
				// double d = Math.min(nextLink.getD(swapA),
				// nextLink.getD(inFrontOfMe));
				double d = nextLink.getD(swapA);
				d *= this.ratio;
				triggerSWAP(swapA, this, time + d + this.tFree);
			}
		} else {
			triggerTTA(swapA, this, time + this.tFree);
		}

	}

	private void checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(
			CAMoveableEntity swapA, CASingleLaneLink nextLink, double time) {
		CAMoveableEntity inFrontOfMe = nextLink.getParticles()[nextLink
				.getNumOfCells() - 1];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == 1) {
				// double d = Math.min(nextLink.getD(swapA),
				// nextLink.getD(inFrontOfMe));
				double d = nextLink.getD(swapA);
				d *= this.ratio;
				triggerSWAP(swapA, this, time + d + this.tFree);
			}
		} else {
			triggerTTA(swapA, this, time + this.tFree);
		}
	}

	private void handleSwapWithDownStreamEnd(CAMoveableEntity a, double time,
			CASingleLaneLink nextLink) {

		CAMoveableEntity swapA = nextLink.getParticles()[nextLink
				.getNumOfCells() - 1];
		this.pollAgent(0);
		this.putAgent(swapA);
		nextLink.getParticles()[nextLink.getNumOfCells() - 1] = a;
		a.materialize(nextLink.getNumOfCells() - 1, -1);
		nextLink.fireDownstreamLeft(swapA, time);
		nextLink.fireDownstreamEntered(a, time);
		a.moveOverNode(nextLink, time);

		nextLink.getLastLeftTimes()[nextLink.getNumOfCells() - 1] = time;
		this.towardsLinkLastExitTimes.put(nextLink, time);

		// check post-conditions & new events
		checkPostConditionForAgentEnteredLinkFromDownstreamEnd(nextLink, a,
				time);
		checkPostConditionForAgentSwapedToNode(swapA, time);
	}

	@Override
	public Node getNode() {
		return this.node;
	}

	public double getLastNodeExitTimeForAgent(CAMoveableEntity a) {
		Id<Link> n = a.getNextLinkId();
		CASingleLaneLink nextLink = (CASingleLaneLink) this.net.getCALink(n);
		return this.towardsLinkLastExitTimes.get(nextLink);
	}

	@Override
	public void lock() {
		this.lock.lock();
		for (CASingleLaneLink l : this.links) {
			l.lock.lock();
		}

	}

	@Override
	public void unlock() {
		for (CASingleLaneLink l : this.links) {
			l.lock.unlock();
		}
		this.lock.unlock();

	}

	@Override
	public boolean tryLock() {
		if (!this.lock.tryLock()) {
			return false;
		}
		int locked = 0;
		for (; locked < this.links.size(); locked++) {
			CASingleLaneLink l = this.links.get(locked);
			if (!l.lock.tryLock()) {
				break;
			}
		}
		if (locked < this.links.size()) {
			for (int i = 0; i < locked; i++) {
				CASingleLaneLink l = this.links.get(i);
				l.lock.unlock();
			}
			this.lock.unlock();
			return false;
		}

		return true;
	}

	@Override
	public boolean isLocked() {
		if (this.lock.isLocked()) {
			return true;
		}
		for (CASingleLaneLink l : this.links) {
			if (l.isLocked()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int threadNR() {
		return this.threadNR;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}
}
