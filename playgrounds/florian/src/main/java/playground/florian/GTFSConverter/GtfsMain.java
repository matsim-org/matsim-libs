package playground.florian.GTFSConverter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
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

import playground.mzilske.osm.OTFVisClient;
import playground.mzilske.osm.WGS84ToOSMMercator;

public class GtfsMain {
	
	public static void main(String[] args) {
		Scenario scenario = readScenario();
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");
		scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		scenario.getConfig().getQSimConfigGroup().setSnapshotPeriod(1);
		scenario.getConfig().otfVis().setDrawTransitFacilities(false);
		runScenario(scenario);
	}

	private static Scenario readScenario() {
		GtfsConverter gtfs = new GtfsConverter("../../matsim/input/urbana-champaign", new WGS84ToOSMMercator.Project());
		gtfs.setCreateShapedNetwork(true);
		//		gtfs.setDate(20110711);
		gtfs.convert();
		// gtfs.writeScenario();
		Scenario scenario = gtfs.getScenario();
		return scenario;
	}

	private static void runScenario(Scenario scenario) {
		runWithClassicOTFVis(scenario);
		// runWithOSM(scenario);
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
