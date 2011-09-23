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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * @author laemmel
 * 
 */
public class PhysicalFloor implements Floor {


	//profiling
	private long forceTime = 0;
	private long movementTime = 0;


	// needed to generated "finish lines"
	private static final double COS_LEFT = Math.cos(Math.PI / 2);
	// needed to generated "finish lines"
	private static final double SIN_LEFT = Math.sin(Math.PI / 2);
	// needed to generated "finish lines"
	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	// needed to generated "finish lines"
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);

	private final List<ForceModule> forceModules = new ArrayList<ForceModule>();
	protected final List<DynamicForceModule> dynamicForceModules = new ArrayList<DynamicForceModule>();

	protected final Set<Agent2D> agents = new LinkedHashSet<Agent2D>();
	private final Scenario scenario;
	private HashMap<Id, LineString> finishLines;

	private final GeometryFactory geofac = new GeometryFactory();

	private final double sim2DTimeStepSize;
	private final boolean emitXYZAzimuthEvents;
	private Envelope envelope;
	private final Sim2DConfigGroup sim2DConfig;
	private final EventsManager em;
	private final Collection<? extends Link> links;


	private static final boolean MS_FORCE_UPDATE = true;
	private static final int MAX_NUM_OF_THREADS = 4;

	public PhysicalFloor(Scenario scenario, EventsManager em, boolean emitEvents) {
		this.scenario = scenario;
		this.sim2DConfig = ((Sim2DConfigGroup)scenario.getConfig().getModule("sim2d"));
		this.sim2DTimeStepSize = this.sim2DConfig.getTimeStepSize();
		this.links = scenario.getNetwork().getLinks().values();
		this.em = em;
		this.emitXYZAzimuthEvents = emitEvents;

	}

	/**
	 * 
	 */
	public void init() {
		calculateEnvelope();


		if (this.sim2DConfig.isEnableCircularAgentInteractionModule()){
			this.dynamicForceModules.add(new CircularAgentInteractionModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableCollisionPredictionAgentInteractionModule()){
			this.dynamicForceModules.add(new  CollisionPredictionAgentInteractionModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableDrivingForceModule()) {
			this.forceModules.add(new DrivingForceModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableEnvironmentForceModule()){
			this.forceModules.add(new EnvironmentForceModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableCollisionPredictionEnvironmentForceModule()){
			this.forceModules.add(new CollisionPredictionEnvironmentForceModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnablePathForceModule()){
			this.forceModules.add(new PathForceModule(this, this.scenario));
		}

		for (ForceModule m : this.forceModules) {
			m.init();
		}

		for (ForceModule m : this.dynamicForceModules) {
			m.init();
		}

		createFinishLines();

	}

	private void calculateEnvelope() {
		this.envelope = new Envelope();
		for (Link link : this.links) {
			this.envelope.expandToInclude(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			this.envelope.expandToInclude(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		}

	}

	public Envelope getEnvelope() {
		return this.envelope;
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

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v2.simulation.floor.FloorTMP#move(double)
	 */
	@Override
	public void move(double time) {


		long now = System.currentTimeMillis();
		updateForces(time);
		long then = System.currentTimeMillis();
		this.forceTime += then-now;

		now = System.currentTimeMillis();
		moveAgents(time);
		then = System.currentTimeMillis();
		this.movementTime += then-now;
	}

	/**
	 * 
	 */
	protected void moveAgents(double time) {
		Iterator<Agent2D> it = this.agents.iterator();

		for (; it.hasNext();) {
			Agent2D agent = it.next();
			if (moveAgentAndCheckForEndOfLeg(agent, time)){

				it.remove();
				// (the above removes the Agent2D "wrapper" since endLegAndAssumeControl only moves the wrapped agent, not
				// the wrapper)

			}
		}

	}

	protected boolean moveAgentAndCheckForEndOfLeg(Agent2D agent, double time) {
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

			return true;
			// (returning "true" removes (or should remove) the Agent2D "wrapper" since endLegAndAssumeControl only moves
			// the wrapped agent, not the wrapper)

		}

		agent.moveToPostion(newPos);

		if (this.emitXYZAzimuthEvents) {
			XYVxVyEvent e = new XYVxVyEventImpl(agent.getId(), (Coordinate) agent.getPosition().clone(), agent.getVx(), agent.getVy(), time);
			this.em.processEvent(e);
		}
		//			if (Sim2DConfig.DEBUG) {
		//				ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), new Coordinate(agent.getPosition().x + vx , agent.getPosition().y + vy , 0), 0.0f, 1.f, 1.f, 9);
		//				getSim2D().getEventsManager().processEvent(arrow);
		return false;
	}
	private void validateVelocity(Force f, double v0) {
		double v = Math.sqrt(Math.pow(f.getVx(), 2)+Math.pow(f.getVy(), 2));
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
	protected boolean checkForEndOfLinkReached(Agent2D agent, Coordinate oldPos, Coordinate newPos, double time) {
		LineString finishLine = this.finishLines.get(agent.getCurrentLinkId());
		LineString trajectory = this.geofac.createLineString(new Coordinate[] { oldPos, newPos });
		if (trajectory.crosses(finishLine)) {
			LinkLeaveEventImpl e = new LinkLeaveEventImpl(time, agent.getId(), agent.getCurrentLinkId());
			this.em.processEvent(e);

			Id id = agent.chooseNextLinkId();

			// end of route
			if (id == null) {
				agent.endLegAndAssumeControl(time);

				return true;
				// (returning "true" removes (or should remove) the Agent2D "wrapper" since endLegAndAssumeControl only moves
				// the wrapped agent, not the wrapper)

			} else {
				agent.notifyMoveOverNode(id);
				LinkEnterEventImpl e2 = new LinkEnterEventImpl(time, agent.getId(), agent.getCurrentLinkId());
				this.em.processEvent(e2);
			}
		}

		return false;
	}



	/**
	 * 
	 */
	protected void updateForces(double time) {
		for (DynamicForceModule m : this.dynamicForceModules) {
			m.update(time);
		}


		if (MS_FORCE_UPDATE) {
			updateForcesMultiThreaded(time);
		} else {
			for (Agent2D agent : this.agents) {
				updateForces(agent,time);
			}
		}


	}

	private void updateForcesMultiThreaded(double time) {
		if (this.agents.size() == 0) {
			return;
		}

		int numOfThreads = Math.min(MAX_NUM_OF_THREADS, this.agents.size() / 25 + 1);

		BlockingQueue<Agent2D> queue = new ArrayBlockingQueue<Agent2D>(this.agents.size(),false,this.agents);
		//		ArrayBlockingQueue<Runnable> queueII = new ArrayBlockingQueue<Runnable>(5);
		//		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(numOfThreads, numOfThreads, 100, TimeUnit.SECONDS,queueII);
		//		for (int i = 0; i < 1; i++) {
		//			MultiThreadedForceUpdater t1 = new MultiThreadedForceUpdater(queue,this.forceModules,this.dynamicForceModules);
		//			threadPool.execute(t1);
		//			//			t1.run();
		//		}
		//		threadPool.shutdown();

		List<Thread> threads = new ArrayList<Thread>();

		for (int i = 0; i < numOfThreads; i++) {
			MultiThreadedForceUpdater t1 = new MultiThreadedForceUpdater(queue,this.forceModules,this.dynamicForceModules);
			Thread th1 = new Thread(t1);
			th1.start();
			threads.add(th1);
		}

		for (int i = 0; i < numOfThreads; i++) {
			try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//			}
			}
		}
	}
	protected void updateForces(Agent2D agent, double time) {



		for (ForceModule m : this.dynamicForceModules) {
			m.run(agent);
		}



		for (ForceModule m : this.forceModules) {
			m.run(agent);
		}
	}


	/**
	 * 
	 * @param module
	 */
	public void addForceModule(ForceModule module) {
		this.forceModules.add(module);
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
	public void agentDepart(MobsimDriverAgent pda) {
		Agent2D agent = new Agent2D(pda);
		Activity act = (Activity) getPreviousPlanElement(pda);
		if (act.getCoord() != null) {
			agent.setPostion(MGC.coord2Coordinate(act.getCoord()));
		} else {
			Coord coord = this.scenario.getNetwork().getLinks().get(act.getLinkId()).getCoord();
			agent.setPostion(new Coordinate(coord.getX()+MatsimRandom.getRandom().nextDouble()*2-1,coord.getY()+MatsimRandom.getRandom().nextDouble()*2-1));
		}
		this.agents.add(agent);
		for (DynamicForceModule m : this.dynamicForceModules) {
			m.forceUpdate();
		}

	}

	@Deprecated //add method to PlanAgent
	private PlanElement getPreviousPlanElement(MobsimAgent ma) {

		PlanAgent pda = (PlanAgent) ma ;
		Leg leg = (Leg) pda.getCurrentPlanElement();
		Plan plan = pda.getSelectedPlan();
		List<PlanElement> l = plan.getPlanElements();
		for (int i = 1; i < l.size(); i++) {
			if (l.get(i).equals(leg)) {
				return l.get(i-1);
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	public Collection<? extends Link> getLinks() {
		return this.links;
	}

	protected EventsManager getEventsManager() {
		return this.em;
	}


	//	public void printTimings() {
	//		System.err.println("force calculation took: " + this.forceTime);
	//		System.err.println("movement took: " + this.movementTime);
	//	}

}
