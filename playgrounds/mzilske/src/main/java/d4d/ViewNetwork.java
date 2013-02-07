package d4d;

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class ViewNetwork {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("/Users/zilske/d4d/output/network.xml");
		System.out.println("Nodes: " + scenario.getNetwork().getNodes().size());
		System.out.println("Links: " + scenario.getNetwork().getLinks().size());
		
		EventsManager events = EventsUtils.createEventsManager();
		OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events);
		OTFClientLive.run(config, server);
	}
	
}
