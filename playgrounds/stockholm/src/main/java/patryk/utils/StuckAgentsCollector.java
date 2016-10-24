package patryk.utils;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;


public class StuckAgentsCollector {
	
	private final static String EVENTSFILE = "600.events.xml.gz";

	
	
	public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("prep_extern/input/network_v09.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile("600.plans.xml.gz");
        Population population = scenario.getPopulation();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		StuckAgents handler = new StuckAgents();
		eventsManager.addHandler(handler);

		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(EVENTSFILE);
			
		ArrayList<Id<Link>> stuckLinkIDs = handler.getPersonStuck();
		ArrayList<Id<Person>> stuckPersonIDs = handler.getPersonStuckIDs();
		
		Double[][] linkInfo = new Double[stuckLinkIDs.size()][2];
		
		for (int i = 0; i < stuckLinkIDs.size(); i++) {
			Link l = network.getLinks().get(stuckLinkIDs.get(i));
			linkInfo[i][0] = l.getCapacity();
			linkInfo[i][1] = l.getFreespeed();
		}
		
		for(int row = 0; row < stuckLinkIDs.size(); row++) {
			System.out.println(stuckLinkIDs.get(row).toString()+" "+linkInfo[row][0]+" "+linkInfo[row][1]);
		}
		
		System.out.println(stuckLinkIDs);
		
	}

}
