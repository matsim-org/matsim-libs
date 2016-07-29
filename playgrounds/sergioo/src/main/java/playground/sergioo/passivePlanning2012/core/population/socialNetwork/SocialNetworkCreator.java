package playground.sergioo.passivePlanning2012.core.population.socialNetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

public class SocialNetworkCreator implements PersonAlgorithm {
	private static Map<Id<ActivityFacility>, Set<Id<Person>>> map = new HashMap<Id<ActivityFacility>, Set<Id<Person>>>();
	
	public static void main(String[] args) {
		SocialNetwork socialNetwork = new SocialNetwork();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		StreamingUtils.setIsStreaming(((Population)scenario.getPopulation()), true);
		StreamingUtils.addAlgorithm(((Population)scenario.getPopulation()), new SocialNetworkCreator());
		new PopulationReader(scenario).readFile(args[0]);
		for(Set<Id<Person>> persons:map.values())
			for(Id<Person> personA:persons)
				for(Id<Person> personB:persons)
					if(!personA.equals(personB))
						socialNetwork.relate(personA, personB, "neighbor");
		new SocialNetworkWriter(socialNetwork).write(args[1]);
	}

	@Override
	public void run(Person person) {
		Id<ActivityFacility> facilityId = ((Activity)person.getSelectedPlan().getPlanElements().get(0)).getFacilityId();
		Set<Id<Person>> persons = map.get(facilityId);
		if(persons == null) {
			persons = new HashSet<Id<Person>>();
			map.put(facilityId, persons);
		}
		persons.add(person.getId());
	}

}
