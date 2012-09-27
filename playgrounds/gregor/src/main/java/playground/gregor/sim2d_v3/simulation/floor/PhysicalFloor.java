/* *********************************************************************** *
 * project: org.matsim.*
 * PhysicalFloor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.simulation.floor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v3.controller.PedestrianSignal;
import playground.gregor.sim2d_v3.events.XYDataFilter;
import playground.gregor.sim2d_v3.events.XYVxVyEvent;
import playground.gregor.sim2d_v3.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v3.simulation.floor.forces.Force;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.CollisionPredictionAgentInteractionModule;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.CollisionPredictionEnvironmentForceModule;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.DrivingForceModule;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCAForce;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.PathForceModule;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.SignalsPerception;
import playground.gregor.sim2d_v3.simulation.floor.forces.reactive.CircularAgentInteractionModule;
import playground.gregor.sim2d_v3.simulation.floor.forces.reactive.EnvironmentForceModuleII;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * @author laemmel
 */
public class PhysicalFloor implements Floor {

	//profiling
	private long forceTime = 0;
	private long movementTime = 0;

	private final Scenario scenario;

	private Envelope envelope;
	private final Sim2DConfigGroup sim2DConfig;
	private final EventsManager em;
	private final Collection<? extends Link> links;
	private FinishLineCrossedChecker finishLineCrossChecker;
	private final Map<Id, PedestrianSignal> signals;
	private final ForceUpdater forceUpdater;
	private final Random random;
	private final AgentsMover agentsMover;

	public PhysicalFloor(Scenario scenario, EventsManager em, boolean emitEvents, boolean filterEvents, 
			Map<Id, PedestrianSignal> signals, InternalInterface internalInterface) {
		this.signals = signals;
		this.scenario = scenario;
		this.sim2DConfig = ((Sim2DConfigGroup)scenario.getConfig().getModule("sim2d"));
		this.links = scenario.getNetwork().getLinks().values();
		this.em = em;

		this.scenario.getConfig().getQSimConfigGroup().setNumberOfThreads(1);
		int numOfThreads = this.scenario.getConfig().getQSimConfigGroup().getNumberOfThreads();
		if (numOfThreads > 1) {
			this.agentsMover = new ParallelAgentsMover(scenario, em, emitEvents, filterEvents, 
					internalInterface, new ConcurrentSkipListSet<Agent2D>(new Agent2DComparator()));			
		} else {
			this.agentsMover = new AgentsMover(scenario, em, emitEvents, filterEvents, 
					internalInterface, new ConcurrentSkipListSet<Agent2D>(new Agent2DComparator()));
		}

		this.forceUpdater = new ForceUpdaterImpl(this.agentsMover.getAgents(), numOfThreads);
		this.random = MatsimRandom.getLocalInstance();
	}

