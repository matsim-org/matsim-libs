package playground.wrashid.msimoni.analyses;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class MainCordonOutflow {
	public static void main(String[] args) {
		String networkFile = "\\\\kosrae.ethz.ch\\ivt-home\\simonimi\\thesis\\output_no_pricing_v3_1pct\\output_network.xml.gz";
		String eventsFile = "\\\\kosrae.ethz.ch\\ivt-home\\simonimi\\thesis\\output_no_pricing_v3_1pct\\ITERS\\it.50\\50.events.xml.gz";
		Coord center = null; // center=null means use all links
		int binSizeInSeconds = 300; // 5 minute bins

		double radiusInMeters = 1500;
		double length = 50.0;

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		center = new Coord(682548.0, 247525.5);

		Map<Id<Link>, Link> links = LinkSelector.selectLinks(scenario.getNetwork(),
				center, radiusInMeters, length);

		CordonOutflowCollector outflowCollector= new CordonOutflowCollector(links.keySet(), binSizeInSeconds);
		
		outflowCollector.reset(0);

		EventsManager events = EventsUtils.createEventsManager();

		events.addHandler(outflowCollector); 

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		
		outflowCollector.printOutflowTimeBins();
	}
}
