/* *********************************************************************** *
 * project: org.matsim.*
 * PhysicalSim2DSection.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.simulation.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.Sim2DQTransitionLink;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;

import com.vividsolutions.jts.geom.Coordinate;

public class PhysicalSim2DSection {

	private final Section sec;

	List<Segment> obstacles;
	Segment [] openings;

	Map<Id,LinkInfo> linkInfos = new HashMap<Id,LinkInfo>();

	private final Sim2DScenario sim2dsc;

	private final double offsetY;

	private final double offsetX;

	private final List<Sim2DAgent> inBuffer = new LinkedList<Sim2DAgent>();
	protected final List<Sim2DAgent> agents = new LinkedList<Sim2DAgent>();

	protected final float timeStepSize;

	protected final PhysicalSim2DEnvironment penv;

	private final Map<Segment,PhysicalSim2DSection> neighbors = new HashMap<Segment,PhysicalSim2DSection>();

	public PhysicalSim2DSection(Section sec, Sim2DScenario sim2dsc, double offsetX, double offsetY, PhysicalSim2DEnvironment penv) {
		this.sec = sec;
		this.sim2dsc = sim2dsc;
		this.timeStepSize = (float) sim2dsc.getSim2DConfig().getTimeStepSize();
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.penv = penv;
		init();
	}

	//physics
	public int getNumberOfAllAgents() {
		return this.inBuffer.size() + this.agents.size();
	}

	public void addAgentToInBuffer(Sim2DAgent agent) {
		this.inBuffer.add(agent);
		agent.setPSec(this);
	}

	public void updateAgents() {
		this.agents.addAll(this.inBuffer);
		this.inBuffer.clear();
		Iterator<Sim2DAgent> it = this.agents.iterator();
		while (it.hasNext()) {
			Sim2DAgent agent = it.next();
			updateAgent(agent);
		}
	}



	public void moveAgents(double time) {
		Iterator<Sim2DAgent> it = this.agents.iterator();
		while (it.hasNext()) {
			Sim2DAgent agent = it.next();
			float [] v = agent.getVelocity();
			float dx = v[0] * this.timeStepSize;
			float dy = v[1] * this.timeStepSize;
			Id currentLinkId = agent.getCurrentLinkId();
			LinkInfo li = this.linkInfos.get(currentLinkId);
			float [] oldPos = agent.getPos();
			float newXPosX = oldPos[0] + dx;
			float newXPosY = oldPos[1] + dy;
			float lefOfFinishLine = CGAL.isLeftOfLine(newXPosX, newXPosY, li.finishLine.x0, li.finishLine.y0, li.finishLine.x1, li.finishLine.y1);
			if (lefOfFinishLine >= 0) { //agent has reached the end of link
				Id nextLinkId = agent.chooseNextLinkId();
				if (nextLinkId == null) {
					throw new RuntimeException("agent with id:" + agent.getId() + " has reached the end of her current leg, which is not (yet) supported in the Sim2D.");
				}
				if (this.linkInfos.get(nextLinkId) == null) { //agent leaves current section
					PhysicalSim2DSection nextSection = this.penv.getPhysicalSim2DSectionAssociatedWithLinkId(nextLinkId);
					if (nextSection == null) { //agent intends to leave sim2d
						Sim2DQTransitionLink loResLink = this.penv.getLowResLink(nextLinkId);
						if (loResLink.hasSpace()) {
							it.remove();
							QVehicle veh = agent.getQVehicle();
							veh.setCurrentLink(loResLink.getLink());
							loResLink.addFromIntersection(veh);
							this.penv.getEventsManager().processEvent(new LinkLeaveEvent(time, agent.getId(), currentLinkId, agent.getQVehicle().getId()));
							agent.notifyMoveOverNode(nextLinkId);
							this.penv.getEventsManager().processEvent(new LinkEnterEvent(time, agent.getId(), nextLinkId, agent.getQVehicle().getId()));
						} else {
							dx = 0;
							dy = 0;
						}
					} else {
						it.remove(); //removing the agent from the agents list
						nextSection.addAgentToInBuffer(agent);
						this.penv.getEventsManager().processEvent(new LinkLeaveEvent(time, agent.getId(), currentLinkId, agent.getQVehicle().getId()));
						agent.notifyMoveOverNode(nextLinkId);
						this.penv.getEventsManager().processEvent(new LinkEnterEvent(time, agent.getId(), nextLinkId, agent.getQVehicle().getId()));
					}
				} else {
					this.penv.getEventsManager().processEvent(new LinkLeaveEvent(time, agent.getId(), currentLinkId, agent.getQVehicle().getId()));
					agent.notifyMoveOverNode(nextLinkId);
					this.penv.getEventsManager().processEvent(new LinkEnterEvent(time, agent.getId(), nextLinkId, agent.getQVehicle().getId()));
				}
			}
//			this.penv.getEventsManager().processEvent(new XYVxVyEventImpl(agent.getId(),agent.getPos()[0],agent.getPos()[1],agent.getVelocity()[0],agent.getVelocity()[1],time));
			agent.move(dx, dy);
		}
	}

	private void updateAgent(Sim2DAgent agent) {
		//		agent.calcNeighbors(this);
		//		agent.setObstacles(this.obstacles);
		agent.updateVelocity();
		//		this.agents.r
		//		List<Sim2DAgent> neighbors = calculateNeighbors(agent);

	}

	//initialization 
	private void init() {
		Coordinate[] coords = this.sec.getPolygon().getCoordinates();
		int[] openings = this.sec.getOpenings();
		int oidx = 0;

		List<Segment> obst = new ArrayList<Segment>();
		List<Segment> open = new ArrayList<Segment>();

		for (int i = 0; i < coords.length-1; i++){
			Coordinate c0 = coords[i];
			Coordinate c1 = coords[i+1];
			Segment seg = new Segment();
			seg.x0 = (float) (c0.x-this.offsetX);
			seg.x1 = (float) (c1.x-this.offsetX);
			seg.y0 = (float) (c0.y-this.offsetY);
			seg.y1 = (float) (c1.y-this.offsetY);

			float dx = seg.x1-seg.x0;
			float dy = seg.y1-seg.y0;
			double length = Math.sqrt(dx*dx+dy*dy);
			dx /= length;
			dy /= length;
			seg.dx = dx;
			seg.dy = dy;

			if (oidx < openings.length && i == openings[oidx]) {
				oidx++;
				open.add(seg);
			} else {
				obst.add(seg);
			}
		}
		this.obstacles = obst;
		this.openings = open.toArray(new Segment[0]);

		Map<Id, ? extends Link> links = this.sim2dsc.getMATSimScenario().getNetwork().getLinks();
		for (Id id : this.sec.getRelatedLinkIds()) {
			Link l = links.get(id);
			LinkInfo li = new LinkInfo();
			this.linkInfos.put(id, li);

			Coord from = l.getFromNode().getCoord();
			Coord to = l.getToNode().getCoord();
			Segment seg = new Segment();
			seg.x0 = (float) (from.getX()-this.offsetX);
			seg.x1 = (float) (to.getX()-this.offsetX);
			seg.y0 = (float) (from.getY()-this.offsetY);
			seg.y1 = (float) (to.getY()-this.offsetY);

			float dx = seg.x1-seg.x0;
			float dy = seg.y1-seg.y0;
			double length = Math.sqrt(dx*dx+dy*dy);
			dx /= length;
			dy /= length;

			li.link = seg;
			li.dx = dx;
			li.dy = dy;
			Segment fromOpening = getTouchingSegment(seg.x0,seg.y0, seg.x1,seg.y1,this.openings);
			Segment toOpening = getTouchingSegment(seg.x1,seg.y1, seg.x0,seg.y0,this.openings);
			li.fromOpening = fromOpening;
			if (toOpening != null) {
				li.finishLine = toOpening;
				li.width = (float)Math.sqrt((toOpening.x0-toOpening.x1)*(toOpening.x0-toOpening.x1)+(toOpening.y0-toOpening.y1)*(toOpening.y0-toOpening.y1)); 
			} else {
				Segment finishLine = new Segment();
				finishLine.x0 = seg.x1;
				finishLine.y0 = seg.y1;

				//all polygons are clockwise oriented so we rotate to the right here 
				finishLine.x1 = finishLine.x0 + li.dy;
				finishLine.y1 = finishLine.y0 - li.dx;
				li.finishLine = finishLine;
				li.width = 20; //TODO section width [gl Jan'13]
			}
		}

	}

	/*package*/ void connect() {
		Map<Id, ? extends Link> links = this.sim2dsc.getMATSimScenario().getNetwork().getLinks();
		for (Id id : this.sec.getRelatedLinkIds()) {
			Link l = links.get(id);
			LinkInfo li = this.linkInfos.get(l.getId());
			for (Link ll : l.getToNode().getOutLinks().values()) {
				PhysicalSim2DSection psec = this.penv.getPhysicalSim2DSectionAssociatedWithLinkId(ll.getId());
				if (psec != this) {
					this.neighbors.put(li.finishLine, psec);
				}
			}
		}
	}

	//
	public LinkInfo getLinkInfo(Id id) {
		return this.linkInfos.get(id);
	}

	/*package*/ void putLinkInfo(Id id, LinkInfo li) {
		this.linkInfos.put(id, li);
	}

	private Segment getTouchingSegment(float x0, float y0, float x1, float y1, Segment[] openings) {

		for (Segment opening : openings) {

			boolean onSegment = CGAL.isOnVector(x0, y0,opening.x0, opening.y0, opening.x1, opening.y1); // this is a necessary but not sufficient condition since a section is only approx convex. so we need the following test as well
			if (onSegment) {
				float isLeft0  = CGAL.isLeftOfLine(opening.x0, opening.y0,x0, y0, x1, y1);
				float isLeft1  = CGAL.isLeftOfLine(opening.x1, opening.y1,x0, y0, x1, y1);
				if (isLeft0 * isLeft1 >= 0){ // coordinate is on a vector given by the segment opening but not on the segment itself. This case is unlikely to occur! 
					onSegment = false;
				}
				if (onSegment) {
					return opening;
				}
			}
		}
		return null;
	}


	public static final class Segment {
		public float  x0;
		public float  x1;
		public float  y0;
		public float  y1;
		public float dx;//normalized!!
		public float dy;//normalized!!
	}

	public static final class LinkInfo {

		public float width;
		public float dx;
		public float dy;
		public Segment link;
		Segment fromOpening;
		Segment finishLine;
	}

	public Id getId() {
		return this.sec.getId();
	}

	public List<Sim2DAgent> getAgents() {
		return this.agents;
	}

	public Segment [] getOpenings() {
		return this.openings;

	}

	public void debug(VisDebugger visDebugger) {
		if (visDebugger.isFirst()) {
			for (Segment seg : this.obstacles) {
				visDebugger.addLineStatic(seg.x0, seg.y0, seg.x1, seg.y1, 0, 0, 0, 128);
			}
			for (Segment seg : this.openings) {
				visDebugger.addLineStatic(seg.x0, seg.y0, seg.x1, seg.y1, 0, 192, 64, 128);
			}
			for (Id key : this.linkInfos.keySet()) {
				Link l = this.sim2dsc.getMATSimScenario().getNetwork().getLinks().get(key);
				float x0 = (float) (l.getFromNode().getCoord().getX() - this.offsetX);
				float x1 = (float) (l.getToNode().getCoord().getX() - this.offsetX);
				float y0 = (float) (l.getFromNode().getCoord().getY() - this.offsetY);
				float y1 = (float) (l.getToNode().getCoord().getY() - this.offsetY);
				visDebugger.addLineStatic(x0, y0, x1, y1, 0, 0, 0, 255);
			}
		}
		if (this.agents.size() > 0) {
			Coordinate[] coords = this.sec.getPolygon().getExteriorRing().getCoordinates();
			float [] x = new float[coords.length];
			float [] y = new float[coords.length];
			for (int i = 0; i < coords.length; i++) {
				Coordinate c = coords[i];
				x[i] = (float) (c.x - this.offsetX);
				y[i] = (float) (c.y - this.offsetY);
			}
			visDebugger.addPolygon(x, y,32, 255, 64, 32);
			//			visDebugger.addText((float)(this.sec.getPolygon().getCentroid().getX()-this.offsetX),(float) (this.sec.getPolygon().getCentroid().getY()-this.offsetY),""+this.sec.getId());
		}
		for (Sim2DAgent agent : this.agents) {
			agent.debug(visDebugger);
		}
		for (Sim2DAgent agent : this.inBuffer) {
			agent.debug(visDebugger);
		}
	}

	public PhysicalSim2DSection getNeighbor(Segment opening) {
		return this.neighbors .get(opening);
	}

	public void putNeighbor(Segment finishLine, PhysicalSim2DSection psec) {
		this.neighbors.put(finishLine, psec);
	}

	public List<Segment> getObstacles() {
		return this.obstacles;
	}


}
