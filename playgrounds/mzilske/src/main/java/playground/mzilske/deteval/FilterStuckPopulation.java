package playground.mzilske.deteval;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class FilterStuckPopulation {
	
	
	public static void main(String[] args) {
		MutableScenario filtersc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final MutableScenario targetsc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		String networkFileName = "../../run951/951.output_network.xml.gz";
		new MatsimNetworkReader(targetsc.getNetwork()).readFile(networkFileName);
		new MatsimNetworkReader(filtersc.getNetwork()).readFile(networkFileName);
		String eventsFileName = "../../run951/it.100/951.100.events.txt.gz";
		final Set<Id> personIds = new HashSet<Id>();
		
		String inputPlansFile = "../../run951/951.output_plans.xml.gz";
		String outputPlansFile = "../../run951/951.filtered_plans.xml.gz";

		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		events.addHandler(new PersonStuckEventHandler() {
			
			@Override
			public void reset(int iteration) {
				
			}
			
			@Override
			public void handleEvent(PersonStuckEvent event) {
				personIds.add(event.getPersonId());
			}
		});
		new MatsimEventsReader(events).readFile(eventsFileName);
		System.out.println(personIds.size());
		
		Population filterpop = (Population) filtersc.getPopulation();
		StreamingDeprecated.setIsStreaming(filterpop, true);
		
		final MatsimReader plansReader = new PopulationReader(filtersc);
		
		StreamingDeprecated.addAlgorithm(filterpop, new PersonAlgorithm() {
		
			@Override
			public void run(Person person) {
				if (personIds.contains(person.getId())) {
					targetsc.getPopulation().addPerson(person);
				}
			}
			
		});
	
		plansReader.readFile(inputPlansFile);
		PopulationWriter populationWriter = new PopulationWriter(targetsc.getPopulation(), targetsc.getNetwork());
		populationWriter.write(outputPlansFile);
		
	}

}
