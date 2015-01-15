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
public class CAMultiLaneNode implements CANode {

	private static final Logger log = Logger.getLogger(CAMultiLaneNode.class);

	private final Node node;
	private final AbstractCANetwork net;

	private final List<CAMultiLaneLink> links = new ArrayList<CAMultiLaneLink>();

	private final Map<CALink, double[]> towardsLinkLastExitTimes = new HashMap<CALink, double[]>();
	// private final Map<CALink, CAMoveableEntity> towardsLinkLastExitAgents =
	// new HashMap<CALink, CAMoveableEntity>();

	// private final double width;

	private final double tFree;

	private final double ratio;

	private final double epsilon;

	final ReentrantLock lock = new ReentrantLock();

	private int threadNR;

	private final int lanes;

	private final CAMoveableEntity[] slots;

	private final double width;

	private double x;

	private double y;

	public CAMultiLaneNode(Node node, AbstractCANetwork net) {

		this.threadNR = MatsimRandom.getRandom().nextInt(
				AbstractCANetwork.NR_THREADS);
		this.node = node;
		this.net = net;
		double width = 0;
		for (Link l : node.getInLinks().values()) {
			if (l.getCapacity() > width) {
				width = l.getCapacity();
			}
		}
		this.width = width;
		this.lanes = (int) (width / (AbstractCANetwork.PED_WIDTH / 1) + 0.5);
		double laneWidth = width / lanes;
		this.ratio = AbstractCANetwork.PED_WIDTH / laneWidth;
		double laneCellLength = this.ratio
				/ (AbstractCANetwork.RHO_HAT * AbstractCANetwork.PED_WIDTH);
		this.tFree = laneCellLength / AbstractCANetwork.V_HAT;
		this.epsilon = tFree / 1000;

		this.slots = new CAMoveableEntity[lanes];

		this.x = node.getCoord().getX();
		this.y = node.getCoord().getY();

	}

	public void setThreadNr(int thread) {
		this.threadNR = thread;
	}

	public double getWidth() {
		return width;
	}

	/* package */double getNodeRatio() {
		return this.ratio;
	}

	@Override
	public void addLink(CALink link) {
		if (!(link instanceof CAMultiLaneLink)) {
			throw new RuntimeException("Incompatible link type:"
					+ link.getClass().toString());
		}
		this.links.add((CAMultiLaneLink) link);
		this.towardsLinkLastExitTimes.put(link, new double[lanes]);
	}

	public CAMoveableEntity peekForAgentInSlot(int slot) {
		return slots[slot];
	}

	public void putAgentInSlot(int slot, CAMoveableEntity a) {
		if (slots[slot] != null) {
			throw new RuntimeException("Slot: " + slot + " is not empty!");
		}
		slots[slot] = a;
		a.moveToNode(this);

	}

	public CAMoveableEntity pollAgentFromSlot(int slot) {
		CAMoveableEntity ret = slots[slot];
		slots[slot] = null;
		return ret;
	}

	// TODO rename method to drawAgentWhoWantsToEnterLink
	public CAMoveableEntity peekForAgentWhoWantsToEnterLaneOnLink(Id<Link> id) {

		List<CAMoveableEntity> cands = new ArrayList<>();
		for (int i = 0; i < lanes; i++) {
			if (slots[i] != null && slots[i].getNextLinkId().equals(id)) {
				cands.add(slots[i]);
			}
		}
		if (cands.size() == 0) {
			return null;
		}

		// TODO create local instance of random to make this deterministic [GL
		// Nov '14]
		return cands.get(MatsimRandom.getRandom().nextInt(cands.size()));
	}

	public boolean tryTriggerAgentsWhoWantToEnterLaneOnLink(Id<Link> id,
			double time) {
		boolean ret = false;
		List<CAMoveableEntity> cands = new ArrayList<>();
		for (int i = 0; i < lanes; i++) {
			if (slots[i] != null && slots[i].getNextLinkId().equals(id)) {
				cands.add(slots[i]);
				ret = true;
			}
		}
		if (cands.size() > 0) {
			// TODO create local instance of random to make this deterministic
			// [GL Nov '14]
			CAMoveableEntity cand = cands.get(MatsimRandom.getRandom().nextInt(
					cands.size()));

			// here, the agent on the node (A) can move on since an oncoming
			// agent (B) just entered that node an therefore a free spot
			// appeared on the next link. Conceptionally this is something like
			// a swap and agent A should experience the conflict delay of D.
			// Another interpretation of the situation would be that A 'follows'
			// B and should experience the time gap delay z. For now we leave it
			// by z [GL Nov '14].
			double z = CAMultiLaneLink.getZ(cand);
			z *= this.ratio;
			triggerTTA(cand, this, time + z);
		}

		return ret;
	}

