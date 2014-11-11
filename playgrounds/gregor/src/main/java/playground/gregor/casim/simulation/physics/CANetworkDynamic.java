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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

import playground.gregor.casim.monitoring.CALinkMonitorExact;
import playground.gregor.casim.simulation.CANetsimEngine;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;

/**
 * Centerpiece of the bidirectional 1d ca simulation. Basic idea is based on
 * Flötteröd and Lämmel (forthcoming); Bidirectional pedestrian fundamental
 * diagram. Transportation Research Part B
 * 
 * @author laemmel
 *
 */
public class CANetworkDynamic {

	public static double RHO = 1;

	// Floetteroed Laemmel parameters
	public static final double RHO_HAT = 6.69;
	public static final double V_HAT = 1.27;

	public static final double ALPHA = 0.;
	public static final double BETA = 0.39;
	public static final double GAMMA = 1.43;

	public static final double PED_WIDTH = .61;

	private static final Logger log = Logger.getLogger(CANetworkDynamic.class);

	public static boolean EMIT_VIS_EVENTS = false;

	private static double EPSILON = 0.0001;

	// private final PriorityQueue<CAEvent> events = new PriorityQueue<CAEvent>(
	// 1000000);

	private final CAEventsPaulPriorityQueue events = new CAEventsPaulPriorityQueue();
	private final Network net;

	private final Map<Id<Node>, CANode> caNodes = new HashMap<Id<Node>, CANode>();
	private final Map<Id<Link>, CALink> caLinks = new HashMap<Id<Link>, CALink>();
	private final EventsManager em;

	private CALinkMonitorExact monitor;

	private Set<CAMoveableEntity> agents = new HashSet<CAMoveableEntity>();

	private CANetsimEngine engine;

	private static int EXP_WARN_CNT;

	public static int NR_THREADS = 8;
	private final CyclicBarrier barrier = new CyclicBarrier(NR_THREADS + 1);
	private final Worker[] workers = new Worker[NR_THREADS];

	private final ConcurrentLinkedQueue<CAEvent> cache = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<CAEvent> remaining = new ConcurrentLinkedQueue<>();

	private double tFreeMin;

	private final CASimDensityEstimator dens;

	public CANetworkDynamic(Network net, EventsManager em, CANetsimEngine engine) {
		this.net = net;
		this.em = em;
		this.engine = engine;
		this.dens = new CASimDensityEstimator(this);
		init();
	}

	private void init() {
		this.tFreeMin = Double.POSITIVE_INFINITY;
		for (Node n : this.net.getNodes().values()) {
			CANodeDynamic caNode = new CANodeDynamic(n, this);
			this.caNodes.put(n.getId(), caNode);
		}
		for (Link l : this.net.getLinks().values()) {
			CANodeDynamic us = (CANodeDynamic) this.caNodes.get(l.getFromNode()
					.getId());
			CANodeDynamic ds = (CANodeDynamic) this.caNodes.get(l.getToNode()
					.getId());
			Link rev = null;
			for (Link ll : l.getToNode().getOutLinks().values()) {
				if (ll.getToNode() == l.getFromNode()) {
					rev = ll;
				}
			}
			if (rev != null) {
				CALink revCA = this.caLinks.get(rev.getId());
				if (revCA != null) {
					this.caLinks.put(l.getId(), revCA);
					continue;
				}
			}
			CALinkDynamic caL = new CALinkDynamic(l, rev, ds, us, this);
			if (caL.getTFree() < this.tFreeMin) {
				this.tFreeMin = caL.getTFree();
			}
			us.addLink(caL);
			ds.addLink(caL);
			this.caLinks.put(l.getId(), caL);
		}
		for (int i = 0; i < NR_THREADS; i++) {
			Worker w = new Worker(barrier, remaining);
			workers[i] = w;
			Thread t = new Thread(w);
			t.setDaemon(true);
			t.setName(Worker.class.toString() + i);
			t.start();
		}

	}

	/* package */void registerAgent(CAMoveableEntity a) {
		if (!this.agents.add(a)) {
			throw new RuntimeException("Agent: " + a
					+ " has already been registered!");
		}
	}

