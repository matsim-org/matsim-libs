package playground.mzilske.osm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.VisGUIMouseHandler;
import org.matsim.vis.otfvis2.OTFVisLiveServer;

public class OSMLiveRun {
	
	public static void main(String[] args) {
		Scenario scenario = readScenario();
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");
		scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		scenario.getConfig().getQSimConfigGroup().setStartTime(60*60*8);
		scenario.getConfig().getQSimConfigGroup().setSnapshotPeriod(10);
		scenario.getConfig().otfVis().setDrawTransitFacilities(false);
		runScenario(scenario);
	}

	private static Scenario readScenario() {
		Config config = ConfigUtils.createConfig();
		QSimConfigGroup qsim = new QSimConfigGroup();
		config.addQSimConfigGroup(qsim);
		config.network().setInputFile("input/network.xml");
		config.plans().setInputFile("input/plans.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private static void runScenario(Scenario scenario) {
		// runWithClassicOTFVis(scenario);
		runWithOSM(scenario);
	}

	private static void runWithClassicOTFVis(Scenario scenario) {
		EventsManager events = EventsUtils.createEventsManager();
		ControlerIO controlerIO = new ControlerIO(scenario.getConfig().controler().getOutputDirectory());
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			SignalEngine engine = new QSimSignalEngine(new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events).createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}
		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(qSim);
		qSim.addFeature(queueSimulationFeature);
		queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis().isShowTeleportedAgents());
		qSim.setControlerIO(controlerIO);
		qSim.setIterationNumber(scenario.getConfig().controler().getLastIteration());
		qSim.run();
	}

	private static void runWithOSM(Scenario scenario) {
		FreespeedTravelTimeCost costCalculator = new FreespeedTravelTimeCost(scenario.getConfig().planCalcScore());
		new PersonPrepareForSim(new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), (NetworkImpl) scenario.getNetwork(), costCalculator, costCalculator, new DijkstraFactory()), (NetworkImpl) scenario.getNetwork()).run(scenario.getPopulation());
		VisGUIMouseHandler.ORTHO = true;
		OTFOGLDrawer.USE_GLJPANEL = true;
		EventsManager events = EventsUtils.createEventsManager();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		qSim.addSnapshotWriter(server.getSnapshotReceiver());
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("wurst", server);
		
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(false);
		client.run();
		qSim.run();
	}
}
