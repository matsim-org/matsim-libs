/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.withinday.controller;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTime;
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorFactory;

/**
 * This controller should give an example what is needed to run
 * simulations with WithinDayReplanning.
 *
 * The path to a config file is needed as argument to run the
 * simulation.
 *
 * By default "test/scenarios/berlin/config.xml" should work.
 *
 * @author Christoph Dobler
 */
public class WithinDayController extends Controler {

	private static final Logger log = Logger.getLogger(WithinDayController.class);
	
	private TravelTimeCollectorFactory travelTimeCollectorFactory = new TravelTimeCollectorFactory();
	private TravelTimeCollector travelTime;
	private ActivityReplanningMap activityReplanningMap;
	private LinkReplanningMap linkReplanningMap;
	
	private boolean replanningManagerInitialized = false;
	private ReplanningManager replanningManager;
	private FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
	
	public WithinDayController(String[] args) {
		super(args);
		
		init();
	}

	public WithinDayController(Config config) {
		super(config);
	
		init();
	}
	
	/*
	 * ===================================================================
	 * Those methods initialize objects that might be typically be used
	 * by within-day replanning code.
	 * ===================================================================
	 */
	public void createAndInitTravelTimeCollector() {
		this.createAndInitTravelTimeCollector(null);
	}
	
	public void createAndInitTravelTimeCollector(Set<String> analyzedModes) {
		if (this.events == null) {
			log.warn("Cannot create and init the TravelTimeCollector. EventsManager has not be initialized yet!");
			return;
		}
		if (travelTime == null) {
			travelTime = travelTimeCollectorFactory.createTravelTimeCollector(this.scenarioData, analyzedModes);
			fosl.addSimulationInitializedListener((TravelTimeCollector)travelTime);
			fosl.addSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);
			fosl.addSimulationAfterSimStepListener((TravelTimeCollector)travelTime);
			this.events.addHandler((TravelTimeCollector)travelTime);
		}
	}
	
	public TravelTimeCollector getTravelTimeCollector() {
		return this.travelTime;
	}
	
	public TravelTimeCollectorFactory getTravelTimeCollectorFactory() {
		return this.travelTimeCollectorFactory;
	}
	
	public void createAndInitActivityReplanningMap() {
		if (this.events == null) {
			log.warn("Cannot create and init the ActivityReplanningMap. EventsManager has not be initialized yet!");
			return;
		}
		if (activityReplanningMap == null) {
			activityReplanningMap = new ActivityReplanningMap();
			this.getEvents().addHandler(activityReplanningMap);
			fosl.addSimulationListener(activityReplanningMap);			
		}
	}
	
	public ActivityReplanningMap getActivityReplanningMap() {
		return this.activityReplanningMap;
	}
	
	public void createAndInitLinkReplanningMap() {
		this.createAndInitLinkReplanningMap(null);
	}
	
	public void createAndInitLinkReplanningMap(MultiModalTravelTime travelTime) {
		if (this.events == null) {
			log.warn("Cannot create and init the LinkReplanningMap. EventsManager has not be initialized yet!");
			return;
		}
		if (linkReplanningMap == null) {
			linkReplanningMap = new LinkReplanningMap(this.network, travelTime);
			this.getEvents().addHandler(linkReplanningMap);
			fosl.addSimulationListener(linkReplanningMap);
		}
	}
	
	
	public LinkReplanningMap getLinkReplanningMap() {
		return this.linkReplanningMap;
	}
	
	/*
	 * TODO: Add a Within-Day Group to the Config. Then this method
	 * can be called on startup.
	 */
	public void initReplanningManager(int numOfThreads) {
		if (!replanningManagerInitialized) {
			log.info("Initialize ReplanningManager");
			replanningManager.initializeReplanningModules(numOfThreads);
			fosl.addSimulationListener(replanningManager);
			replanningManagerInitialized = true;
		}
	}
	
	public ReplanningManager getReplanningManager() {
		return this.replanningManager;
	}
	
	public FixedOrderSimulationListener getFixedOrderSimulationListener() {
		return this.fosl;
	}
	/*
	 * ===================================================================
	 */
	
	private void init() {
		super.getQueueSimulationListener().add(fosl);
		
		this.replanningManager = new ReplanningManager();
	}
	
	@Override
	protected void setUp() {
				
		// set WithinDayQSimFactory
		super.setMobsimFactory(new WithinDayQSimFactory(replanningManager));
		/*
		 * SimStepParallelEventsManagerImpl might be moved to org.matsim.
		 * Then this piece of code could be placed in the controller.
		 */
		if (this.events instanceof ParallelEventsManagerImpl) {
			log.info("Replacing ParallelEventsManagerImpl with SimStepParallelEventsManagerImpl. This is needed for Within-Day Replanning.");
			
			final String PARALLEL_EVENT_HANDLING = "parallelEventHandling";
			final String NUMBER_OF_THREADS = "numberOfThreads";
			String numberOfThreads = this.config.findParam(PARALLEL_EVENT_HANDLING, NUMBER_OF_THREADS);
			
			int numOfThreads = 1;
			if (numberOfThreads != null) {
				numOfThreads = Integer.parseInt(numberOfThreads);
			}
			
			SimStepParallelEventsManagerImpl manager = new SimStepParallelEventsManagerImpl(numOfThreads);
			this.fosl.addSimulationAfterSimStepListener(manager);
			this.events = manager;
		}

		super.setUp();
	}

	@Override
	protected void runMobSim() {
		// ensure that all modules have been initialized
		if (replanningManager == null) {
			log.warn("Within-day replanning modules have not been initialized! Force initialization using 1 replanning thread. " +
					"Please call createAndInitReplanningManager(int numOfThreads).");
			initReplanningManager(1);
		}
		
		super.runMobSim();
	}

}