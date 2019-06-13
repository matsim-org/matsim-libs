package vwExamples.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class LinkDelayAnalysis {
	String networkfile;
	String eventfile;
	CustomDelayAnalysisTool eventhandler;

	LinkDelayAnalysis(String networkfile, String eventfile) {
		this.networkfile = networkfile;
		this.eventfile = eventfile;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(this.networkfile);

		//Create an events manager
		EventsManager eventsmanager = EventsUtils.createEventsManager();
		//Add handler to events manager
		this.eventhandler = new CustomDelayAnalysisTool(scenario.getNetwork(), eventsmanager);
		eventsmanager.addHandler(this.eventhandler);
		

		//Read events
		new MatsimEventsReader(eventsmanager).readFile(this.eventfile);

	}

	public static void main(String[] args) {
		
		String eventfile= "D:\\Matsim\\Axer\\Hannover\\ZIM\\output_10pct\\vw236_nocad.0.1\\vw236_nocad.0.1.output_events.xml.gz";
		String networkfile= "D:\\Matsim\\Axer\\Hannover\\ZIM\\output_10pct\\vw236_nocad.0.1\\vw236_nocad.0.1.output_network.xml.gz";
		
		LinkDelayAnalysis linkanalysis = new LinkDelayAnalysis(networkfile,eventfile);
		double totalDelay= linkanalysis.eventhandler.getTotalDelay();
		System.out.println(totalDelay);

	}

}
