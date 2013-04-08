/* *********************************************************************** *
 * project: org.matsim.*
 * BurgdorfController.java
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

package playground.christoph.burgdorf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.filter.CollectionAgentFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.LinkFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.christoph.burgdorf.withinday.identifiers.ParkingIdentifierFactory;
import playground.christoph.burgdorf.withinday.replanners.ParkingReplannerFactory;

/**
 *  
 * @author cdobler
 */
public class BurgdorfController extends WithinDayController implements StartupListener {

	private static String runId = "SamstagToBurgdorf";
//	private static String runId = "SonntagFromBurgdorf";
	
	private DuringLegIdentifierFactory duringLegFactory;
	private DuringLegIdentifier parkingIdentifier;
	
	private CollectionAgentFilterFactory visitorAgentFilterFactory;
	private TransportModeFilterFactory carLegAgentsFilterFactory;
	private LinkFilterFactory linkFilterFactory;
	
	private double duringLegReroutingShare = 1.00;
	
	private boolean useWithinDayReplanning = false;
//	private boolean useWithinDayReplanning = true;
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: BurgdorfController config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new BurgdorfController(args);
			controler.setOverwriteFiles(true);
			controler.getConfig().controler().setRunId(runId);
			controler.run();
		}
		System.exit(0);
	}
	
	public BurgdorfController(String[] args) {
		super(args);
		
		init();
	}
	
	private void init() {
		
		// register this as a Controller Listener
		super.addControlerListener(this);
	}
		
	@Override
	public void notifyStartup(StartupEvent event) {
						
		if (useWithinDayReplanning) {
			/*
			 * Initialize TravelTimeCollector.
			 */
			Set<String> analyzedModes = new HashSet<String>();
			analyzedModes.add(TransportMode.car);
			super.createAndInitTravelTimeCollector(analyzedModes);
			
			/*
			 * Create and initialize replanning manager and replanning maps.
			 */
			super.createAndInitLinkReplanningMap();			
		}
		/*
		 * Get number of threads from config file and initialize WithinDayEngine.
		 */
		int numReplanningThreads = this.config.global().getNumberOfThreads();
		super.initWithinDayEngine(numReplanningThreads);
	}
	
	@Override
	protected void setUp() {
		/*
		 * The Controler initialized the LeastCostPathCalculatorFactory here, which is required
		 * by the replanners.
		 */
		super.setUp();
				
		// initialize Identifiers and Replanners
		if (useWithinDayReplanning) {
			this.initIdentifiers();
			this.initReplanners();			
		}
	}
	
	private void initIdentifiers() {
		
		/*
		 * During Leg Identifiers
		 */		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);

		Set<Id> visitorAgents = new HashSet<Id>();
		for (Person person : scenarioData.getPopulation().getPersons().values()) {
			if (person.getId().toString().toLowerCase().contains("visitor")) visitorAgents.add(person.getId());
		}
		visitorAgentFilterFactory = new CollectionAgentFilterFactory(visitorAgents);
		
		carLegAgentsFilterFactory = new TransportModeFilterFactory(duringLegRerouteTransportModes);
		this.getMobsimListeners().add(carLegAgentsFilterFactory);			

		Set<Id> parkingDecisionLinks = new HashSet<Id>();
		for (String string : ParkingInfrastructure.parkingDecisionLinks) parkingDecisionLinks.add(scenarioData.createId(string));
		linkFilterFactory = new LinkFilterFactory(parkingDecisionLinks);
		this.getMobsimListeners().add(linkFilterFactory);
		
		duringLegFactory = new ParkingIdentifierFactory(this.getLinkReplanningMap());
		duringLegFactory.addAgentFilterFactory(visitorAgentFilterFactory);
		duringLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
		duringLegFactory.addAgentFilterFactory(linkFilterFactory);
		this.parkingIdentifier = duringLegFactory.createIdentifier();
	}
	
	private void initReplanners() {
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.getPopulation().getFactory()).getModeRouteFactory();
								
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();	
//		travelTimes.put(TransportMode.car, this.getTravelTimeCalculator());	// TravelTimeCalculator?!
		travelTimes.put(TransportMode.car, this.getTravelTimeCollector());
		
		// add time dependent penalties to travel costs within the affected area
		TravelDisutilityFactory disutilityFactory = this.getTravelDisutilityFactory();
		
		LeastCostPathCalculatorFactory factory = this.getLeastCostPathCalculatorFactory();
	
		AbstractMultithreadedModule router = new ReplanningModule(config, network, disutilityFactory, travelTimes, factory, routeFactory);

		/*
		 * During Leg Replanner
		 */
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
		
		duringLegReplannerFactory = new ParkingReplannerFactory(this.scenarioData, this.getWithinDayEngine(), router, duringLegReroutingShare);
		duringLegReplannerFactory.addIdentifier(this.parkingIdentifier);
		this.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
	}
}