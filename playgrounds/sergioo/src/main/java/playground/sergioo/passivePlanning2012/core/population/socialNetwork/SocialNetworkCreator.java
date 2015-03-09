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
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.PersonAlgorithm;

public class SocialNetworkCreator implements PersonAlgorithm {
	private static Map<Id<ActivityFacility>, Set<Id<Person>>> map = new HashMap<Id<ActivityFacility>, Set<Id<Person>>>();
	
	public static void main(String[] args) {
		SocialNetwork socialNetwork = new SocialNetwork();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		((PopulationImpl)scenario.getPopulation()).setIsStreaming(true);
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new SocialNetworkCreator());
		new MatsimPopulationReader(scenario).readFile(args[0]);
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
