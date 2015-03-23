/* *********************************************************************** *
 * project: org.matsim.*
 * InitialRoutesControlerListener.java
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

package playground.christoph.controler;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.util.MultiNodeDijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.withinday.controller.ExperiencedPlansWriter;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegStartedIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

public class WithinDayInitialRoutesControlerListener implements StartupListener, IterationStartsListener {

	private DuringLegIdentifierFactory duringLegFactory;
	private DuringLegIdentifierFactory startedLegFactory;
	private DuringLegAgentSelector legPerformingIdentifier;
	private DuringLegAgentSelector legStartedIdentifier;
	
//	private DuringActivityIdentifierFactory activityEndingFactory;
//	private DuringActivityIdentifier activityEndingIdentifier;
	
	private TransportModeFilterFactory carLegAgentsFilterFactory;
	private WithinDayControlerListener withinDayControlerListener;
	private ExperiencedPlansWriter experiencedPlansWriter;

	private boolean initialLegRerouting = true;
	private boolean duringLegRerouting = true;
	
	private double duringLegReroutingShare = 0.10;
		
	public WithinDayInitialRoutesControlerListener() {
		init();
	}

	public void setDuringLegReroutingShare(double share) {
		this.duringLegReroutingShare = share;
	}
	
	public void setDuringLegReroutingEnabled(boolean enabled) {
		this.duringLegRerouting = enabled;
	}
	
	public void setInitialLegReroutingEnabled(boolean enabled) {
		this.initialLegRerouting = enabled;
	}
	
	public WithinDayControlerListener getWithinDayControlerListener() {
		return this.withinDayControlerListener;
	}
	
	private void init() {
		/*
		 * Create a WithinDayControlerListener but do NOT register it as ControlerListener.
		 * It implements the StartupListener interface as this class also does. The
		 * StartupEvent is passed over to it when this class handles the event. 
		 */
		this.withinDayControlerListener = new WithinDayControlerListener();

		// workaround
		this.withinDayControlerListener.setLeastCostPathCalculatorFactory(new MultiNodeDijkstraFactory());
	}


	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		
		/*
		 * The withinDayControlerListener is also a StartupListener. Its notifyStartup(...)
		 * method has to be called first. There, the within-day module is initialized.
		 */
		this.withinDayControlerListener.notifyStartup(event);
		
		this.experiencedPlansWriter = new ExperiencedPlansWriter(this.withinDayControlerListener.getMobsimDataProvider());
		controler.addControlerListener(this.experiencedPlansWriter);
		
		new PrepareInitialRoutes(controler.getScenario()).run();
		
		this.initIdentifiers();
		this.initReplanners(controler.getScenario());
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		/*
		 * Disable Within-Day Replanning after first Iteration.
		 */
		if (event.getIteration() > 0) {
			this.withinDayControlerListener.getWithinDayEngine().doInitialReplanning(false);
			this.withinDayControlerListener.getWithinDayEngine().doDuringLegReplanning(false);
			this.withinDayControlerListener.getWithinDayEngine().doDuringActivityReplanning(false);
			
			event.getControler().getEvents().removeHandler(this.withinDayControlerListener.getTravelTimeCollector());
			this.withinDayControlerListener.getFixedOrderSimulationListener().removeSimulationListener(
					this.withinDayControlerListener.getTravelTimeCollector());
		}
	}
		
	private void initIdentifiers() {
		
		/*
		 * During Leg Identifiers
		 */		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);
		
		if (initialLegRerouting || duringLegRerouting) {
			carLegAgentsFilterFactory = new TransportModeFilterFactory(duringLegRerouteTransportModes,
					this.withinDayControlerListener.getMobsimDataProvider());
		}

		if (duringLegRerouting) {
			duringLegFactory = new LeaveLinkIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap(),
					this.withinDayControlerListener.getMobsimDataProvider());
			duringLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
			this.legPerformingIdentifier = duringLegFactory.createIdentifier();	
			this.legPerformingIdentifier.addAgentFilter(new ProbabilityFilterFactory(this.duringLegReroutingShare).createAgentFilter());
		}
		
		if (initialLegRerouting) {
//			this.activityEndingFactory = new ActivityEndIdentifierFactory(this.withinDayControlerListener.getActivityReplanningMap());
//			this.activityEndingIdentifier = this.activityEndingFactory.createIdentifier();
			this.startedLegFactory = new LegStartedIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap(),
					this.withinDayControlerListener.getMobsimDataProvider());
			this.startedLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
			this.legStartedIdentifier = startedLegFactory.createIdentifier();			
		}
	}
	
	protected void initReplanners(Scenario scenario) {
		
		TravelDisutility travelDisutility = this.withinDayControlerListener.getTravelDisutilityFactory().createTravelDisutility(
				this.withinDayControlerListener.getTravelTimeCollector(), scenario.getConfig().planCalcScore()); 
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, this.withinDayControlerListener.getTravelTimeCollector());
		
		/*
		 * Replanners
		 */
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
//		WithinDayDuringActivityReplannerFactory duringActivityReplannerFactory;
		
		if (duringLegRerouting) {
			duringLegReplannerFactory = new CurrentLegReplannerFactory(scenario, this.withinDayControlerListener.getWithinDayEngine(),
					this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
			duringLegReplannerFactory.addIdentifier(this.legPerformingIdentifier);
			this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
		}
		
		if (initialLegRerouting) {
			duringLegReplannerFactory = new CurrentLegReplannerFactory(scenario, this.withinDayControlerListener.getWithinDayEngine(),
					this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
			duringLegReplannerFactory.addIdentifier(this.legStartedIdentifier);
			this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
//			duringActivityReplannerFactory = new NextLegReplannerFactory(scenario, this.withinDayControlerListener.getWithinDayEngine(), 
//					this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
//			duringActivityReplannerFactory.addIdentifier(this.activityEndingIdentifier);
//			this.withinDayControlerListener.getWithinDayEngine().addDuringActivityReplannerFactory(duringActivityReplannerFactory);
		}
	}
}