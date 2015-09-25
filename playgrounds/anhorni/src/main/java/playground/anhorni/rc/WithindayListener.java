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

package playground.anhorni.rc;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;

import java.util.Set;

public class WithindayListener implements StartupListener {
	
	protected Scenario scenario;
	protected WithinDayControlerListener withinDayControlerListener;
	private Set<Id<Link>> links;
	private static final Logger log = Logger.getLogger(WithindayListener.class);

	public WithindayListener(Controler controler, Set<Id<Link>> links) {
		this.links = links;
		
		this.scenario = controler.getScenario();
		this.withinDayControlerListener = new WithinDayControlerListener();
		
		// Use a Scoring Function, that only scores the travel times!
		controler.setScoringFunctionFactory(new OnlyTravelTimeDependentScoringFunctionFactory());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(new OnlyTimeDependentTravelDisutilityFactory());
			}
		});

		// workaround
		this.withinDayControlerListener.setLeastCostPathCalculatorFactory(new DijkstraFactory());
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		log.info("doing within day replanning ...");
		
		this.addNetworkChange(event.getControler(), links);
		
		this.withinDayControlerListener.notifyStartup(event);
		
		this.initWithinDayReplanning(this.scenario);				
	}
	
	private void initWithinDayReplanning(Scenario scenario) {		
		TravelDisutility travelDisutility = withinDayControlerListener.getTravelDisutilityFactory()
				.createTravelDisutility(withinDayControlerListener.getTravelTimeCollector(), scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, withinDayControlerListener.getTravelTimeCollector());
		
		LeaveLinkIdentifierFactory duringLegIdentifierFactory = new LeaveLinkIdentifierFactory(withinDayControlerListener.getLinkReplanningMap(),
				withinDayControlerListener.getMobsimDataProvider());
				
		TunnelLinksFilterFactory linkFilterFactory = new TunnelLinksFilterFactory(links, withinDayControlerListener.getMobsimDataProvider(), scenario.getNetwork());
		duringLegIdentifierFactory.addAgentFilterFactory(linkFilterFactory);
				
		CurrentLegReplannerFactory duringLegReplannerFactory = new CurrentLegReplannerFactory(scenario, withinDayControlerListener.getWithinDayEngine(),
				withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
		duringLegReplannerFactory.addIdentifier(duringLegIdentifierFactory.createIdentifier());
		
		//withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
		withinDayControlerListener.getWithinDayEngine().addTimedDuringLegReplannerFactory(duringLegReplannerFactory, 15.5*3600.0, Double.MAX_VALUE);
	}
	
	public void addNetworkChange(Controler controler, Set<Id<Link>> links) {
        NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();
		NetworkChangeEvent networkChangeEvent0;
		networkChangeEvent0 = network.getFactory().createNetworkChangeEvent(15.49 * 3600.0);		
		
		for (Id<Link> id : links) {
			Link link = network.getLinks().get(id);
			networkChangeEvent0.addLink(link);
		}		
		networkChangeEvent0.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 0.0));
		networkChangeEvent0.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 0.0));
		network.addNetworkChangeEvent(networkChangeEvent0);
	}

}