	/**
	 * 
	 */
	public void init() {
		calculateEnvelope();

		if (this.sim2DConfig.isEnableVelocityObstacleModule()) {
			this.forceUpdater.addDynamicForceModule(new ORCAForce(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableCircularAgentInteractionModule()){
			this.forceUpdater.addDynamicForceModule(new CircularAgentInteractionModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableCollisionPredictionAgentInteractionModule()){
			this.forceUpdater.addDynamicForceModule(new CollisionPredictionAgentInteractionModule(this, this.scenario));
		}

		if (this.scenario.getConfig().scenario().isUseSignalSystems()) {
			//this is not the right way to do this, if driving velocity is calculated before SingalsPerception then we lag one
			//time step behind
			this.forceUpdater.addForceModule(new SignalsPerception(this.signals));
		}

		if (this.sim2DConfig.isEnableDrivingForceModule()) {
			this.forceUpdater.addForceModule(new DrivingForceModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableEnvironmentForceModule()){
			this.forceUpdater.addForceModule(new EnvironmentForceModuleII(this, this.scenario));
		}

		if (this.sim2DConfig.isEnableCollisionPredictionEnvironmentForceModule()){
			this.forceUpdater.addForceModule(new CollisionPredictionEnvironmentForceModule(this, this.scenario));
		}

		if (this.sim2DConfig.isEnablePathForceModule()){
			this.forceUpdater.addForceModule(new PathForceModule(this, this.scenario));
		}

		this.forceUpdater.init();

		this.finishLineCrossChecker = new FinishLineCrossedChecker(this.scenario);
		this.finishLineCrossChecker.init();

		this.agentsMover.setFinishLineCrossedChecker(this.finishLineCrossChecker);
	}

	public ForceUpdater getForceUpdater() {
		return this.forceUpdater;
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

	public AgentsMover getAgentsMover() {
		return this.agentsMover;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v2.simulation.floor.FloorTMP#move(double)
	 */
	@Override
	public void move(double time) {

		long now = System.currentTimeMillis();
		this.forceUpdater.updateForces(time);
		long then = System.currentTimeMillis();
		this.forceTime += then-now;

		now = System.currentTimeMillis();
		this.agentsMover.moveAgents(time);
		then = System.currentTimeMillis();
		this.movementTime += then-now;
	}

	/*
	 * For backwards compatibility. This method is overwritten by PhantomFloor.
	 */
	protected void moveAgents(double time) {
		//		agentsMover.moveAgents(time);
	}

	/**
	 * 
	 * @return list of agents
	 */
	public Set<Agent2D> getAgents() {
		return Collections.unmodifiableSet(this.agentsMover.agents);
	}

	/**
	 * @param agent
	 */
	public void agentDepart(Agent2D agent) {

		Activity act = (Activity) getPreviousPlanElement(agent);
		if (act.getCoord() != null) {
			Coord coord = act.getCoord();
			agent.setPostion(new Coordinate(coord.getX()+this.random.nextDouble()-.5,coord.getY()+this.random.nextDouble()-.5));
			//MGC.coord2Coordinate(act.getCoord()));
		} else {
			Coord coord = this.scenario.getNetwork().getLinks().get(act.getLinkId()).getCoord();
			agent.setPostion(new Coordinate(coord.getX()+this.random.nextDouble()*2-1,coord.getY()+this.random.nextDouble()*2-1));
		}
		this.agentsMover.addAgent(agent);

		Id lid = act.getLinkId();
		Link l = this.scenario.getNetwork().getLinks().get(lid);
		double fromX = l.getFromNode().getCoord().getX();
		double fromY = l.getFromNode().getCoord().getY();
		double toX = l.getToNode().getCoord().getX();
		double toY = l.getToNode().getCoord().getY();
		double vx = agent.getDesiredVelocity()*((toX-fromX)/l.getLength());
		double vy = agent.getDesiredVelocity()*((toY-fromY)/l.getLength());
		agent.getForce().setVx(vx);
		agent.getForce().setVy(vy);
		//		if (agent.getId().toString().contains("r")) {
		//			agent.getForce().setVx(1.34);
		//		} else {
		//			agent.getForce().setVy(-1.34);
		//		}

		this.forceUpdater.updateDynamicForces();
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

	/*
	 * For backwards compatibility. Only used by PhantomFloor.
	 */
	protected EventsManager getEventsManager() {
		return this.em;
	}

	public void afterSim() {
		this.forceUpdater.afterSim();
		this.agentsMover.afterSim();
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

	public static class AgentsMover {

		private final XYDataFilter xyDataFilter;
		private final Set<Agent2D> agents;
		private final EventsManager em;
		private final Sim2DConfigGroup sim2DConfig;
		private final double sim2DTimeStepSize;
		private final boolean filterEvents;
		private final boolean emitXYZAzimuthEvents;
		private final InternalInterface internalInterface;

		protected double time;
		protected FinishLineCrossedChecker finishLineCrossChecker;

		public AgentsMover(Scenario scenario, EventsManager em, boolean emitEvents, boolean filterEvents, InternalInterface internalInterface, Set<Agent2D> agents) {
			this.em = em;
			this.sim2DConfig = ((Sim2DConfigGroup)scenario.getConfig().getModule("sim2d"));
			this.sim2DTimeStepSize = this.sim2DConfig.getTimeStepSize();
			this.emitXYZAzimuthEvents = emitEvents;
			this.filterEvents = filterEvents;
			this.internalInterface = internalInterface;

			this.agents = agents;

			this.xyDataFilter = new XYDataFilter();
		}

		public void addAgent(Agent2D agent) {
			this.agents.add(agent);
		}

		public void removeAgent(Agent2D agent) {
			this.agents.remove(agent);
		}

		public Set<Agent2D> getAgents() {
			return this.agents;
		}

		public void setTime(double time) {
			this.time = time;
		}

		public void setFinishLineCrossedChecker(FinishLineCrossedChecker finishLineCrossChecker) {
			this.finishLineCrossChecker = finishLineCrossChecker;
		}

		public void afterSim() {

			// print filter events statistics
			if (this.filterEvents) this.xyDataFilter.afterSim();
		}

		protected void moveAgents(double time) {
			Iterator<Agent2D> it = this.agents.iterator();

			for (; it.hasNext();) {
				Agent2D agent = it.next();
				if (moveAgentAndCheckForEndOfLeg(agent, time)) {

					it.remove();
					// (the above removes the Agent2D "wrapper" since endLegAndAssumeControl only moves the wrapped agent, not
					// the wrapper)
				}
			}
		}

		protected boolean moveAgentAndCheckForEndOfLeg(Agent2D agent, double time) {

			Force f = agent.getForce();
			Coordinate oldPos = agent.getPosition();

			f.update(agent.getWeight(), this.sim2DTimeStepSize);
			//			f.update(this.sim2DTimeStepSize,agent.getWeight());
//			validateVelocity(f, agent.getDesiredVelocity());

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

			agent.translate(dx, dy, vx, vy);

			// if events should be emitted
			if (this.emitXYZAzimuthEvents) {
				XYVxVyEventImpl e = new XYVxVyEventImpl(agent.getDelegate().getId(), agent.getPosition().x, agent.getPosition().y, agent.getVx(), agent.getVy(), time);

				// if events are not filtered or the filter decides to process the event
				if (!this.filterEvents || this.xyDataFilter.processXYVxVyEvent(e)) {
					this.em.processEvent(e);					
				}
			}
			//			if (Sim2DConfig.DEBUG) {
			//				ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), agent.getPosition(), new Coordinate(agent.getPosition().x + vx , agent.getPosition().y + vy , 0), 0.0f, 1.f, 1.f, 9);
			//				getSim2D().getEventsManager().processEvent(arrow);
			return false;
		}

		private void validateVelocity(Force f, double v0) {
			double v = Math.sqrt(Math.pow(f.getVx(), 2) + Math.pow(f.getVy(), 2));
			if (v > 1.25*v0) {
				double scale = (1.25 * v0) / v;
				f.setVx(f.getVx() * scale);
				f.setVy(f.getVy() * scale);
			}
		}

		/**
		 * @param newPos
		 * @param oldPos
		 */
		protected boolean checkForEndOfLinkReached(Agent2D agent2D, Coordinate oldPos, Coordinate newPos, double time) {
			MobsimDriverAgent agent = agent2D.getDelegate();
			if (this.finishLineCrossChecker.crossesFinishLine(agent.getCurrentLinkId(), agent.chooseNextLinkId(), oldPos, newPos)) {
				LinkLeaveEvent e = new LinkLeaveEvent(time, agent.getId(), agent.getCurrentLinkId(), null);
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
					agent2D.notifyMoveOverNode(id, time);
					LinkEnterEvent e2 = new LinkEnterEvent(time, agent.getId(), agent.getCurrentLinkId(),null);
					this.em.processEvent(e2);
				}
			}

			return false;
		}
	}

	/*
	 * For parallel simulation.
	 */
	private static class AgentsMoverRunner extends AgentsMover implements Runnable {

		private volatile boolean simulationRunning = true;

		private CyclicBarrier startBarrier;
		private CyclicBarrier endBarrier;
		private final Set<Agent2D> allAgents;

		/*
		 * allAgents contains all 2D agents in the physical floor, agentsToHandle only
		 * those which are moved by the current thread. When a handled agent reaches
		 * its destination, it has to be remove from both sets!
		 */
		public AgentsMoverRunner(Scenario scenario, EventsManager em, boolean emitEvents, boolean filterEvents, 
				InternalInterface internalInterface, Set<Agent2D> agentsToHandle, Set<Agent2D> allAgents) {
			super(scenario, em, emitEvents, filterEvents, internalInterface, agentsToHandle);
			this.allAgents = allAgents;
		}

		@Override
		public void afterSim() {
			this.simulationRunning = false;
			super.afterSim();
		}

		public void setStartBarrier(CyclicBarrier startBarrier) {
			this.startBarrier = startBarrier;
		}

		public void setEndBarrier(CyclicBarrier endBarrier) {
			this.endBarrier = endBarrier;
		}

		@Override
		protected void moveAgents(double time) {
			// Iterate over those agents which are handled by this thread.
			Iterator<Agent2D> it = this.getAgents().iterator();

			for (; it.hasNext();) {
				Agent2D agent = it.next();
				if (moveAgentAndCheckForEndOfLeg(agent, time)) {

					/*
					 * Remove the agent from this thread's set and the set in the
					 * main thread that contains all agents.
					 */
					it.remove();
					this.allAgents.remove(agent);
				}
			}
		}

		@Override
		public void run() {

			// The method is ended when the simulationRunning Flag is set to false.
			while(true) {
				try {
					// The Threads wait at the startBarrier until they are triggered.
					this.startBarrier.await();

					// Check if Simulation is still running. Otherwise print CPU usage and end Thread.
					if (!this.simulationRunning) {
						Gbl.printCurrentThreadCpuTime();
						return;
					}

					this.moveAgents(this.time);

					/*
					 * The End of the Moving is synchronized with the endBarrier. If 
					 * all Threads reach this Barrier the main Thread can go on.
					 */
					this.endBarrier.await();
				} catch (InterruptedException e) {
					Gbl.errorMsg(e);
				} catch (BrokenBarrierException e) {
					Gbl.errorMsg(e);
				}
			}
		}
	}

	private static class ParallelAgentsMover extends AgentsMover {

		private final int numOfThreads;
		private Thread[] threads;
		private AgentsMoverRunner[] runners;
		private Set<Agent2D>[] agents;
		private int roundRobin = 0;
		private CyclicBarrier startBarrier;
		private CyclicBarrier endBarrier;

		public ParallelAgentsMover(Scenario scenario, EventsManager em, boolean emitEvents, 
				boolean filterEvents, InternalInterface internalInterface, Set<Agent2D> agents) {
			super(scenario, em, emitEvents, filterEvents, internalInterface, agents);
			this.numOfThreads = scenario.getConfig().getQSimConfigGroup().getNumberOfThreads();
			initParallelAgentMovers(scenario, em, emitEvents, filterEvents, internalInterface);
		}

		@Override
		public void addAgent(Agent2D agent) {
			// add the agent to the super class which collects all agents
			super.addAgent(agent);

			// assign it to one of the threads
			this.agents[this.roundRobin % this.numOfThreads].add(agent);
			this.roundRobin++;
		}

		@Override
		public void removeAgent(Agent2D agent) {
			// remove the agent from the super class which collects all agents
			super.removeAgent(agent);

			// remove it the thread where it was assigned to
			for (Set<Agent2D> set : this.agents) {
				if (set.remove(agent)) return;
			}
		}

		@Override
		public void setFinishLineCrossedChecker(FinishLineCrossedChecker finishLineCrossChecker) {
			super.setFinishLineCrossedChecker(finishLineCrossChecker);

			for (AgentsMover agentsMover : this.runners) {
				agentsMover.setFinishLineCrossedChecker(finishLineCrossChecker);
			}
		}

		private void initParallelAgentMovers(Scenario scenario, EventsManager em, boolean emitEvents, 
				boolean filterEvents, InternalInterface internalInterface) {

			this.threads = new Thread[this.numOfThreads];
			this.runners = new AgentsMoverRunner[this.numOfThreads];
			this.agents = new Set[this.numOfThreads];

			this.startBarrier = new CyclicBarrier(this.numOfThreads + 1);
			this.endBarrier = new CyclicBarrier(this.numOfThreads + 1);

			// setup threads
			for (int i = 0; i < this.numOfThreads; i++) {

				Set<Agent2D> agentsToHandle = new LinkedHashSet<Agent2D>();
				this.agents[i] = agentsToHandle;

				AgentsMoverRunner mover = new AgentsMoverRunner(scenario, em, emitEvents, filterEvents, 
						internalInterface, agentsToHandle, this.getAgents());
				mover.setStartBarrier(this.startBarrier);
				mover.setEndBarrier(this.endBarrier);

				Thread thread = new Thread(mover);
				thread.setName(mover.getClass().toString() + i);

				thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
				this.threads[i] = thread;
				this.runners[i] = mover;

				thread.start();
			}
		}

		@Override
		protected void moveAgents(double time) {
			try {

				// set current Time
				for (AgentsMover mover : this.runners) {
					mover.setTime(time);
				}

				//				// assign agents to the threads
				//				int roundRobin = 0;
				//				for (Agent2D agent : this.agents) {
				//					this.updaters[roundRobin % this.numOfThreads].addAgent(agent);
				//					roundRobin++;
				//				}

				this.startBarrier.await();

				this.endBarrier.await();
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			} catch (BrokenBarrierException e) {
				Gbl.errorMsg(e);
			}
		}

		@Override
		public void afterSim() {			
			/*
			 * Calling the afterSim Method of the agent movers
			 * will set their simulationRunning flag to false.
			 */
			for (AgentsMover mover : this.runners) {
				mover.afterSim();
			}

			/*
			 * Triggering the startBarrier of the parallel AgentMovers.
			 * They will check whether the Simulation is still running.
			 * It is not, so the Threads will stop running.
			 */
			try {
				this.startBarrier.await();
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			} catch (BrokenBarrierException e) {
				Gbl.errorMsg(e);
			}
		}
	}

	public class Agent2DComparator implements Comparator<Agent2D>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Agent2D agent1, Agent2D agent2) {
			// let the one with the larger id be first (=smaller)
			return agent2.getDelegate().getId().compareTo(agent1.getDelegate().getId());
		}

	}
}
