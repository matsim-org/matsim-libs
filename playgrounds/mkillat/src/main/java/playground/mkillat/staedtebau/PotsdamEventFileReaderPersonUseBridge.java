package playground.mkillat.staedtebau;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;



public class PotsdamEventFileReaderPersonUseBridge  {

	public static Map<Id, Double> EventFileReader(String configdatei, String inputFile) {
		
		
		Id id1 = new IdImpl(65);
		Id id2 = new IdImpl(67);
		Id id3 = new IdImpl(69);
		Id id4 = new IdImpl(73);
		Id id5 = new IdImpl(64);
		Id id6 = new IdImpl(68);
		Id id7 = new IdImpl(55);
		Id id8 = new IdImpl(54);
		
		
		Config config = org.matsim.core.config.ConfigUtils.loadConfig(configdatei);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EventsManager events = new EventsUtils().createEventsManager();
		PotsdamEventHandlerPersonUseBridge handler = new PotsdamEventHandlerPersonUseBridge(scenario, id1, id2, id3, id4, id5, id5, id7, id8);
		events.addHandler(handler);		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		System.out.println("Event file "+inputFile+" wurde gelesen!");

		
		return handler.getPersonsOnBridge();
	}
}
