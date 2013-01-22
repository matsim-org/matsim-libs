package playground.mzilske.vbb;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class Convert {
	
	public static void main(String[] args) {
		Scenario scenario = readScenario();
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");
		scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		scenario.getConfig().getQSimConfigGroup().setSnapshotPeriod(1);
		scenario.getConfig().otfVis().setDrawTransitFacilities(false);
	//	new NetworkWriter(scenario.getNetwork()).write("/Users/zilske/gtfs-bvg/network.xml");
	//	new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("/Users/zilske/gtfs-bvg/transit-schedule.xml");
	//	new VehicleWriterV1(((ScenarioImpl) scenario).getVehicles()).writeFile("/Users/zilske/gtfs-bvg/transit-vehicles.xml");
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		OTFClientLive.run(scenario.getConfig(), server);
		qSim.run();
	}

	private static Scenario readScenario() {
		// GtfsConverter gtfs = new GtfsConverter("/Users/zilske/Documents/torino", new GeotoolsTransformation("WGS84", CRS));

		final String CRS = "EPSG:3395";
		GtfsConverter gtfs = new GtfsConverter("/Users/zilske/gtfs-bvg", new GeotoolsTransformation("WGS84", CRS));
		gtfs.setCreateShapedNetwork(false); // Shaped network doesn't work yet.
		//		gtfs.setDate(20110711);
		gtfs.convert();
		// gtfs.writeScenario();
		Scenario scenario = gtfs.getScenario();
		scenario.getConfig().global().setCoordinateSystem(CRS);
		return scenario;
	}



}
