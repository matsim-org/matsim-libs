/* *********************************************************************** *
 * project: org.matsim.*
 * QSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

package org.mjanowski.master;

import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.core.mobsim.qsim.HasAgentTracker;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngineI;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.mjanowski.MySimConfig;

import javax.inject.Inject;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This has developed over the last couple of months/years towards an increasingly pluggable module.  The current (dec'2011)
 * approach consists of the following elements (and presumably more, developed by mzilske):<ul>
 * <li> QSim itself should have all basic functionality to execute a typical agent plan, i.e. activities and legs.  In this basic
 * version, all legs are teleported.
 * <li> In addition, there are "engines" that plug into QSim.  Those are time-step driven, as is QSim.  Many engines move
 * particles around, i.e. they execute the different modes.  Others are responsible for, e.g., time-variant networks or signals.
 * <li> A special engine is the netsim engine, which is the original "queue"
 * engine.  It is invoked by default, and it carries the "NetsimNetwork" for which there is a getter.
 * <li> Engines that move particles around need to be able to "end legs".
 * This used to be such that control went to the agents, which
 * reinserted themselves into QSim.  This has now been changed: The agents compute their next state, but the engines are
 * responsible for reinsertion into QSim.  For this, they obtain an "internal interface" during engine addition.  Naming
 * conventions will be adapted to this in the future.
 * <li> <i>A caveat is that drivers that move around other agents (such as TransitDriver, TaxicabDriver) need to become
 * "engines".</i>  Possibly, something that executes a leg is not really the same as an "engine", but this is what we have
 * for the time being.
 * <li> Engines that offer new modes also need to be registered as "DepartureHandler"s.
 *  * </ul>
 * Future plans include: pull the agent counter write methods back into QSim (no big deal, I hope); pull the actstart/end,
 * agent departure/arrival back into QSim+engines; somewhat separate the teleportation engine and the activities engine from the
 * framework part of QSim.
 * <p></p>
 * @author dstrippgen
 * @author mrieser
 * @author dgrether
 * @author knagel
 */
public final class MasterSim implements Netsim {

	final private static Logger log = Logger.getLogger(MasterSim.class);

	private final EventsManager events;
	private final MobsimTimer simTimer;
	private final MasterSimListenerManager listenerManager;
	private final Scenario scenario;
	private CountDownLatch iterationEndedLatch;
	private MasterMain masterMain;



	/**
	 * Constructs an instance of this simulation which does not do anything by itself, but accepts handlers for Activities and Legs.
	 * Use this constructor if you want to plug together your very own simulation, i.e. you are writing some of the simulation
	 * logic yourself.
	 *
	 * If you wish to use QSim as a product and run a simulation based on a Config file, rather use QSimFactory as your entry point.
	 *
	 */
	@Inject
	private MasterSim(final Scenario sc, EventsManager events, Injector childInjector ) {
		this.scenario = sc;
		this.events = events;
		this.listenerManager = new MasterSimListenerManager(this);
		this.simTimer = new MobsimTimer( sc.getConfig().qsim().getTimeStepSize());
	}

	@Override
	public AgentCounter getAgentCounter() {
		return null;
	}

	// ============================================================================================================================
	// "run" method:

	public void run() {

		MySimConfig mySimConfig = (MySimConfig) scenario.getConfig().getModules().get("mySimConfig");
		int workersNumber = mySimConfig.getWorkersNumber();
		iterationEndedLatch = new CountDownLatch(workersNumber);
		events.initProcessing();
		masterMain = new MasterMain(mySimConfig, scenario.getNetwork(), this);
		try {
			iterationEndedLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void afterMobsim() {
		iterationEndedLatch.countDown();
	}

	@Override
	public EventsManager getEventsManager() {
		return events;
	}

	@Override
	public NetsimNetwork getNetsimNetwork() {
		return null;
		//todo ???
	}


	@Override
	public Scenario getScenario() {
		return this.scenario;
	}

	@Override
	public MobsimTimer getSimTimer() {
		return this.simTimer;
	}

	@Override
	public void addQueueSimulationListeners(MobsimListener listener) {
		this.listenerManager.addQueueSimulationListener(listener);
	}

	@Inject
	void addQueueSimulationListeners(Set<MobsimListener> listeners) {
		for (MobsimListener listener : listeners) {
			this.listenerManager.addQueueSimulationListener(listener);
		}
	}

	@Override
	public double getStopTime() {
		return 0;
	}
}