	/* package */void unregisterAgent(CAMoveableEntity a) {
		if (!this.agents.remove(a)) {
			throw new RuntimeException("Could not unregister agent: " + a + "!");
		}
	}

	/* package */void updateRho() {

		for (CAMoveableEntity a : this.agents) {
			this.dens.handle(a);
		}
		this.dens.await();
	}

	public void doSimStep(double time) {

		{
			Iterator<CAEvent> it = this.cache.iterator();
			while (it.hasNext()) {
				CAEvent next = it.next();
				this.events.add(next);
				it.remove();
			}
		}
		updateRho();
		// draw2();
		while (this.events.peek() != null
				&& this.events.peek().getEventExcexutionTime() < time + 1) {

			double timeFrameEnd = events.peek().getEventExcexutionTime()
					+ this.tFreeMin;
			// + EPSILON;
			// + this.tFreeMin;

			while (this.events.peek() != null
					&& this.events.peek().getEventExcexutionTime() < time + 1
					&& this.events.peek().getEventExcexutionTime() < timeFrameEnd) {
				CAEvent e = this.events.poll();
				// log.info("==> " + e);

				if (e.isObsolete()) {
					if (EXP_WARN_CNT++ < 10) {
						log.info("dropping obsolete event: " + e);
						if (EXP_WARN_CNT == 10) {
							log.info(Gbl.FUTURE_SUPPRESSED);
						}
					}
					continue;
				}
				this.workers[e.getCANetworkEntity().threadNR()].add(e);
			}
			for (int i = 0; i < NR_THREADS; i++) {
				this.workers[i].add(new CAEvent(time, null, null,
						CAEventType.END_OF_FRAME));
			}
			try {
				this.barrier.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}
			while (this.remaining.peek() != null) {
				CAEvent e = this.remaining.poll();
				e.getCANetworkEntity().handleEvent(e);
			}
			Iterator<CAEvent> it = this.cache.iterator();
			while (it.hasNext()) {
				CAEvent next = it.next();
				this.events.add(next);
				it.remove();
			}
		}
		if (EMIT_VIS_EVENTS) {
			// updateDensity();
			draw2(time);
		}

		// if (this.events.peek() == null) {
		// for (int i = 0; i < NR_THREADS; i++) {
		// this.workers[i].add(new CAEvent(time, null, null,
		// CAEventType.END_OF_SIM));
		// }
		// }
	}

	@Deprecated
	/* package */void run() {
		{
			Iterator<CAEvent> it = this.cache.iterator();
			while (it.hasNext()) {
				CAEvent next = it.next();
				this.events.add(next);
				it.remove();
			}
		}
		// draw2();
		updateRho();
		double time = 0;
		while (this.events.peek() != null) {
			if (this.events.peek().getEventExcexutionTime() > time + 1) {
				updateRho();
				time = this.events.peek().getEventExcexutionTime();
			}

			double timeFrameEnd = events.peek().getEventExcexutionTime();
			// + EPSILON;
			// + this.tFreeMin / 10;
			int cnt = 0;
			while (this.events.peek() != null
					&& this.events.peek().getEventExcexutionTime() <= timeFrameEnd) {

				CAEvent e = this.events.poll();
				// log.info("==> " + e);

				if (this.monitor != null) {
					this.monitor.trigger(e.getEventExcexutionTime());
				}

				if (e.isObsolete()) {
					if (EXP_WARN_CNT++ < 10) {
						log.info("dropping obsolete event: " + e);
						if (EXP_WARN_CNT == 10) {
							log.info(Gbl.FUTURE_SUPPRESSED);
						}
					}
					continue;
				}

				this.workers[e.getCANetworkEntity().threadNR()].add(e);
			}
			for (int i = 0; i < NR_THREADS; i++) {
				this.workers[i].add(new CAEvent(timeFrameEnd, null, null,
						CAEventType.END_OF_FRAME));
			}
			try {
				this.barrier.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}
			while (this.remaining.peek() != null) {
				CAEvent e = this.remaining.poll();
				e.getCANetworkEntity().handleEvent(e);
			}

			Iterator<CAEvent> it = this.cache.iterator();
			while (it.hasNext()) {
				CAEvent next = it.next();
				this.events.add(next);
				it.remove();
			}
			if (EMIT_VIS_EVENTS) {
				// updateDensity();
				draw2(timeFrameEnd);
			}
		}

		afterSim();
	}

