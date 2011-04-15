/* *********************************************************************** *
 * project: org.matsim.*
 * Floor.java
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
package playground.gregor.sim2d_v2.simulation.floor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZAzimuthEventImpl;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;
import playground.gregor.sim2d_v2.simulation.Sim2D;

/**
 * @author laemmel
 * 
 */
public class Floor {

	private static final double PI_HALF = Math.PI / 2;
	private static final double TWO_PI = 2 * Math.PI;
	// needed to generated "finish lines"
	private static final double COS_LEFT = Math.cos(Math.PI / 2);
	// needed to generated "finish lines"
	private static final double SIN_LEFT = Math.sin(Math.PI / 2);
	// needed to generated "finish lines"
	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	// needed to generated "finish lines"
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);

	private final List<ForceModule> forceModules = new ArrayList<ForceModule>();
	private final List<DynamicForceModule> dynamicForceModules = new ArrayList<DynamicForceModule>();

	private final Set<Agent2D> agents = new LinkedHashSet<Agent2D>();
	private final Scenario2DImpl scenario;
	private final List<Link> links;
	private final Sim2D sim2D;
	private HashMap<Id, LineString> finishLines;

	private final GeometryFactory geofac = new GeometryFactory();

	private final boolean ms = false;
	private final double sim2DTimeStepSize;
	private final boolean emitXYZAzimuthEvents = true;

	public Floor(Scenario2DImpl scenario, List<Link> list, Sim2D sim) {
		this.scenario = scenario;
		this.sim2DTimeStepSize = ((Sim2DConfigGroup)scenario.getConfig().getModule("sim2d")).getTimeStepSize();
		this.links = list;
		this.sim2D = sim;

	}

	/**
	 * 
	 */
	public void init() {
		//		this.dynamicForceModules.add(new CircularAgentInteractionModule(this, this.scenario));
		//
		this.dynamicForceModules.add(new  CollisionPredictionAgentInteractionModule(this, this.scenario));

		this.forceModules.add(new DrivingForceModule(this, this.scenario));
		//		this.forceModules.add(new EnvironmentForceModule(this, this.scenario));
		this.forceModules.add(new CollisionPredictionEnvironmentForceModule(this, this.scenario));
		this.forceModules.add(new PathForceModule(this, this.scenario));

		for (ForceModule m : this.forceModules) {
			m.init();
		}

		for (ForceModule m : this.dynamicForceModules) {
			m.init();
		}

		// TODO phantom force module

		createFinishLines();

	}

	private void createFinishLines() {

		this.finishLines = new HashMap<Id, LineString>();
		for (Link link : this.links) {
			Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
			Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
			Coordinate c = new Coordinate(from.x - to.x, from.y - to.y);
			// length of finish line is 30 m// TODO does this make sense?
			double scale = 30 / Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
			c.x *= scale;
			c.y *= scale;
			Coordinate c1 = new Coordinate(COS_LEFT * c.x + SIN_LEFT * c.y, -SIN_LEFT * c.x + COS_LEFT * c.y);
			c1.x += to.x;
			c1.y += to.y;
			Coordinate c2 = new Coordinate(COS_RIGHT * c.x + SIN_RIGHT * c.y, -SIN_RIGHT * c.x + COS_RIGHT * c.y);
			c2.x += to.x;
			c2.y += to.y;
			LineString ls = this.geofac.createLineString(new Coordinate[] { c1, c2 });

			this.finishLines.put(link.getId(), ls);
		}
	}

	/**
	 * @param time
	 */
	public void move(double time) {

		updateForces(time);
		moveAgents(time);

	}

	/**
	 * 
	 */
	private void moveAgents(double time) {
		Iterator<Agent2D> it = this.agents.iterator();

		for (; it.hasNext();) {
			Agent2D agent = it.next();
			Force f = agent.getForce();
			Coordinate oldPos = agent.getPosition();

			f.update(agent.getWeight(),this.sim2DTimeStepSize);
			//			f.update(this.sim2DTimeStepSize,agent.getWeight());
			validateVelocity(f,agent.getDesiredVelocity());

			double vx = f.getVx();
			double vy = f.getVy();


			Coordinate newPos = new Coordinate(oldPos.x + f.getVx()* this.sim2DTimeStepSize, oldPos.y + f.getVy()* this.sim2DTimeStepSize, 0);

			agent.setCurrentVelocity(vx,vy);

			//			System.out.println("ID:" + agent.getId() + "  velocity:" + Math.sqrt(Math.pow(vx, 2)+Math.pow(vy, 2)) + "    vx:" + vx + "    vy:" + vy + "   " + newPos);

			boolean endOfLeg = checkForEndOfLinkReached(agent, oldPos, newPos, time);
			if (endOfLeg) {
				it.remove();
				continue;
			}

			double azimuth = getAzimuth(oldPos, newPos);
			agent.moveToPostion(newPos);

			if (this.emitXYZAzimuthEvents ) {
				XYZAzimuthEvent e = new XYZAzimuthEventImpl(agent.getPerson().getId(), agent.getPosition(), azimuth, time);
				getSim2D().getEventsManager().processEvent(e);
			}
			//			if (Sim2DConfig.DEBUG) {
			//				ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), new Coordinate(agent.getPosition().x + vx , agent.getPosition().y + vy , 0), 0.0f, 1.f, 1.f, 9);
			//				getSim2D().getEventsManager().processEvent(arrow);
			//			}
		}

	}

	private void validateVelocity(Force f, double v0) {
		double v = Math.sqrt(Math.pow(f.getVx(), 2)+Math.pow(f.getVy(), 2));
		//		System.out.println("v:" + v);
		if (v > 1.25*v0) {
			double scale = (1.25*v0)/v;
			f.setVx(f.getVx()*scale);
			f.setVy(f.getVy()*scale);
		}

	}

	/**
	 * @param newPos
	 * @param oldPos
	 * 
	 */
	private boolean checkForEndOfLinkReached(Agent2D agent, Coordinate oldPos, Coordinate newPos, double time) {
		LineString finishLine = this.finishLines.get(agent.getCurrentLinkId());
		LineString trajectory = this.geofac.createLineString(new Coordinate[] { oldPos, newPos });
		if (trajectory.crosses(finishLine)) {
			LinkLeaveEventImpl e = new LinkLeaveEventImpl(time, agent.getId(), agent.getCurrentLinkId());
			this.sim2D.getEventsManager().processEvent(e);

			Id id = agent.chooseNextLinkId();

			// end of route
			if (id == null) {
				agent.endLegAndAssumeControl(time);
				return true;

			} else {
				agent.notifyMoveOverNode();
				LinkEnterEventImpl e2 = new LinkEnterEventImpl(time, agent.getId(), agent.getCurrentLinkId());
				this.sim2D.getEventsManager().processEvent(e2);
			}
		}

		return false;
	}

	/**
	 * @param newPos
	 * @param oldPos
	 * @return
	 */
	private double getAzimuth(Coordinate oldPos, Coordinate newPos) {
		double alpha = 0.0;
		double dX = oldPos.x - newPos.x;
		double dY = oldPos.y - newPos.y;
		if (dX > 0) {
			alpha = Math.atan(dY / dX);
		} else if (dX < 0) {
			alpha = Math.PI + Math.atan(dY / dX);
		} else { // i.e. DX==0
			if (dY > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0)
			alpha += TWO_PI;
		return alpha;
	}

	/**
	 * 
	 */
	private void updateForces(double time) {
		for (DynamicForceModule m : this.dynamicForceModules) {
			m.update(time);
		}

		if (this.ms) {
			multitThreadedForceUpdate();
		} else {
			for (Agent2D agent : this.agents) {

				for (ForceModule m : this.dynamicForceModules) {
					m.run(agent);

					//					if (Sim2DConfig.DEBUG) {
					//						Force f = agent.getForce();
					//						double dv = Math.sqrt(Math.pow(f.getXComponent(), 2)+Math.pow(f.getYComponent(), 2));
					//						if (dv*Sim2DConfig.TIME_STEP_SIZE > 2 ) {
					//							System.out.println("dv" + dv);
					//						}
					//					}
				}
				for (ForceModule m : this.forceModules) {
					m.run(agent);
					//					if (Sim2DConfig.DEBUG) {
					//						Force f = agent.getForce();
					//						double dv = Math.sqrt(Math.pow(f.getXComponent(), 2)+Math.pow(f.getYComponent(), 2));
					//						if (dv*Sim2DConfig.TIME_STEP_SIZE > 2 ) {
					//							System.out.println("dv" + dv);
					//						}
					//					}
				}
			}
		}

		//				for (Agent2D agent : this.agents) {
		//					validateForce(agent);
		//				}
	}

	/**
	 * 
	 */
	private void multitThreadedForceUpdate() {

		List<Agent2D> l1 = new ArrayList<Agent2D>();
		List<Agent2D> l2 = new ArrayList<Agent2D>();
		Iterator<Agent2D> it = this.agents.iterator();
		for (int i = 0; i < this.agents.size(); i++) {
			if (i < this.agents.size() / 2) {
				l1.add(it.next());
			} else {
				l2.add(it.next());
			}
		}

		MultiThreadedForceUpdater u1 = new MultiThreadedForceUpdater(l1, this.forceModules, this.dynamicForceModules);
		MultiThreadedForceUpdater u2 = new MultiThreadedForceUpdater(l2, this.forceModules, this.dynamicForceModules);

		Thread t1 = new Thread(u1);
		Thread t2 = new Thread(u2);

		t1.start();
		t2.start();

		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	//	private void validateForce(Agent2D agent) {
	//		Force force = agent.getForce();
	//		double norm = Math.sqrt(Math.pow(force.getXComponent(), 2) + Math.pow(force.getYComponent(), 2));
	//		if (norm > agent.getDesiredVelocity() ) {
	//			force.setXComponent(force.getXComponent() * ((agent.getDesiredVelocity() ) / norm));
	//			force.setYComponent(force.getYComponent() * ((agent.getDesiredVelocity() ) / norm));
	//		}
	//	}

	/**
	 * 
	 * @param module
	 */
	public void addForceModule(ForceModule module) {
		this.forceModules.add(module);
	}

	/**
	 * @param agent
	 */
	public void addAgent(Agent2D agent) {
		Activity act = (Activity) agent.getCurrentPlanElement();
		if (act.getCoord() != null) {
			agent.setPostion(MGC.coord2Coordinate(act.getCoord()));
		} else {
			agent.setPostion(MGC.coord2Coordinate(this.scenario.getNetwork().getLinks().get(act.getLinkId()).getCoord()));
		}

	}

	/**
	 * 
	 * @return list of agents
	 */
	public Set<Agent2D> getAgents() {
		return this.agents;
	}

	/**
	 * @param agent
	 */
	public void agentDepart(Agent2D agent) {
		this.agents.add(agent);
		for (DynamicForceModule m : this.dynamicForceModules) {
			m.forceUpdate();
		}

	}

	/**
	 * @return
	 */
	public List<Link> getLinks() {
		return this.links;
	}


	/**
	 * @return the sim2D
	 */
	public Sim2D getSim2D() {
		return this.sim2D;
	}

	private static class MultiThreadedForceUpdater implements Runnable {

		private final List<Agent2D> agents;
		private final List<ForceModule> fm;
		private final List<DynamicForceModule> dfm;

		public MultiThreadedForceUpdater(List<Agent2D> agents, List<ForceModule> fm, List<DynamicForceModule> dfm) {
			this.agents = agents;
			this.fm = fm;
			this.dfm = dfm;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			for (Agent2D agent : this.agents) {
				for (ForceModule m : this.dfm) {
					m.run(agent);
				}
				for (ForceModule m : this.fm) {
					m.run(agent);
				}
			}
		}

	}
}