	@Override
	public void handleEvent(CAEvent e) {
		CAMoveableEntity a = e.getCAAgent();

		// validate situation
		if (this.slots[a.getLane()] != a) {
			// log.info("Agent: " + a + " is no longer there, dropping event.");
			return;
		}

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
		CAMultiLaneLink nextLink = (CAMultiLaneLink) this.net
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
			CAMultiLaneLink nextLink, CAMoveableEntity a, double time) {

		// check pre-condition
		double z = CAMultiLaneLink.getZ(a);
		z *= this.ratio;
		// 1. try to find empty slot with last left time < time - z
		List<Integer> cands = new ArrayList<Integer>(nextLink.getNrLanes());
		for (int lane = 0; lane < nextLink.getNrLanes(); lane++) {
			if (nextLink.getParticles(lane)[0] == null) {
				if (nextLink.getLastLeftTimes(lane)[0] <= (time - z + epsilon)) {
					cands.add(lane);
				}
			}
		}
		if (cands.size() > 0) {
			Integer lane = cands.get(MatsimRandom.getRandom().nextInt(
					cands.size()));
			handleTTAEnterNextLinkFromUpstreamEndOnPrecondition1(nextLink, a,
					time, lane);
			return;
		}
		// 2.try to find empty slot with last left time >= time -z
		// maybe we should look for slow with that minimizes (last left time -
		// time -z)
		for (int lane = 0; lane < nextLink.getNrLanes(); lane++) {
			if (nextLink.getParticles(lane)[0] == null) {
				handleTTAEnterNextLinkFromUpstreamEndOnPrecondition2(nextLink,
						a, time, lane);
				return;
			}
		}

	}

	private void handleTTAEnterNextLinkFromUpstreamEndOnPrecondition1(
			CAMultiLaneLink nextLink, CAMoveableEntity a, double time, int lane) {

		int nodeSlot = a.getLane();
		this.towardsLinkLastExitTimes.get(nextLink)[nodeSlot] = time;

		this.pollAgentFromSlot(nodeSlot);

		nextLink.getParticles(lane)[0] = a;
		a.materialize(0, 1, lane);
		a.moveOverNode(nextLink, time);
		nextLink.fireUpstreamEntered(a, time);

		// check post-condition and generate events
		// first for persons behind
		triggerPrevAgent(time);

		// second for oneself
		checkPostConditionForAgentEnteredLinkFromUpstreamEnd(nextLink, a, time,
				lane);

	}

