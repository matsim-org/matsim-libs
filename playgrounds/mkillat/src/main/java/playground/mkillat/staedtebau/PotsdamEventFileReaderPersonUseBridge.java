package playground.mkillat.staedtebau;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;



public class PotsdamEventFileReaderPersonUseBridge  {

	public static Map<Id<Person>, Double> EventFileReader(String configdatei, String inputFile) {
		
		
		Id<Link> id1 = Id.create(65, Link.class);
		Id<Link> id2 = Id.create(67, Link.class);
		Id<Link> id3 = Id.create(69, Link.class);
		Id<Link> id4 = Id.create(73, Link.class);
		Id<Link> id5 = Id.create(64, Link.class);
		Id<Link> id6 = Id.create(68, Link.class);
		Id<Link> id7 = Id.create(55, Link.class);
		Id<Link> id8 = Id.create(54, Link.class);
		
		
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
