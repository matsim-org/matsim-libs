package playground.mzilske.d4d;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.ScoreStats;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class MyController extends AbstractController {

	private Config config;
	private Scenario scenario;
	private EventsManager eventsManager;
	private CalcLegTimes legTimes;
	
	
	private TravelTimeCalculator travelTime;
	private Map<Id, List<Sighting>> sightings;
	private CreatePopulation scenarioReader;
	private ScoreStats scoreStats;
	private AgentLocator agentLocator;

	@Override
	protected void loadCoreListeners() {
		
		// optional: score stats
				this.scoreStats = new ScoreStats(this.scenario.getPopulation(),
						this.getControlerIO().getOutputFilename("scorestats"), true);
				this.addControlerListener(this.scoreStats);
		
		final PlansDumping plansDumping = new PlansDumping( this.scenario, this.config.controler().getFirstIteration(), 
				1, stopwatch, getControlerIO() );
		this.addCoreControlerListener(plansDumping);
		
		this.addCoreControlerListener(new LegTimesListener(legTimes, getControlerIO()));

		
		final StrategyManager strategyManager = createStrategyManager() ;
		this.addCoreControlerListener(new PlansReplanning( strategyManager, this.scenario.getPopulation() ));

		this.addCoreControlerListener(createPlansScoring());

		
		
		final EventsHandling eventsHandling = new EventsHandling((EventsManagerImpl) eventsManager,
				5, this.config.controler().getEventsFileFormats(),
				getControlerIO() );

		this.addCoreControlerListener(eventsHandling);
		
	}

	private ControlerListener createPlansScoring() {
		Config config = scenario.getConfig();
		Network network = scenario.getNetwork() ;
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory( config.planCalcScore(), network );
//		ScoringFunctionFactory scoringFunctionFactory = new ScoringFunctionFactory() {
//
//			@Override
//			public ScoringFunction createNewScoringFunction(final Plan plan) {
//				
//				ScoringFunction scoringFunction = new ScoringFunction() {
//					Sightings sightingsForThisAgent = new Sightings( sightings.get(plan.getPerson().getId()));
//
//					double score = 0.0;
//					@Override
//					public void handleActivity(Activity activity) {
//						Sighting sighting = sightingsForThisAgent.sightings.next();
//						long sightingTime = sighting.getDateTime();
//						if (  (sightingTime >= activity.getStartTime() || activity.getStartTime() == Time.UNDEFINED_TIME)
//								&& (sightingTime <= activity.getEndTime() || activity.getEndTime() == Time.UNDEFINED_TIME)) {
//						} else {
//							score -= 100;
//						}
//					}
//
//					@Override
//					public void handleLeg(Leg leg) {
//						// TODO Auto-generated method stub
//						
//					}
//
//					@Override
//					public void agentStuck(double time) {
//						// TODO Auto-generated method stub
//						
//					}
//
//					@Override
//					public void addMoney(double amount) {
//						// TODO Auto-generated method stub
//						
//					}
//
//					@Override
//					public void finish() {
//						// TODO Auto-generated method stub
//						
//					}
//
//					@Override
//					public double getScore() {
//						return score;
//					}
//
//					@Override
//					public void reset() {
//						sightingsForThisAgent = new Sightings( sightings.get(plan.getPerson().getId()));
//						score = 0.0;
//					}
//					
//				};
//				return scoringFunction;
//			}
//			
//		};
		final PlansScoring plansScoring = new PlansScoring( scenario, eventsManager, getControlerIO(), scoringFunctionFactory );
		return plansScoring;
	}

	private StrategyManager createStrategyManager() {
		StrategyManager strategyManager = new StrategyManager() ;

		strategyManager.setPlanSelectorForRemoval( new WorstPlanForRemovalSelector() ) ;

		PlanStrategy strategy1 = new PlanStrategyImpl( new ExpBetaPlanChanger(this.config.planCalcScore().getBrainExpBeta()) ) ;
	
		PlanStrategyImpl strategy2 = new PlanStrategyImpl( new ExpBetaPlanSelector(this.config.planCalcScore())) ;
		strategy2.addStrategyModule( new AbstractMultithreadedModule(this.scenario.getConfig().global().getNumberOfThreads()) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return createRoutingAlgorithm();
			}

			private PlanAlgorithm createRoutingAlgorithm() {
				TripRouter tripRouter = new TripRouter();
				tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork(), travelTime.getLinkTravelTimes()));
				tripRouter.setRoutingModule("other", new LegRouterWrapper("other", scenario.getPopulation().getFactory(), new TeleportationLegRouter(new ModeRouteFactory(), 1.38889, 1.0)));
				return new PlanRouter(tripRouter);
			}
		});
		
		PlanStrategyImpl strategy3 = new PlanStrategyImpl( new ExpBetaPlanSelector(this.config.planCalcScore())) ;
		strategy3.addStrategyModule( new ChangeLegMode(8, new String[]{"car", "other"}, true));
		strategy3.addStrategyModule( new AbstractMultithreadedModule(this.scenario.getConfig().global().getNumberOfThreads()) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return createRoutingAlgorithm();
			}

			private PlanAlgorithm createRoutingAlgorithm() {
				TripRouter tripRouter = new TripRouter();
				tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork(), travelTime.getLinkTravelTimes()));
				tripRouter.setRoutingModule("other", new LegRouterWrapper("other", scenario.getPopulation().getFactory(), new TeleportationLegRouter(new ModeRouteFactory(), 1.38889, 1.0)));
				return new PlanRouter(tripRouter);
			}
		});
		
		
		
		PlanStrategyImpl strategy4 = new PlanStrategyImpl( new ExpBetaPlanSelector(this.config.planCalcScore())) ;
		strategy4.addStrategyModule( new AbstractMultithreadedModule(this.scenario.getConfig().global().getNumberOfThreads()) {

			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return new PlanAlgorithm() {

					@Override
					public void run(Plan plan) {

						Sightings sightingsForThisAgent = new Sightings( sightings.get(plan.getPerson().getId()));
						for (PlanElement planElement : plan.getPlanElements()) {
							if (planElement instanceof Activity) {
								Sighting sighting = sightingsForThisAgent.sightings.next();
								ActivityImpl activity = (ActivityImpl) planElement;
								activity.setLinkId(null);
								Geometry cell = scenarioReader.getCellTowers().getCell(sighting.getCellTowerId());
								Point p = getRandomPointInFeature(cell);
								Coord newCoord = new CoordImpl(p.getX(), p.getY());
								activity.setCoord(newCoord);
							}
						}
					}
					
				};
			}
			
		});
		strategy4.addStrategyModule( new AbstractMultithreadedModule(this.scenario.getConfig().global().getNumberOfThreads()) {

			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return new org.matsim.population.algorithms.XY2Links((ScenarioImpl) scenario);
			}
			
		});
		strategy4.addStrategyModule( new AbstractMultithreadedModule(this.scenario.getConfig().global().getNumberOfThreads()) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return createRoutingAlgorithm();
			}

			private PlanAlgorithm createRoutingAlgorithm() {
				TripRouter tripRouter = new TripRouter();
				tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork(), travelTime.getLinkTravelTimes()));
				tripRouter.setRoutingModule("other", new LegRouterWrapper("unknown", scenario.getPopulation().getFactory(), new TeleportationLegRouter(new ModeRouteFactory(), 1.38889, 1.0)));
				return new PlanRouter(tripRouter);
			}
		});
		
		
		strategyManager.addStrategyForDefaultSubpopulation(strategy1, 0.6) ;
		strategyManager.addStrategyForDefaultSubpopulation(strategy2, 0.2) ;		
		strategyManager.addStrategyForDefaultSubpopulation(strategy3, 0.2) ;
		

		return strategyManager ;
	}

	@Override
	protected void runMobSim(int iteration) {
		
		//runQSim();
		runJDEQSim();
		
		
	}

	private void runJDEQSim() {
		new JDEQSimulation(scenario, eventsManager).run();
	}

	private void runQSim() {
		QSimConfigGroup conf = scenario.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		QNetsimEngineFactory netsimEngFactory = new ParallelQNetsimEngineFactory();

		QSim qSim = new QSim(scenario, new SynchronizedEventsManagerImpl(eventsManager));
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());


		BushwhackingEngine bushwhackingEngine = new BushwhackingEngine();
		qSim.addMobsimEngine(bushwhackingEngine);
		qSim.addDepartureHandler(bushwhackingEngine);

		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		//	OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config,scenario, eventsManager, qSim);

		
		qSim.addMobsimEngine(new SightingsEngine(sightings, agentLocator));

		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setMapOverlayMode(true);
		// config.otfVis().setMapBaseUrl("http://localhost:8080/geoserver/wms?service=WMS&");
		// config.otfVis().setMapBaseUrl("http://localhost:8080/geoserver/wms?service=WMS&");
		// config.otfVis().setMapLayer("mz:clipped");
		//	OTFClientLive.run(config, server);


		qSim.run();
	}

	@Override
	protected void prepareForSim() {





	}

	public void run() throws FileNotFoundException {
		config = ConfigUtils.createConfig();

		config.controler().setLastIteration(500);
		config.global().setCoordinateSystem("EPSG:3395");
		config.global().setNumberOfThreads(8);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(false);
		config.controler().setWriteSnapshotsInterval(5);
		config.qsim().setStorageCapFactor(0.01);
		config.qsim().setFlowCapFactor(0.01);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setNumberOfThreads(8);
		config.qsim().setEndTime(27*60*60);
		config.plansCalcRoute().setTeleportedModeSpeed("other", 1.38889); // 5 km/h beeline
		config.controler().setWriteEventsInterval(0);
		ActivityParams sighting = new ActivityParams("sighting");
		// sighting.setOpeningTime(0.0);
		// sighting.setClosingTime(0.0);
		sighting.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sighting);
		config.planCalcScore().setTraveling_utils_hr(0);
		config.planCalcScore().setConstantCar(0);
		config.planCalcScore().setMonetaryDistanceCostRateCar(0);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.setParam("JDEQSim", "flowCapacityFactor", "0.01");
		config.setParam("JDEQSim", "storageCapacityFactor", "0.01");
		double endTime= 60*60*32;
		config.setParam("JDEQSim", "endTime", Double.toString(endTime));
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("/Users/zilske/d4d/output/network.xml");
		AltPopulationReaderMatsimV5 altPopulationReaderMatsimV5 = new AltPopulationReaderMatsimV5(scenario);
		//	altPopulationReaderMatsimV5.readFile("/Users/zilske/d4d/output/population.xml");
		altPopulationReaderMatsimV5.readFile("/Users/zilske/d4d/output/population.xml");


		scenarioReader = new CreatePopulation();
		sightings = scenarioReader.readNetworkAndSightings(config);

		
		
		
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new PersonAlgorithm() {

			@Override
			public void run(Person person) {
				PlanUtils.insertLinkIdsIntoGenericRoutes(person.getSelectedPlan());
			}

		});



		setupOutputDirectory(config.controler().getOutputDirectory(), config.controler().getRunId(), true);
		
		
		this.eventsManager = EventsUtils.createEventsManager(config); 
		// add a couple of useful event handlers:
		//this.eventsManager.addHandler(new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network));
		this.legTimes = new CalcLegTimes();
		this.eventsManager.addHandler(legTimes);
		this.travelTime = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(this.scenario.getNetwork(), this.config.travelTimeCalculator());
		this.eventsManager.addHandler(travelTime);	
		agentLocator = new AgentLocator();
		this.eventsManager.addHandler(agentLocator);	
		
		
		
		
		
		run(config);
	}

	@Override
	protected boolean continueIterations(int iteration) {
		return ( iteration <= config.controler().getLastIteration() ) ;
	}

	private Point getRandomPointInFeature(Geometry ft) {
		Random rnd = new Random();
		Point p = null;
		double x, y;
		do {
			x = ft.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (ft.getEnvelopeInternal().getMaxX() - ft.getEnvelopeInternal().getMinX());
			y = ft.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (ft.getEnvelopeInternal().getMaxY() - ft.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!ft.contains(p));
		return p;
	}
	

	public static void main(String[] args) throws FileNotFoundException {
		MyController myController = new MyController();
		myController.run();
	}

}
