/* *********************************************************************** *
 * project: org.matsim.*
 * DemoController.java
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

package playground.christoph.oldenburg;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.InitialIdentifierImplFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.NextLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;

public class DemoController extends WithinDayController implements MobsimInitializedListener, StartupListener, 
	MobsimBeforeSimStepListener, IterationEndsListener {

	private static final Logger log = Logger.getLogger(DemoController.class);
	
	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 2;

	protected InitialIdentifier initialIdentifier;
	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayInitialReplanner initialReplanner;
	protected WithinDayDuringActivityReplanner duringActivityReplanner;
	protected WithinDayDuringLegReplanner duringLegReplanner;
	
	protected EvacuationTimeAnalyzer evacuationTimeAnalyzer;
	
	public DemoController(String[] args) {
		super(args);

		init();
	}

	private void init() {
		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
		
		// register this as a Controller and Simulation Listener
		super.getFixedOrderSimulationListener().addSimulationListener(this);
		super.addControlerListener(this);
	}
	
	/*
	 * New Routers for the replanning are used instead of using the controller's.
	 * By doing this every person can use a personalized Router.
	 */
	protected void initReplanners(QSim sim) {

		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) sim.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, costFactory, this.getTravelTimeCollectorFactory(), factory, routeFactory);
		
		this.initialIdentifier = new InitialIdentifierImplFactory(sim).createIdentifier();
		this.initialReplanner = new CreateEvacuationPlanReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.getReplanningManager().addIntialReplanner(this.initialReplanner);
		
		ActivityReplanningMap activityReplanningMap = super.getActivityReplanningMap();
		this.duringActivityIdentifier = new ActivityEndIdentifierFactory(activityReplanningMap).createIdentifier();
		this.duringActivityReplanner = new NextLegReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.getReplanningManager().addDuringActivityReplanner(this.duringActivityReplanner);
		
		LinkReplanningMap linkReplanningMap = super.getLinkReplanningMap();
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.duringLegReplanner = new CurrentLegReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.getReplanningManager().addDuringLegReplanner(this.duringLegReplanner);
	}
	
	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		super.createAndInitReplanningManager(numReplanningThreads);
		super.createAndInitTravelTimeCollector();
		super.createAndInitActivityReplanningMap();
		super.createAndInitLinkReplanningMap();
		
		// initialize the EvacuationTimeAnalyzer
		evacuationTimeAnalyzer = new EvacuationTimeAnalyzer();
		super.getEvents().addHandler(evacuationTimeAnalyzer);
	}
	
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {		
		initReplanners((QSim)e.getQueueSimulation());
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		/*
		 * This is just a workaround:
		 * If the free speed of a link is adapted, also the vehicles that are currently
		 * traveling over this link has to be adapted. We check whether a link has
		 * been affected by a change event that occurred in the previous time step.
		 * Imho this should be performed in the recalcTimeVariantAttributes method
		 * in the NetsimLink class.
		 */
		// Das könnte man so sehen.  Besprichst Du es mal mit den Autoren (Dominik & Gregor)?
		// Aber: Ist die Rechnung denn richtig?  Müsste man nicht schauen, wie weit das Fahrzeug auf der Kante
		// bereits ist?  Ansonsten ersetzt man einfach einen Fehler durch einen anderen, oder?  kai, nov'11
		for (NetworkChangeEvent networkChangeEvent : ((NetworkImpl) this.getNetwork()).getNetworkChangeEvents()) {
			if(networkChangeEvent.getStartTime() == e.getSimulationTime()) {
				for (Link link : networkChangeEvent.getLinks()) {

//					LinkedList<QVehicle> vehicles = ((QSim)e.getQueueSimulation()).getNetsimNetwork().getNetsimLink(link.getId()).getVehQueue();
					// (original)
					
					// replaced by:
					Collection<MobsimVehicle> vehicles = 
						((QSim)e.getQueueSimulation()).getNetsimNetwork().getNetsimLink(link.getId()).getAllNonParkedVehicles() ;
					
					// reason for this change: I always wanted to take getVehQueue out of the public interface since this is imo an
					// implementation detail which should not be exposed.  As a side effect, this now returns MobsimVehicles
					// instead of QVehicles.  As long as everything is plugged together correctly, it should still work ok.
					// kai, nov'11
					
					for (MobsimVehicle mvehicle : vehicles) {
						QVehicle vehicle = (QVehicle) mvehicle ;
						double before = vehicle.getEarliestLinkExitTime();
						vehicle.setEarliestLinkExitTime(e.getSimulationTime() + link.getLength() / link.getFreespeed(e.getSimulationTime()));
						double after = vehicle.getEarliestLinkExitTime();
					}
				}
			}
		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Longest evacuation time: " + Time.writeTime(evacuationTimeAnalyzer.longestEvacuationTime));
		log.info("Mean evacuation time: " + Time.writeTime(evacuationTimeAnalyzer.sumEvacuationTimes / scenarioData.getPopulation().getPersons().size()));
	}
	
	@Override
	protected void runMobSim() {
		
		super.runMobSim();
	}
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {		
			final DemoController controller = new DemoController(new String[]{args[0]});
			
			// do not dump plans, network and facilities and the end
//			controller.setDumpDataAtEnd(false);
			
			// overwrite old files
			controller.setOverwriteFiles(true);

			controller.run();
		}
		System.exit(0);
	}
	
}
