package playground.mkillat.pt_test;


import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class MyEventFileReaderPt2 {

	public static List <Id> EventFileReader (String configFile, String inputFile, Id line, Id route, Id stopA, Id stopB){
		
		
		
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		EventsManager events = new EventsUtils().createEventsManager();
		MyEventHandlerLinksBetweenStops handler = new MyEventHandlerLinksBetweenStops(stopA, stopB , line, route );
		events.addHandler(handler);

		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		System.out.println("Event file "+inputFile+" wurde gelesen!");

		return handler.getLinks();

	}
	
}