	private void checkPostConditionForAgentEnteredLinkFromUpstreamEnd(
			CAMultiLaneLink nextLink, CAMoveableEntity a, double time, int lane) {
		CAMoveableEntity inFrontOfMe = nextLink.getParticles(lane)[1];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == -1) { // oncoming
				double d = CAMultiLaneLink.getD(a);
				d *= this.ratio;
				triggerSWAP(a, nextLink, time + d + nextLink.getTFree());
			}
		} else {
			triggerTTA(a, nextLink, time + nextLink.getTFree());
		}

	}

	private void handleTTAEnterNextLinkFromUpstreamEndOnPrecondition2(
			CAMultiLaneLink nextLink, CAMoveableEntity a, double time, int lane) {
		double z = CAMultiLaneLink.getZ(a);
		z *= this.ratio;
		double zStar = z - (time - nextLink.getLastLeftTimes(lane)[0]);
		double nextTime = time + zStar;

		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	// ///////////////////////

	private void handleTTAEnterNextLinkFromDownstreamEnd(
			CAMultiLaneLink nextLink, CAMoveableEntity a, double time) {

		int idxLastCell = nextLink.getNumOfCells() - 1;

		// check pre-condition
		double z = CAMultiLaneLink.getZ(a);
		z *= this.ratio;
		// 1. try to find empty slot with last left time < time - z
		List<Integer> cands = new ArrayList<Integer>(nextLink.getNrLanes());
		for (int lane = 0; lane < nextLink.getNrLanes(); lane++) {
			if (nextLink.getParticles(lane)[idxLastCell] == null) {
				if (nextLink.getLastLeftTimes(lane)[idxLastCell] <= (time - z + epsilon)) {
					cands.add(lane);
				}
			}
		}
		if (cands.size() > 0) {
			Integer lane = cands.get(MatsimRandom.getRandom().nextInt(
					cands.size()));
			handleTTAEnterNextLinkFromDownstreamEndOnPrecondition1(nextLink, a,
					time, lane);
			return;
		}
		// 2.try to find empty slot with last left time >= time -z
		// maybe we should look for slow with that minimizes (last left time -
		// time -z)
		for (int lane = 0; lane < nextLink.getNrLanes(); lane++) {
			if (nextLink.getParticles(lane)[idxLastCell] == null) {
				handleTTAEnterNextLinkFromDownstreamEndOnPrecondition2(
						nextLink, a, time, lane);
				return;
			}
		}

	}

	private void handleTTAEnterNextLinkFromDownstreamEndOnPrecondition1(
			CAMultiLaneLink nextLink, CAMoveableEntity a, double time, int lane) {

		int nodeSlot = a.getLane();

		this.towardsLinkLastExitTimes.get(nextLink)[nodeSlot] = time;

		this.pollAgentFromSlot(nodeSlot);

		nextLink.getParticles(lane)[nextLink.getNumOfCells() - 1] = a;
		a.materialize(nextLink.getNumOfCells() - 1, -1, lane);
		a.moveOverNode(nextLink, time);
		nextLink.fireDownstreamEntered(a, time);

		// check post-condition and generate events
		// first for persons behind
		triggerPrevAgent(time);

		// second for oneself
		checkPostConditionForAgentEnteredLinkFromDownstreamEnd(nextLink, a,
				time, lane);

	}

	private void checkPostConditionForAgentEnteredLinkFromDownstreamEnd(
			CAMultiLaneLink nextLink, CAMoveableEntity a, double time, int lane) {
		CAMoveableEntity inFrontOfMe = nextLink.getParticles(lane)[nextLink
				.getNumOfCells() - 2];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == 1) { // oncoming
				double d = CAMultiLaneLink.getD(a);
				d *= this.ratio;
				triggerSWAP(a, nextLink, time + d + nextLink.getTFree());
			}
		} else {
			triggerTTA(a, nextLink, time + nextLink.getTFree());
		}

	}

	private void handleTTAEnterNextLinkFromDownstreamEndOnPrecondition2(
			CAMultiLaneLink nextLink, CAMoveableEntity a, double time, int lane) {
		double z = CAMultiLaneLink.getZ(a);
		z *= this.ratio;
		double zStar = z
				- (time - nextLink.getLastLeftTimes(lane)[nextLink
						.getNumOfCells() - 1]);
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
		List<Tuple<CAMultiLaneLink, CAMoveableEntity>> cands = new ArrayList<Tuple<CAMultiLaneLink, CAMoveableEntity>>();
		for (CAMultiLaneLink l : this.links) {
			if (l.getDownstreamCANode() == this) {
				for (int lane = 0; lane < l.getNrLanes(); lane++) {
					CAMoveableEntity cand = l.getParticles(lane)[l
							.getNumOfCells() - 1];
					if (cand != null && cand.getDir() == 1) {
						cands.add(new Tuple<CAMultiLaneLink, CAMoveableEntity>(
								l, cand));
						cap += l.getLink().getCapacity();
					}
				}
			} else if (l.getUpstreamCANode() == this) {
				for (int lane = 0; lane < l.getNrLanes(); lane++) {
					CAMoveableEntity cand = l.getParticles(lane)[0];
					if (cand != null && cand.getDir() == -1) {
						cands.add(new Tuple<CAMultiLaneLink, CAMoveableEntity>(
								l, cand));
						cap += l.getLink().getCapacity();
					}
				}
			}
		}
		if (cands.size() == 0) {
			return;
		}

		double rnd = cap * MatsimRandom.getRandom().nextDouble();
		double incr = cap / cands.size();
		double comp = incr;
		for (Tuple<CAMultiLaneLink, CAMoveableEntity> t : cands) {
			if (rnd <= comp) {
				double z = CAMultiLaneLink.getZ(t.getSecond());
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
		CAMultiLaneLink nextLink = (CAMultiLaneLink) this.net
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
			CAMultiLaneLink nextLink) {

		CAMoveableEntity swapA = null;
		List<Integer> cands = new ArrayList<Integer>(nextLink.getNrLanes());
		for (int lane = 0; lane < nextLink.getNrLanes(); lane++) {
			CAMoveableEntity cand = nextLink.getParticles(lane)[0];
			if (cand != null && cand.getDir() == -1) {
				cands.add(lane);
			}
		}
		if (cands.size() == 0) {
			// log.info("situation at link's upstream end for agent: " + a
			// + " has changed, dropping event.");
			return;
		}
		int lane = cands.get(MatsimRandom.getRandom().nextInt(cands.size()));
		swapA = nextLink.getParticles(lane)[0];
		swapA.invalidate();

		this.pollAgentFromSlot(a.getLane());
		this.putAgentInSlot(a.getLane(), swapA);

		swapA.materialize(-100, Integer.MIN_VALUE, a.getLane());

		nextLink.getParticles(lane)[0] = a;
		a.materialize(0, 1, lane);
		nextLink.fireUpstreamLeft(swapA, time);
		nextLink.fireUpstreamEntered(a, time);
		a.moveOverNode(nextLink, time);

		nextLink.getLastLeftTimes(lane)[0] = time;
		this.towardsLinkLastExitTimes.get(nextLink)[a.getLane()] = time;

		checkPostConditionForAgentEnteredLinkFromUpstreamEnd(nextLink, a, time,
				lane);
		if (swapA.getNextLinkId() == null) {
			letAgentArrive(swapA, time);
		} else {
			checkPostConditionForAgentSwapedToNode(swapA, time);
		}

	}

	private void checkPostConditionForAgentSwapedToNode(CAMoveableEntity swapA,
			double time) {
		Id<Link> nextLinkId = swapA.getNextLinkId();

		CAMultiLaneLink nextLink = (CAMultiLaneLink) this.net
				.getCALink(nextLinkId);
		if (nextLink.getDownstreamCANode() == this) {

			checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(
					swapA, nextLink, time);
		} else {
			checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(
					swapA, nextLink, time);
		}

	}

	/* package */void checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(
			CAMoveableEntity swapA, CAMultiLaneLink nextLink, double time) {

		int cellIdx = 0;

		// count options (SWAP and TTA) and make choice
		// according to the shares of SWAP and TTA
		int optSwap = 0;
		int optTTA = 0;
		for (int lane = 0; lane < nextLink.getNrLanes(); lane++) {
			CAMoveableEntity cand = nextLink.getParticles(lane)[cellIdx];
			if (cand == null) {
				optTTA++;
			} else if (cand.getDir() == -1) {
				optSwap++;
			}
		}
		int options = optSwap + optTTA;
		if (options == 0) {
			return; // all occupied nothing we can do here :-(
		}

		// There are likely situations where TTA and SWAP are possible. The
		// action should be chosen so that the overall flow composition gets not
		// disturbed. For now the option with most opportunities is chosen to
		// increase the likelihood that it is still valid when the create event
		// will be executed. If this does not work out well we could try making
		// this probabilistic. [GL Nov '14]

		if (optSwap >= optTTA) {
			double d = CAMultiLaneLink.getD(swapA);
			d *= this.ratio;
			triggerSWAP(swapA, this, time + d + this.tFree);
		} else {
			triggerTTA(swapA, this, time + this.tFree);
		}

	}

	/* package */void checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(
			CAMoveableEntity swapA, CAMultiLaneLink nextLink, double time) {

		int cellIdx = nextLink.getNumOfCells() - 1;

		// count options (SWAP and TTA) and make choice
		// according to the shares of SWAP and TTA
		int optSwap = 0;
		int optTTA = 0;
		for (int lane = 0; lane < nextLink.getNrLanes(); lane++) {
			CAMoveableEntity cand = nextLink.getParticles(lane)[cellIdx];
			if (cand == null) {
				optTTA++;
			} else if (cand.getDir() == 1) {
				optSwap++;
			}
		}
		int options = optSwap + optTTA;
		if (options == 0) {
			return; // all occupied nothing we can do here :-(
		}

		// There are likely situations where TTA and SWAP are possible. The
		// action should be chosen so that the overall flow composition gets not
		// disturbed. For now the option with most opportunities is chosen to
		// increase the likelihood that it is still valid when the create event
		// will be executed. If this does not work out well we could try making
		// this probabilistic. [GL Nov '14]

		if (optSwap >= optTTA) {
			double d = CAMultiLaneLink.getD(swapA);
			d *= this.ratio;
			triggerSWAP(swapA, this, time + d + this.tFree);
		} else {
			triggerTTA(swapA, this, time + this.tFree);
		}

	}

	private void handleSwapWithDownStreamEnd(CAMoveableEntity a, double time,
			CAMultiLaneLink nextLink) {

		int cellIdx = nextLink.getNumOfCells() - 1;

		List<Integer> cands = new ArrayList<Integer>(nextLink.getNrLanes());
		for (int lane = 0; lane < nextLink.getNrLanes(); lane++) {
			CAMoveableEntity cand = nextLink.getParticles(lane)[cellIdx];
			if (cand != null && cand.getDir() == 1) {
				cands.add(lane);
			}
		}
		if (cands.size() == 0) {
			// log.info("situation at link's downstream end for agent: " + a
			// + " has changed, dropping event.");
			return;
		}

		int lane = cands.get(MatsimRandom.getRandom().nextInt(cands.size()));
		CAMoveableEntity swapA = nextLink.getParticles(lane)[cellIdx];
		swapA.invalidate();

		this.pollAgentFromSlot(a.getLane());
		this.putAgentInSlot(a.getLane(), swapA);

		swapA.materialize(-200, Integer.MIN_VALUE, a.getLane());

		nextLink.getParticles(lane)[nextLink.getNumOfCells() - 1] = a;
		a.materialize(nextLink.getNumOfCells() - 1, -1, lane);
		nextLink.fireDownstreamLeft(swapA, time);
		nextLink.fireDownstreamEntered(a, time);
		a.moveOverNode(nextLink, time);

		nextLink.getLastLeftTimes(lane)[nextLink.getNumOfCells() - 1] = time;
		this.towardsLinkLastExitTimes.get(nextLink)[a.getLane()] = time;

		// check post-conditions & new events
		checkPostConditionForAgentEnteredLinkFromDownstreamEnd(nextLink, a,
				time, lane);
		if (swapA.getNextLinkId() == null) {
			letAgentArrive(swapA, time);
		} else {
			checkPostConditionForAgentSwapedToNode(swapA, time);
		}
	}

	@Override
	public Node getNode() {
		return this.node;
	}

	public double[] getLastNodeExitTimeForAgent(CAMoveableEntity a) {
		Id<Link> n = a.getNextLinkId();
		CAMultiLaneLink nextLink = (CAMultiLaneLink) this.net.getCALink(n);
		return this.towardsLinkLastExitTimes.get(nextLink);
	}

	@Override
	public void lock() {
		this.lock.lock();
		for (CAMultiLaneLink l : this.links) {
			l.lock.lock();
		}

	}

	@Override
	public void unlock() {
		for (CAMultiLaneLink l : this.links) {
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
			CAMultiLaneLink l = this.links.get(locked);
			if (!l.lock.tryLock()) {
				break;
			}
		}
		if (locked < this.links.size()) {
			for (int i = 0; i < locked; i++) {
				CAMultiLaneLink l = this.links.get(i);
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
		for (CAMultiLaneLink l : this.links) {
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

	public double getTFree() {
		return this.tFree;
	}

	public final int getNRLanes() {
		return this.lanes;
	}

	@Override
	public String toString() {
		return this.getNode().getId().toString();
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	public void letAgentArrive(CAMoveableEntity a, double time) {
		CANetsimEngine engine = this.net.getEngine();
		if (engine != null) {
			engine.letVehicleArrive((CAVehicle) a);
		}
		this.slots[a.getLane()] = null;
		this.net.unregisterAgent(a);
		triggerPrevAgent(time);
	}

}
