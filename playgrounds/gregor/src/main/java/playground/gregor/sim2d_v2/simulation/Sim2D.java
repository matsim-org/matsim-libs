/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2D.java
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
package playground.gregor.sim2d_v2.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListenerManager;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.comparators.PlanAgentDepartureTimeComparator;
import org.matsim.ptproject.qsim.helpers.AgentCounter;
import org.matsim.ptproject.qsim.helpers.QSimTimer;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.interfaces.SimTimerI;

import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.floor.Floor;

/**
 * implementation of a 2d pedestrian transport simulation some day 2d
 * functionality might be merged into QSim for that reason so far a lot of copy
 * and paste code from org.matsim.ptproject.qsim.Qsim
 * 
 * @author laemmel
 * 
 */
public class Sim2D implements Mobsim {

	private static final Logger log = Logger.getLogger(Sim2D.class);

	private EventsManagerImpl events;
	private final Scenario2DImpl scenario;

	private QSimTimer simTimer;

	private SimulationListenerManager listenerManager;

	private AgentCounter agentCounter;

	private Sim2DEngine sim2DEngine;

	private Agent2DFactory agentFactory;

	private final Collection<MobsimAgent> agents = new ArrayList<MobsimAgent>();

	private double stopTime;

	private final Queue<PlanAgent> activityEndsList = new PriorityQueue<PlanAgent>(500, new PlanAgentDepartureTimeComparator());

	private static final int INFO_PERIOD = 3600;
	private double infoTime;

	private final Date realWorldStarttime = new Date();

	private Integer iterationNumber = null;

	private final List<DepartureHandler> departureHandlers = new ArrayList<DepartureHandler>();

	public Sim2D(EventsManagerImpl events, Scenario2DImpl scenario2DData) {
		this(events, scenario2DData, new DefaultSim2DEngineFactory());
	}

	/**
	 * @param events
	 * @param scenario2dData
	 */
	public Sim2D(EventsManagerImpl events, Scenario2DImpl scenario2DData, DefaultSim2DEngineFactory factory) {
		this.events = events;
		this.scenario = scenario2DData;
		init(factory);
	}

