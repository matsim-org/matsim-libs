package d4d;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class MyController extends AbstractController {

	private Config config;
	private Scenario scenario;
	private EventsManager eventsManager;
	private CalcLegTimes legTimes;

	@Override
	protected void loadCoreListeners() {
		this.addCoreControlerListener(new LegTimesListener(legTimes, controlerIO));
		final EventsHandling eventsHandling = new EventsHandling((EventsManagerImpl) eventsManager,
				this.config.controler().getWriteEventsInterval(), this.config.controler().getEventsFileFormats(),
				controlerIO );
		this.addCoreControlerListener(eventsHandling);
	}

	@Override
	protected void runMobSim(int iteration) {
		QSimConfigGroup conf = scenario.getConfig().getQSimConfigGroup();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();
		QNetsimEngineFactory netsimEngFactory;
		if (numOfThreads > 1) {
			eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
			netsimEngFactory = new ParallelQNetsimEngineFactory();
		} else {
			netsimEngFactory = new DefaultQSimEngineFactory();
		}
		QSim qSim = new QSim(scenario, eventsManager);
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
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config,scenario, eventsManager, qSim);
		OTFClientLive.run(config, server);
		
		qSim.run();
	}

	@Override
	protected void prepareForSim() {
		RunScenario scenarioReader = new RunScenario();
		scenario = scenarioReader.readScenario(config);
		TripRouter tripRouter = new TripRouter();
		tripRouter.setRoutingModule("unknown", new LegRouterWrapper(
				"unknown",
				scenario.getPopulation().getFactory(),
				new TeleportationLegRouter(
						((PopulationFactoryImpl) (scenario.getPopulation().getFactory())).getModeRouteFactory(),
					2,
					1.4)));
		PlanRouter planRouter = new PlanRouter(tripRouter, ((ScenarioImpl) scenario).getActivityFacilities());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			planRouter.run(person);
		}
	}

	public void run() {
		config = ConfigUtils.createConfig();
		config.addQSimConfigGroup(new QSimConfigGroup());
		config.controler().setLastIteration(0);
		config.global().setCoordinateSystem("EPSG:3395");
		config.otfVis().setShowTeleportedAgents(true);
		config.controler().setWriteEventsInterval(0);
		setupOutputDirectory(config.controler().getOutputDirectory(), config
				.controler().getRunId(), true);
		this.eventsManager = EventsUtils.createEventsManager(config); 
		// add a couple of useful event handlers:
		//this.eventsManager.addHandler(new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network));
		this.legTimes = new CalcLegTimes();
		this.eventsManager.addHandler(legTimes);
		run(config);
	}
	
	@Override
	protected boolean continueIterations(int iteration) {
		return ( iteration <= config.controler().getLastIteration() ) ;
	}


	public static void main(String[] args) {
		MyController myController = new MyController();
		myController.run();
	}

}
