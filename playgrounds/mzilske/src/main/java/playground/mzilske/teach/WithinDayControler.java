///* *********************************************************************** *
// * project: org.matsim.*
// * WithinDayControler.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2008 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.mzilske.teach;
//
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.core.config.Config;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
//import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
//import org.matsim.core.replanning.StrategyManager;
//import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
//import org.matsim.core.router.util.DijkstraFactory;
//import org.matsim.core.router.util.TravelTime;
//import org.matsim.ptproject.qsim.QSim;
//import org.matsim.ptproject.qsim.interfaces.QVehicle;
//import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;
//
//import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
//import playground.christoph.network.MyLinkFactoryImpl;
//import playground.christoph.replanning.MyStrategyManagerConfigLoader;
//import playground.christoph.replanning.TravelTimeCollector;
//import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
//import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;
//import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
//import playground.christoph.withinday.mobsim.KnowledgeWithinDayQSim;
//import playground.christoph.withinday.mobsim.KnowledgeWithinDayQSimFactory;
//import playground.christoph.withinday.mobsim.ReplanningManager;
//import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
//import playground.christoph.withinday.replanning.CurrentLegReplanner;
//import playground.christoph.withinday.replanning.ReplanningIdGenerator;
//import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;
//import playground.christoph.withinday.replanning.identifiers.LeaveLinkIdentifier;
//import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
//import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;
//
///**
// * This Controler should give an Example what is needed to run
// * Simulations with WithinDayReplanning.
// *
// * The Path to a Config File is needed as Argument to run the
// * Simulation.
// *
// * By default "../matsim/test/scenarios/berlin/config.xml" should work.
// *
// * @author Christoph Dobler
// */
//
//public class WithinDayControler extends Controler {
//
//	private int numReplanningThreads = 1;
//
//	private TravelTime travelTime;
//	
//	private ParallelDuringLegReplanner parallelLeaveLinkReplanner;
//	private DuringLegIdentifier duringLegIdentifier;
//	private WithinDayDuringLegReplanner duringLegReplanner;
//	
//	private ReplanningManager replanningManager;
//	private KnowledgeWithinDayQSim sim;
//	private FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();
//
//	private static final Logger log = Logger.getLogger(WithinDayControler.class);
//
//	public WithinDayControler(String[] args)
//	{
//		super(args);
//
//		setConstructorParameters();
//	}
//
//	// only for Batch Runs
//	public WithinDayControler(Config config)
//	{
//		super(config);
//
//		setConstructorParameters();
//	}
//
//	private void setConstructorParameters()
//	{
//		/*
//		 * Use MyLinkImpl. They can carry some additional Information like their
//		 * TravelTime or VehicleCount.
//		 * This is needed to be able to use a LinkVehiclesCounter.
//		 */
//		this.getNetwork().getFactory().setLinkFactory(new MyLinkFactoryImpl());
//
//		// Use a Scoring Function, that only scores the travel times!
//		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
//	}
//
//	@Override
//	protected void runMobSim()
//	{	
//		replanningManager = new ReplanningManager();
//		sim = new KnowledgeWithinDayQSimFactory().createMobsim(this.scenarioData, this.events);
//
//		ReplanningFlagInitializer rfi = new ReplanningFlagInitializer(this);
//		foqsl.addQueueSimulationInitializedListener(rfi);
//		foqsl.addQueueSimulationInitializedListener(replanningManager);
//		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);
//		sim.addQueueSimulationListeners(foqsl);
//
//		log.info("Initialize Parallel Replanning Modules");
//		this.parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(numReplanningThreads, this);
//		
//		log.info("Initialize Replanning Routers");
//		travelTime = new TravelTimeCollector(network);
//		foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
//		foqsl.addQueueSimulationAfterSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
//		this.events.addHandler((TravelTimeCollector)travelTime);	// for TravelTimeCollector
//		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
//		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCost, travelTime, new DijkstraFactory());
//		this.duringLegIdentifier = new LeaveLinkIdentifier(this);
//		this.duringLegReplanner = new CurrentLegReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData, this.getEvents());
//		this.duringLegReplanner.setAbstractMultithreadedModule(router);
//		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
//		this.parallelLeaveLinkReplanner.addWithinDayReplanner(this.duringLegReplanner);
//
//		DuringLegReplanningModule leaveLinkReplanning = new DuringLegReplanningModule(parallelLeaveLinkReplanner);
//
//		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);
//
//		sim.run();
//	}
//
//	/**
//	 * @return A fully initialized StrategyManager for the plans replanning.
//	 */
//	@Override
//	protected StrategyManager loadStrategyManager() {
//		StrategyManager manager = new StrategyManager();
//		MyStrategyManagerConfigLoader.load(this, this.config, manager);
//		return manager;
//	}
//
//
//	public static class ReplanningFlagInitializer implements SimulationInitializedListener{
//
//		private WithinDayControler withinDayControler;
//		private Map<Id, WithinDayPersonAgent> withinDayPersonAgents;
//				
//		public ReplanningFlagInitializer(WithinDayControler controler)
//		{
//			this.withinDayControler = controler;
//		}
//		
//		@Override
//		public void notifySimulationInitialized(SimulationInitializedEvent e)
//		{
//			collectAgents((QSim)e.getQueueSimulation());
//			setReplanningFlags();
//		}
//		
//		protected void setReplanningFlags()
//		{
//			for (WithinDayPersonAgent withinDayPersonAgent : this.withinDayPersonAgents.values())
//			{
//				withinDayPersonAgent.addWithinDayReplanner(withinDayControler.duringLegReplanner);
//				withinDayControler.replanningManager.doLeaveLinkReplanning(true);
//			}
//		}
//		
//		protected void collectAgents(QSim sim)
//		{
//			this.withinDayPersonAgents = new TreeMap<Id, WithinDayPersonAgent>();
//			
//			for (QLinkInternalI queueLink : sim.getQNetwork().getLinks().values())
//			{
//				for (QVehicle queueVehicle : queueLink.getAllVehicles())
//				{
//					WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) queueVehicle.getDriver();
//					this.withinDayPersonAgents.put(withinDayPersonAgent.getPerson().getId(), withinDayPersonAgent);
//				}
//			}
//		}
//		
//	}
//	
//	/*
//	 * ===================================================================
//	 * main
//	 * ===================================================================
//	 */
//	
//	public static void main(final String[] args) {
//		if ((args == null) || (args.length == 0)) {
//			System.out.println("No argument given!");
//			System.out.println("Usage: Controler config-file [dtd-file]");
//			System.out.println();
//		} else {
//			final WithinDayControler controler = new WithinDayControler(args);
//			controler.setOverwriteFiles(true);
//			controler.run();
//		}
//		System.exit(0);
//	}
//
//	public static class FreeSpeedTravelTime implements TravelTime, Cloneable
//	{
//
//		public double getLinkTravelTime(Link link, double time)
//		{
//			return link.getFreespeed(time);
//		}
//		
//		@Override
//		public FreeSpeedTravelTime clone()
//		{
//			return new FreeSpeedTravelTime();
//		}
//	}
//}
