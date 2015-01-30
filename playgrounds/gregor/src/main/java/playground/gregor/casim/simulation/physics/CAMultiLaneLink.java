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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.simulation.CANetsimEngine;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;

/**
 * This link models the dynamics for pedestrian streams (uni- and bidirectional)
 * 
 * @author laemmel
 *
 */
public class CAMultiLaneLink implements CANetworkEntity, CALink {

	private static final Logger log = Logger.getLogger(CAMultiLaneLink.class);

	static final double LANESWITCH_TS_TTA = 1.8;
	private static final double LANESWITCH_TS_SWAP = 0;
	// private static final double LANESWITCH_TS_TTA = 1111.8;
	// private static final double LANESWITCH_TS_SWAP = 1111;

	private final Link dsl;
	private final Link usl;

	private final CAMoveableEntity[][] particles;
	private final double[][] lastLeftDsTimes;
	private final double[][] lastLeftUsTimes;

	private final int size;

	private final CAMultiLaneNode ds;

	private final CAMultiLaneNode us;

	private final AbstractCANetwork net;

	// private final double cellLength;

	private double width;

	private final double tFree;

	private double ratio;
	private final double epsilon; // TODO check if this is still needed [GL Nov.
									// '14]

	final ReentrantLock lock = new ReentrantLock();

	private int threadNr;

	private final int lanes;

	private double laneWidth;

	private double laneCellLength;

	private final double x;

	private final double y;

	private static int EXP_WARN_CNT = 0;

	private final LinkedHashSet<CAMoveableEntity> dsWaitQ = new LinkedHashSet<>();
	private final LinkedHashSet<CAMoveableEntity> usWaitQ = new LinkedHashSet<>();

	static MultiLaneDensityEstimator k; // TODO clean this up

	public CAMultiLaneLink(Link dsl, Link usl, CAMultiLaneNode ds,
			CAMultiLaneNode us, AbstractCANetwork net) {

		this.threadNr = MatsimRandom.getRandom().nextInt(
				AbstractCANetwork.NR_THREADS);

		this.width = dsl.getCapacity();// TODO this is a misuse of the link's
										// capacity attribute. Needs to be
										// fixed!
		this.lanes = (int) (this.width / (AbstractCANetwork.PED_WIDTH / 1) + 0.5);
		this.laneWidth = this.width / lanes;
		this.ratio = AbstractCANetwork.PED_WIDTH / this.laneWidth;
		this.laneCellLength = this.ratio
				/ (AbstractCANetwork.RHO_HAT * AbstractCANetwork.PED_WIDTH);
		this.size = (int) (0.5 + dsl.getLength() / this.laneCellLength);

		this.dsl = dsl;
		this.usl = usl;
		this.particles = new CAMoveableEntity[lanes][this.size];
		this.lastLeftDsTimes = new double[lanes][this.size];
		this.lastLeftUsTimes = new double[lanes][this.size];
		this.ds = ds;
		this.us = us;
		this.tFree = this.laneCellLength / AbstractCANetwork.V_HAT;
		this.epsilon = tFree / 1000;
		this.net = net;

		x = this.getLink().getToNode().getCoord().getX();
		y = this.getLink().getToNode().getCoord().getY();
	}

	public void setThreadNr(int thread) {
		this.threadNr = thread;
	}

	public double getLaneWidth() {
		return laneWidth;
	}

	/* package */static double getD(CAMoveableEntity a) {
		final double rho = a.getRho();
		final double tmp = Math.pow(rho * AbstractCANetwork.PED_WIDTH,
				AbstractCANetwork.GAMMA);
		final double d = AbstractCANetwork.ALPHA + AbstractCANetwork.BETA * tmp;
		return d;
	}

	/* package */static double getZ(CAMoveableEntity a) {
		double d = getD(a);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		// double z = this.tFree + d;
		return z;
	}

	@Override
	public void handleEvent(CAEvent e) {
		CAMoveableEntity a = e.getCAAgent();
		if (e.getCAEventType() != CAEventType.TTE
				&& this.particles[a.getLane()][a.getPos()] != a) {
			// log.warn("Agent: " + a + " not on expected position!!");
			return;
		}
		a.setRho(k.estRho(a));
		double time = e.getEventExcexutionTime();
		if (e.getCAEventType() == CAEventType.SWAP) {
			handelSwap(a, time);
		} else if (e.getCAEventType() == CAEventType.TTA) {
			handleTTA(a, time);
		} else if (e.getCAEventType() == CAEventType.TTE) {
			handleTTE(a, time);
		} else {
			throw new RuntimeException("Unknown event type: "
					+ e.getCAEventType());
		}
	}

	private void handleTTE(CAMoveableEntity a, double time) {
		// if (a.getId().toString().equals("3096")) {
		// log.error("Gotcha!");
		// }
		int dir = a.getDir();
		int desiredPos = -1;
		if (dir == 1) {
			desiredPos = this.size - 1;
		} else if (dir == -1) {
			desiredPos = 0;
		}
		List<Integer> cands = new ArrayList<>();
		for (int lane = 0; lane < lanes; lane++) {
			if (this.particles[lane][desiredPos] == null) {
				cands.add(lane);
			}
		}
		if (cands.size() > 0) {
			int cand = cands
					.get(MatsimRandom.getRandom().nextInt(cands.size()));
			if (dir == 1) {
				handleTTEDownStream(a, time, cand, desiredPos);
			} else {
				handleTTEUpStream(a, time, cand, desiredPos);
			}
		}

	}

