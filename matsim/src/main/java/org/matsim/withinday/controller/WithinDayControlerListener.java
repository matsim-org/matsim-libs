/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Attempt to realize functionality provided by WithinDayController by
 * a ControlerListener (similar to what is done by the MultiModalControlerListener).
 *
 * Note: this class has to be registered as Controller Listener!
 *
 * @author cdobler
 */
public class WithinDayControlerListener implements StartupListener {

	private static final Logger log = LogManager.getLogger(WithinDayControlerListener.class);

	private int numReplanningThreads = 0;
	@Inject private WithinDayTravelTime withinDayTravelTime;
	@Inject private ActivityReplanningMap activityReplanningMap;
	@Inject private LinkReplanningMap linkReplanningMap;
	@Inject private MobsimDataProvider mobsimDataProvider;
	@Inject private EarliestLinkExitTimeProvider earliestLinkExitTimeProvider;

	@Inject private EventsManager eventsManager;
	@Inject private Scenario scenario;

	@Inject private WithinDayEngine withinDayEngine;
	@Inject private FixedOrderSimulationListener fosl;
	private final Map<String, TravelTime> multiModalTravelTimes = new HashMap<String, TravelTime>();

	public void setNumberOfReplanningThreads(int threads) {
		throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
	}

	public void setTravelDisutilityFactory(TravelDisutilityFactory travelDisutilityFactory) {
		throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
	}

	public void setLeastCostPathCalculatorFactory(LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
	}

	public void setTransitRouterFactory(Provider<TransitRouter> transitRouterFactory) {
		throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
	}

	public void setModesAnalyzedByWithinDayTravelTime(Set<String> modes) {
		throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
	}

	public Map<String, TravelTime> getMultiModalTravelTimes() {
		return Collections.unmodifiableMap(this.multiModalTravelTimes);
	}

	public void addMultiModalTravelTimes(Map<String, TravelTime> multiModalTravelTimes) {
		for (Entry<String, TravelTime> entry : multiModalTravelTimes.entrySet()) {
			this.addMultiModalTravelTime(entry.getKey(), entry.getValue());
		}
	}

	public void addMultiModalTravelTime(String mode, TravelTime travelTime) {
		throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
	}

	public WithinDayTravelTime getWithinDayTravelTime() {
		return this.withinDayTravelTime;
	}

	public EarliestLinkExitTimeProvider getEarliestLinkExitTimeProvider() {
		return this.earliestLinkExitTimeProvider;
	}

	public void setWithinDayTripRouterFactory(Provider<TripRouter> tripRouterFactory) {
		throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
	}

	public FixedOrderSimulationListener getFixedOrderSimulationListener() {
		return this.fosl;
	}
	/*
	 * ===================================================================
	 */

	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		this.initWithinDayEngine(this.numReplanningThreads);
		this.createAndInitMobsimDataProvider();
		this.createAndInitActivityReplanningMap();
		this.createAndInitLinkReplanningMap();
	}

	/*
	 * ===================================================================
	 * creation and initialization methods
	 * ===================================================================
	 */

	/*
	 * TODO: Add a Within-Day Group to the Config. Then this method
	 * can be called on startup.
	 */
	private void initWithinDayEngine(int numOfThreads) {
		log.info("Initialize WithinDayEngine");
	}

	private void createAndInitMobsimDataProvider() {
		this.getFixedOrderSimulationListener().addSimulationListener(this.mobsimDataProvider);
	}

	private void createAndInitActivityReplanningMap() {
		fosl.addSimulationListener(activityReplanningMap);
	}

	private void createAndInitLinkReplanningMap() {
		this.fosl.addSimulationListener(linkReplanningMap);
	}

}
