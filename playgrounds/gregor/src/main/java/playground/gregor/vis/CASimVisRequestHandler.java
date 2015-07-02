/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.gregor.vis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CALink;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.casim.simulation.physics.CAMultiLaneLink;
import playground.gregor.casim.simulation.physics.CAMultiLaneNode;
import playground.gregor.casim.simulation.physics.CANetworkEntity;
import playground.gregor.casim.simulation.physics.CANode;
import playground.gregor.proto.ProtoFrame.Frame;
import playground.gregor.proto.ProtoFrame.Frame.Builder;
import playground.gregor.proto.ProtoFrame.Frame.Event;
import playground.gregor.proto.ProtoFrame.Frame.Event.Type;
import playground.gregor.proto.ProtoFrame.FrameRqst;
import playground.gregor.sim2d_v4.cgal.TwoDObject;
import playground.gregor.sim2d_v4.cgal.TwoDTree;

import com.vividsolutions.jts.geom.Envelope;

public class CASimVisRequestHandler implements VisRequestHandler {

	private static final int MAX_AGENTS = 2000;

	private TwoDTree<EventContainer> sceneTree;// = new TwoDTree<>(e);
	private final Map<String, List<EventContainer>> sceneViews = new HashMap<>();
	private final Map<String, Integer> agentCnt = new HashMap<>();

	private double lastSnapshotTime;

	private final Event.Builder eb = Event.newBuilder();

	private Envelope e = null;

	private final CyclicBarrier barrier = new CyclicBarrier(2);

	public void intitialize(AbstractCANetwork net) {
		List<EventContainer> cnt = new ArrayList<>();
		for (CALink l : net.getLinks().values()) {

			double x = l.getLink().getCoord().getX();
			double y = l.getLink().getCoord().getY();

			EventContainer c = new EventContainer();
			c.x = x;
			c.y = y;
			c.e = l;
			c.linkId = l.getLink().getId().toString();
			cnt.add(c);
		}

		for (CANode n : net.getNodes().values()) {
			EventContainer c = new EventContainer();
			c.x = n.getX();
			c.y = n.getY();
			if (e == null) {
				e = new Envelope(c.x, c.y, c.x, c.y);
			} else {
				e.expandToInclude(c.x, c.y);
			}
			c.e = n;
			cnt.add(c);
		}
		this.sceneTree = new TwoDTree<>(e);
		this.sceneTree.buildTwoDTree(cnt);
	}

	@Override
	public void update(double time) {
		for (Entry<String, List<EventContainer>> e : this.sceneViews.entrySet()) {
			List<EventContainer> l = e.getValue();
			int cnt = 0;
			for (EventContainer c : l) {
				update(c, time);
				cnt += c.evs.size();
			}
			this.agentCnt.put(e.getKey(), cnt);
		}
		this.lastSnapshotTime = time;
		try {
			this.barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}

	private void update(EventContainer c, double time) {
		if (c.lastUpdate == time) {
			return;
		}
		c.lock.lock();
		c.evs.clear();
		CANetworkEntity e = c.e;
		if (e instanceof CAMultiLaneLink) {
			update((CAMultiLaneLink) e, c);
		} else if (e instanceof CAMultiLaneNode) {
			update((CAMultiLaneNode) e, c);
		}
		c.lock.unlock();
	}

	private void update(CAMultiLaneNode n, EventContainer c) {
		int lanes = n.getNrLanes();
		double laneWidth = n.getWidth() / lanes;

		double x = n.getNode().getCoord().getX();
		double y0 = n.getNode().getCoord().getY() - laneWidth * lanes / 2
				+ laneWidth / 2;

		for (int slot = 0; slot < lanes; slot++) {
			CAMoveableEntity agent = n.peekForAgentInSlot(slot);
			if (agent != null) {
				Event ev = eb.setX(x).setY(y0).setVx(0).setVy(0)
						.setEvntType(Type.POS).setId(agent.getId().toString())
						.build();
				c.evs.add(ev);
			}
			y0 += laneWidth;
		}
	}

	private void update(CAMultiLaneLink l, EventContainer cnt) {
		double dx = l.getLink().getToNode().getCoord().getX()
				- l.getLink().getFromNode().getCoord().getX();
		double dy = l.getLink().getToNode().getCoord().getY()
				- l.getLink().getFromNode().getCoord().getY();
		double length = Math.sqrt(dx * dx + dy * dy);
		dx /= length;
		dy /= length;
		double ldx = dx;
		double ldy = dy;
		double incr = l.getLink().getLength() / l.getNumOfCells();
		dx *= incr;
		dy *= incr;
		double laneWidth = l.getLaneWidth();
		double hx = -ldy;
		double hy = ldx;
		hx *= laneWidth;
		hy *= laneWidth;
		int lanes = l.getNrLanes();
		double x0 = l.getLink().getFromNode().getCoord().getX() - hx * lanes
				/ 2 + hx / 2 + dx / 2;
		double y0 = l.getLink().getFromNode().getCoord().getY() - hy * lanes
				/ 2 + hy / 2 + dy / 2;
		for (int lane = 0; lane < l.getNrLanes(); lane++) {
			double x = x0 + lane * hx;
			double y = y0 + lane * hy;
			for (int i = 0; i < l.getNumOfCells(); i++) {
				if (l.getParticles(lane)[i] != null) {
					double ddx = 1;
					if (l.getParticles(lane)[i].getDir() == -1) {
						ddx = -1;
					}
					Event ev = eb.setEvntType(Type.POS).setX(x).setY(y)
							.setVx(ldx * ddx).setVy(ldy * ddx)
							.setId(l.getParticles(lane)[i].getId().toString())
							.build();
					cnt.evs.add(ev);
				}
				x += dx;
				y += dy;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.vis.VisRequestHandlerI#handleRequest(playground.gregor
	 * .proto.ProtoFrame.FrameRqst)
	 */
	@Override
	public Frame handleRequest(FrameRqst request) {
		try {
			this.barrier.await();
		} catch (InterruptedException | BrokenBarrierException e1) {
			throw new RuntimeException(e1);
		}
		String id = request.getId();
		Builder fb = Frame.newBuilder();
		fb.setTime(lastSnapshotTime);
		List<EventContainer> containers = this.sceneViews.get(id);
		int cnt = this.agentCnt.get(id);
		for (EventContainer c : containers) {
			c.lock.lock();
			if (cnt <= MAX_AGENTS) {
				for (Event e : c.evs) {
					fb.addEvnt(e);
				}
			} else if (c.linkId != null && c.evs.size() > 0) {

				fb.addEvnt(eb.setEvntType(Type.LINK_INF).setId(c.linkId)
						.setNrAgents(c.evs.size()).build());
			}
			c.lock.unlock();

		}

		Envelope reqE = new Envelope(request.getTlX(), request.getBrX(),
				request.getTlY(), request.getBrY());
		if (this.e.contains(reqE)) {
			List<EventContainer> ls = this.sceneTree.get(reqE);
			this.sceneViews.put(request.getId(), ls);
		} else {
			List<EventContainer> ls = this.sceneTree.get(e);
			this.sceneViews.put(request.getId(), ls);
		}
		return fb.build();

	}

	private static final class EventContainer implements TwoDObject {
		double lastUpdate = -1;
		public double y;
		public double x;
		// private
		private Lock lock = new ReentrantLock();
		final List<Event> evs = new ArrayList<>();

		private String linkId = null;

		private CANetworkEntity e;

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

	}

	@Override
	public void registerClient(String id) {
		this.sceneViews.put(id, new ArrayList<EventContainer>());
	}

}