	/**
	 * @param factory
	 */
	private void init(DefaultSim2DEngineFactory factory) {
		log.info("Using Sim2D...");
		Scenario sc = getScenario();
		this.listenerManager = new SimulationListenerManager(this);
		this.agentCounter = new AgentCounter();
		this.simTimer = new QSimTimer(sc.getConfig().getQSimConfigGroup().getTimeStepSize());

		// create Sim2DEngine
		this.sim2DEngine = factory.createSim2DEngine(this, MatsimRandom.getRandom());
		this.agentFactory = new Agent2DFactory(this);

		// TODO may be we have to create a departure handler somewhere here? --
		// yes we do!
		this.departureHandlers.add(new Sim2DDepartureHandler(this));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.mobsim.framework.IOSimulation#setControlerIO(org.matsim
	 * .core.controler.ControlerIO)
	 */
	@Override
	public void setControlerIO(ControlerIO cio) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.mobsim.framework.IOSimulation#setIterationNumber(java
	 * .lang.Integer)
	 */
	@Override
	public void setIterationNumber(Integer iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.mobsim.framework.Simulation#run()
	 */
	@Override
	public void run() {
		prepareSim();
		this.listenerManager.fireQueueSimulationInitializedEvent();
		// do iterations
		boolean doContinue = true;
		double time = this.simTimer.getTimeOfDay();
		while (doContinue) {
			this.listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			beforeSimStep(time);
			doContinue = doSimStep(time);
			afterSimStep(time);
			this.listenerManager.fireQueueSimulationAfterSimStepEvent(time);
			if (doContinue) {
				time = this.simTimer.incrementTime();
			}

		}

		this.listenerManager.fireQueueSimulationBeforeCleanupEvent();
		cleanupSim(time);
		// delete reference to clear memory
		this.listenerManager = null;
	}

	/**
	 * @param time
	 */
	private void cleanupSim(double time) {
		if (this.sim2DEngine != null) {
			this.sim2DEngine.afterSim();
		}

		double now = this.simTimer.getTimeOfDay();
		for (PlanAgent agent : this.activityEndsList) {
			if (agent.getDestinationLinkId() != null) {
				this.events.processEvent(this.events.getFactory().createAgentStuckEvent(now, agent.getId(), agent.getDestinationLinkId(), null));
			}
		}
		this.activityEndsList.clear();
		this.sim2DEngine = null;
		this.events = null; // delete events object to free events handlers, if
		// they are nowhere else referenced
	}

	/**
	 * @param time
	 */
	protected void afterSimStep(final double time) {
		// left empty for inheritance
	}

	/**
	 * @param time
	 * @return
	 */
	private boolean doSimStep(double time) {
		// "facilities" "engine":
		handleActivityEnds(time);

		if (this.sim2DEngine != null) {
			this.sim2DEngine.doSimStep(time);
		}

		printSimLog(time);

		return (this.agentCounter.isLiving() && (this.stopTime > time));
	}

	/**
	 * @param time
	 */
	private void handleActivityEnds(double time) {
		while (this.activityEndsList.peek() != null) {
			PlanAgent agent = this.activityEndsList.peek();
			if (agent.getActivityEndTime() <= time) {
				this.activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent); // TODO do wie need
				// this??
				agent.endActivityAndAssumeControl(time);
				// gives control to agent; comes back via "agentDeparts" or
				// "scheduleActivityEnd"
			} else {
				return;
			}
		}

	}

	/**
	 * @param agent
	 */
	private void unregisterAgentAtActivityLocation(PlanAgent agent) {
		log.error("Not implemented!! Not sure if we need this!!");

	}

	/**
	 * @param time
	 */
	private void beforeSimStep(double time) {
		// left empty for inheritance
	}

	/**
	 * 
	 */
	private void prepareSim() {
		if (this.events == null) {
			throw new RuntimeException("No valid Events Object (events == null)");
		}
		if (this.sim2DEngine != null) {
			this.sim2DEngine.onPrepareSim();
		}

		createAgents();
		initSimTimer();
		// infoTime may be < simStartTime, this ensures to print out the info at
		// the very first timestep already
		this.infoTime = Math.floor(this.simTimer.getSimStartTime() / INFO_PERIOD) * INFO_PERIOD;

		// TODO <-- put in change events engine right here
	}

	/**
	 * 
	 */
	private void initSimTimer() {
		double startTime = this.scenario.getConfig().getQSimConfigGroup().getStartTime();
		this.stopTime = this.scenario.getConfig().getQSimConfigGroup().getEndTime();
		if (startTime == Time.UNDEFINED_TIME)
			startTime = 0.0;
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0))
			this.stopTime = Double.MAX_VALUE;
		this.simTimer.setSimStartTime(24 * 3600);
		this.simTimer.setTime(startTime);
		// set sim start time to config-value ONLY if this is LATER than the
		// first plans starttime
		double simStartTime = 0;
		PlanAgent firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(startTime, firstAgent.getActivityEndTime()));
		}
		this.simTimer.setSimStartTime(simStartTime);
		this.simTimer.setTime(simStartTime);

	}

	/**
	 * 
	 */
	private void createAgents() {
		if (this.scenario.getPopulation() == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			PersonDriverAgentImpl agent = this.agentFactory.createPersonAgent(p);
			this.agents.add(agent);
			if (agent.initializeAndCheckIfAlive()) {
				Floor floor = this.sim2DEngine.getFloor(agent.getCurrentLinkId());
				floor.addAgent((Agent2D) agent);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.ptproject.qsim.interfaces.QSimI#agentDeparts(org.matsim.core
	 * .mobsim.framework.PersonAgent, org.matsim.api.core.v01.Id)
	 */
	@Override
	public void arrangeAgentDeparture(PlanAgent agent) {
		double now = getSimTimer().getTimeOfDay();
		Leg leg = agent.getCurrentLeg();
		// Route route = leg.getRoute();
		String mode = leg.getMode();
		Id linkId = agent.getCurrentLinkId() ;
		this.events.processEvent(this.events.getFactory().createAgentDepartureEvent(now, agent.getId(), linkId, mode));
		if (handleKnownLegModeDeparture(now, agent, linkId, leg)) {
			return;
		} else {
			throw new RuntimeException("not (yet) implemented!");
		}

	}

	/**
	 * @param now
	 * @param agent
	 * @param linkId
	 * @param leg
	 * @return
	 */
	private boolean handleKnownLegModeDeparture(double now, PlanAgent agent, Id linkId, Leg leg) {
		for (DepartureHandler departureHandler : this.departureHandlers) {
			if (departureHandler.handleDeparture(now, agent, linkId, leg)) {
				return true;
			}
			// The code is not (yet?) very
			// beautiful. But structurally, this goes through all departure
			// handlers and tries to
			// find one that feels responsible. If it feels responsible, it
			// returns true, and so this method returns true.
			// Otherwise, this method will return false, and then teleportation
			// will be called. kai, jun'10
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.QSimI#getAgentCounter()
	 */
	@Override
	public AgentCounterI getAgentCounter() {
		return this.agentCounter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.QSimI#getEventsManager()
	 */
	@Override
	public EventsManager getEventsManager() {
		return this.events;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.QSimI#getScenario()
	 */
	@Override
	public Scenario getScenario() {
		return this.scenario;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.QSimI#getSimTimer()
	 */
	@Override
	public SimTimerI getSimTimer() {
		return this.simTimer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.ptproject.qsim.interfaces.QSimI#scheduleActivityEnd(org.matsim
	 * .core.mobsim.framework.PersonAgent)
	 */
	@Override
	public void scheduleActivityEnd(PlanAgent agent) {
		// yy can't make this final since it is overwritten by christoph. kai,
		// oct'10
		this.activityEndsList.add(agent);
		registerAgentAtActivityLocation(agent);

	}

	/**
	 * @param agent
	 */
	private void registerAgentAtActivityLocation(PlanAgent agent) {
		// if the "activities" engine were separate, this would need to be
		// public. kai, aug'10
		if (agent instanceof PersonDriverAgentImpl) { // yyyyyy is this
			// necessary?
			// DefaultPersonDriverAgent pa = (DefaultPersonDriverAgent) agent;
			PlanElement pe = agent.getCurrentPlanElement();
			if (pe instanceof Leg) {
				throw new RuntimeException();
			} else {
				Activity act = (Activity) pe;
				Id linkId = act.getLinkId();
				Floor f = this.sim2DEngine.getFloor(linkId);
				f.addAgent((Agent2D) agent);

			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.ptproject.qsim.interfaces.QSimI#setAgentFactory(org.matsim
	 * .ptproject.qsim.AgentFactory)
	 */
	@Override
	public void setAgentFactory(AgentFactory agentFactory) {
		throw new RuntimeException("not (yet) implemented!");

	}

	// ############################################################################################################################
	// utility methods (presumably no state change)
	// ############################################################################################################################

	private void printSimLog(final double time) {
		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			Date endtime = new Date();
			long diffreal = (endtime.getTime() - this.realWorldStarttime.getTime()) / 1000;
			double diffsim = time - this.simTimer.getSimStartTime();
			log.info("SIMULATION (NEW QSim) AT " + Time.writeTime(time) + " (it." + this.iterationNumber + "): #Veh=" + this.agentCounter.getLiving() + " lost=" + this.agentCounter.getLost() + " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim / (diffreal + Double.MIN_VALUE)));

			Gbl.printMemoryUsage();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.ptproject.qsim.interfaces.QSimI#registerAgentAtPtWaitLocation
	 * (org.matsim.core.mobsim.framework.PersonAgent)
	 */
	@Override
	public void registerAgentAtPtWaitLocation(PlanAgent planAgent) {
		throw new RuntimeException("not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.ptproject.qsim.interfaces.QSimI#unregisterAgentAtPtWaitLocation
	 * (org.matsim.core.mobsim.framework.PersonAgent)
	 */
	@Override
	public void unregisterAgentAtPtWaitLocation(PlanAgent planAgent) {
		throw new RuntimeException("not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.QSimI#getQNetwork()
	 */
	@Override
	public NetsimNetwork getNetsimNetwork() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * 
	 */
	public Sim2DEngine getSim2DEngine() {
		return this.sim2DEngine;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.QSimI#getActivityEndsList()
	 */
	@Override
	public Collection<PlanAgent> getActivityEndsList() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.ptproject.qsim.interfaces.QSimI#rescheduleActivityEnd(org.
	 * matsim.core.mobsim.framework.PersonAgent, double, double)
	 */
	@Override
	public void rescheduleActivityEnd(PersonAgent agent, double oldTime, double newTime) {
		throw new RuntimeException("not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.matsim.core.mobsim.framework.ObservableSimulation#
	 * addQueueSimulationListeners
	 * (org.matsim.core.mobsim.framework.listeners.SimulationListener)
	 */
	@Override
	public void addQueueSimulationListeners(SimulationListener listener) {
		throw new RuntimeException("not implemented");
	}

}
