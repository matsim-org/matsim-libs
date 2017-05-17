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

package playground.kai.run;


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
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * @author nagel
 *
 */
public class KnSimplifiedController extends AbstractController {

	public static Logger log = Logger.getLogger(KnSimplifiedController.class);

	private Config config;

	private final Scenario scenario;	

	private EventsManager events;

	private Network network  ;
	private Population population  ;

	private CalcLegTimes legTimes;

	private TravelTimeCalculator travelTimeCalculator;

	public KnSimplifiedController(Scenario sc) {
		this.scenario = sc;
		this.config = sc.getConfig();
	}

	public void run() {
		// yy move config reading into the c'tor.  Lock config there.  Force everybody who wants to modify the config to
		// do this by loading it before the controler.
		// yy move events into the c'tor.  ... Similarly.
		// (Yet with this simplified controler you cannot get at the scenario or the events between ctor and run,
		// so this is really of less importance here.  kai, sep'12)
		this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		ControlerUtils.checkConfigConsistencyAndWriteToLog(this.config, "Complete config dump after reading the config file:");
		this.setupOutputDirectory(config.controler().getOutputDirectory(), config.controler().getRunId(), OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		this.network = this.scenario.getNetwork();
		this.population = this.scenario.getPopulation();
		this.events = EventsUtils.createEventsManager(config); 
		// add a couple of useful event handlers:
		this.events.addHandler(new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network));
		this.legTimes = new CalcLegTimes();
		this.events.addHandler(legTimes);
		this.travelTimeCalculator = TravelTimeCalculator.create(this.network, this.config.travelTimeCalculator());
		this.events.addHandler(travelTimeCalculator);	
		super.run(config);
	}

	/**
	 * The order how the listeners are added may be important! As
	 * dependencies between different listeners exist or listeners may read
	 * and write to common variables, the order is important. 
	 * <br>
	 * The example given in the old Controler was: The
	 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
	 * turn is used by the PlansScoring-Listener. I would argue that such dependencies are not necessary with the
	 * code as designed her: One could first define the scoring function completely, and then add it where needed. kai, jun'12
	 * <br>
	 * IMPORTANT: The execution order is reverse to the order the listeners
	 * are added to the list.
	 */
	@Override
	protected void loadCoreListeners() {

//		final DumpDataAtEnd dumpDataAtEnd = new DumpDataAtEndImpl(scenario, getControlerIO());
//		this.addControlerListener(dumpDataAtEnd);
		
		final PlansScoring plansScoring = createPlansScoring();
		this.addControlerListener(plansScoring);

		final StrategyManager strategyManager = createStrategyManager() ;
//		this.addCoreControlerListener(new PlansReplanningImpl( strategyManager, this.population ));

//		final PlansDumping plansDumping = new PlansDumpingImpl( this.scenario, this.config.controler().getFirstIteration(),
//				this.config.controler().getWritePlansInterval(), stopwatch, getControlerIO() );
//		this.addCoreControlerListener(plansDumping);

//		this.addCoreControlerListener(new LegTimesControlerListener(legTimes, getControlerIO()));
		
//		final EventsHandling eventsHandling = new EventsHandlingImpl(events,
//				this.config.services().getWriteEventsInterval(), this.config.services().getEventsFileFormats(),
//				getControlerIO() );
//		this.addCoreControlerListener(eventsHandling);
		// must be last being added (=first being executed)
		throw new RuntimeException("This doesn't work anymore. Come to MZ, who will gladly help you repair it.");

	}

	/**
	 * Design thoughts:<ul>
	 * <li> At this point, my tendency is to not provide a default version here.  Reason is that this would need to be based
	 * on the config to be consistent with what is done in the mobsim, router, and scoring builders.  This, however, would
	 * obfuscate the strategy more than it would help.  kai, sep'12
	 */
	private StrategyManager createStrategyManager() {
		StrategyManager strategyManager = new StrategyManager() ;

		strategyManager.setPlanSelectorForRemoval( new WorstPlanForRemovalSelector() ) ;

		PlanStrategy strategy1 = new PlanStrategyImpl( new ExpBetaPlanChanger(this.config.planCalcScore().getBrainExpBeta()) ) ;
		strategyManager.addStrategyForDefaultSubpopulation(strategy1, 0.9) ;

		PlanStrategyImpl strategy2 = new PlanStrategyImpl( new ExpBetaPlanSelector(this.config.planCalcScore())) ;
		strategy2.addStrategyModule( new AbstractMultithreadedModule(this.scenario.getConfig().global().getNumberOfThreads()) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return createRoutingAlgorithm();
			}
		}) ;
		strategyManager.addStrategyForDefaultSubpopulation(strategy2, 0.1) ;

		return strategyManager ;
	}

	@Override
	protected void prepareForSim() {
		ControlerUtils.checkConfigConsistencyAndWriteToLog(this.config, "Config dump before doIterations:");
		ParallelPersonAlgorithmUtils.run(this.population, this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmUtils.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(createRoutingAlgorithm(), KnSimplifiedController.this.scenario);
			}
		});
	}
	
	private PlansScoring createPlansScoring() {
		return SimplifiedControlerUtils.createPlansScoringDefault( this.scenario, this.events, this.getControlerIO() );
	}

	private PlanAlgorithm createRoutingAlgorithm() {
		return SimplifiedControlerUtils.createRoutingAlgorithmDefault( this.scenario, this.travelTimeCalculator.getLinkTravelTimes() );
	}

	@Override
	protected void runMobSim() {
		SimplifiedControlerUtils.runMobsimDefault(scenario, events, this.getIterationNumber(), getControlerIO() );
	}
	
	@Override
	protected boolean continueIterations(int iteration) {
		return SimplifiedControlerUtils.continueIterationsDefault(config,iteration) ;
	}


	


}
