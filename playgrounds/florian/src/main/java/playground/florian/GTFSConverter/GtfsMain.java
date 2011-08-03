package playground.florian.GTFSConverter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.run.OTFVis;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.mzilske.osm.JXMapOTFVisClient;
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
		GtfsConverter gtfs = new GtfsConverter("../../matsim/input/sample-feed", new WGS84ToOSMMercator.Project());
		gtfs.setCreateShapedNetwork(false);
		//		gtfs.setDate(20110711);
		gtfs.convert();
		// gtfs.writeScenario();
		Scenario scenario = gtfs.getScenario();
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
		qSim.setControlerIO(controlerIO);
		qSim.setIterationNumber(scenario.getConfig().controler().getLastIteration());
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		OTFClientLive.run(scenario.getConfig(), server);

		qSim.run();
	}

	private static void runWithOSM(Scenario scenario) {
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		JXMapOTFVisClient.run(scenario.getConfig(), server);
		qSim.run();
	}
	
}
