/* *********************************************************************** *
 * project: org.matsim.*
 * SnapshotGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.mzilske.vis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;

import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

import com.vividsolutions.jts.index.bintree.Bintree;
import com.vividsolutions.jts.index.bintree.Interval;

public class BintreeGenerator implements AgentDepartureEventHandler, AgentArrivalEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentWait2LinkEventHandler, AgentStuckEventHandler {

	private static final Id nullId = new IdImpl("");
	
	public static class Trajectory {

		public Id personId;
		public double x;
		public double y;
		public double dx;
		public double dy;
		public int startTime;
		public int endTime;

		public Trajectory(Id personId, double x, double y, double dx, double dy, int startTime, int endTime) {
			this.personId = personId;
			this.x = x;
			this.y = y;
			this.dx = dx;
			this.dy = dy;
			this.startTime = startTime;
			this.endTime = endTime;
		}

	}

	private final Network network;
	private final HashMap<Id, EventAgent> eventAgents;
	private final Bintree bintree = new Bintree();
	private TDoubleArrayList xs = new TDoubleArrayList();
	private TDoubleArrayList ys = new TDoubleArrayList();
	private TDoubleArrayList dxs = new TDoubleArrayList();
	private TDoubleArrayList dys = new TDoubleArrayList();
	private TIntArrayList startTimes = new TIntArrayList();
	private TIntArrayList endTimes = new TIntArrayList();;

	Bintree getBintree() {
		return bintree;
	}

	public BintreeGenerator(final Network network) {
		this.network = network;
		this.eventAgents = new HashMap<Id, EventAgent>();
		reset(-1);
	}

	public void handleEvent(final AgentDepartureEvent event) {
		agentDeparture(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	private void agentDeparture(Id personId, Id linkId, double time) {
		EventAgent agent = new EventAgent();
		eventAgents.put(personId, agent);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		linkLeave(event.getPersonId(), event.getLinkId(), (int) event.getTime());
		eventAgents.remove(event.getPersonId());
	}

	public void handleEvent(final LinkEnterEvent event) {
		linkEnter(event.getPersonId(), event.getLinkId(), (int) event.getTime());
	}

	private void linkEnter(Id personId, Id linkId, int time) {
		EventAgent agent = eventAgents.get(personId);
		if (agent != null) {
			agent.startTime = time;
			Link link = network.getLinks().get(linkId);
			agent.startCoord = link.getFromNode().getCoord();
			agent.endCoord = link.getToNode().getCoord();
		}
	}

	public void handleEvent(final LinkLeaveEvent event) {
		linkLeave(event.getPersonId(), event.getLinkId(), (int) event.getTime());
	}

	private void linkLeave(Id personId, Id linkId, int endTime) {
		EventAgent agent = eventAgents.get(personId);
		if (agent != null) {
			if (agent.startCoord != null) {
				enterTrajectory(personId, endTime, agent);
			}
		}
	}

	private void enterTrajectory(Id personId, int endTime, EventAgent agent) {
		double x = agent.startCoord.getX();
		double y = agent.startCoord.getY();
		int startTime = agent.startTime;
		int dTime = endTime - startTime;
		double dx = (agent.endCoord.getX() - x) / dTime;
		double dy = (agent.endCoord.getY() - y) / dTime;
		
		int index = xs.size();
		xs.add(x);
		ys.add(y);
		startTimes.add(startTime);
		endTimes.add(endTime);
		dxs.add(dx);
		dys.add(dy);
		
		
		// Trajectory trajectory = new Trajectory(personId, x, y, dx, dy, startTime, endTime);
		
		bintree.insert(new Interval(startTime, endTime), index);
	}


	public void handleEvent(final AgentWait2LinkEvent event) {
		// wait2Link(event.getPersonId(), event.getLinkId(), event.getTime());
	}

	public void handleEvent(final AgentStuckEvent event) {
		linkLeave(event.getPersonId(), event.getLinkId(), (int) event.getTime());
		eventAgents.remove(event.getPersonId());
	}

	public Trajectory getTrajectory(int index) {
		double x = xs.get(index);
		double y = ys.get(index);
		int startTime = startTimes.get(index);
		int endTime = endTimes.get(index);
		double dx = dxs.get(index);
		double dy = dys.get(index);
		return new Trajectory(nullId, x, y, dx, dy, startTime, endTime);
	}
	
	public void reset(final int iteration) {
		this.eventAgents.clear();
	}

	public void finish() {
		
	}

	private static class EventAgent {
		public Coord endCoord;
		public Coord startCoord;
		protected int startTime;
		protected double speed = 0.0;
		protected int lane = 1;
	}


}
