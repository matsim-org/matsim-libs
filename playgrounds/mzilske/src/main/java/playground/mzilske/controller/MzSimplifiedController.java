/* *********************************************************************** *
 * project: kai
 * GautengOwnController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mzilske.controller;


import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.old.ModularPlanRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PseudoTransitLegRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;

/**
 * @author nagel
 *
 */
public class MzSimplifiedController extends AbstractController {

	public static Logger log = Logger.getLogger(MzSimplifiedController.class);

	private Config config;

	private final Scenario scenarioData;	

	private EventsManager eventsManager;

	private Network network  ;
	private Population population  ;

	private CalcLegTimes legTimes;

	private TravelTimeCalculator travelTime;

	// ############################################################################################################################
	// ############################################################################################################################
	//	stuff that is related to the control flow	


	
	public MzSimplifiedController(Scenario sc) {
		this.scenarioData = sc;
	}

	public void run() {
		this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		checkConfigConsistencyAndWriteToLog(this.config, "Complete config dump after reading the config file:");
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
		this.eventsManager = EventsUtils.createEventsManager(config); 
		// add a couple of useful event handlers:
		this.eventsManager.addHandler(new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network));
		this.legTimes = new CalcLegTimes();
		this.eventsManager.addHandler(legTimes);
		this.travelTime = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		this.eventsManager.addHandler(travelTime);
		super.run(config);
	}

	/**
	 * The order how the listeners are added may be important! As
	 * dependencies between different listeners exist or listeners may read
	 * and write to common variables, the order is important. 
	 * <br/> 
	 * The example given in the old Controler was: The
	 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
	 * turn is used by the PlansScoring-Listener. I would argue that such dependencies are not necessary with the
	 * code as designed her: One could first define the scoring function completely, and then add it where needed. kai, jun'12
	 * <br/>
	 * IMPORTANT: The execution order is reverse to the order the listeners
	 * are added to the list.
	 */
	@Override
	protected void loadCoreListeners() {

		final DumpDataAtEnd dumpDataAtEnd = new DumpDataAtEnd(scenarioData, getControlerIO());
		this.addControlerListener(dumpDataAtEnd);
		
		final PlansScoring plansScoring = buildPlansScoring();
		this.addControlerListener(plansScoring);

		final StrategyManager strategyManager = buildStrategyManager() ;
		this.addCoreControlerListener(new PlansReplanning( strategyManager, this.population ));

		final PlansDumping plansDumping = new PlansDumping( this.scenarioData, this.config.controler().getFirstIteration(), 
				this.config.controler().getWritePlansInterval(), stopwatch, getControlerIO() );
		this.addCoreControlerListener(plansDumping);

		this.addCoreControlerListener(new LegTimesListener(legTimes, getControlerIO()));
		final EventsHandling eventsHandling = new EventsHandling((EventsManagerImpl) eventsManager,
				this.config.controler().getWriteEventsInterval(), this.config.controler().getEventsFileFormats(),
				getControlerIO() );
		this.addCoreControlerListener(eventsHandling); 
		// must be last being added (=first being executed)
	}
	private PlansScoring buildPlansScoring() {
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory( this.config.planCalcScore(), this.network );
		final PlansScoring plansScoring = new PlansScoring( this.scenarioData, this.eventsManager, getControlerIO(), scoringFunctionFactory );
		return plansScoring;
	}

	private StrategyManager buildStrategyManager() {
		StrategyManager strategyManager = new StrategyManager() ;
		{
			strategyManager.setPlanSelectorForRemoval( new WorstPlanForRemovalSelector() ) ;
		}
		{
			PlanStrategy strategy = new PlanStrategyImpl( new ExpBetaPlanChanger(this.config.planCalcScore().getBrainExpBeta()) ) ;
			strategyManager.addStrategyForDefaultSubpopulation(strategy, 0.9) ;
		}
		{
			PlanStrategyImpl strategy = new PlanStrategyImpl( new ExpBetaPlanSelector(this.config.planCalcScore())) ;
			strategy.addStrategyModule( new AbstractMultithreadedModule(this.scenarioData.getConfig().global().getNumberOfThreads()) {

				@Override
				public PlanAlgorithm getPlanAlgoInstance() {
					return createRoutingAlgorithm();
				}
				
			}) ;
			strategyManager.addStrategyForDefaultSubpopulation(strategy, 0.1) ;
		}
		return strategyManager ;
	}


	private ModularPlanRouter createRoutingAlgorithm() {
		// factory to generate routes:
		final ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();


		// travel disutility (generalized cost)
		final TravelDisutility travelDisutility = new TravelTimeAndDistanceBasedTravelDisutility(travelTime.getLinkTravelTimes(), config.planCalcScore());
		
		final FreespeedTravelTimeAndDisutility ptTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);

		// define the factory for the "computer science" router.  Needs to be a factory because it might be used multiple
		// times (e.g. for car router, pt router, ...)
		final LeastCostPathCalculatorFactory leastCostPathFactory = new DijkstraFactory();

		// plug it together
		final ModularPlanRouter plansCalcRoute = new ModularPlanRouter();
		
		Collection<String> networkModes = this.config.plansCalcRoute().getNetworkModes();
		for (String mode : networkModes) {
			plansCalcRoute.addLegHandler(mode, new NetworkLegRouter(this.network, leastCostPathFactory.createPathCalculator(network, travelDisutility, travelTime.getLinkTravelTimes()), routeFactory));
		}
		Map<String, Double> teleportedModeSpeeds = this.config.plansCalcRoute().getTeleportedModeSpeeds();
		for (Entry<String, Double> entry : teleportedModeSpeeds.entrySet()) {
			plansCalcRoute.addLegHandler(entry.getKey(), new TeleportationLegRouter(routeFactory, entry.getValue(), this.config.plansCalcRoute().getBeelineDistanceFactor()));
		}
		Map<String, Double> teleportedModeFreespeedFactors = this.config.plansCalcRoute().getTeleportedModeFreespeedFactors();
		for (Entry<String, Double> entry : teleportedModeFreespeedFactors.entrySet()) {
			plansCalcRoute.addLegHandler(entry.getKey(), new PseudoTransitLegRouter(this.network, leastCostPathFactory.createPathCalculator(network, ptTimeCostCalc, ptTimeCostCalc), entry.getValue(), this.config.plansCalcRoute().getBeelineDistanceFactor(), routeFactory));
		}
		
		// return it:
		return plansCalcRoute;
	}
	
	@Override
	protected void prepareForSim() {
		checkConfigConsistencyAndWriteToLog(this.config, "Config dump before doIterations:");
		ParallelPersonAlgorithmRunner.run(this.population, this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(createRoutingAlgorithm(), MzSimplifiedController.this.scenarioData);
			}
		});
	}
	
	@Override
	protected void runMobSim(int iteration) {
		QSim simulation = new QSim( this.scenarioData, this.eventsManager ) ;
		if (config.controler().getWriteSnapshotsInterval() != 0 && iteration % config.controler().getWriteSnapshotsInterval() == 0) {
			// yyyy would be nice to have the following encapsulated in some way:
			// === begin ===
			SnapshotWriterManager manager = new SnapshotWriterManager(config);
			SnapshotWriterFactory snapshotWriterFactory = new OTFFileWriterFactory() ;
			String baseFileName = snapshotWriterFactory.getPreferredBaseFilename();
			String fileName = getControlerIO().getIterationFilename(iteration, baseFileName);
			SnapshotWriter snapshotWriter = snapshotWriterFactory.createSnapshotWriter(fileName, this.scenarioData);
			manager.addSnapshotWriter(snapshotWriter);
			// === end ===
			simulation.addQueueSimulationListeners(manager);
		}
		Mobsim sim = simulation;
		sim.run();
	}
	@Override
	protected boolean continueIterations(int iteration) {
		return ( iteration <= config.controler().getLastIteration() ) ;
	}

}
