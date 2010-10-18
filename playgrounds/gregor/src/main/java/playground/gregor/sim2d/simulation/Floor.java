/* *********************************************************************** *
 * project: org.matsim.*
 * Floor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.sim2d.simulation.Agent2D.AgentState;
import playground.gregor.sim2d_v2.controller.Sim2DConfig;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class Floor {

	private static final Logger log = Logger.getLogger(Floor.class);

	private double lastUpdate = 0;

	private final Set<Agent2D> agents = new HashSet<Agent2D>();
	private final Map<Agent2D, Force> agentForceMapping = new HashMap<Agent2D, Force>();
	private final SegmentedStaticForceField staticForceField;

	// private List<double[]> forceInfos;

	// private QuadTree<Agent2D> agentsQuad;

	private final Map<Link, LineString> finishLines;
	private final Map<Link, LineString> linksGeos;
	private final Map<Link, Coordinate> drivingDirections;

	private Quadtree agentsQuadII;

	public Floor(Map<Link, LineString> finishLines, Map<Link, LineString> linksGeos, Map<Link, Coordinate> drivingDirections, SegmentedStaticForceField sff) {
		this.finishLines = finishLines;
		this.linksGeos = linksGeos;
		this.drivingDirections = drivingDirections;
		this.staticForceField = sff;

		// QUICK HACK
		Envelope e = new Envelope();
		for (Link l : this.finishLines.keySet()) {
			e.expandToInclude(l.getCoord().getX(), l.getCoord().getY());
		}

		// this.agentsQuad = new
		// QuadTree<Agent2D>(e.getMinX()-400,e.getMinY()-400,e.getMaxX()+400,e.getMaxY()+400);
		this.agentsQuadII = new Quadtree();
	}

	public void move(double time) {
		updateForces();
		moveAgents();
		updateQuadII(time);
	}

	private void updateQuadII(double time) {
		if (time < this.lastUpdate + Sim2DConfig.NEIGHBORHOOD_UPDATE) {
			return;
		}

		this.agentsQuadII = new Quadtree();

		for (Agent2D agent : this.agents) {
			// if (agent.getState() != AgentState.MOVING) {
			// continue;
			// }
			Envelope e = new Envelope(agent.getPosition());
			this.agentsQuadII.insert(e, agent);
			// this.agentsQuad.put(agent.getPosition().x, agent.getPosition().y,
			// agent);
		}
		for (Agent2D agent : this.agents) {
			double sensing = 8;
			List l = null;
			do {

				double minX = agent.getPosition().x - sensing;
				double maxX = agent.getPosition().x + sensing;
				double minY = agent.getPosition().y - sensing;
				double maxY = agent.getPosition().y + sensing;
				Envelope e = new Envelope(minX, maxX, minY, maxY);
				// Rect r = new QuadTree.Rect(minX, minY, maxX, maxY);
				// Collection<Agent2D> others = new ArrayList<Agent2D>();
				// this.agentsQuad.get(r,others);
				l = this.agentsQuadII.query(e);
				sensing *= 0.5;
			} while (l.size() > 50 && sensing >= 2);

			agent.setNeighbors((ArrayList<Agent2D>) l);
		}

		this.lastUpdate = time;

	}

	// private void updateQuad(double time) {
	//		
	// if (time < this.lastUpdate + Sim2DConfig.NEIGHBORHOOD_UPDATE) {
	// return;
	// }
	//		
	//		
	// this.agentsQuad.clear();
	// for (Agent2D agent : this.agents) {
	// this.agentsQuad.put(agent.getPosition().x, agent.getPosition().y, agent);
	// }
	// for (Agent2D agent : this.agents) {
	// double minX = agent.getPosition().x - 8.5;
	// double maxX = agent.getPosition().x + 8.5;
	// double minY = agent.getPosition().y - 8.5;
	// double maxY = agent.getPosition().y + 8.5;
	// Rect r = new QuadTree.Rect(minX, minY, maxX, maxY);
	// Collection<Agent2D> others = new ArrayList<Agent2D>();
	// this.agentsQuad.get(r,others);
	// agent.setNeighbors(others);
	// }
	//		
	// this.lastUpdate = time;
	// }

	private void moveAgents() {
		for (Agent2D agent : this.agents) {
			Force force = this.agentForceMapping.get(agent);
			Coordinate oldPos = agent.getPosition();

			agent.setPosition(oldPos.x + force.getFx(), oldPos.y + force.getFy());
			force.setOldFx(force.getFx());
			force.setOldFy(force.getFy());
		}

	}

	private void updateForces() {

		for (Agent2D agent : this.agents) {
			Force force = this.agentForceMapping.get(agent);
			if (agent.getState() == AgentState.ACTING) {
				// Force force = this.agentForceMapping.get(agent);
				force.setFx(0.);
				force.setFy(0.);
			} else {
				if (agent.departed()) {
					agentDepart(agent);
				}

				// updateAgentForce(agent);
				updateAgentInteractionForce(agent, force);
				updateAgentEnvForce(agent, force);
				if (agent.getState() == AgentState.MOVING) {
					if (updateDrivingForce(agent, force)) {
						updatePathForce(agent, force);
					}
				}
				if (agent.getState() == AgentState.ARRIVING) {
					updateDriveToActForce(agent, force);
				}

				validateForce(agent, force);
			}

		}
	}

	private void validateForce(Agent2D agent, Force force) {
		double norm = Math.sqrt(Math.pow(force.getFx(), 2) + Math.pow(force.getFy(), 2));
		if (norm > agent.getDisiredVelocity() * Sim2DConfig.TIME_STEP_SIZE) {
			force.setFx(force.getFx() * ((agent.getDisiredVelocity() * Sim2DConfig.TIME_STEP_SIZE) / norm));
			force.setFy(force.getFy() * ((agent.getDisiredVelocity() * Sim2DConfig.TIME_STEP_SIZE) / norm));
		}
	}

	private void updateDriveToActForce(Agent2D agent, Force force) {
		if (agent.checkForActivityReached()) {
			agentArrival(agent, agent.getAct().getLinkId());
			// force.driveX = 0;
			// force.driveY = 0;
		} else {
			Coordinate dest = MGC.coord2Coordinate(agent.getAct().getCoord());
			double dist = dest.distance(agent.getPosition());
			double driveX = (dest.x - agent.getPosition().x) / dist;
			double driveY = (dest.y - agent.getPosition().y) / dist;

			if (0 < Sim2DConfig.TIME_STEP_SIZE * agent.getDisiredVelocity()) {

			}
			double scale = dist < Sim2DConfig.TIME_STEP_SIZE * agent.getDisiredVelocity() ? dist : Sim2DConfig.TIME_STEP_SIZE * agent.getDisiredVelocity();

			driveX *= scale;
			driveY *= scale;

			force.setFx(force.getFx() + ((driveX - force.getOldFx()) / Sim2DConfig.tau));
			force.setFy(force.getFy() + ((driveY - force.getOldFy()) / Sim2DConfig.tau));
			// force.driveX = (driveX-force.getOldFx() )/Sim2DConfig.tau;
			// force.driveY = (driveY-force.getOldFy())/Sim2DConfig.tau;
		}

	}

	private void updatePathForce(Agent2D agent, Force force) {

		Link link = agent.getCurrentLink();
		LineString lsL = this.linksGeos.get(link);
		Coordinate c = this.drivingDirections.get(link);
		Point p = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y);
		double dist = p.distance(lsL);
		if (dist <= 0) {
			return;
		}

		double hypotenuse = agent.getPosition().distance(MGC.coord2Coordinate(link.getFromNode().getCoord()));

		double dist2 = Math.sqrt(Math.pow(hypotenuse, 2) - Math.pow(dist, 2));
		if (Double.isNaN(dist2)) {
			System.err.println("dist2 was NaN!");
			return;
		}
		double deltaX = (link.getFromNode().getCoord().getX() + c.x * dist2) - agent.getPosition().x;
		double deltaY = (link.getFromNode().getCoord().getY() + c.y * dist2) - agent.getPosition().y;

		double f = Math.exp(dist / Sim2DConfig.Bpath);
		deltaX *= f / dist;
		deltaY *= f / dist;

		force.setFx(force.getFx() + Sim2DConfig.Apath * deltaX / agent.getWeight());
		force.setFy(force.getFy() + Sim2DConfig.Apath * deltaY / agent.getWeight());

		// log.info(agent.getId() + " deltaX: " + Sim2DConfig.Apath *
		// deltaX/agent.getWeight() + " deltaY:" + Sim2DConfig.Apath *
		// deltaY/agent.getWeight());

		// DEBUG
		// force.pathX = Sim2DConfig.Apath * deltaX/agent.getWeight();
		// force.pathY = Sim2DConfig.Apath * deltaY/agent.getWeight();
	}

	private boolean updateDrivingForce(Agent2D agent, Force force) {
		Link link = agent.getCurrentLink();
		Id oldLink = link.getId();
		LineString ls = this.finishLines.get(link);
		Point p = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y);
		double dist = p.distance(ls);
		if (dist <= 1.) {
			link = agent.chooseNextLink();

			if (link != null) {
				Id newLink = link.getId();
				agentNextLink(agent, oldLink, newLink);
				ls = this.finishLines.get(link);
				p = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y);
				dist = p.distance(ls);
			}
		}
		if (link == null) {
			return false;
		} else {

			Coordinate d = this.drivingDirections.get(link);
			double driveX = d.x;
			double driveY = d.y;

			driveX *= Sim2DConfig.TIME_STEP_SIZE * agent.getDisiredVelocity();
			driveY *= Sim2DConfig.TIME_STEP_SIZE * agent.getDisiredVelocity();

			if (dist < Sim2DConfig.TIME_STEP_SIZE * agent.getDisiredVelocity()) {
				link = agent.chooseNextLink();
				Id newLink = link.getId();
				agentNextLink(agent, oldLink, newLink);
			}

			if (Double.isNaN(driveX) || Double.isNaN(driveY)) {
				throw new RuntimeException();
			}

			force.setFx(force.getFx() + ((driveX - force.getOldFx()) / Sim2DConfig.tau));
			force.setFy(force.getFy() + ((driveY - force.getOldFy()) / Sim2DConfig.tau));
			// force.driveX = (driveX-force.getOldFx() )/Sim2DConfig.tau;
			// force.driveY = (driveY-force.getOldFy())/Sim2DConfig.tau;

		}
		return true;

	}

	private void agentNextLink(Agent2D agent, Id oldLink, Id newLink) {

		Event e1 = new LinkLeaveEventImpl(SimulationTimer.getTime(), agent.getId(), oldLink);
		Event e2 = new LinkEnterEventImpl(SimulationTimer.getTime(), agent.getId(), newLink);
		Sim2D.getEvents().processEvent(e1);
		Sim2D.getEvents().processEvent(e2);

	}

	private void agentArrival(Agent2D agent, Id linkId) {
		Event e1 = new AgentArrivalEventImpl(SimulationTimer.getTime(), agent.getId(), linkId, TransportMode.car);
		Event e2 = new ActivityStartEventImpl(SimulationTimer.getTime(), agent.getId(), agent.getCurrentLink().getId(), agent.getAct().getFacilityId(), agent.getAct().getType());
		Sim2D.getEvents().processEvent(e1);
		Sim2D.getEvents().processEvent(e2);
	}

	private void agentDepart(Agent2D agent) {
		Event e1 = new AgentDepartureEventImpl(SimulationTimer.getTime(), agent.getId(), agent.getCurrentLink().getId(), TransportMode.car);
		Event e2 = new ActivityEndEventImpl(SimulationTimer.getTime(), agent.getId(), agent.getCurrentLink().getId(), null, agent.getOldAct().getType());
		Sim2D.getEvents().processEvent(e2);
		Sim2D.getEvents().processEvent(e1);

	}

	private void updateAgentEnvForce(Agent2D agent, Force force) {
		double x = 0;
		double y = 0;

		// Collection<Force> fs =
		// this.staticForceField.getForcesWithin(agent.getPosition(), 1.);
		// if (fs.size() > 0) {
		// for (Force f : fs) {
		// x += f.getFx();
		// y += f.getFy();
		// }
		// x /= fs.size();
		// y /= fs.size();
		// }
		Force f = this.staticForceField.getForceWithin(agent.getPosition(), agent.getCurrentLink().getId(), Sim2DConfig.STATIC_FORCE_RESOLUTION);

		if (f != null) {
			x = f.getFx();
			y = f.getFy();
		}

		force.setFx(force.getFx() + (Sim2DConfig.Apw * x / agent.getWeight()));
		force.setFy(force.getFy() + (Sim2DConfig.Apw * y / agent.getWeight()));
		// force.envX = Sim2DConfig.Apw * x/agent.getWeight();
		// force.envY = Sim2DConfig.Apw * y/agent.getWeight();

	}

	private void updateAgentInteractionForce(Agent2D agent, Force force) {
		double interactionX = 0;
		double interactionY = 0;

		ArrayList<Agent2D> others = agent.getNeighbors();
		// log.info("others:" + others.size());
		for (int i = 0; i < others.size(); i++) {
			Agent2D other = others.get(i);
			if (other.equals(agent)) {
				continue;
			}

			double x = agent.getPosition().x - other.getPosition().x;
			double y = agent.getPosition().y - other.getPosition().y;
			double sqrLength = Math.pow(x, 2) + Math.pow(y, 2);

			if (sqrLength > Sim2DConfig.PSqrSensingRange) {
				// if (Math.sqrt(sqrLength) > 10) {
				//
				// log.info("dist:" + Math.sqrt(sqrLength));
				// }
				continue;
			}

			if (sqrLength < 0.0001) {
				sqrLength = 0.0001;
			}

			double length = Math.sqrt(sqrLength);

			// double contrariness = getContrariness(force,length,x,y);

			double exp = Math.exp(Sim2DConfig.Bp / length) / length; // *
																		// contrariness/length;
			x *= exp;
			y *= exp;

			interactionX += x;
			interactionY += y;
		}

		force.setFx(force.getFx() + (Sim2DConfig.App * interactionX / agent.getWeight()));
		// force.interactionX = Sim2DConfig.App *
		// force.interactionX/agent.getWeight();
		force.setFy(force.getFy() + (Sim2DConfig.App * interactionY / agent.getWeight()));
		// force.interactionY = Sim2DConfig.App *
		// force.interactionY/agent.getWeight();

	}

	private double getContrariness(Force force, double length, double x, double y) {
		if (length > 0.45) {
			double dDriveX = force.getOldFx() - x / length;
			double dDriveY = force.getOldFy() - y / length;
			return Math.sqrt(Math.pow(dDriveX, 2) + Math.pow(dDriveY, 2));
			// if (contrariness < 0.1 && contrariness > 0) {
			// log.info("oldFx/Fy:" + force.getOldFx() + "/" + force.getOldFy()
			// + "  x/y:" + x + "/" + y + "  contr:" + contrariness);
			// }
		} else {
			return 1;
		}
	}

	public void addAgent(Agent2D agent) {
		this.agents.add(agent);
		Force force = new Force();
		this.agentForceMapping.put(agent, force);
	}

	public Set<Agent2D> getAgents() {
		return this.agents;
	}

	public double getAgentVelocity(Agent2D agent) {
		Force force = this.agentForceMapping.get(agent);

		return Math.sqrt(Math.pow(force.getFx(), 2) + Math.pow(force.getFx(), 2));
	}

	public Force getAgentForce(Agent2D agent) {
		return this.agentForceMapping.get(agent);
	}

	public void removeAgent(Agent2D agent) {
		this.agents.remove(agent);
		this.agentForceMapping.remove(agent);
	}
}
