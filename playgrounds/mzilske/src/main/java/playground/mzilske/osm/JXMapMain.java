package playground.mzilske.osm;

import org.jdesktop.swingx.mapviewer.wms.WMSService;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class JXMapMain {


	public static void main(String[] args) {
		String filename = "/Users/michaelzilske/gregor-scenario-11-09-08/network.xml";
		Config config = ConfigUtils.createConfig();
		config.otfVis().setMaximumZoom(17);
		config.global().setCoordinateSystem("EPSG:32633");
		config.addQSimConfigGroup(new QSimConfigGroup());
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(filename);
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		WMSService wms = new WMSService("http://localhost:8080/geoserver/wms?service=WMS&","mz:poly");
		// JXMapOTFVisClient.run(scenario.getConfig(), server, wms);
		JXMapOTFVisClient.run(scenario.getConfig(), server);
		qSim.run();
	}

	
}
