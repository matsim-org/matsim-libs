package playground.dziemke.potsdam.analysis.aggregated;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;


public class PotsdamEventFileReaderLoad {

	public static Map<Id, Integer> EventFileReader(String configFile, String eventsFile) {
		
		Map<Id,Integer> IdLoadMap = new HashMap<Id, Integer>();
		
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EventsManager events = EventsUtils.createEventsManager();
		PotsdamEventHandlerLoad handler = new PotsdamEventHandlerLoad(scenario);
		events.addHandler(handler);		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		System.out.println("Event file "+eventsFile+" wurde gelesen!");
		
		for (Id id: scenario.getNetwork().getLinks().keySet()){
			IdLoadMap.put(id, handler.getLoad(id));
		}
		
		return IdLoadMap;
	}
}
