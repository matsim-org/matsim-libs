/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.rc.microwdr;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;


public class WithindayListener implements StartupListener, IterationStartsListener {
	
	protected Scenario scenario;
	private Controler controler;
	protected WithinDayControlerListener withinDayControlerListener;
	private static final Logger log = Logger.getLogger(WithindayListener.class);

	public WithindayListener(Controler controler) {		
		this.scenario = controler.getScenario();
		this.withinDayControlerListener = new WithinDayControlerListener();
		this.controler = controler;
		
		// Use a Scoring Function, that only scores the travel times!
		//controler.setScoringFunctionFactory(new OnlyTravelTimeDependentScoringFunctionFactory());
		//controler.setTravelDisutilityFactory(new OnlyTimeDependentTravelDisutilityFactory());
		
		// workaround
		this.withinDayControlerListener.setLeastCostPathCalculatorFactory(new DijkstraFactory());
				
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		log.info("doing within day replanning ...");
		
		this.withinDayControlerListener.notifyStartup(event);
		
		this.initWithinDayReplanning(this.scenario);				
	}
	
	private void initWithinDayReplanning(Scenario scenario) {		
		TravelDisutility travelDisutility = withinDayControlerListener.getTravelDisutilityFactory()
				.createTravelDisutility(withinDayControlerListener.getTravelTimeCollector(), scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, withinDayControlerListener.getTravelTimeCollector());
		
		LeaveLinkIdentifierFactory duringLegIdentifierFactory = new LeaveLinkIdentifierFactory(withinDayControlerListener.getLinkReplanningMap(),
				withinDayControlerListener.getMobsimDataProvider());
				
		StuckAgentsFilterFactory stuckAgentsFilterFactory = new StuckAgentsFilterFactory(withinDayControlerListener, scenario.getNetwork());
		duringLegIdentifierFactory.addAgentFilterFactory(stuckAgentsFilterFactory);
						
		CurrentLegMicroReplannerFactory duringLegReplannerFactory = new CurrentLegMicroReplannerFactory(scenario, withinDayControlerListener.getWithinDayEngine(),
				withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext, this.controler);
		duringLegReplannerFactory.addIdentifier(duringLegIdentifierFactory.createIdentifier());
		
		withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Population pop = event.getControler().getScenario().getPopulation();
		for (Person p : pop.getPersons().values()) {
			((PersonImpl)p).setAge(100);
		}	
	}
}