	private void draw2(double time) {
		for (CALink l : this.caLinks.values()) {
			double dx = l.getLink().getToNode().getCoord().getX()
					- l.getLink().getFromNode().getCoord().getX();
			double dy = l.getLink().getToNode().getCoord().getY()
					- l.getLink().getFromNode().getCoord().getY();
			double length = Math.sqrt(dx * dx + dy * dy);
			dx /= length;
			dy /= length;
			double ldx = dx;
			double ldy = dy;

			double hy = dx;
			double hx = -dy;

			double hy0 = -hy * l.getLink().getCapacity() / 2;
			double hx0 = -hx * l.getLink().getCapacity() / 2;
			hx *= PED_WIDTH;
			hy *= PED_WIDTH;

			double lanes = l.getLink().getCapacity() / PED_WIDTH;

			double incr = l.getLink().getLength() / l.getNumOfCells();
			dx *= incr;
			dy *= incr;
			double width = l.getLink().getCapacity();
			double x = l.getLink().getFromNode().getCoord().getX();// +dx/2;
			double y = l.getLink().getFromNode().getCoord().getY();// +dy/2;
			for (int i = 0; i < l.getNumOfCells(); i++) {
				if (l.getParticles()[i] != null) {

					double lane = l.getParticles()[i].hashCode() % lanes;

					double ddx = 1;
					if (l.getParticles()[i].getDir() == -1) {
						ddx = -1;
					}
					;
					XYVxVyEventImpl e = new XYVxVyEventImpl(
							l.getParticles()[i].getId(), x + dx / 2 + hx0
									+ lane * hx, y + dy / 2 + hy0 + lane * hy,
							ldx * ddx, ldy * ddx, time);

					this.em.processEvent(e);
					// System.out.println(l.getParticles()[i]);
				} else {
					// RectEvent e = new RectEvent(time, x, y+width/2, dx,
					// width, false);
					// this.em.processEvent(e);
				}
				x += dx;
				y += dy;
			}
		}
		for (CANode n : this.caNodes.values()) {
			if (n.peekForAgent() != null) {
				double x = n.getNode().getCoord().getX();
				double y = n.getNode().getCoord().getY();
				XYVxVyEventImpl e = new XYVxVyEventImpl(n.peekForAgent()
						.getId(), x, y, 0, 0, time);
				this.em.processEvent(e);
			}
		}
	}

	public void pushEvent(CAEvent event) {

		// log.info("<== " + event);
		event.getCAAgent().setCurrentEvent(event);
		// this.events.add(event);
		this.cache.add(event);
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

	public CANetsimEngine getEngine() {
		return this.engine;
	}

	public Map<Id<Link>, CALink> getLinks() {
		return this.caLinks;
	}

	public Map<Id<Node>, CANode> getNodes() {
		return this.caNodes;
	}

	private static final class Worker implements Runnable {

		private final LinkedBlockingQueue<CAEvent> queue = new LinkedBlockingQueue<>();
		private final Queue<CAEvent> remaining;
		private CyclicBarrier barrier;

		public Worker(CyclicBarrier barrier, Queue<CAEvent> remaining) {
			this.barrier = barrier;
			this.remaining = remaining;
		}

		@Override
		public void run() {

			try {
				while (true) {
					CAEvent event = queue.take();

					if (event.getCAEventType() == CAEventType.END_OF_FRAME) {
						this.barrier.await();
						continue;
					} else if (event.getCAEventType() == CAEventType.END_OF_SIM) {
						break;
					}

					if (event.tryLock()) {
						event.getCANetworkEntity().handleEvent(event);
						event.unlock();
					} else {
						remaining.add(event);
					}
				}
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}

			Gbl.printCurrentThreadCpuTime();
		}

		public void add(CAEvent e) {
			this.queue.add(e);
		}
	}

	public void afterSim() {
		for (CALink caLink : this.getLinks().values()) {
			caLink.reset();
		}
		this.dens.shutdown();
		for (int i = 0; i < NR_THREADS; i++) {
			this.workers[i].add(new CAEvent(Double.NaN, null, null,
					CAEventType.END_OF_SIM));
		}

	}

}
