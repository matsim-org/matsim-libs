package playground.dziemke.potsdam.analysis.disaggregated.revised;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;


public class PotsdamEventsFileReaderBridgeUsers  {

	public static List<Id> EventFileReader(String configFile, String eventsFile) {
				
		Id id1 = new IdImpl(841);
		Id id2 = new IdImpl(843);
		
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		PotsdamEventsHandlerBridgeUsers handler = new PotsdamEventsHandlerBridgeUsers(scenario, id1, id2);
		eventsManager.addHandler(handler);		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFile);
		
		System.out.println("Event file "+eventsFile+" wurde gelesen!");

		
		return handler.getPersonsOnBridge();
	}
}
