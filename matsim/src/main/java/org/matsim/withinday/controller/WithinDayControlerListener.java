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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;
import org.matsim.withinday.trafficmonitoring.TransportModeProvider;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

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

	private static final Logger log = Logger.getLogger(WithinDayControlerListener.class);
	
	private boolean locked = false;
	
	private int numReplanningThreads = 0;
	private TravelTimeCollector travelTimeCollector;
	private Set<String> travelTimeCollectorModes = null;
	private ActivityReplanningMap activityReplanningMap;
	private LinkReplanningMap linkReplanningMap;
	private MobsimDataProvider mobsimDataProvider;
	private TransportModeProvider transportModeProvider;
	private EarliestLinkExitTimeProvider earliestLinkExitTimeProvider;

	private EventsManager eventsManager;
	private Scenario scenario;
	private TravelDisutilityFactory travelDisutilityFactory;
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	private TransitRouterFactory transitRouterFactory;

	private WithinDayEngine withinDayEngine;
	private TripRouterFactory withinDayTripRouterFactory;
	private final FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
	private final Map<String, TravelTime> multiModalTravelTimes = new HashMap<String, TravelTime>();

	/*
	 * ===================================================================
	 * getters and setters
	 * ===================================================================
	 */	
	public int getNumberOfReplanningThreads() {
		return this.numReplanningThreads;
	}
	
	public void setNumberOfReplanningThreads(int threads) {
		if (locked) throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
		this.numReplanningThreads = threads;
	}
	
	public TravelDisutilityFactory getTravelDisutilityFactory() {
		return this.travelDisutilityFactory;
	}

	public void setTravelDisutilityFactory(TravelDisutilityFactory travelDisutilityFactory) {
		if (locked) throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
		this.travelDisutilityFactory = travelDisutilityFactory;
	}

	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return leastCostPathCalculatorFactory;
	}

	public void setLeastCostPathCalculatorFactory(LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		if (locked) throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
	}

	public TransitRouterFactory getTransitRouterFactory() {
		return transitRouterFactory;
	}

	public void setTransitRouterFactory(TransitRouterFactory transitRouterFactory) {
		if (locked) throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
		this.transitRouterFactory = transitRouterFactory;
	}
	
	public void setModesAnalyzedByTravelTimeCollector(Set<String> modes) {
		if (locked) throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
		this.travelTimeCollectorModes = modes;
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
		if (locked) throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
		if (this.multiModalTravelTimes.put(mode, travelTime) != null) {
			log.info("A previously used TravelTime object for mode " + mode + " was replaced.");
		}
	}
	
	public Set<String> getModesAnalyzedByTravelTimeCollector() {
		return Collections.unmodifiableSet(this.travelTimeCollectorModes);
	}

	public TravelTimeCollector getTravelTimeCollector() {
		return this.travelTimeCollector;
	}

	public ActivityReplanningMap getActivityReplanningMap() {
		return this.activityReplanningMap;
	}

	public LinkReplanningMap getLinkReplanningMap() {
		return this.linkReplanningMap;
	}
	
	public WithinDayEngine getWithinDayEngine() {
		return this.withinDayEngine;
	}
	
	public TransportModeProvider getTransportModeProvider() {
		return this.transportModeProvider;
	}
	
	public EarliestLinkExitTimeProvider getEarliestLinkExitTimeProvider() {
		return this.earliestLinkExitTimeProvider;
	}
	
	public MobsimDataProvider getMobsimDataProvider() {
		return this.mobsimDataProvider;
	}
	
	public void setWithinDayTripRouterFactory(TripRouterFactory tripRouterFactory) {
		if (locked) throw new RuntimeException(this.getClass().toString() + " configuration has already been locked!");
		this.withinDayTripRouterFactory = tripRouterFactory;
	}

	public TripRouterFactory getWithinDayTripRouterFactory() {
		return this.withinDayTripRouterFactory;
	}
	
	/**
	 * Uses travel times from the travel time collector for car trips.
	 */
	public TripRouter getTripRouterInstance() {
		TravelDisutility travelDisutility = this.travelDisutilityFactory.createTravelDisutility(this.getTravelTimeCollector(), 
				this.scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, this.getTravelTimeCollector());
		return this.withinDayTripRouterFactory.instantiateAndConfigureTripRouter(routingContext);
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
		
		Controler controler = event.getControler();
		controler.getMobsimListeners().add(fosl);
		
		this.scenario = controler.getScenario();
		this.eventsManager = controler.getEvents();
				
		this.withinDayEngine = controler.getInjector().getInstance(WithinDayEngine.class);

		if (this.numReplanningThreads == 0) {
			this.numReplanningThreads = controler.getConfig().global().getNumberOfThreads();
		} else log.info("Number of replanning threads has already been set - it is NOT overwritten!");
				
		this.initWithinDayEngine(this.numReplanningThreads);
		this.createAndInitEarliestLinkExitTimeProvider();
		this.createAndInitMobsimDataProvider();
		this.createAndInitTravelTimeCollector();
		this.createAndInitActivityReplanningMap();
		this.createAndInitLinkReplanningMap();
		
		if (this.leastCostPathCalculatorFactory == null) {
			// at this point, the LeastCostPathCalculatorFactory is not set in the controler
//			this.leastCostPathCalculatorFactory = controler.getLeastCostPathCalculatorFactory();
			
			// the controler set its LeastCostPathCalculatorFactory like this 
			this.leastCostPathCalculatorFactory = new TripRouterFactoryBuilderWithDefaults().
					createDefaultLeastCostPathCalculatorFactory(controler.getScenario());
		} else log.info("LeastCostPathCalculatorFactory has already been set - it is NOT overwritten!");
		
		if (this.travelDisutilityFactory == null) {
			this.travelDisutilityFactory = controler.getTravelDisutilityFactory();
		} else log.info("TravelDisutilityFactory has already been set - it is NOT overwritten!");
		
		if (scenario.getConfig().scenario().isUseTransit()) {
			if (this.transitRouterFactory == null) {
				this.transitRouterFactory = controler.getTransitRouterFactory();
			} else log.info("TransitRouterFactory has already been set - it is NOT overwritten!");			
		}
		
		if (this.withinDayTripRouterFactory == null) {
			this.initWithinDayTripRouterFactory();
		} else log.info("WithinDayTripRouterFactory has already been set - it is NOT re-initialized!");
		
		// disable possibility to set factories
		this.locked = true;
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
		withinDayEngine.initializeReplanningModules(numOfThreads);
	}

	private void initWithinDayTripRouterFactory() {
		TripRouterFactoryBuilderWithDefaults tripRouterFactoryBuilder = new TripRouterFactoryBuilderWithDefaults();
		this.withinDayTripRouterFactory = tripRouterFactoryBuilder.build(this.scenario);
	}	
	
	private void createAndInitTravelTimeCollector() {
		this.createAndInitTravelTimeCollector(this.travelTimeCollectorModes);
	}

	private void createAndInitTravelTimeCollector(Set<String> analyzedModes) {
		travelTimeCollector = new TravelTimeCollector(this.scenario, analyzedModes);
		fosl.addSimulationListener(travelTimeCollector);
		this.eventsManager.addHandler(travelTimeCollector);
	}
		
	private void createAndInitEarliestLinkExitTimeProvider() {
		if (this.multiModalTravelTimes.size() == 0) {
			this.earliestLinkExitTimeProvider = new EarliestLinkExitTimeProvider(this.scenario);
		} else {
			/*
			 * Use TravelTime objects from the multi-modal map but replace car travel time
			 * (which is a TravelTimeCollector) with a FreeSpeedTravelTime object.
			 */
			Map<String, TravelTime> earliestLinkExitTravelTimes = new HashMap<String, TravelTime>();
			earliestLinkExitTravelTimes.putAll(this.multiModalTravelTimes);
			earliestLinkExitTravelTimes.put(TransportMode.car, new FreeSpeedTravelTime());
			this.earliestLinkExitTimeProvider = new EarliestLinkExitTimeProvider(this.scenario, earliestLinkExitTravelTimes);
		}
		this.eventsManager.addHandler(this.earliestLinkExitTimeProvider);
		this.transportModeProvider = this.earliestLinkExitTimeProvider.getTransportModeProvider();
	}
	
	private void createAndInitMobsimDataProvider() {
		this.mobsimDataProvider = new MobsimDataProvider();
		this.getFixedOrderSimulationListener().addSimulationListener(this.mobsimDataProvider);
	}
	
	private void createAndInitActivityReplanningMap() {
		activityReplanningMap = new ActivityReplanningMap(this.mobsimDataProvider);
		this.eventsManager.addHandler(activityReplanningMap);
		fosl.addSimulationListener(activityReplanningMap);
	}
	
	private void createAndInitLinkReplanningMap() {
		this.linkReplanningMap = new LinkReplanningMap(this.earliestLinkExitTimeProvider);
		this.eventsManager.addHandler(linkReplanningMap);
		this.fosl.addSimulationListener(linkReplanningMap);
	}

}