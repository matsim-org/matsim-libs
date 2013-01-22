package playground.mzilske.vbb;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class Run {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.network().setInputFile("/Users/zilske/gtfs-bvg/network.xml");
		config.transit().setTransitScheduleFile("/Users/zilske/gtfs-bvg/transit-schedule.xml");
		config.transit().setVehiclesFile("/Users/zilske/gtfs-bvg/transit-vehicles.xml");
		config.otfVis().setMapOverlayMode(true);

		final String CRS = "EPSG:3395";
		config.global().setCoordinateSystem(CRS);
		QSimConfigGroup qSimConfigGroup = new QSimConfigGroup();
		config.addQSimConfigGroup(qSimConfigGroup);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);

		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config,scenario, events, qSim);
		OTFClientLive.run(config, server);

		qSim.run();
	
	}

}
