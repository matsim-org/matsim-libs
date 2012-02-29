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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.ptproject.qsim.InternalInterface;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.controller.PedestrianSignal;
import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v2.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.Force;
import playground.gregor.sim2d_v2.simulation.floor.forces.ForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.CollisionPredictionAgentInteractionModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.CollisionPredictionEnvironmentForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.DrivingForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.PathForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.SignalsPerception;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.VelocityObstacleForceII;
import playground.gregor.sim2d_v2.simulation.floor.forces.reactive.CircularAgentInteractionModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.reactive.EnvironmentForceModuleII;
import playground.gregor.sim2d_v2.simulation.floor.forces.reactive.PhysicalAgentInteractionForce;
import playground.gregor.sim2d_v2.simulation.floor.forces.reactive.PhysicalEnvironmentForce;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * @author laemmel
 * 
 */
public class PhysicalFloor implements Floor {


	//profiling
	private long forceTime = 0;
	private long movementTime = 0;




	private final List<ForceModule> forceModules = new ArrayList<ForceModule>();
	protected final List<DynamicForceModule> dynamicForceModules = new ArrayList<DynamicForceModule>();

	protected final Set<Agent2D> agents = new LinkedHashSet<Agent2D>();
	private final Scenario scenario;


	private final double sim2DTimeStepSize;
	private final boolean emitXYZAzimuthEvents;
	private Envelope envelope;
	private final Sim2DConfigGroup sim2DConfig;
	private final EventsManager em;
	private final Collection<? extends Link> links;
	private FinishLineCrossedChecker finishLineCrossChecker;
	private final Map<Id, PedestrianSignal> signals;
	private final InternalInterface internalInterface;


	private static final boolean MS_FORCE_UPDATE = false;
	private static final int MAX_NUM_OF_THREADS = 4;

	public PhysicalFloor(Scenario scenario, EventsManager em, boolean emitEvents, Map<Id, PedestrianSignal> signals, InternalInterface internalInterface) {
		this.signals = signals;
		this.scenario = scenario;
		this.sim2DConfig = ((Sim2DConfigGroup)scenario.getConfig().getModule("sim2d"));
		this.sim2DTimeStepSize = this.sim2DConfig.getTimeStepSize();
		this.links = scenario.getNetwork().getLinks().values();
		this.em = em;
		this.emitXYZAzimuthEvents = emitEvents;
		this.internalInterface = internalInterface;


	}

