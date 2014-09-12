package playground.sergioo.passivePlanning2012.core.population.socialNetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;

public class SocialNetworkCreator implements PersonAlgorithm {
	private static Map<Id, Set<Id>> map = new HashMap<Id, Set<Id>>();
	
	public static void main(String[] args) {
		SocialNetwork socialNetwork = new SocialNetwork();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		((PopulationImpl)scenario.getPopulation()).setIsStreaming(true);
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new SocialNetworkCreator());
		new MatsimPopulationReader(scenario).readFile(args[0]);
		for(Set<Id> persons:map.values())
			for(Id personA:persons)
				for(Id personB:persons)
					if(!personA.equals(personB))
						socialNetwork.relate(personA, personB, "neighbor");
		new SocialNetworkWriter(socialNetwork).write(args[1]);
	}

	@Override
	public void run(Person person) {
		Id facilityId = ((Activity)person.getSelectedPlan().getPlanElements().get(0)).getFacilityId();
		Set<Id> persons = map.get(facilityId);
		if(persons == null) {
			persons = new HashSet<Id>();
			map.put(facilityId, persons);
		}
		persons.add(person.getId());
	}

}
