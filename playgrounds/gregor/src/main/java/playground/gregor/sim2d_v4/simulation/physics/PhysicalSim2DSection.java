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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.TwoDTree;
import playground.gregor.sim2d_v4.events.Sim2DAgentDestructEvent;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class PhysicalSim2DSection {

	private static final Logger log = Logger.getLogger(PhysicalSim2DSection.class);

	private final Section sec;

	List<Segment> obstacles;
	Segment [] openings;//TODO why not using a list, is array really faster?? [gl April '13]

	//	Map<Id,LinkInfo> linkInfos = new HashMap<Id,LinkInfo>();

	private final Sim2DScenario sim2dsc;

	private final List<Sim2DAgent> inBuffer = new LinkedList<Sim2DAgent>();
	protected final List<Sim2DAgent> agents = new LinkedList<Sim2DAgent>();

	protected final double timeStepSize;

	protected final PhysicalSim2DEnvironment penv;

	private final Map<Segment,PhysicalSim2DSection> neighbors = new HashMap<Segment,PhysicalSim2DSection>();

	private final TwoDTree<Sim2DAgent> agentTwoDTree;

	//	private final Map<Segment,Id> openingLinkIdMapping = new HashMap<Segment,Id>();

	private int numOpenings;


	//	private VisDebugger debugger;

	public PhysicalSim2DSection(Section sec, Sim2DScenario sim2dsc, PhysicalSim2DEnvironment penv) {
		this.sec = sec;
		this.sim2dsc = sim2dsc;
		this.timeStepSize = sim2dsc.getSim2DConfig().getTimeStepSize();
		this.penv = penv;
		Envelope e = this.sec.getPolygon().getEnvelopeInternal();
		this.agentTwoDTree = new TwoDTree<Sim2DAgent>(new Envelope(e.getMinX(),e.getMaxX(),e.getMinY(),e.getMaxY()));
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

	public void updateAgents(double time) {
		this.agentTwoDTree.clear();
		this.agents.addAll(this.inBuffer);
		this.inBuffer.clear();
		this.agentTwoDTree.buildTwoDTree(this.agents);
		Iterator<Sim2DAgent> it = this.agents.iterator();
		while (it.hasNext()) {
			Sim2DAgent agent = it.next();
			updateAgent(agent, time);
		}
	}



	public void moveAgents(double time) {


		Iterator<Sim2DAgent> it = this.agents.iterator();
		while (it.hasNext()) {
			Sim2DAgent agent = it.next();

			double [] v = agent.getVelocity();
			double dx = v[0] * this.timeStepSize;
			double dy = v[1] * this.timeStepSize;
			double [] oldPos = agent.getPos();
			double oldX = oldPos[0];
			double oldY = oldPos[1];
			double newXPosX = oldPos[0] + dx;
			double newXPosY = oldPos[1] + dy;

			boolean mv = agent.move(dx, dy,time);
			if (agent.hasLeft2DSim()) {
				it.remove();
				this.penv.getEventsManager().processEvent(new Sim2DAgentDestructEvent(time, agent));
			} else if (mv) {
				for (int i = 0; i < this.numOpenings; i++) {
					Segment opening = this.openings[i];
					double leftOfOpening = CGAL.isLeftOfLine(newXPosX, newXPosY, opening.x0, opening.y0, opening.x1, opening.y1);
					if (leftOfOpening >= 0) {

						double l0 = CGAL.isLeftOfLine(opening.x0, opening.y0,oldX,oldY,newXPosX,newXPosY);
						double l1 = CGAL.isLeftOfLine(opening.x1, opening.y1,oldX,oldY,newXPosX,newXPosY);
						if (l0*l1 < - CGAL.EPSILON) {
							PhysicalSim2DSection nextSection = this.neighbors.get(opening);
							if (nextSection == null) {
								break; //agent was pushed out of the sim2d environment
							}
							it.remove();
							nextSection.addAgentToInBuffer(agent);
							break;
						}
					}
				}
			}

		}
	}

	//	private Id getLinkId(Segment opening) {
	//		return this.openingLinkIdMapping.get(opening);
	//	}

	private void updateAgent(Sim2DAgent agent, double time) {
		//		agent.calcNeighbors(this);
		//		agent.setObstacles(this.obstacles);
		agent.updateVelocity(time);
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
			seg.x0 = c0.x;
			seg.x1 = c1.x;
			seg.y0 = c0.y;
			seg.y1 = c1.y;

			double dx = seg.x1-seg.x0;
			double dy = seg.y1-seg.y0;
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
		this.numOpenings = this.openings.length;

		//		Map<Id, ? extends Link> links = this.getSim2dsc().getMATSimScenario().getNetwork().getLinks();
		//		for (Id id : this.sec.getRelatedLinkIds()) {
		//			Link l = links.get(id);
		//			LinkInfo li = new LinkInfo();
		//			this.linkInfos.put(id, li);
		//
		//			Coord from = l.getFromNode().getCoord();
		//			Coord to = l.getToNode().getCoord();
		//			Segment seg = new Segment();
		//			seg.x0 = from.getX()-this.offsetX;
		//			seg.x1 = to.getX()-this.offsetX;
		//			seg.y0 = from.getY()-this.offsetY;
		//			seg.y1 = to.getY()-this.offsetY;
		//
		//			double dx = seg.x1-seg.x0;
		//			double dy = seg.y1-seg.y0;
		//			double length = Math.sqrt(dx*dx+dy*dy);
		//			dx /= length;
		//			dy /= length;
		//
		//			li.link = seg;
		//			li.dx = dx;
		//			li.dy = dy;
		//			Segment fromOpening = getTouchingSegment(seg.x0,seg.y0, seg.x1,seg.y1,this.openings);
		//			Segment toOpening = getTouchingSegment(seg.x1,seg.y1, seg.x0,seg.y0,this.openings);
		//			li.fromOpening = fromOpening;
		//			if (toOpening != null) {
		//				li.finishLine = toOpening;
		//				li.width = Math.sqrt((toOpening.x0-toOpening.x1)*(toOpening.x0-toOpening.x1)+(toOpening.y0-toOpening.y1)*(toOpening.y0-toOpening.y1)); 
		//				this.openingLinkIdMapping .put(toOpening,id);
		//			} else {
		//				Segment finishLine = new Segment();
		//				finishLine.x0 = seg.x1;
		//				finishLine.y0 = seg.y1;
		//
		//				//all polygons are clockwise oriented so we rotate to the right here 
		//				finishLine.x1 = finishLine.x0 + li.dy;
		//				finishLine.y1 = finishLine.y0 - li.dx;
		//				li.finishLine = finishLine;
		//				li.width = 10; //TODO section width [gl Jan'13]
		//			}
		//		}

	}

	/*package*/ void connect() {
		for (Segment opening : this.openings) {
			for (Id n : this.sec.getNeighbors()) {
				PhysicalSim2DSection nPSec = this.penv.psecs.get(n);
				if (isConnectedViaOpening(opening,nPSec)){
					this.neighbors.put(opening, nPSec);
					break;
				}
			}
		}
	}

	private boolean isConnectedViaOpening(Segment opening,
			PhysicalSim2DSection nPSec) {
		for (Segment nOpening : nPSec.getOpenings()) {
			if (nOpening.equalInverse(opening)) {
				return true;
			}
		}
		return false;
	}

	//	//
	//	public LinkInfo getLinkInfo(Id id) {
	//		return this.linkInfos.get(id);
	//	}
	//
	//	/*package*/ void putLinkInfo(Id id, LinkInfo li) {
	//		this.linkInfos.put(id, li);
	//	}

	//	private Segment getTouchingSegment(double x0, double y0, double x1, double y1, Segment[] openings) {
	//
	//		for (Segment opening : openings) {
	//
	//			boolean onSegment = CGAL.isOnVector(x0, y0,opening.x0, opening.y0, opening.x1, opening.y1); // this is a necessary but not sufficient condition since a section is only approx convex. so we need the following test as well
	//			if (onSegment) {
	//				double isLeft0  = CGAL.isLeftOfLine(opening.x0, opening.y0,x0, y0, x1, y1);
	//				double isLeft1  = CGAL.isLeftOfLine(opening.x1, opening.y1,x0, y0, x1, y1);
	//				if (isLeft0 * isLeft1 >= 0){ // coordinate is on a vector given by the segment opening but not on the segment itself. This case is unlikely to occur! 
	//					onSegment = false;
	//				}
	//				if (onSegment) {
	//					return opening;
	//				}
	//			}
	//		}
	//		return null;
	//	}


	public static final class Segment {//TODO move this inner class elsewhere [gl April '13]
		public double  x0;
		public double  x1;
		public double  y0;
		public double  y1;
		public double dx;//normalized!!
		public double dy;//normalized!!

		/*package*/ boolean equalInverse(Segment other) {
			if (this.x0 == other.x1 && this.x1 == other.x0 && this.y0 == other.y1 && this.y1 == other.y0) {
				return true;
			}
			return false;
		}
	}



	public Id getId() {
		return this.sec.getId();
	}

	public List<Sim2DAgent> getAgents() {

		return this.agents;
	}

	public List<Sim2DAgent> getAgents(Envelope e) {
		return this.agentTwoDTree.get(e);
	}

	public Segment [] getOpenings() {
		return this.openings;

	}

	public PhysicalSim2DSection getNeighbor(Segment opening) {
		return this.neighbors.get(opening);
	}

	/*package*/ void putNeighbor(Segment finishLine, PhysicalSim2DSection psec) {
		this.neighbors.put(finishLine, psec);
	}

	public PhysicalSim2DEnvironment getPhysicalEnvironment() {
		return this.penv;
	}

	public List<Segment> getObstacles() {
		return this.obstacles;
	}

	public Sim2DScenario getSim2dsc() {
		return this.sim2dsc;
	}


}
