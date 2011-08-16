package playground.florian.GTFSConverter;

import org.jdesktop.swingx.mapviewer.wms.WMSService;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.mzilske.osm.JXMapOTFVisClient;

public class GtfsMain {
	
	private static final String CRS = "EPSG:3395";

	public static void main(String[] args) {
		Scenario scenario = readScenario();
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");
		scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		scenario.getConfig().getQSimConfigGroup().setSnapshotPeriod(1);
		scenario.getConfig().otfVis().setDrawTransitFacilities(false);
		runScenario(scenario);
	}

	private static Scenario readScenario() {
		GtfsConverter gtfs = new GtfsConverter("../../matsim/input/sample-feed", new GeotoolsTransformation("WGS84", CRS));
		gtfs.setCreateShapedNetwork(false);
		//		gtfs.setDate(20110711);
		gtfs.convert();
		// gtfs.writeScenario();
		Scenario scenario = gtfs.getScenario();
		scenario.getConfig().global().setCoordinateSystem(CRS);
		return scenario;
	}

	private static void runScenario(Scenario scenario) {
		// runWithClassicOTFVis(scenario);
		runWithOSM(scenario);
	}

	private static void runWithClassicOTFVis(Scenario scenario) {
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		OTFClientLive.run(scenario.getConfig(), server);
		qSim.run();
	}

	private static void runWithOSM(Scenario scenario) {
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		WMSService wms = new WMSService("http://localhost:8080/geoserver/wms?service=WMS&","mz:beatty");
		// JXMapOTFVisClient.run(scenario.getConfig(), server, wms);
		JXMapOTFVisClient.run(scenario.getConfig(), server);
		qSim.run();
	}
	
}