	private void handleTTEUpStream(CAMoveableEntity a, double time, int lane,
			int desiredPos) {

		this.net.registerAgent(a);
		if (!this.usWaitQ.remove(a)) {
			throw new RuntimeException("Agent:" + a
					+ " is not in the upstream waiting queue!");
		}
		;
		this.particles[lane][desiredPos] = a;
		a.materialize(desiredPos, -1, lane);
		if (a instanceof CAVehicle) {
			this.net.getEventsManager().processEvent(
					new Wait2LinkEvent((int) time, ((CAVehicle) a).getDriver()
							.getId(), this.usl.getId(), ((CAVehicle) a)
							.getVehicleId()));
		}
		checkPostConditionForAgentOnUpStreamAdvance(desiredPos + 1, a, time,
				lane);

	}

	private void handleTTEDownStream(CAMoveableEntity a, double time, int lane,
			int desiredPos) {
		this.net.registerAgent(a);
		if (!this.dsWaitQ.remove(a)) {
			throw new RuntimeException("Agent:" + a
					+ " is not in the downstream waiting queue!");
		}
		this.particles[lane][desiredPos] = a;
		a.materialize(desiredPos, 1, lane);
		if (a instanceof CAVehicle) {
			this.net.getEventsManager().processEvent(
					new Wait2LinkEvent((int) time, ((CAVehicle) a).getDriver()
							.getId(), this.dsl.getId(), ((CAVehicle) a)
							.getVehicleId()));
		}
		checkPostConditionForAgentOnDownStreamAdvance(desiredPos - 1, a, time,
				lane);
	}

	private void handleTTA(CAMoveableEntity a, double time) {

		int idx = a.getPos();
		int dir = a.getDir();
		int nextIdx = idx + dir;
		if (nextIdx < 0) {
			handleTTAUpStreamNode(a, time);
		} else if (nextIdx >= this.size) {
			handleTTADownStreamNode(a, time);
		} else {
			handleTTAOnLink(a, time, dir);
		}

	}

	private void handleTTAOnLink(CAMoveableEntity a, double time, int dir) {
		if (dir == -1) {
			handleTTAOnLinkUpStream(a, time);
		} else {
			handleTTAOnLinkDownStream(a, time);
		}
	}

	private void handleTTAOnLinkDownStream(CAMoveableEntity a, double time) {
		int idx = a.getPos();
		int lane = a.getLane();
		int dir = a.getDir();

		// check pre-condition
		int newLane = this.particles[lane][idx + dir] == null ? lane : -1;
		double z = getZ(a);
		z *= this.ratio;
		z -= epsilon;

		if (a.getRho() >= LANESWITCH_TS_TTA) {
			newLane = -1;
			int from = lane > 0 ? lane - 1 : lane;
			int to = lane < (this.lanes - 1) ? lane + 1 : lane;
			// 1. collect candidates
			List<Integer> cands = new ArrayList<>();// maybe gnu trove
													// collections
													// would helpful here to
													// speed
													// things up a bit
			for (int i = from; i <= to; i++) {
				if (this.particles[i][idx + dir] == null) {
					double timeGap = time - this.lastLeftDsTimes[i][idx + dir];
					if (timeGap >= z) {// candidate
						cands.add(i);
					}
				}
			}

			// 2. choose the one with the largest free space
			int bestTr = 0;
			for (int cand : cands) {
				int tr = dir;
				;
				while (tr < 10) { // figure this out, something like 1/rhoHat
									// [GL,
									// Jan '15]
					if (this.particles[cand][idx + tr] == null) {
						tr += dir;
					} else {
						break;
					}
					if (idx + tr >= this.size - 1) {
						break;
					}
				}

				if (tr > bestTr) {
					bestTr = tr;
					newLane = cand;
				}
			}
		}
		if (newLane > -1) {
			handleTTAOnLinkDownStreamOnPreCondition1(a, time, idx, lane,
					newLane);
		} else if (this.particles[lane][idx + 1] == null) {
			handleTTAOnLinkDownStreamOnPreCondition2(a, time, idx, lane);
		} else {
			handleTTAOnLinkDownStreamOnPreCondition3(a, time);
		}
	}

	private void handleTTAOnLinkDownStreamOnPreCondition1(CAMoveableEntity a,
			double time, int idx, int currLane, int newLane) {

		this.lastLeftDsTimes[currLane][idx] = time;
		this.particles[currLane][idx] = null;
		this.particles[newLane][idx + 1] = a;
		a.setLane(newLane);
		a.proceed();

		// check post-condition and generate events
		// first for persons behind
		checkPostConditionForPersonBehindOnDownStreamAdvance(idx, time,
				currLane);

		// second for oneself
		checkPostConditionForAgentOnDownStreamAdvance(idx, a, time, newLane);

	}

