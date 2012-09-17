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

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.mobsim.WithinDayEngine;
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
	private TravelTimeCollector travelTimeCollector;
	private ActivityReplanningMap activityReplanningMap;
	private LinkReplanningMap linkReplanningMap;

	private boolean withinDayEngineInitialized = false;
	private WithinDayEngine withinDayEngine;
	private FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();

	public WithinDayController(String[] args) {
		super(args);

		init();
	}

	public WithinDayController(Config config) {
		super(config);

		init();
	}

	public WithinDayController(Scenario scenario) {
		super(scenario);

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
		if (travelTimeCollector == null) {
			travelTimeCollector = travelTimeCollectorFactory.createTravelTimeCollector(this.scenarioData, analyzedModes);
			fosl.addSimulationInitializedListener(travelTimeCollector);
			fosl.addSimulationBeforeSimStepListener(travelTimeCollector);
			fosl.addSimulationAfterSimStepListener(travelTimeCollector);
			this.events.addHandler(travelTimeCollector);
		}
	}

	public TravelTimeCollector getTravelTimeCollector() {
		return this.travelTimeCollector;
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
	
	public void createAndInitLinkReplanningMap(Map<String, TravelTime> travelTime) {
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
	public void initWithinDayEngine(int numOfThreads) {
		if (!withinDayEngineInitialized) {
			log.info("Initialize ReplanningManager");
			withinDayEngine.initializeReplanningModules(numOfThreads);
			withinDayEngineInitialized = true;
		}
	}

	public WithinDayEngine getWithinDayEngine() {
		return this.withinDayEngine;
	}

	public FixedOrderSimulationListener getFixedOrderSimulationListener() {
		return this.fosl;
	}
	/*
	 * ===================================================================
	 */

	private void init() {
		super.getQueueSimulationListener().add(fosl);
		
		this.withinDayEngine = new WithinDayEngine();
	}

	@Override
	protected void setUp() {

		// set WithinDayQSimFactory
		super.setMobsimFactory(new WithinDayQSimFactory(withinDayEngine));

		super.setUp();
	}

	@Override
	protected void runMobSim() {
		// ensure that all modules have been initialized
		if (withinDayEngine == null) {
			log.warn("Within-day replanning modules have not been initialized! Force initialization using 1 replanning thread. " +
					"Please call createAndInitReplanningManager(int numOfThreads).");
			initWithinDayEngine(1);
		}

		super.runMobSim();
	}

}