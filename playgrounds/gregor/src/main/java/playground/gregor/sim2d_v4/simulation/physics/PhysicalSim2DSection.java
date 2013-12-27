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
import org.matsim.core.gbl.Gbl;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.cgal.TwoDTree;
import playground.gregor.sim2d_v4.events.Sim2DAgentDestructEvent;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class PhysicalSim2DSection {

	private static final Logger log = Logger.getLogger(PhysicalSim2DSection.class);

	private static int leaveNotOrderlyCnt = 0;

	private final Section sec;

	List<LineSegment> obstacles;
	LineSegment [] openings;//TODO why not using a list, is array really faster?? [gl April '13]

	private final Sim2DScenario sim2dsc;

	protected final List<Sim2DAgent> inBuffer = new LinkedList<Sim2DAgent>();
	protected final List<Sim2DAgent> agents = new LinkedList<Sim2DAgent>();

	protected final double timeStepSize;

	protected final PhysicalSim2DEnvironment penv;

	private final Map<LineSegment,PhysicalSim2DSection> neighbors = new HashMap<LineSegment,PhysicalSim2DSection>();
	private final Map<PhysicalSim2DSection,LineSegment> neighborsInvMapping = new HashMap<PhysicalSim2DSection, LineSegment>();


	protected final TwoDTree<Sim2DAgent> agentTwoDTree;

	private int numOpenings;



	public PhysicalSim2DSection() {
		this.sec = null;
		this.sim2dsc = null;
		this.timeStepSize = Double.NaN;
		this.penv = null;
		this.agentTwoDTree = null;
	}

	public PhysicalSim2DSection(Section sec, Sim2DScenario sim2dsc, PhysicalSim2DEnvironment penv) {
		this.sec = sec;
		this.sim2dsc = sim2dsc;
		this.timeStepSize = sim2dsc.getSim2DConfig().getTimeStepSize();
		this.penv = penv;
		Envelope e = this.sec.getPolygon().getEnvelopeInternal();
		this.agentTwoDTree = new TwoDTree<Sim2DAgent>(new Envelope(e.getMinX(),e.getMaxX(),e.getMinY(),e.getMaxY()));
		init();
	}

	public int getNumberOfAllAgents() {
		return this.inBuffer.size() + this.agents.size();
	}

	public void addAgentToInBuffer(Sim2DAgent agent) {
		this.inBuffer.add(agent);
		agent.setPSec(this);
	}

	public void prepare() {
		this.agentTwoDTree.clear();
		this.agents.addAll(this.inBuffer);
		this.inBuffer.clear();
		if (!Sim2DConfig.EXPERIMENTAL_VD_APPROACH) {
			this.agentTwoDTree.buildTwoDTree(this.agents);
		}		
	}

	public void updateAgents(double time) {
		Iterator<Sim2DAgent> it = this.agents.iterator();
		while (it.hasNext()) {
			
			Sim2DAgent agent = it.next();
//			if (agent.getId().toString().equals("13818")) {
//				System.out.println("got you");
//			}
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
			boolean mv;
			if (agent.hasLeft2DSim()) {
				agent.moveGhost(dx, dy, time);
				if (agent.ttl-- <= 0) {
					it.remove();
					this.penv.getEventsManager().processEvent(new Sim2DAgentDestructEvent(time, agent));
					if (leaveNotOrderlyCnt < 10) {
						log.warn("Agent: " + agent + " did not leave 2D sim orderly! There might be a bug!");
						leaveNotOrderlyCnt++;
						if (leaveNotOrderlyCnt == 10) {
							log.warn(Gbl.FUTURE_SUPPRESSED);
						}

					}
					continue;
				}
				mv = true;
			} else {
				mv = agent.move(dx, dy,time);
//				if (agent.getId().toString().equals("b66") && agent.getCurrentLinkId().toString().equals("l9")){
//					System.out.println("got you!");
//				}
			}
			if (mv) {
				for (int i = 0; i < this.numOpenings; i++) {
					LineSegment opening = this.openings[i];
					double leftOfOpening = CGAL.isLeftOfLine(newXPosX, newXPosY, opening.x0, opening.y0, opening.x1, opening.y1);
					if (leftOfOpening >= 0) {
						double l1 = CGAL.isLeftOfLine(oldX, oldY, opening.x0, opening.y0, opening.x1, opening.y1);
						double l2 = CGAL.isLeftOfLine(opening.x0, opening.y0,oldX,oldY,newXPosX,newXPosY);
						double l3 = CGAL.isLeftOfLine(opening.x1, opening.y1,oldX,oldY,newXPosX,newXPosY);
						if (l2*l3 < 0 && leftOfOpening*l1 < 0) {
							PhysicalSim2DSection nextSection = this.neighbors.get(opening);
//							if (nextSection instanceof TransitionAreaII) {
//								System.out.println("got you!!");
//							}
							if (nextSection == null) {//agent was pushed out of the sim2d environment
								if  (agent.hasLeft2DSim()) {
									it.remove();
									this.penv.getEventsManager().processEvent(new Sim2DAgentDestructEvent(time, agent));
								}
								break; 
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

	protected void updateAgent(Sim2DAgent agent, double time) {
		//		agent.calcNeighbors(this);
		//		agent.setObstacles(this.obstacles);
//		if (agent.getId().toString().equals("b66")){
//			System.out.println("got you!");
//		}
		agent.updateVelocity();

	}

	//initialization 
	private void init() {
		Coordinate[] coords = this.sec.getPolygon().getCoordinates();
		int[] openings = this.sec.getOpenings();
		int oidx = 0;

		List<LineSegment> obst = new ArrayList<LineSegment>();
		List<LineSegment> open = new ArrayList<LineSegment>();

		for (int i = 0; i < coords.length-1; i++){
			Coordinate c0 = coords[i];
			Coordinate c1 = coords[i+1];
			LineSegment seg = new LineSegment();
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
		this.openings = open.toArray(new LineSegment[0]);
		this.numOpenings = this.openings.length;
	}

	/*package*/ void connect() {
		for (LineSegment opening : this.openings) {
			for (Id n : this.sec.getNeighbors()) {
				PhysicalSim2DSection nPSec = this.penv.psecs.get(n);
				if (isConnectedViaOpening(opening,nPSec)){
					this.neighbors.put(opening, nPSec);
					this.neighborsInvMapping.put(nPSec,opening);
					break;
				}
			}
		}
	}

	private boolean isConnectedViaOpening(LineSegment opening,
			PhysicalSim2DSection nPSec) {
		for (LineSegment nOpening : nPSec.getOpenings()) {
			if (nOpening.equalInverse(opening)) {
				return true;
			}
		}
		return false;
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

	public LineSegment [] getOpenings() {
		return this.openings;

	}

	public PhysicalSim2DSection getNeighbor(LineSegment opening) {
		return this.neighbors.get(opening);
	}

	public LineSegment getConnectingOpening(PhysicalSim2DSection neighbor) {
		return this.neighborsInvMapping.get(neighbor);
	}

	/*package*/ void putNeighbor(LineSegment finishLine, PhysicalSim2DSection psec) {
		this.neighbors.put(finishLine, psec);
	}

	public PhysicalSim2DEnvironment getPhysicalEnvironment() {
		return this.penv;
	}

	public List<LineSegment> getObstacles() {
		return this.obstacles;
	}

	public Sim2DScenario getSim2dsc() {
		return this.sim2dsc;
	}

	@Override
	public String toString() {
		return "id:" + this.sec.getId() + " agents in section:" + this.agents.size();
	}

}
