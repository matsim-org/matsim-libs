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
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class FilterSample {
	
	static int count = 0;
	
	
	public static void main(String[] args) {
		MutableScenario filtersc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final MutableScenario targetsc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		String networkFileName = "../../run951/951.output_network.xml.gz";
		new MatsimNetworkReader(targetsc.getNetwork()).readFile(networkFileName);
		new MatsimNetworkReader(filtersc.getNetwork()).readFile(networkFileName);
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
		System.out.println(personIds.size());
		
		Population filterpop = (Population) filtersc.getPopulation();
		StreamingUtils.setIsStreaming(filterpop, true);
		
		final MatsimReader plansReader = new PopulationReader(filtersc);
		
		StreamingUtils.addAlgorithm(filterpop, new PersonAlgorithm() {
		
			@Override
			public void run(Person person) {
				if (count % 10 == 0) {
					targetsc.getPopulation().addPerson(person);
				}
				count++;
			}
			
		});
	
		plansReader.readFile(inputPlansFile);
		PopulationWriter populationWriter = new PopulationWriter(targetsc.getPopulation(), targetsc.getNetwork());
		populationWriter.write(outputPlansFile);
		
	}

}
