/* *********************************************************************** *
 * project: org.matsim.*
 * DemoRunner.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.InitialIdentifierImplFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.NextLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

import java.util.Collection;

public class DemoRunner implements MobsimInitializedListener, StartupListener, 
	MobsimBeforeSimStepListener, IterationEndsListener {

	private static final Logger log = Logger.getLogger(DemoRunner.class);
	
	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 1;

	protected InitialIdentifier initialIdentifier;
	protected DuringActivityAgentSelector duringActivityIdentifier;
	protected DuringLegAgentSelector duringLegIdentifier;
	protected WithinDayInitialReplannerFactory initialReplannerFactory;
	protected WithinDayDuringActivityReplannerFactory duringActivityReplannerFactory;
	protected WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
	
	protected Scenario scenario;
	protected WithinDayControlerListener withinDayControlerListener;
	protected EvacuationTimeAnalyzer evacuationTimeAnalyzer;
	
	public DemoRunner(Controler controler) {
		
		this.scenario = controler.getScenario();
		
		this.withinDayControlerListener = new WithinDayControlerListener();
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this);

		// let Agents adapt their original plans
//		ExperimentalBasicWithindayAgent.copySelectedPlan = false;
		Logger.getLogger(this.getClass()).fatal("copySelectedPlan no longer possible. kai, feb'14") ;
		System.exit(-1); 

		
		// Use a Scoring Function, that only scores the travel times!
		controler.setScoringFunctionFactory(new OnlyTravelTimeDependentScoringFunctionFactory());
	}
	
	/*
	 * New Routers for the replanning are used instead of using the controller's.
	 * By doing this every person can use a personalized Router.
	 */
	protected void initReplanners() {

		TravelDisutility travelDisutility = this.withinDayControlerListener.getTravelDisutilityFactory().createTravelDisutility(
				this.withinDayControlerListener.getTravelTimeCollector(), this.scenario.getConfig().planCalcScore()); 
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, this.withinDayControlerListener.getTravelTimeCollector());

		this.initialIdentifier = new InitialIdentifierImplFactory(this.withinDayControlerListener.getMobsimDataProvider()).createIdentifier();
		this.initialReplannerFactory = new CreateEvacuationPlanReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(),
				this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
		this.initialReplannerFactory.addIdentifier(this.initialIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addIntialReplannerFactory(this.initialReplannerFactory);
		
		ActivityReplanningMap activityReplanningMap = this.withinDayControlerListener.getActivityReplanningMap();
		this.duringActivityIdentifier = new ActivityEndIdentifierFactory(activityReplanningMap).createIdentifier();
		this.duringActivityReplannerFactory = new NextLegReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(),
				this.withinDayControlerListener.getWithinDayTripRouterFactory());
		this.duringActivityReplannerFactory.addIdentifier(this.duringActivityIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addDuringActivityReplannerFactory(this.duringActivityReplannerFactory);
		
		LinkReplanningMap linkReplanningMap = this.withinDayControlerListener.getLinkReplanningMap();
		MobsimDataProvider mobsimDataProvider = this.withinDayControlerListener.getMobsimDataProvider();
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap, mobsimDataProvider).createIdentifier();
		this.duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(),
				this.withinDayControlerListener.getWithinDayTripRouterFactory());
		this.duringLegReplannerFactory.addIdentifier(this.duringLegIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(this.duringLegReplannerFactory);
	}
	
	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		
		this.withinDayControlerListener.notifyStartup(event);
				
		// initialize the EvacuationTimeAnalyzer
		evacuationTimeAnalyzer = new EvacuationTimeAnalyzer();
		event.getControler().getEvents().addHandler(evacuationTimeAnalyzer);
		
		this.initReplanners();
	}
	
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		
		VehiclesFactory vehiclesFactory = VehicleUtils.getFactory();
		VehicleType vehicleType = VehicleUtils.getDefaultVehicleType();
		QSim qsim = (QSim) e.getQueueSimulation();
		for (MobsimAgent agent : qsim.getAgents()) {
			Id agentId = agent.getId();
			Id linkId = agent.getCurrentLinkId();
			
			qsim.createAndParkVehicleOnLink(vehiclesFactory.createVehicle(agentId, vehicleType), linkId);			
		}
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
		for (NetworkChangeEvent networkChangeEvent : ((NetworkImpl) scenario.getNetwork()).getNetworkChangeEvents()) {
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
//						double before = vehicle.getEarliestLinkExitTime();
						vehicle.setEarliestLinkExitTime(e.getSimulationTime() + link.getLength() / link.getFreespeed(e.getSimulationTime()));
//						double after = vehicle.getEarliestLinkExitTime();
					}
				}
			}
		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Longest evacuation time: " + Time.writeTime(evacuationTimeAnalyzer.longestEvacuationTime));
		log.info("Mean evacuation time: " + Time.writeTime(evacuationTimeAnalyzer.sumEvacuationTimes / scenario.getPopulation().getPersons().size()));
	}
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {		
			final Controler controler = new Controler(args[0]);
			
			DemoRunner demoController = new DemoRunner(controler);
			controler.addControlerListener(demoController);
			
			// do not dump plans, network and facilities and the end
//			controller.setDumpDataAtEnd(false);
			
			// overwrite old files
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

			controler.run();
		}
		System.exit(0);
	}
	
}