	/**
	 * 
	 */
	public void init() {
		calculateEnvelope();

		
		
		
		if (this.sim2DConfig.isEnableVelocityObstacleModule()) {
			this.dynamicForceModules.add(new VelocityObstacleForceII(this, this.scenario));
			this.dynamicForceModules.add(new PhysicalAgentInteractionForce(this, this.scenario));
			this.forceModules.add(new PhysicalEnvironmentForce(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableCircularAgentInteractionModule()){
			this.dynamicForceModules.add(new CircularAgentInteractionModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableCollisionPredictionAgentInteractionModule()){
			this.dynamicForceModules.add(new  CollisionPredictionAgentInteractionModule(this, this.scenario));
		}

		if (this.scenario.getConfig().scenario().isUseSignalSystems()) {
			//this is not the right way to do this, if driving velocity is calculated before SingalsPerception then we lag one
			//time step behind
			this.forceModules.add(new SignalsPerception(this.signals));
		}
		
		if (this.sim2DConfig.isEnableDrivingForceModule()) {
			this.forceModules.add(new DrivingForceModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableEnvironmentForceModule()){
			this.forceModules.add(new EnvironmentForceModuleII(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableCollisionPredictionEnvironmentForceModule()){
			this.forceModules.add(new CollisionPredictionEnvironmentForceModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnablePathForceModule()){
			this.forceModules.add(new PathForceModule(this, this.scenario));
		}


		//		//testing only
		//		this.forceModules.add(new PhysicalEnvironmentForce(this, this.scenario));
		//		this.dynamicForceModules.add(new PhysicalAgentInteractionForce(this, this.scenario));

		for (ForceModule m : this.forceModules) {
			m.init();
		}

		for (ForceModule m : this.dynamicForceModules) {
			m.init();
		}

		this.finishLineCrossChecker  = new FinishLineCrossedChecker(this.scenario);
		this.finishLineCrossChecker.init();


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


		double dx = f.getVx()* this.sim2DTimeStepSize;
		double dy = f.getVy()* this.sim2DTimeStepSize;
		Coordinate newPos = new Coordinate(oldPos.x + dx, oldPos.y + dy, 0);

		//		agent.setCurrentVelocity(vx,vy);

		//			System.out.println("ID:" + agent.getId() + "  velocity:" + Math.sqrt(Math.pow(vx, 2)+Math.pow(vy, 2)) + "    vx:" + vx + "    vy:" + vy + "   " + newPos);

		boolean endOfLeg = checkForEndOfLinkReached(agent, oldPos, newPos, time);
		if (endOfLeg) {

			return true;
			// (returning "true" removes (or should remove) the Agent2D "wrapper" since endLegAndAssumeControl only moves
			// the wrapped agent, not the wrapper)

		}

		agent.translate(dx,dy,vx,vy);

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
		if (v > 2*v0) {
			double scale = (2*v0)/v;
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
		if (this.finishLineCrossChecker.crossesFinishLine(agent.getCurrentLinkId(),agent.chooseNextLinkId(),oldPos,newPos)) {
			LinkLeaveEventImpl e = new LinkLeaveEventImpl(time, agent.getId(), agent.getCurrentLinkId(), null);
			this.em.processEvent(e);

			Id id = agent.chooseNextLinkId();

			// end of route
			if (id == null) {
				agent.endLegAndComputeNextState(time);
				this.internalInterface.arrangeNextAgentState(agent);
				return true;
				// (returning "true" removes (or should remove) the Agent2D "wrapper" since endLegAndAssumeControl only moves
				// the wrapped agent, not the wrapper)

			} else {
				agent.notifyMoveOverNode(id,time);
				LinkEnterEventImpl e2 = new LinkEnterEventImpl(time, agent.getId(), agent.getCurrentLinkId(),null);
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
			MultiThreadedForceUpdater t1 = new MultiThreadedForceUpdater(queue,this.forceModules,this.dynamicForceModules,time);
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
			m.run(agent,time);
		}



		for (ForceModule m : this.forceModules) {
			m.run(agent,time);
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
	public void agentDepart(Agent2D agent) {
		
		
		Activity act = (Activity) getPreviousPlanElement(agent);
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
	private PlanElement getPreviousPlanElement(Agent2D ma) {

		PlanAgent pda = (PlanAgent) ma.getDelegate() ;
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

//	//DEBUG
//	public void drawGeometries(double time) {
//		for (Agent2D agent : this.agents) {
//			CCWPolygon geo = agent.getGeometry();
//			drawGeometry(geo, agent.getId(), time);
//		}
//		
//	}
//
//	private void drawGeometry(CCWPolygon geo, Id id, double time) {
//		Coordinate[] ring = geo.getCCWRing();
//		for (int i = 0; i < ring.length-1; i++) {
//			drawSeg(id,i,ring[i], ring[i+1], time);
//		}
//		drawSeg(id,ring.length-1,ring[ring.length-1], ring[0], time);
//	}
//
//	private void drawSeg(Id id, int i, Coordinate coordinate,
//			Coordinate coordinate2, double time) {
//		
//		ArrowEvent ev = new ArrowEvent(id, coordinate, coordinate2, 0.f, 0.f, 0.f, i, time);
//		this.em.processEvent(ev);
//		
//	}


	//	public void printTimings() {
	//		System.err.println("force calculation took: " + this.forceTime);
	//		System.err.println("movement took: " + this.movementTime);
	//	}

}