	private void checkPostConditionForPersonBehindOnDownStreamAdvance(int idx,
			double time, int lane) {
		if (idx - 1 < 0) {

			if (!this.usWaitQ.isEmpty()) {
				CAMoveableEntity el = this.usWaitQ.iterator().next();
				triggerTTE(el, this, time);// move wait first
			}
			this.us.tryTriggerAgentsWhoWantToEnterLaneOnLink(this.dsl.getId(),
					time);
		} else {
			CAMoveableEntity toBeTriggered = this.particles[lane][idx - 1];
			if (toBeTriggered != null) {
				if (toBeTriggered.getDir() == 1) {
					toBeTriggered.setRho(k.estRho(toBeTriggered));
					double z = getZ(toBeTriggered);
					z *= this.ratio;
					triggerTTA(toBeTriggered, this, time + z);
				}
			}
		}
	}

	private void checkPostConditionForAgentOnDownStreamAdvance(int idx,
			CAMoveableEntity a, double time, int lane) {
		if (idx + 2 >= this.size) {
			checkPostConditionForAgentOnDownStreamAdvanceWhoIsInFrontOfNode(a,
					time);

		} else {
			CAMoveableEntity inFrontOfMe = this.particles[lane][idx + 2];
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getDir() == -1) { // oncoming
					double d = getD(a);
					d *= this.ratio;
					triggerSWAP(a, this, time + d + this.tFree);
				}
			} else {
				triggerTTA(a, this, time + this.tFree);
			}
		}

	}

	private void checkPostConditionForAgentOnDownStreamAdvanceWhoIsInFrontOfNode(
			CAMoveableEntity a, double time) {

		// See discussion in
		// CANodeParallelQueues.checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd
		// and adapt accordingly [GL Nov '14]

		// count options (SWAP and TTA) and make choice
		// according to the shares of SWAP and TTA
		int optTTA = 0;
		int optSWAP = 0;

		for (int slot = 0; slot < ds.getNRLanes(); slot++) {
			CAMoveableEntity cand = ds.peekForAgentInSlot(slot);
			if (cand == null) {
				optTTA++;
			} else if (this.usl != null
					&& cand.getNextLinkId().equals(this.usl.getId())) {
				optSWAP++;
			}
		}

		if (optTTA + optSWAP == 0) {
			return;
		}

		// There are likely situations where TTA and SWAP are possible. The
		// action should be chosen so that the overall flow composition gets not
		// disturbed. For now the option with most opportunities is chosen to
		// increase the likelihood that it is still valid when the create event
		// will be executed. If this does not work out well we could try making
		// this probabilistic. [GL Nov '14]

		if (optSWAP >= optTTA) {
			double d = CAMultiLaneLink.getD(a);
			d *= this.ds.getNodeRatio();
			triggerSWAP(a, this, time + d + this.tFree);
		} else {
			triggerTTA(a, this, time + this.tFree);
		}

	}

	private void handleTTAOnLinkDownStreamOnPreCondition2(CAMoveableEntity a,
			double time, int idx, int lane) {

		double z = getZ(a);
		z *= this.ratio;
		double zStar = z - (time - this.lastLeftDsTimes[lane][idx + 1]);
		double nextTime = time + zStar;
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnLinkDownStreamOnPreCondition3(CAMoveableEntity a,
			double time) {
		// nothing to be done here.
	}

	private void handleTTAOnLinkUpStream(CAMoveableEntity a, double time) {
		int idx = a.getPos();
		int lane = a.getLane();
		int dir = a.getDir();

		// check pre-condition
		int newLane = this.particles[lane][idx + dir] == null ? lane : -1;
		double z = getZ(a);
		z *= this.ratio;
		z -= epsilon;

		if (a.getRho() >= LANESWITCH_TS_TTA) {
			newLane = -1;
			int from = lane > 0 ? lane - 1 : lane;
			int to = lane < (this.lanes - 1) ? lane + 1 : lane;
			// 1. collect candidates
			List<Integer> cands = new ArrayList<>();// maybe gnu trove
													// collections
													// would helpful here to
													// speed
													// things up a bit
			for (int i = from; i <= to; i++) {
				if (this.particles[i][idx - 1] == null) {
					double timeGap = time - this.lastLeftUsTimes[i][idx - 1];
					if (timeGap >= z) {// candidate
						cands.add(i);
					}
				}
			}

			// 2. choose the one with the largest free space
			int bestTr = 0;
			for (int cand : cands) {
				int tr = dir;
				;
				while (tr > -10) { // figure this out, something like 1/rhoHat
									// [GL,
									// Jan '15]
					if (this.particles[cand][idx + tr] == null) {
						tr += dir;
					} else {
						break;
					}
					if (idx + tr <= 0) {
						break;
					}
				}

				if (tr < bestTr) {
					bestTr = tr;
					newLane = cand;
				}
			}
		}
		if (newLane > -1) {
			handleTTAOnLinkUpStreamOnPreCondition1(a, time, idx, lane, newLane);
		} else if (this.particles[lane][idx - 1] == null) {
			handleTTAOnLinkUpStreamOnPreCondition2(a, time, idx, lane);
		} else {
			handleTTAOnLinkUpStreamOnPreCondition3(a, time);
		}

	}

	private void handleTTAOnLinkUpStreamOnPreCondition1(CAMoveableEntity a,
			double time, int idx, int currLane, int newLane) {

		this.lastLeftUsTimes[currLane][idx] = time;
		this.particles[currLane][idx] = null;
		this.particles[newLane][idx - 1] = a;
		a.setLane(newLane);
		a.proceed();

		// check post-condition and generate events
		// first for persons behind
		checkPostConditionForPersonBehindOnUpStreamAdvance(idx, time, currLane);

		// second for oneself
		checkPostConditionForAgentOnUpStreamAdvance(idx, a, time, newLane);

	}

	private void checkPostConditionForPersonBehindOnUpStreamAdvance(int idx,
			double time, int lane) {
		if (idx + 1 >= this.size) {
			if (!this.dsWaitQ.isEmpty()) {
				CAMoveableEntity el = this.dsWaitQ.iterator().next();
				triggerTTE(el, this, time);// move wait first
			}
			if (this.usl != null) {
				this.ds.tryTriggerAgentsWhoWantToEnterLaneOnLink(
						this.usl.getId(), time);
			}
		} else {
			CAMoveableEntity toBeTriggered = this.particles[lane][idx + 1];
			if (toBeTriggered != null) {
				if (toBeTriggered.getDir() == -1) {
					toBeTriggered.setRho(k.estRho(toBeTriggered));
					double z = getZ(toBeTriggered);
					z *= this.ratio;
					triggerTTA(toBeTriggered, this, time + z);
				}
			}
		}
	}

	private void checkPostConditionForAgentOnUpStreamAdvance(int idx,
			CAMoveableEntity a, double time, int lane) {
		if (idx - 2 < 0) {
			checkPostConditionForAgentOnUpStreamAdvanceWhoIsInFrontOfNode(a,
					time);
		} else {
			CAMoveableEntity inFrontOfMe = this.particles[lane][idx - 2];
			if (inFrontOfMe != null) {
				if (inFrontOfMe.getDir() == 1) { // oncoming
					double d = getD(a);
					d *= this.ratio;
					triggerSWAP(a, this, time + d + this.tFree);
				}
			} else {
				triggerTTA(a, this, time + this.tFree);
			}
		}

	}

	private void checkPostConditionForAgentOnUpStreamAdvanceWhoIsInFrontOfNode(
			CAMoveableEntity a, double time) {

		// See discussion in
		// CANodeParallelQueues.checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd
		// and adapt accordingly [GL Nov '14]

		// count options (SWAP and TTA) and make choice
		// according to the shares of SWAP and TTA
		int optTTA = 0;
		int optSWAP = 0;

		for (int slot = 0; slot < us.getNRLanes(); slot++) {
			CAMoveableEntity cand = us.peekForAgentInSlot(slot);
			if (cand == null) {
				optTTA++;
			} else if (cand.getNextLinkId().equals(this.dsl.getId())) {
				optSWAP++;
			}
		}

		if (optTTA + optSWAP == 0) {
			return;
		}
		// There are likely situations where TTA and SWAP are possible. The
		// action should be chosen so that the overall flow composition gets not
		// disturbed. For now the option with most opportunities is chosen to
		// increase the likelihood that it is still valid when the create event
		// will be executed. If this does not work out well we could try making
		// this probabilistic. [GL Nov '14]

		if (optSWAP >= optTTA) {
			double d = CAMultiLaneLink.getD(a);
			d *= this.us.getNodeRatio();
			triggerSWAP(a, this, time + d + this.tFree);
		} else {
			triggerTTA(a, this, time + this.tFree);
		}

	}

	private void handleTTAOnLinkUpStreamOnPreCondition2(CAMoveableEntity a,
			double time, int idx, int lane) {
		double z = getZ(a);
		z *= this.ratio;
		double zStar = z - (time - this.lastLeftUsTimes[lane][idx - 1]);
		double nextTime = time + zStar;
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnLinkUpStreamOnPreCondition3(CAMoveableEntity a,
			double time) {
		// nothing to be done here.
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

	private void triggerTTE(CAMoveableEntity toBeTriggered, CANetworkEntity ne,
			double time) {
		// if (toBeTriggered.getId().toString().equals("3096")) {
		// log.error("Gotcha!");
		// }
		CAEvent e = new CAEvent(time, toBeTriggered, ne, CAEventType.TTE);
		this.net.pushEvent(e);

	}

	private void handleTTADownStreamNode(CAMoveableEntity a, double time) {
		if (a.getNextLinkId() == null) {
			letAgentArrive(a, time, this.size - 1, a.getLane());
			checkPostConditionForPersonBehindOnDownStreamAdvance(this.size - 1,
					time, a.getLane());
			return;
		}

		// check pre-condition
		double z = CAMultiLaneLink.getZ(a);
		z *= ds.getNodeRatio();
		// 1. try to find empty slot with last left time < time - z
		double[] exitTimes = ds.getLastNodeExitTimeForAgent(a);
		List<Integer> cands = new ArrayList<Integer>(ds.getNRLanes());
		for (int slot = 0; slot < ds.getNRLanes(); slot++) {
			if (ds.peekForAgentInSlot(slot) == null) {
				if (exitTimes[slot] <= (time - z + epsilon)) {
					cands.add(slot);
				}
			}
		}
		if (cands.size() > 0) {
			Integer slot = cands.get(MatsimRandom.getRandom().nextInt(
					cands.size()));
			handleTTADownStreamNodeOnPreCondition1(a, time, slot);
			return;
		}
		// 2.try to find empty slot with last left time >= time -z
		// maybe we should look for slow with that minimizes (last left time -
		// time -z)
		for (int slot = 0; slot < ds.getNRLanes(); slot++) {
			if (ds.peekForAgentInSlot(slot) == null) {
				handleTTANodeOnPreCondition2(a, time, this.ds, slot);
				return;
			}
		}

	}

	private void handleTTADownStreamNodeOnPreCondition1(CAMoveableEntity a,
			double time, int nodeLane) {

		int lane = a.getLane();
		this.lastLeftDsTimes[lane][this.size - 1] = time;
		this.particles[lane][this.size - 1] = null;
		this.ds.putAgentInSlot(nodeLane, a);
		a.materialize(Integer.MAX_VALUE, Integer.MAX_VALUE, nodeLane);

		fireDownstreamLeft(a, time);

		// check post-condition and generate events
		// first for persons behind or on node

		if (!dsWaitQ.isEmpty()) {
			CAMoveableEntity el = dsWaitQ.iterator().next();
			triggerTTE(el, this, time);// move wait first
		}

		if (this.usl != null) {
			this.ds.tryTriggerAgentsWhoWantToEnterLaneOnLink(this.usl.getId(),
					time);
		}
		// || !this.ds.tryTriggerAgentsWhoWantToEnterLaneOnLink(lane,
		// this.usl.getId(), lanes, time)) {

		checkPostConditionForPersonBehindOnDownStreamAdvance(this.size - 1,
				time, lane);
		// }

		// second for oneself
		checkPostConditionForOneSelfOnNodeAdvance(this.ds, a, time);

		// throw new RuntimeException("maybe trigger someone on the link");
	}

	private void checkPostConditionForOneSelfOnNodeAdvance(CAMultiLaneNode n,
			CAMoveableEntity a, double time) {
		Id<Link> nextCALinkId = a.getNextLinkId();
		CAMultiLaneLink nextCALink = (CAMultiLaneLink) this.net
				.getCALink(nextCALinkId);
		if (nextCALink.getUpstreamCANode() == n) {
			n.checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(
					a, nextCALink, time);
		} else if (nextCALink.getDownstreamCANode() == n) {
			n.checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(
					a, nextCALink, time);
		} else {
			log.warn("inconsitent network, agent:" + a + " becomes stuck!");
			return;
		}

	}

	private void handleTTANodeOnPreCondition2(CAMoveableEntity a, double time,
			CAMultiLaneNode n, int slot) {

		double z = getZ(a);
		z *= n.getNodeRatio();

		double zStar = z - (time - n.getLastNodeExitTimeForAgent(a)[slot]);
		double nextTime = time + zStar;
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);

	}

	private void handleTTAUpStreamNode(CAMoveableEntity a, double time) {
		if (a.getNextLinkId() == null) {
			int lane = a.getLane();

			letAgentArrive(a, time, 0, lane);
			// check post-condition and generate events
			// first for persons behind
			checkPostConditionForPersonBehindOnUpStreamAdvance(0, time, lane);
			return;
		}

		// check pre-condition
		double z = CAMultiLaneLink.getZ(a);
		z *= us.getNodeRatio();
		// 1. try to find empty slot with last left time < time - z
		double[] exitTimes = us.getLastNodeExitTimeForAgent(a);
		List<Integer> cands = new ArrayList<Integer>(us.getNRLanes());
		for (int slot = 0; slot < us.getNRLanes(); slot++) {
			if (us.peekForAgentInSlot(slot) == null) {
				if (exitTimes[slot] <= (time - z + epsilon)) {
					cands.add(slot);
				}
			}
		}
		if (cands.size() > 0) {
			Integer slot = cands.get(MatsimRandom.getRandom().nextInt(
					cands.size()));
			handleTTAUpStreamNodeOnPreCondition1(a, time, slot);
			return;
		}
		// 2.try to find empty slot with last left time >= time -z
		// maybe we should look for slow with that minimizes (last left time -
		// time -z)
		for (int slot = 0; slot < us.getNRLanes(); slot++) {
			if (us.peekForAgentInSlot(slot) == null) {
				handleTTANodeOnPreCondition2(a, time, this.us, slot);
				return;
			}
		}

	}

	private void handleTTAUpStreamNodeOnPreCondition1(CAMoveableEntity a,
			double time, int nodeSlot) {

		int lane = a.getLane();
		this.lastLeftUsTimes[lane][0] = time;
		this.particles[lane][0] = null;
		this.us.putAgentInSlot(nodeSlot, a);

		a.materialize(Integer.MAX_VALUE, Integer.MAX_VALUE, nodeSlot);

		fireUpstreamLeft(a, time);

		// check post-condition and generate events
		// first for persons behind or on node

		if (!this.usWaitQ.isEmpty()) {
			CAMoveableEntity el = this.usWaitQ.iterator().next();
			triggerTTE(el, this, time);// move wait first
		}

		this.us.tryTriggerAgentsWhoWantToEnterLaneOnLink(this.dsl.getId(), time);
		checkPostConditionForPersonBehindOnUpStreamAdvance(0, time, lane);
		// }

		// second for oneself
		checkPostConditionForOneSelfOnNodeAdvance(this.us, a, time);

		// throw new RuntimeException("maybe trigger someone on node!");
	}

	private void handelSwap(CAMoveableEntity a, double time) {
		int idx = a.getPos();
		int dir = a.getDir();
		int nbIdx = idx + dir;
		if (nbIdx < 0) {
			swapWithUpStreamNode(a, time);
		} else if (nbIdx >= this.size) {
			swapWithDownStreamNode(a, time);
		} else {
			int lane = a.getLane();
			swapOnLink(a, idx, dir, time, lane);
		}

	}

	private void swapWithDownStreamNode(CAMoveableEntity a, double time) {

		if (usl == null) {
			log.warn(a);
		}

		CAMoveableEntity peek = this.ds
				.peekForAgentWhoWantsToEnterLaneOnLink(this.usl.getId());
		if (peek == null) {
			// log.info("situation for agent: " + a
			// + " at downstream node has changed, dropping event.");
			return;
		}

		int lane = a.getLane();
		int nodeSlot = peek.getLane();
		CAMoveableEntity swapA = this.ds.pollAgentFromSlot(nodeSlot);

		swapA.invalidate();

		swapA.materialize(this.size - 1, -1, lane);
		swapA.moveOverNode(this, time);

		this.particles[lane][this.size - 1] = swapA;
		this.lastLeftDsTimes[lane][this.size - 1] = time;
		this.ds.putAgentInSlot(nodeSlot, a);
		a.materialize(Integer.MIN_VALUE, Integer.MIN_VALUE, nodeSlot);
		swapA.setRho(k.estRho(swapA));

		fireDownstreamLeft(a, time);
		fireDownstreamEntered(swapA, time);

		// check post-condition and generate events
		// first for swapA
		checkPostConditionForAgentOnUpStreamAdvance(this.size, swapA, time,
				lane);

		// second for oneself
		if (a.getNextLinkId() == null) {
			this.ds.letAgentArrive(a, time);
		} else {
			checkPostConditionForOneSelfOnNodeAdvance(this.ds, a, time);
		}
	}

	private void swapWithUpStreamNode(CAMoveableEntity a, double time) {

		CAMoveableEntity peek = this.us
				.peekForAgentWhoWantsToEnterLaneOnLink(dsl.getId());

		// validate situation
		if (peek == null) {
			// log.info("situation for: " + a
			// + " at upstream node has changed, dropping event.");
			return;
		}
		int nodeLane = peek.getLane();
		int lane = a.getLane();

		CAMoveableEntity swapA = this.us.pollAgentFromSlot(nodeLane);

		this.particles[lane][0] = swapA;
		this.lastLeftUsTimes[lane][0] = time;
		this.us.putAgentInSlot(nodeLane, a);
		a.materialize(Integer.MIN_VALUE, Integer.MIN_VALUE, nodeLane);
		swapA.materialize(0, 1, lane);
		swapA.moveOverNode(this, time);
		swapA.invalidate();

		swapA.setRho(k.estRho(swapA));

		fireUpstreamLeft(a, time);
		fireUpstreamEntered(swapA, time);

		// check post-condition and generate events
		// first for swapA
		checkPostConditionForAgentOnDownStreamAdvance(-1, swapA, time, lane);

		// second for oneself
		if (a.getNextLinkId() == null) {
			this.us.letAgentArrive(a, time);
		} else {
			checkPostConditionForOneSelfOnNodeAdvance(this.us, a, time);
		}
	}

	// TODO us generic ids for event firing
	@Override
	public void fireDownstreamEntered(CAMoveableEntity a, double time) {
		LinkEnterEvent e = new LinkEnterEvent((int) time, a.getId(),
				this.usl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
		// System.out.println("down");

	}

	@Override
	public void fireUpstreamEntered(CAMoveableEntity a, double time) {
		LinkEnterEvent e = new LinkEnterEvent((int) time, a.getId(),
				this.dsl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
		// System.out.println("up");
	}

	@Override
	public void fireDownstreamLeft(CAMoveableEntity a, double time) {
		LinkLeaveEvent e = new LinkLeaveEvent((int) time, a.getId(),
				this.dsl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);

	}

	@Override
	public void fireUpstreamLeft(CAMoveableEntity a, double time) {
		LinkLeaveEvent e = new LinkLeaveEvent((int) time, a.getId(),
				this.usl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
	}

	@Override
	public int getNumOfCells() {
		return this.size;
	}

	private void swapOnLink(CAMoveableEntity a, int idx, int dir, double time,
			int lane) {

		if (dir == 1) {
			swapOnLinkDownStream(a, idx, time, lane);
		} else {
			swapOnLinkUpStream(a, idx, time, lane);
		}

	}

	private void swapOnLinkDownStream(CAMoveableEntity a, int idx, double time,
			int lane) {

		int nbIdx = idx + 1;
		CAMoveableEntity nb = this.particles[lane][nbIdx];

		// check if nb is null; if so trigger a new TTA event
		if (nb == null || nb.getDir() == 1) {
			triggerTTA(a, this, time);
			return;
		}

		nb.setRho(k.estRho(nb));

		int bestLaneA = lane;
		// check for lane switch
		if (a.getRho() >= LANESWITCH_TS_SWAP) {

			int bestTr = 1;
			for (; bestTr < 10; bestTr++) {
				int pos = nbIdx + bestTr * a.getDir();
				if (pos >= this.size || this.particles[lane][pos] != null) {
					break;
				}
			}
			if (lane > 0) {
				int tr = 0;
				for (; tr < 10; tr++) {
					int pos = nbIdx + tr * a.getDir();
					if (pos >= this.size
							|| this.particles[lane - 1][pos] != null) {
						break;
					}
				}
				if (tr > bestTr) {
					bestTr = tr;
					bestLaneA = lane - 1;
				}
			}
			if (lane < this.lanes - 1) {
				int tr = 0;
				for (; tr < 10; tr++) {
					int pos = nbIdx + tr * a.getDir();
					if (pos >= this.size
							|| this.particles[lane + 1][pos] != null) {
						break;
					}
				}
				if (tr > bestTr) {
					bestTr = tr;
					bestLaneA = lane + 1;
				}
			}

		}
		this.particles[bestLaneA][nbIdx] = a;
		a.setLane(bestLaneA);

		int bestLaneNb = lane;
		// check for lane switch
		if (nb.getRho() >= LANESWITCH_TS_SWAP) {

			int bestTr = 1;
			for (; bestTr < 10; bestTr++) {
				int pos = idx + bestTr * nb.getDir();
				if (pos < 0 || this.particles[lane][pos] != null) {
					break;
				}
			}
			if (lane > 0) {
				int tr = 0;
				for (; tr < 10; tr++) {
					int pos = idx + tr * nb.getDir();
					if (pos < 0 || this.particles[lane - 1][pos] != null) {
						break;
					}
				}
				if (tr > bestTr) {
					bestTr = tr;
					bestLaneNb = lane - 1;
				}
			}
			if (lane < this.lanes - 1) {
				int tr = 0;
				for (; tr < 10; tr++) {
					int pos = idx + tr * nb.getDir();
					if (pos < 0 || this.particles[lane + 1][pos] != null) {
						break;
					}
				}
				if (tr > bestTr) {
					bestTr = tr;
					bestLaneNb = lane + 1;
				}
			}

		}
		this.particles[bestLaneNb][idx] = nb;
		nb.setLane(bestLaneNb);

		this.lastLeftUsTimes[lane][nbIdx] = time;
		this.lastLeftDsTimes[lane][idx] = time;

		nb.invalidate();
		nb.proceed();
		a.proceed();

		checkPostConditionForAgentOnDownStreamAdvance(idx, a, time, bestLaneA);
		if (bestLaneNb != lane) {
			this.particles[lane][idx] = null;
			checkPostConditionForPersonBehindOnDownStreamAdvance(idx, time,
					lane);
		}
		checkPostConditionForAgentOnUpStreamAdvance(nbIdx, nb, time, bestLaneNb);
		if (bestLaneA != lane) {
			this.particles[lane][nbIdx] = null;
			checkPostConditionForPersonBehindOnUpStreamAdvance(nbIdx, time,
					lane);
		}

	}

	private void swapOnLinkUpStream(CAMoveableEntity a, int idx, double time,
			int lane) {

		int nbIdx = idx - 1;
		CAMoveableEntity nb = this.particles[lane][nbIdx];
		// check if nb is null; if so trigger a new TTA event
		if (nb == null || nb.getDir() == -1) {
			triggerTTA(a, this, time);
			return;
		}
		nb.setRho(k.estRho(nb));
		int bestLaneA = lane;
		// check for lane switch
		if (a.getRho() >= LANESWITCH_TS_SWAP) {

			int bestTr = 1;
			for (; bestTr < 10; bestTr++) {
				int pos = nbIdx + bestTr * a.getDir();
				if (pos < 0 || this.particles[lane][pos] != null) {
					break;
				}
			}
			if (lane > 0) {
				int tr = 0;
				for (; tr < 10; tr++) {
					int pos = nbIdx + tr * a.getDir();
					if (pos < 0 || this.particles[lane - 1][pos] != null) {
						break;
					}
				}
				if (tr > bestTr) {
					bestTr = tr;
					bestLaneA = lane - 1;
				}
			}
			if (lane < this.lanes - 1) {
				int tr = 0;
				for (; tr < 10; tr++) {
					int pos = nbIdx + tr * a.getDir();
					if (pos < 0 || this.particles[lane + 1][pos] != null) {
						break;
					}
				}
				if (tr > bestTr) {
					bestTr = tr;
					bestLaneA = lane + 1;
				}
			}

		}
		this.particles[bestLaneA][nbIdx] = a;
		a.setLane(bestLaneA);

		int bestLaneNb = lane;
		// check for lane switch
		if (nb.getRho() >= LANESWITCH_TS_SWAP) {

			int bestTr = 1;
			for (; bestTr < 10; bestTr++) {
				int pos = idx + bestTr * nb.getDir();
				if (pos >= this.size || this.particles[lane][pos] != null) {
					break;
				}
			}
			if (lane > 0) {
				int tr = 0;
				for (; tr < 10; tr++) {
					int pos = idx + tr * nb.getDir();
					if (pos >= this.size
							|| this.particles[lane - 1][pos] != null) {
						break;
					}
				}
				if (tr > bestTr) {
					bestTr = tr;
					bestLaneNb = lane - 1;
				}
			}
			if (lane < this.lanes - 1) {
				int tr = 0;
				for (; tr < 10; tr++) {
					int pos = idx + tr * nb.getDir();
					if (pos >= this.size
							|| this.particles[lane + 1][pos] != null) {
						break;
					}
				}
				if (tr > bestTr) {
					bestTr = tr;
					bestLaneNb = lane + 1;
				}
			}

		}
		this.particles[bestLaneNb][idx] = nb;
		nb.setLane(bestLaneNb);

		this.lastLeftDsTimes[lane][nbIdx] = time;
		this.lastLeftUsTimes[lane][idx] = time;

		nb.invalidate();
		nb.proceed();
		a.proceed();

		checkPostConditionForAgentOnUpStreamAdvance(idx, a, time, bestLaneA);
		if (bestLaneNb != lane) {
			this.particles[lane][idx] = null;
			checkPostConditionForPersonBehindOnUpStreamAdvance(idx, time, lane);
		}

		checkPostConditionForAgentOnDownStreamAdvance(nbIdx, nb, time,
				bestLaneNb);
		if (bestLaneA != lane) {
			this.particles[lane][nbIdx] = null;
			checkPostConditionForPersonBehindOnDownStreamAdvance(nbIdx, time,
					lane);
		}

	}

	@Override
	public CANode getUpstreamCANode() {
		return this.us;
	}

	@Override
	public CANode getDownstreamCANode() {
		return this.ds;
	}

	public CAMoveableEntity[] getParticles(int lane) {
		return this.particles[lane];
	}

	@Override
	public int getSize() {
		return this.size;
	}

	public double[] getLastLeftDsTimes(int lane) {
		return this.lastLeftDsTimes[lane];
	}

	public double[] getLastLeftUsTimes(int lane) {
		return this.lastLeftUsTimes[lane];
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

	public final int getNrLanes() {
		return this.lanes;
	}

	@Override
	public void letAgentDepart(CAVehicle veh, double now) {
		Id<Link> currentLinkId = veh.getDriver().getCurrentLinkId();
		Link link = this.net.getNetwork().getLinks().get(currentLinkId);
		Node toNode = link.getToNode();
		CANode toCANode = this.net.getNodes().get(toNode.getId());

		if (this.ds == toCANode) {
			if (this.dsWaitQ.size() < this.lanes) {
				triggerTTE(veh, this, now);
			}
			veh.materialize(-1, 1, -1);// moves ds
			this.dsWaitQ.add(veh);
		} else if (this.us == toCANode) {
			if (this.usWaitQ.size() < this.lanes) {
				triggerTTE(veh, this, now);
			}
			veh.materialize(-1, -1, -1);// moves us;
			this.usWaitQ.add(veh);
		} else {
			throw new RuntimeException("inconsitent network or plan!");
		}
		EventsManager eventsManager = this.net.getEventsManager();
		eventsManager.processEvent(new PersonEntersVehicleEvent(now, veh
				.getDriver().getId(), veh.getId()));
		// VIS only
		if (AbstractCANetwork.EMIT_VIS_EVENTS) {
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(now, veh);
			this.net.getEventsManager().processEvent(ee);
		}

	}

	private void letAgentArrive(CAMoveableEntity a, double time, int idx,
			int lane) {
		if (a.getDir() == 1) {
			this.lastLeftDsTimes[lane][idx] = time;
		} else {
			this.lastLeftUsTimes[lane][idx] = time;
		}
		this.particles[lane][idx] = null;
		CANetsimEngine engine = this.net.getEngine();
		if (engine != null) {
			engine.letVehicleArrive((CAVehicle) a);
		}
		this.net.unregisterAgent(a);

		checkPostConditionForPersonBehindOnDownStreamAdvance(idx, time, lane);
		checkPostConditionForPersonBehindOnUpStreamAdvance(idx, time, lane);

		// this.net.registerAgent(a);
		// this.net.unregisterAgent(a);
	}

	@Override
	public void reset() {
		for (int i = 0; i < this.lanes; i++) {
			for (int j = 0; j < this.size; j++) {
				lastLeftDsTimes[i][j] = 0;
				lastLeftUsTimes[i][j] = 0;
				if (particles[i][j] != null) {
					CAMoveableEntity part = particles[i][j];
					this.net.unregisterAgent(part);
					particles[i][j] = null;
					if (part instanceof CAVehicle) {
						double now = net.getEngine().getMobsim().getSimTimer()
								.getTimeOfDay();
						CAVehicle veh = (CAVehicle) part;

						Id<Link> currentLinkId = veh.getDir() == 1 ? this.dsl
								.getId() : this.usl.getId();
						this.net.getEventsManager().processEvent(
								new PersonStuckEvent(now, veh.getDriver()
										.getId(), currentLinkId, veh
										.getDriver().getMode()));

						net.getEngine().getMobsim().getAgentCounter().incLost();
						net.getEngine().getMobsim().getAgentCounter()
								.decLiving();
					}
				}
			}
		}

	}

	@Override
	public void lock() {
		this.ds.lock.lock();
		this.us.lock.lock();
		this.lock.lock();
	}

	@Override
	public void unlock() {
		this.ds.unlock();
		this.us.unlock();
		// this.lock.unlock();
	}

	@Override
	public boolean tryLock() {
		if (!this.ds.tryLock()) {
			// log.warn("ds lock failed");
			return false;
		}
		if (!this.us.tryLock()) {
			this.ds.unlock();
			// log.warn("us lock failed");
			return false;
		}

		// if (!this.lock.tryLock()) {
		// this.ds.lock.unlock();
		// this.us.lock.unlock();
		// return false;
		// }

		return true;
	}

	@Override
	public boolean isLocked() {
		if (this.ds.lock.isLocked()) {
			return true;
		}
		if (this.us.lock.isLocked()) {
			return true;
		}
		if (this.lock.isLocked()) {
			return true;
		}
		return false;
	}

	@Override
	public int threadNR() {
		return this.threadNr;
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
