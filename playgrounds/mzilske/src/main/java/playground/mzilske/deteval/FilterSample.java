package playground.mzilske.deteval;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.population.algorithms.PersonAlgorithm;

public class FilterSample {
	
	static int count = 0;
	
	
	public static void main(String[] args) {
		ScenarioImpl filtersc = new ScenarioImpl();
		final ScenarioImpl targetsc = new ScenarioImpl();
		
		String networkFileName = "../../run951/951.output_network.xml.gz";
		new MatsimNetworkReader(targetsc).readFile(networkFileName);
		new MatsimNetworkReader(filtersc).readFile(networkFileName);
		final Set<Id> personIds = new HashSet<Id>();
		
		String inputPlansFile = "../../run951/951.output_plans.xml.gz";
		String outputPlansFile = "../../run951/951.filtered_plans.xml.gz";

		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(new AgentStuckEventHandler() {
			
			@Override
			public void reset(int iteration) {
				
			}
			
			@Override
			public void handleEvent(AgentStuckEvent event) {
				personIds.add(event.getPersonId());
			}
		});
		System.out.println(personIds.size());
		
		PopulationImpl filterpop = (PopulationImpl) filtersc.getPopulation();
		filterpop.setIsStreaming(true);
		
		final PopulationReader plansReader = new MatsimPopulationReader(filtersc);
		
		filterpop.addAlgorithm(new PersonAlgorithm() {

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
