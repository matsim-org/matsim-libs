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

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.withinday.mobsim.DuringActivityReplanningModule;
import org.matsim.withinday.mobsim.DuringLegReplanningModule;
import org.matsim.withinday.mobsim.InitialReplanningModule;
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;
import org.matsim.withinday.network.WithinDayLinkFactoryImpl;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import org.matsim.withinday.replanning.parallel.ParallelDuringLegReplanner;
import org.matsim.withinday.replanning.parallel.ParallelInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
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

	private ParallelInitialReplanner parallelInitialReplanner;
	private ParallelDuringActivityReplanner parallelActEndReplanner;
	private ParallelDuringLegReplanner parallelLeaveLinkReplanner;

	private InitialReplanningModule initialReplanningModule;
	private DuringActivityReplanningModule actEndReplanningModule;
	private DuringLegReplanningModule leaveLinkReplanningModule;
	
	private TravelTimeCollector travelTime;
	private ActivityReplanningMap activityReplanningMap;
	private LinkReplanningMap linkReplanningMap;
	
	private ReplanningManager replanningManager;
	private FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();

	private boolean isInitialized = false;
	
	public WithinDayController(String[] args) {
		super(args);
		
		/*
		 * Use MyLinkImpl. They can carry some additional Information like their
		 * TravelTime. This is required by a TravelTimeCollector.
		 */
		this.getNetwork().getFactory().setLinkFactory(new WithinDayLinkFactoryImpl());
	}

	public WithinDayController(Config config) {
		super(config);
	
		/*
		 * Use MyLinkImpl. They can carry some additional Information like their
		 * TravelTime. This is required by a TravelTimeCollector.
		 */
		this.getNetwork().getFactory().setLinkFactory(new WithinDayLinkFactoryImpl());
	}

	public void addIntialReplanner(WithinDayInitialReplanner replanner) {
		this.parallelInitialReplanner.addWithinDayReplanner(replanner);
	}
	
	public void addDuringActivityReplanner(WithinDayDuringActivityReplanner replanner) {		
		this.parallelActEndReplanner.addWithinDayReplanner(replanner);
	}
	
	public void addDuringLegReplanner(WithinDayDuringLegReplanner replanner) {		
		this.parallelLeaveLinkReplanner.addWithinDayReplanner(replanner);
	}
	
	/*
	 * ===================================================================
	 * Those methods initialize objects that might be typically be used
	 * by within-day replanning code.
	 * ===================================================================
	 */
	public void createAndInitTravelTimeCollector() {
		if (this.events == null) {
			log.warn("Cannot create and init the TravelTimeCollector. EventsManager has not be initialized yet!");
			return;
		}
		if (travelTime == null) {
			travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(this.scenarioData);
			fosl.addSimulationInitializedListener((TravelTimeCollector)travelTime);
			fosl.addSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);
			fosl.addSimulationAfterSimStepListener((TravelTimeCollector)travelTime);
			this.events.addHandler((TravelTimeCollector)travelTime);
		}
	}
	
	public TravelTimeCollector getTravelTimeCollector() {
		return this.travelTime;
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
		if (this.events == null) {
			log.warn("Cannot create and init the LinkReplanningMap. EventsManager has not be initialized yet!");
			return;
		}
		if (linkReplanningMap == null) {
			linkReplanningMap = new LinkReplanningMap();
			this.getEvents().addHandler(linkReplanningMap);
			fosl.addSimulationListener(linkReplanningMap);
		}
	}
	
	public LinkReplanningMap getLinkReplanningMap() {
		return this.linkReplanningMap;
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
	
	public void initWithinDayModules(int numReplanningThreads) {
		
		// initialize it only once
		if (isInitialized) return;

		// set WithinDayQSimFactory
		super.setMobsimFactory(new WithinDayQSimFactory());
		super.getQueueSimulationListener().add(fosl);
		
		/*
		 * Crate and initialize a ReplanningManager and a ReplaningFlagInitializer.
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the correct order.
		 */
		log.info("Initialize ReplanningManager");
		replanningManager = new ReplanningManager();
		fosl.addSimulationListener(replanningManager);

		log.info("Initialize Parallel Replanning Modules");
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads);
		this.parallelActEndReplanner = new ParallelDuringActivityReplanner(numReplanningThreads);
		this.parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(numReplanningThreads);
		
		// register them as SimulationListeners
		this.getQueueSimulationListener().add(this.parallelInitialReplanner);
		this.getQueueSimulationListener().add(this.parallelActEndReplanner);
		this.getQueueSimulationListener().add(this.parallelLeaveLinkReplanner);

		log.info("Initialize Replanning Modules");
		initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		actEndReplanningModule = new DuringActivityReplanningModule(parallelActEndReplanner);
		leaveLinkReplanningModule = new DuringLegReplanningModule(parallelLeaveLinkReplanner);

		replanningManager.setInitialReplanningModule(initialReplanningModule);
		replanningManager.setActEndReplanningModule(actEndReplanningModule);
		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanningModule);
	}
	
	@Override
	protected void setUp() {
		/*
		 * SimStepParallelEventsManagerImpl might be moved to org.matsim.
		 * Then this piece of code could be placed in the controller.
		 */
		if (this.events instanceof ParallelEventsManagerImpl) {
			log.info("Replacing ParallelEventsManagerImpl with SimStepParallelEventsManagerImpl. This is needed for Within-Day Replanning.");
			SimStepParallelEventsManagerImpl manager = new SimStepParallelEventsManagerImpl();
			this.fosl.addSimulationAfterSimStepListener(manager);
			this.events = manager;
		}

		super.setUp();
	}

	@Override
	protected void runMobSim() {
		// ensure that all modules have been initialized
		if (!isInitialized) {
			log.warn("Within-day replanning modules have not been initialized! Force initialization using 1 replanning thread.");
			initWithinDayModules(1);
		}
		
		super.runMobSim();
	}

	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final ExampleWithinDayController controller = new ExampleWithinDayController(args);
			controller.run();
		}
		System.exit(0);
	}

}